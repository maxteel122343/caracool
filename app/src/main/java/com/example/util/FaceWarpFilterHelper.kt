package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object FaceWarpFilterHelper {

    private const val MESH_WIDTH = 48
    private const val MESH_HEIGHT = 48

    data class FaceCenterResult(
        val centerX: Float,
        val centerY: Float,
        val radius: Float,
        val faceDetected: Boolean
    )

    /**
     * Detects face and returns the optimal focal center point for the "Cara de Cu" pucker distortion.
     */
    suspend fun detectFaceFocalPoint(bitmap: Bitmap): FaceCenterResult = withContext(Dispatchers.Default) {
        try {
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .build()

            val detector = FaceDetection.getClient(options)
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val faces = detector.process(inputImage).await()

            if (faces.isNotEmpty()) {
                val primaryFace: Face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() } ?: faces[0]
                val bbox = primaryFace.boundingBox

                val noseBase = primaryFace.getLandmark(FaceLandmark.NOSE_BASE)?.position
                val mouthBottom = primaryFace.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position
                val mouthLeft = primaryFace.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
                val mouthRight = primaryFace.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position

                val centerX: Float
                val centerY: Float

                if (noseBase != null && mouthBottom != null) {
                    centerX = (noseBase.x + mouthBottom.x) / 2f
                    centerY = (noseBase.y + mouthBottom.y) / 2f
                } else if (noseBase != null) {
                    centerX = noseBase.x
                    centerY = noseBase.y + bbox.height() * 0.08f
                } else {
                    centerX = bbox.centerX().toFloat()
                    centerY = bbox.centerY().toFloat() + bbox.height() * 0.10f
                }

                val radius = max(bbox.width(), bbox.height()) * 0.68f

                return@withContext FaceCenterResult(
                    centerX = centerX.coerceIn(0f, bitmap.width.toFloat()),
                    centerY = centerY.coerceIn(0f, bitmap.height.toFloat()),
                    radius = radius.coerceIn(50f, max(bitmap.width, bitmap.height).toFloat()),
                    faceDetected = true
                )
            }
        } catch (_: Exception) {
            // Fallback gracefully to bitmap center
        }

        FaceCenterResult(
            centerX = bitmap.width / 2f,
            centerY = bitmap.height * 0.55f,
            radius = min(bitmap.width, bitmap.height) * 0.45f,
            faceDetected = false
        )
    }

    /**
     * Applies the "Cara de Cu" pucker, wrinkle, and radial inward warping on the source bitmap.
     * @param src Original user bitmap
     * @param intensity Distortion level from 0.0 (no distortion) to 1.0 (extreme pucker/wrinkle)
     * @param focalPoint User-specified or auto-detected focal point (relative to bitmap coordinates)
     * @param customRadius Custom radius of effect
     */
    suspend fun applyCaraDeCuWarp(
        src: Bitmap,
        intensity: Float,
        focalPoint: PointF? = null,
        customRadius: Float? = null
    ): Bitmap = withContext(Dispatchers.Default) {
        if (intensity <= 0.01f) {
            return@withContext src.copy(src.config ?: Bitmap.Config.ARGB_8888, true)
        }

        val width = src.width.toFloat()
        val height = src.height.toFloat()

        val centerX = focalPoint?.x ?: (width / 2f)
        val centerY = focalPoint?.y ?: (height * 0.55f)
        val radius = customRadius ?: (min(width, height) * 0.48f)

        val output = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val numCols = MESH_WIDTH
        val numRows = MESH_HEIGHT
        val numVerts = (numCols + 1) * (numRows + 1)
        val warpVerts = FloatArray(numVerts * 2)

        var index = 0
        for (row in 0..numRows) {
            val fy = height * row / numRows
            for (col in 0..numCols) {
                val fx = width * col / numCols

                val dx = fx - centerX
                val dy = fy - centerY
                val dist = hypot(dx, dy)

                if (dist < radius && dist > 0.001f) {
                    val normDist = dist / radius // 0.0 at center, 1.0 at outer edge

                    // 1. Concentric Pucker & Pinch (draws skin, nose, mouth inward into a tight knot)
                    val puckerStrength = (1.0f - normDist) * (1.0f - normDist) * (0.80f * intensity)
                    val pullDisplacement = dist * puckerStrength

                    // 2. Concentric Radial Skin Wrinkles (creates the circular creases around the center)
                    val wrinkleFreq = 16.0f
                    val wrinkleAmp = (1.0f - normDist) * (8.5f * intensity)
                    val wrinkleOffset = sin(normDist * wrinkleFreq) * wrinkleAmp

                    // 3. Radial Pinch Creases (spokes / folds radiating outward from the knot)
                    val angle = atan2(dy, dx)
                    val radialFolds = cos(angle * 8.0f) * (1.0f - normDist) * (4.5f * intensity)

                    val newDist = max(1.0f, dist - pullDisplacement + wrinkleOffset + radialFolds)
                    val scale = newDist / dist

                    warpVerts[index * 2] = (centerX + dx * scale).coerceIn(0f, width)
                    warpVerts[index * 2 + 1] = (centerY + dy * scale).coerceIn(0f, height)
                } else {
                    warpVerts[index * 2] = fx
                    warpVerts[index * 2 + 1] = fy
                }
                index++
            }
        }

        canvas.drawBitmapMesh(src, numCols, numRows, warpVerts, 0, null, 0, paint)
        output
    }

    /**
     * Creates an illustrative sample portrait bitmap for live testing before user picks a photo.
     */
    fun createSampleFaceBitmap(isKoolMode: Boolean = false): Bitmap {
        val width = 720
        val height = 720
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Clean Background
        canvas.drawColor(if (isKoolMode) Color.parseColor("#FFF0F5") else Color.parseColor("#FDF6F0"))

        val cx = width / 2f
        val cy = height / 2f

        // Shoulders / Clothes
        paint.color = if (isKoolMode) Color.parseColor("#D46A7A") else Color.parseColor("#5C4A4A")
        canvas.drawOval(cx - 280f, height - 200f, cx + 280f, height + 180f, paint)

        // Neck
        paint.color = Color.parseColor("#E0A88A")
        canvas.drawRect(cx - 70f, cy + 90f, cx + 70f, cy + 240f, paint)

        // Head / Face
        paint.color = Color.parseColor("#F2C9B0")
        canvas.drawOval(cx - 190f, cy - 240f, cx + 190f, cy + 180f, paint)

        // Hair
        paint.color = Color.parseColor("#382828")
        canvas.drawOval(cx - 200f, cy - 260f, cx + 200f, cy - 80f, paint)

        // Ears
        paint.color = Color.parseColor("#E8B89A")
        canvas.drawOval(cx - 215f, cy - 40f, cx - 180f, cy + 60f, paint)
        canvas.drawOval(cx + 180f, cy - 40f, cx + 215f, cy + 60f, paint)

        // Eyebrows
        paint.color = Color.parseColor("#382828")
        paint.strokeWidth = 14f
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(cx - 130f, cy - 80f, cx - 50f, cy - 90f, paint)
        canvas.drawLine(cx + 50f, cy - 90f, cx + 130f, cy - 80f, paint)

        // Eyes
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawOval(cx - 120f, cy - 65f, cx - 60f, cy - 35f, paint)
        canvas.drawOval(cx + 60f, cy - 65f, cx + 120f, cy - 35f, paint)

        paint.color = Color.parseColor("#4A3030")
        canvas.drawCircle(cx - 90f, cy - 50f, 16f, paint)
        canvas.drawCircle(cx + 90f, cy - 50f, 16f, paint)

        // Nose
        paint.color = Color.parseColor("#D4A07A")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 7f
        canvas.drawLine(cx, cy - 30f, cx - 15f, cy + 35f, paint)
        canvas.drawLine(cx - 15f, cy + 35f, cx + 15f, cy + 35f, paint)

        // Cheeks
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#FFAAA6")
        paint.alpha = 90
        canvas.drawCircle(cx - 110f, cy + 30f, 32f, paint)
        canvas.drawCircle(cx + 110f, cy + 30f, 32f, paint)
        paint.alpha = 255

        // Mouth / Lips
        paint.color = Color.parseColor("#D46A7A")
        canvas.drawOval(cx - 45f, cy + 85f, cx + 45f, cy + 115f, paint)

        // Beard stubble
        if (!isKoolMode) {
            paint.color = Color.parseColor("#382828")
            paint.alpha = 70
            paint.strokeWidth = 3f
            for (i in -10..10) {
                canvas.drawPoint(cx + i * 14f, cy + 135f + kotlin.math.abs(i) * 2f, paint)
                canvas.drawPoint(cx + i * 12f, cy + 155f + kotlin.math.abs(i) * 2f, paint)
            }
        }

        return bitmap
    }
}
