package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object PacocaFrameHelper {
    private const val TAG = "PacocaFrameHelper"

    private val faceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setMinFaceSize(0.15f)
            .build()
        FaceDetection.getClient(options)
    }

    /**
     * Takes an input Bitmap, runs ML Kit face detection, crops the face,
     * places it inside the kawaii Paçoca or Kool character frame, and saves the result to internal storage.
     * Returns the absolute file path of the resulting PNG.
     */
    suspend fun processAndApplyPacocaFrame(
        context: Context,
        sourceBitmap: Bitmap,
        isKoolMode: Boolean = false,
        isFrameEnabled: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        try {
            // 1. Detect face and crop (or clean crop)
            val croppedFace = detectAndCropFace(sourceBitmap)

            // 2. Render Paçoca character frame if enabled, otherwise use clean cropped photo
            val finalBitmap = if (isFrameEnabled) {
                // Unified standard Paçoca frame as requested for both modes
                renderPacocaCharacterFrame(
                    faceBitmap = croppedFace,
                    targetWidth = 960,
                    targetHeight = 1280
                )
            } else {
                // Save clean photo scaled to standard size with soft rounded corners or full fill
                renderCleanPhoto(
                    faceBitmap = croppedFace,
                    targetWidth = 960,
                    targetHeight = 1280
                )
            }

            // 3. Save to internal storage
            val outputDir = File(context.filesDir, "pacoca_faces").apply { if (!exists()) mkdirs() }
            val prefix = if (isFrameEnabled) "framed" else "noframing"
            val outputFile = File(outputDir, "${prefix}_${System.currentTimeMillis()}.png")
            FileOutputStream(outputFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
            }

            Log.d(TAG, "Successfully processed photo (frame=$isFrameEnabled): ${outputFile.absolutePath}")
            outputFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error in processAndApplyPacocaFrame: ${e.message}", e)
            // Fallback: save original bitmap
            val fallbackFile = File(context.filesDir, "pacoca_fallback_${System.currentTimeMillis()}.png")
            FileOutputStream(fallbackFile).use { out ->
                sourceBitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            fallbackFile.absolutePath
        }
    }

    private fun renderCleanPhoto(
        faceBitmap: Bitmap,
        targetWidth: Int = 960,
        targetHeight: Int = 1280
    ): Bitmap {
        val result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Draw clean dark/warm backdrop
        val bgPaint = Paint().apply {
            color = Color.parseColor("#1C1917")
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(), bgPaint)

        // Scale face to fill nicely
        val scale = max(
            targetWidth.toFloat() / faceBitmap.width.toFloat(),
            targetHeight.toFloat() / faceBitmap.height.toFloat()
        )
        val scaledWidth = faceBitmap.width * scale
        val scaledHeight = faceBitmap.height * scale
        val left = (targetWidth - scaledWidth) / 2f
        val top = (targetHeight - scaledHeight) / 2f

        val destRect = RectF(left, top, left + scaledWidth, top + scaledHeight)
        canvas.drawBitmap(faceBitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))

        return result
    }

    /**
     * Processes an image Uri from Gallery or Camera
     */
    suspend fun processAndApplyPacocaFrame(
        context: Context,
        sourceUri: Uri,
        isKoolMode: Boolean = false,
        isFrameEnabled: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        val bitmap = loadBitmapFromUri(context, sourceUri)
            ?: throw IllegalArgumentException("Could not load bitmap from Uri: $sourceUri")
        processAndApplyPacocaFrame(context, bitmap, isKoolMode, isFrameEnabled)
    }

    /**
     * Processes an image from an existing file path
     */
    suspend fun processAndApplyPacocaFrame(
        context: Context,
        filePath: String,
        isKoolMode: Boolean = false,
        isFrameEnabled: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("File does not exist: $filePath")
        }
        val bitmap = BitmapFactory.decodeFile(filePath)
            ?: throw IllegalArgumentException("Could not decode bitmap from file: $filePath")
        processAndApplyPacocaFrame(context, bitmap, isKoolMode, isFrameEnabled)
    }

    /**
     * Uses Google ML Kit to detect the primary face in the bitmap and crops it with balanced margins.
     * If no face is detected, it crops the center of the image.
     */
    suspend fun detectAndCropFace(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        suspendCancellableCoroutine { continuation ->
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            faceDetector.process(inputImage)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        // Pick the largest face detected
                        val largestFace = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }!!
                        val bounds = largestFace.boundingBox

                        // Calculate expanded bounds to capture chin, cheeks and forehead nicely
                        val marginX = (bounds.width() * 0.25f).toInt()
                        val marginY = (bounds.height() * 0.30f).toInt()

                        val left = max(0, bounds.left - marginX)
                        val top = max(0, bounds.top - marginY)
                        val right = min(bitmap.width, bounds.right + marginX)
                        val bottom = min(bitmap.height, bounds.bottom + marginY)

                        val cropWidth = right - left
                        val cropHeight = bottom - top

                        if (cropWidth > 20 && cropHeight > 20) {
                            try {
                                val cropped = Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
                                continuation.resume(cropped)
                                return@addOnSuccessListener
                            } catch (e: Exception) {
                                Log.w(TAG, "Crop failed, falling back: ${e.message}")
                            }
                        }
                    }

                    // Fallback: Center crop
                    val minDim = min(bitmap.width, bitmap.height)
                    val startX = (bitmap.width - minDim) / 2
                    val startY = (bitmap.height - minDim) / 2
                    val fallback = try {
                        Bitmap.createBitmap(bitmap, startX, startY, minDim, minDim)
                    } catch (_: Exception) {
                        bitmap
                    }
                    continuation.resume(fallback)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "ML Kit Face detection failed: ${e.message}, using center crop")
                    val minDim = min(bitmap.width, bitmap.height)
                    val startX = (bitmap.width - minDim) / 2
                    val startY = (bitmap.height - minDim) / 2
                    val fallback = try {
                        Bitmap.createBitmap(bitmap, startX, startY, minDim, minDim)
                    } catch (_: Exception) {
                        bitmap
                    }
                    continuation.resume(fallback)
                }
        }
    }

    /**
     * Renders the complete Kawaii Paçoca Character Frame:
     * - Warm roasted peanut golden body with realistic peanut texture & flecks
     * - Oval transparent window containing the user's cropped face
     * - Big sparkling anime eyes with highlight reflections
     * - Pink glowing blush cheeks
     * - Happy open mouth with pink tongue
     * - Orange padlock accessory on top-right with keyhole
     */
    fun renderPacocaCharacterFrame(
        faceBitmap: Bitmap?,
        targetWidth: Int = 960,
        targetHeight: Int = 1280
    ): Bitmap {
        val result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val w = targetWidth.toFloat()
        val h = targetHeight.toFloat()

        // 1. Define Outer Paçoca Character Body geometry
        val bodyMarginX = w * 0.08f
        val bodyMarginY = h * 0.07f
        val bodyRect = RectF(bodyMarginX, bodyMarginY, w - bodyMarginX, h - bodyMarginY)
        val cornerRadius = w * 0.32f

        // 2. Define Center Oval Hole for the user's face
        val ovalCenterX = w * 0.50f
        val ovalCenterY = h * 0.40f
        val ovalRadiusX = w * 0.25f
        val ovalRadiusY = h * 0.22f
        val faceOvalRect = RectF(
            ovalCenterX - ovalRadiusX,
            ovalCenterY - ovalRadiusY,
            ovalCenterX + ovalRadiusX,
            ovalCenterY + ovalRadiusY
        )

        // Draw Background if desired or transparent
        // Canvas is transparent ARGB_8888 by default.

        // 3. Draw the User's Cropped Face in the Oval Window first
        if (faceBitmap != null && !faceBitmap.isRecycled) {
            val facePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val facePath = Path().apply {
                addOval(faceOvalRect, Path.Direction.CW)
            }

            canvas.save()
            canvas.clipPath(facePath)

            // Scale & Center faceBitmap to cover faceOvalRect
            val scale = max(
                faceOvalRect.width() / faceBitmap.width.toFloat(),
                faceOvalRect.height() / faceBitmap.height.toFloat()
            )
            val scaledW = faceBitmap.width * scale
            val scaledH = faceBitmap.height * scale
            val dx = faceOvalRect.centerX() - (scaledW / 2f)
            val dy = faceOvalRect.centerY() - (scaledH / 2f)

            val matrix = Matrix().apply {
                postScale(scale, scale)
                postTranslate(dx, dy)
            }
            canvas.drawBitmap(faceBitmap, matrix, facePaint)
            canvas.restore()
        } else {
            // Placeholder background inside oval
            val bgInsidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FFF3E0")
            }
            canvas.drawOval(faceOvalRect, bgInsidePaint)
        }

        // 4. Draw the Paçoca Outer Shell (Body with hole cut out)
        val bodyPath = Path().apply {
            addRoundRect(bodyRect, cornerRadius, cornerRadius, Path.Direction.CW)
            // Cut out the inner oval so the face is framed seamlessly
            val innerHolePath = Path().apply {
                addOval(faceOvalRect, Path.Direction.CCW)
            }
            op(innerHolePath, Path.Op.DIFFERENCE)
        }

        // Base peanut body paint with warm golden gradient
        val bodyBasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.5f, h * 0.5f,
                w * 0.6f,
                intArrayOf(
                    Color.parseColor("#ECC37A"), // Center warm golden peanut
                    Color.parseColor("#DFB062"), // Mid crumbly tone
                    Color.parseColor("#C68E3C")  // Darker roasted rim
                ),
                floatArrayOf(0f, 0.65f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(bodyPath, bodyBasePaint)

        // 5. Draw Roasted Peanut Crumb Texture & Flecks onto the paçoca body
        val random = Random(42) // Fixed seed for consistent organic crumb pattern
        val specklePaintDark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8A5016")
        }
        val specklePaintLight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFF5D6")
        }
        val specklePaintMed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B37424")
        }

        canvas.save()
        canvas.clipPath(bodyPath)

        // Generate organic peanut specks across the body
        val speckleCount = 750
        for (i in 0 until speckleCount) {
            val sx = bodyRect.left + random.nextFloat() * bodyRect.width()
            val sy = bodyRect.top + random.nextFloat() * bodyRect.height()
            val sRadius = 1.2f + random.nextFloat() * 3.8f
            val alpha = 40 + random.nextInt(140)

            val p = when (i % 5) {
                0, 1 -> specklePaintDark.apply { this.alpha = alpha }
                2 -> specklePaintLight.apply { this.alpha = (alpha * 0.8f).toInt() }
                else -> specklePaintMed.apply { this.alpha = alpha }
            }

            // Draw slightly irregular peanut grains (small ovals)
            val rx = sRadius * (0.8f + random.nextFloat() * 0.4f)
            val ry = sRadius * (0.8f + random.nextFloat() * 0.4f)
            canvas.drawOval(sx - rx, sy - ry, sx + rx, sy + ry, p)
        }
        canvas.restore()

        // 6. Draw Inner Bevel & Rim around the Oval Face Window (adds depth and 3D frame feeling)
        val innerRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = w * 0.022f
            color = Color.parseColor("#BA8132")
        }
        canvas.drawOval(faceOvalRect, innerRimPaint)

        val innerHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = w * 0.008f
            color = Color.parseColor("#FFF0B8")
            alpha = 180
        }
        val faceOvalHighlight = RectF(
            faceOvalRect.left + 3f,
            faceOvalRect.top + 3f,
            faceOvalRect.right - 3f,
            faceOvalRect.bottom - 3f
        )
        canvas.drawOval(faceOvalHighlight, innerHighlightPaint)

        // 7. Outer Frame Soft 3D Border Stroke
        val outerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = w * 0.018f
            color = Color.parseColor("#A66E20")
            alpha = 160
        }
        canvas.drawRoundRect(bodyRect, cornerRadius, cornerRadius, outerBorderPaint)

        // 8. Kawaii Facial Features Below the Face Oval
        val faceBottomY = faceOvalRect.bottom
        val eyeCenterY = faceBottomY + (h * 0.085f)
        val eyeSpacing = w * 0.24f
        val eyeRadius = w * 0.075f

        val leftEyeX = w * 0.50f - eyeSpacing
        val rightEyeX = w * 0.50f + eyeSpacing

        // --- Eyebrows ---
        val browPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = w * 0.016f
            strokeCap = Paint.Cap.ROUND
            color = Color.parseColor("#42250F")
        }
        val browY = eyeCenterY - eyeRadius * 1.55f
        val browWidth = eyeRadius * 1.1f

        // Left Eyebrow
        val leftBrowPath = Path().apply {
            moveTo(leftEyeX - browWidth * 0.5f, browY + 6f)
            quadTo(leftEyeX, browY - 10f, leftEyeX + browWidth * 0.5f, browY)
        }
        canvas.drawPath(leftBrowPath, browPaint)

        // Right Eyebrow
        val rightBrowPath = Path().apply {
            moveTo(rightEyeX - browWidth * 0.5f, browY)
            quadTo(rightEyeX, browY - 10f, rightEyeX + browWidth * 0.5f, browY + 6f)
        }
        canvas.drawPath(rightBrowPath, browPaint)

        // --- Kawaii Blushing Cheeks ---
        val cheekRadius = w * 0.085f
        val cheekLeftX = leftEyeX - (eyeRadius * 0.85f)
        val cheekRightX = rightEyeX + (eyeRadius * 0.85f)
        val cheekY = eyeCenterY + (eyeRadius * 0.55f)

        val cheekPaintLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cheekLeftX, cheekY, cheekRadius,
                intArrayOf(Color.parseColor("#FF6F91"), Color.parseColor("#00FF6F91")),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            alpha = 190
        }
        canvas.drawCircle(cheekLeftX, cheekY, cheekRadius, cheekPaintLeft)

        val cheekPaintRight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cheekRightX, cheekY, cheekRadius,
                intArrayOf(Color.parseColor("#FF6F91"), Color.parseColor("#00FF6F91")),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            alpha = 190
        }
        canvas.drawCircle(cheekRightX, cheekY, cheekRadius, cheekPaintRight)

        // --- Big Sparkling Eyes ---
        fun drawKawaiiEye(cx: Float, cy: Float) {
            // Outer Eye Iris (Deep Warm Black / Chocolate)
            val irisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#26140A")
            }
            canvas.drawCircle(cx, cy, eyeRadius, irisPaint)

            // Inner Shadow/Glint
            val innerRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#442616")
            }
            canvas.drawCircle(cx, cy + eyeRadius * 0.2f, eyeRadius * 0.75f, innerRing)

            // Specular Highlight (Big dot top-left)
            val highlightBigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
            }
            canvas.drawCircle(cx - eyeRadius * 0.32f, cy - eyeRadius * 0.32f, eyeRadius * 0.36f, highlightBigPaint)

            // Specular Highlight (Small sparkle bottom-right)
            val highlightSmallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                alpha = 220
            }
            canvas.drawCircle(cx + eyeRadius * 0.38f, cy + eyeRadius * 0.32f, eyeRadius * 0.16f, highlightSmallPaint)
        }

        drawKawaiiEye(leftEyeX, eyeCenterY)
        drawKawaiiEye(rightEyeX, eyeCenterY)

        // --- Smiling Open Mouth ---
        val mouthCenterX = w * 0.50f
        val mouthCenterY = eyeCenterY + (h * 0.035f)
        val mouthWidth = w * 0.16f
        val mouthHeight = h * 0.055f

        val mouthRect = RectF(
            mouthCenterX - mouthWidth * 0.5f,
            mouthCenterY - mouthHeight * 0.1f,
            mouthCenterX + mouthWidth * 0.5f,
            mouthCenterY + mouthHeight
        )

        val mouthPath = Path().apply {
            moveTo(mouthRect.left, mouthRect.top)
            quadTo(mouthCenterX, mouthRect.bottom + (mouthHeight * 0.5f), mouthRect.right, mouthRect.top)
            close()
        }

        // Mouth background cavity
        val mouthCavityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38170C")
        }
        canvas.drawPath(mouthPath, mouthCavityPaint)

        // Mouth Tongue
        canvas.save()
        canvas.clipPath(mouthPath)
        val tonguePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF6E7A")
        }
        val tongueRadius = mouthWidth * 0.35f
        canvas.drawCircle(mouthCenterX, mouthRect.bottom + 4f, tongueRadius, tonguePaint)
        canvas.restore()

        // Mouth Outline Border
        val mouthBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = w * 0.012f
            strokeCap = Paint.Cap.ROUND
            color = Color.parseColor("#26140A")
        }
        canvas.drawPath(mouthPath, mouthBorderPaint)

        // 9. Kawaii Orange Padlock Accessory on Top-Right Corner
        drawOrangePadlock(
            canvas = canvas,
            centerX = bodyRect.right - (w * 0.14f),
            centerY = bodyRect.top + (h * 0.075f),
            size = w * 0.17f
        )

        return result
    }

    /**
     * Renders the custom Cara de Kool Frame:
     * - Fleshy textured wrinkled pink monster head (inspired by user uploaded artwork)
     * - Oval transparent cutout for the user's face
     * - Grumpy sleepy heavy-lidded eyes with deep brow folds at top
     * - Folded puckered grumpy mouth at bottom with textured chin folds
     * - Deep organic pink/magenta wrinkles and shading
     * - Orange padlock on top right
     */
    fun renderKoolCharacterFrame(
        faceBitmap: Bitmap?,
        targetWidth: Int = 960,
        targetHeight: Int = 1280
    ): Bitmap {
        val result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val w = targetWidth.toFloat()
        val h = targetHeight.toFloat()

        // 1. Geometry of the Monster Face Silhouette
        val bodyMarginX = w * 0.06f
        val bodyMarginY = h * 0.05f
        val bodyRect = RectF(bodyMarginX, bodyMarginY, w - bodyMarginX, h - bodyMarginY)
        val cornerRadius = w * 0.42f

        // Center Oval Window for user's face
        val ovalCenterX = w * 0.50f
        val ovalCenterY = h * 0.46f
        val ovalRadiusX = w * 0.23f
        val ovalRadiusY = h * 0.24f
        val faceOvalRect = RectF(
            ovalCenterX - ovalRadiusX,
            ovalCenterY - ovalRadiusY,
            ovalCenterX + ovalRadiusX,
            ovalCenterY + ovalRadiusY
        )

        // 2. Draw user face inside oval window
        if (faceBitmap != null && !faceBitmap.isRecycled) {
            val facePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            val facePath = Path().apply {
                addOval(faceOvalRect, Path.Direction.CW)
            }

            canvas.save()
            canvas.clipPath(facePath)

            val scale = max(
                faceOvalRect.width() / faceBitmap.width.toFloat(),
                faceOvalRect.height() / faceBitmap.height.toFloat()
            )
            val scaledW = faceBitmap.width * scale
            val scaledH = faceBitmap.height * scale
            val dx = faceOvalRect.centerX() - (scaledW / 2f)
            val dy = faceOvalRect.centerY() - (scaledH / 2f)

            val matrix = Matrix().apply {
                postScale(scale, scale)
                postTranslate(dx, dy)
            }
            canvas.drawBitmap(faceBitmap, matrix, facePaint)
            canvas.restore()
        } else {
            val bgInsidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FFF0F5")
            }
            canvas.drawOval(faceOvalRect, bgInsidePaint)
        }

        // 3. Draw the Outer Monster Fleshy Shell with center hole cut out
        val bodyPath = Path().apply {
            // Main organic rounded head
            addRoundRect(bodyRect, cornerRadius, cornerRadius * 1.15f, Path.Direction.CW)
            // Cut out the inner oval
            val innerHolePath = Path().apply {
                addOval(faceOvalRect, Path.Direction.CCW)
            }
            op(innerHolePath, Path.Op.DIFFERENCE)
        }

        // Base pink monster gradient
        val bodyBasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.5f, h * 0.45f,
                w * 0.65f,
                intArrayOf(
                    Color.parseColor("#E06A8B"), // Mid pink
                    Color.parseColor("#D45378"), // Deep rich pink
                    Color.parseColor("#A82B51"), // Shadow edge berry
                    Color.parseColor("#731533")  // Darkest outer crease
                ),
                floatArrayOf(0f, 0.55f, 0.85f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(bodyPath, bodyBasePaint)

        // 4. Draw Wrinkle Folds on sides & top
        val wrinkleDarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#69122B")
            style = Paint.Style.STROKE
            strokeWidth = w * 0.022f
            strokeCap = Paint.Cap.ROUND
        }
        val wrinkleLightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFAEC5")
            style = Paint.Style.STROKE
            strokeWidth = w * 0.015f
            strokeCap = Paint.Cap.ROUND
        }

        canvas.save()
        canvas.clipPath(bodyPath)

        // Forehead creases
        for (i in 0..3) {
            val fy = bodyRect.top + h * (0.05f + i * 0.028f)
            val fPath = Path().apply {
                moveTo(w * 0.28f, fy)
                quadTo(w * 0.50f, fy - h * 0.015f, w * 0.72f, fy)
            }
            canvas.drawPath(fPath, wrinkleDarkPaint)
            val fPathLight = Path().apply {
                moveTo(w * 0.28f, fy + 3f)
                quadTo(w * 0.50f, fy - h * 0.015f + 3f, w * 0.72f, fy + 3f)
            }
            canvas.drawPath(fPathLight, wrinkleLightPaint)
        }

        // Side cheek folds (braided fleshy curves)
        val leftCurves = listOf(
            Triple(PointF(w * 0.08f, h * 0.30f), PointF(w * 0.22f, h * 0.38f), PointF(w * 0.26f, h * 0.46f)),
            Triple(PointF(w * 0.08f, h * 0.48f), PointF(w * 0.22f, h * 0.56f), PointF(w * 0.26f, h * 0.65f)),
            Triple(PointF(w * 0.10f, h * 0.66f), PointF(w * 0.24f, h * 0.74f), PointF(w * 0.32f, h * 0.82f))
        )
        for (curve in leftCurves) {
            val pDark = Path().apply {
                moveTo(curve.first.x, curve.first.y)
                quadTo(curve.second.x, curve.second.y, curve.third.x, curve.third.y)
            }
            canvas.drawPath(pDark, wrinkleDarkPaint)
        }

        val rightCurves = listOf(
            Triple(PointF(w * 0.92f, h * 0.30f), PointF(w * 0.78f, h * 0.38f), PointF(w * 0.74f, h * 0.46f)),
            Triple(PointF(w * 0.92f, h * 0.48f), PointF(w * 0.78f, h * 0.56f), PointF(w * 0.74f, h * 0.65f)),
            Triple(PointF(w * 0.90f, h * 0.66f), PointF(w * 0.76f, h * 0.74f), PointF(w * 0.68f, h * 0.82f))
        )
        for (curve in rightCurves) {
            val pDark = Path().apply {
                moveTo(curve.first.x, curve.first.y)
                quadTo(curve.second.x, curve.second.y, curve.third.x, curve.third.y)
            }
            canvas.drawPath(pDark, wrinkleDarkPaint)
        }

        canvas.restore()

        // 5. Bevel / Fleshy Lip around the Oval Hole
        val innerLipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = w * 0.035f
            color = Color.parseColor("#A82B51")
        }
        canvas.drawOval(faceOvalRect, innerLipPaint)

        // 6. Grumpy Sleepy Eyes Above Cutout
        val eyeWidth = w * 0.16f
        val eyeHeight = h * 0.055f
        val leftEyeCenter = PointF(w * 0.35f, faceOvalRect.top - h * 0.045f)
        val rightEyeCenter = PointF(w * 0.65f, faceOvalRect.top - h * 0.045f)

        val eyeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F5E6EC")
        }
        val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C080E")
        }
        val eyelidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B53F62")
        }

        // Left Eye
        val leftEyeRect = RectF(leftEyeCenter.x - eyeWidth / 2f, leftEyeCenter.y - eyeHeight / 2f, leftEyeCenter.x + eyeWidth / 2f, leftEyeCenter.y + eyeHeight / 2f)
        canvas.drawOval(leftEyeRect, eyeBgPaint)
        canvas.drawCircle(leftEyeCenter.x, leftEyeCenter.y + 2f, eyeWidth * 0.22f, pupilPaint)
        // Top eyelid
        val leftLid = Path().apply {
            moveTo(leftEyeRect.left - 4f, leftEyeRect.top - 6f)
            lineTo(leftEyeRect.right + 4f, leftEyeRect.top - 6f)
            lineTo(leftEyeRect.right + 4f, leftEyeCenter.y + 2f)
            quadTo(leftEyeCenter.x, leftEyeCenter.y + 6f, leftEyeRect.left - 4f, leftEyeCenter.y + 2f)
            close()
        }
        canvas.drawPath(leftLid, eyelidPaint)

        // Right Eye
        val rightEyeRect = RectF(rightEyeCenter.x - eyeWidth / 2f, rightEyeCenter.y - eyeHeight / 2f, rightEyeCenter.x + eyeWidth / 2f, rightEyeCenter.y + eyeHeight / 2f)
        canvas.drawOval(rightEyeRect, eyeBgPaint)
        canvas.drawCircle(rightEyeCenter.x, rightEyeCenter.y + 2f, eyeWidth * 0.22f, pupilPaint)
        val rightLid = Path().apply {
            moveTo(rightEyeRect.left - 4f, rightEyeRect.top - 6f)
            lineTo(rightEyeRect.right + 4f, rightEyeRect.top - 6f)
            lineTo(rightEyeRect.right + 4f, rightEyeCenter.y + 2f)
            quadTo(rightEyeCenter.x, rightEyeCenter.y + 6f, rightEyeRect.left - 4f, rightEyeCenter.y + 2f)
            close()
        }
        canvas.drawPath(rightLid, eyelidPaint)

        // Heavy Brow Frown
        val browPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4D0A1E")
            style = Paint.Style.STROKE
            strokeWidth = w * 0.024f
            strokeCap = Paint.Cap.ROUND
        }
        val leftBrowP = Path().apply {
            moveTo(leftEyeCenter.x - eyeWidth * 0.6f, leftEyeCenter.y - eyeHeight * 0.8f)
            quadTo(leftEyeCenter.x, leftEyeCenter.y - eyeHeight * 1.2f, leftEyeCenter.x + eyeWidth * 0.6f, leftEyeCenter.y - eyeHeight * 0.6f)
        }
        canvas.drawPath(leftBrowP, browPaint)

        val rightBrowP = Path().apply {
            moveTo(rightEyeCenter.x - eyeWidth * 0.6f, rightEyeCenter.y - eyeHeight * 0.6f)
            quadTo(rightEyeCenter.x, rightEyeCenter.y - eyeHeight * 1.2f, rightEyeCenter.x + eyeWidth * 0.6f, rightEyeCenter.y - eyeHeight * 0.8f)
        }
        canvas.drawPath(rightBrowP, browPaint)

        // 7. Folded Puckered Lips at the Bottom (Under the Oval Cutout)
        val mouthCenterY = faceOvalRect.bottom + h * 0.055f
        val lipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D45378")
        }
        val lipShadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#731533")
            style = Paint.Style.STROKE
            strokeWidth = w * 0.018f
            strokeCap = Paint.Cap.ROUND
        }

        // Upper puckered lip
        canvas.drawOval(
            RectF(w * 0.41f, mouthCenterY - h * 0.025f, w * 0.59f, mouthCenterY + h * 0.005f),
            lipPaint
        )
        // Lower puckered lip
        canvas.drawOval(
            RectF(w * 0.40f, mouthCenterY - h * 0.005f, w * 0.60f, mouthCenterY + h * 0.035f),
            lipPaint
        )
        // Chin bulb
        canvas.drawOval(
            RectF(w * 0.38f, mouthCenterY + h * 0.035f, w * 0.62f, mouthCenterY + h * 0.090f),
            lipPaint
        )

        // Mouth crease lines
        val lipFold = Path().apply {
            moveTo(w * 0.43f, mouthCenterY)
            quadTo(w * 0.50f, mouthCenterY - 4f, w * 0.57f, mouthCenterY)
        }
        canvas.drawPath(lipFold, lipShadePaint)

        val chinFold = Path().apply {
            moveTo(w * 0.36f, mouthCenterY + h * 0.04f)
            quadTo(w * 0.50f, mouthCenterY + h * 0.055f, w * 0.64f, mouthCenterY + h * 0.04f)
        }
        canvas.drawPath(chinFold, lipShadePaint)

        // 8. Orange Padlock on top-right
        drawOrangePadlock(
            canvas = canvas,
            centerX = bodyRect.right - (w * 0.12f),
            centerY = bodyRect.top + (h * 0.065f),
            size = w * 0.17f
        )

        return result
    }

    private class PointF(val x: Float, val y: Float)

    /**
     * Draws the iconic cute orange padlock on top-right of the frame
     */
    private fun drawOrangePadlock(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float
    ) {
        val half = size / 2f
        val shackleRadius = size * 0.32f
        val shackleStroke = size * 0.16f

        // Shackle (Golden Arc at the top)
        val shacklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = shackleStroke
            strokeCap = Paint.Cap.ROUND
            color = Color.parseColor("#FF9800")
        }
        val shackleTopY = centerY - half * 0.65f
        val shackleRect = RectF(
            centerX - shackleRadius,
            shackleTopY - shackleRadius,
            centerX + shackleRadius,
            shackleTopY + shackleRadius
        )
        canvas.drawArc(shackleRect, 180f, 180f, false, shacklePaint)

        // Lock Body (Rounded Rectangle with orange gradient)
        val lockBodyW = size * 0.85f
        val lockBodyH = size * 0.72f
        val lockBodyRect = RectF(
            centerX - (lockBodyW / 2f),
            centerY - (lockBodyH * 0.2f),
            centerX + (lockBodyW / 2f),
            centerY + (lockBodyH * 0.8f)
        )
        val lockBodyCorner = size * 0.22f

        val lockBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                centerX - (lockBodyW * 0.2f),
                lockBodyRect.top + (lockBodyH * 0.3f),
                lockBodyW,
                intArrayOf(Color.parseColor("#FFA726"), Color.parseColor("#F57C00"), Color.parseColor("#E65100")),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(lockBodyRect, lockBodyCorner, lockBodyCorner, lockBodyPaint)

        // Lock Body Gloss Highlight
        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = size * 0.05f
            color = Color.parseColor("#FFE0B2")
            alpha = 180
        }
        val highlightRect = RectF(
            lockBodyRect.left + 4f,
            lockBodyRect.top + 4f,
            lockBodyRect.right - 4f,
            lockBodyRect.bottom - 4f
        )
        canvas.drawRoundRect(highlightRect, lockBodyCorner * 0.8f, lockBodyCorner * 0.8f, highlightPaint)

        // Keyhole (Dark circle + trapezoid)
        val keyholePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4E2600")
        }
        val keyholeCenterY = lockBodyRect.centerY() - 2f
        canvas.drawCircle(centerX, keyholeCenterY - 4f, size * 0.085f, keyholePaint)

        val keyholeSlot = Path().apply {
            moveTo(centerX - size * 0.055f, keyholeCenterY - 2f)
            lineTo(centerX + size * 0.055f, keyholeCenterY - 2f)
            lineTo(centerX + size * 0.035f, keyholeCenterY + size * 0.16f)
            lineTo(centerX - size * 0.035f, keyholeCenterY + size * 0.16f)
            close()
        }
        canvas.drawPath(keyholeSlot, keyholePaint)
    }

    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val input: InputStream? = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap from uri $uri: ${e.message}")
            null
        }
    }
}
