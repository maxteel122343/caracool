package com.example.service

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class WallpaperTarget {
    LOCK_SCREEN,
    HOME_SCREEN,
    BOTH
}

object WallpaperHelper {

    suspend fun applyWallpaper(
        context: Context,
        target: WallpaperTarget,
        bitmap: Bitmap? = null,
        filePath: String? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val displayMetrics = context.resources.displayMetrics
            val screenWidth = if (displayMetrics.widthPixels > 0) displayMetrics.widthPixels else 1080
            val screenHeight = if (displayMetrics.heightPixels > 0) displayMetrics.heightPixels else 1920

            val resolvedBitmap = when {
                bitmap != null -> bitmap
                !filePath.isNullOrBlank() && File(filePath).exists() -> {
                    decodeSampledBitmap(filePath, screenWidth, screenHeight)
                }
                !filePath.isNullOrBlank() && (filePath.startsWith("content://") || filePath.startsWith("file://")) -> {
                    context.contentResolver.openInputStream(Uri.parse(filePath))?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
                else -> null
            }

            if (resolvedBitmap == null) {
                return@withContext Pair(false, "Não foi possível carregar a imagem para o papel de parede.")
            }

            val scaledBitmap = scaleBitmapToFill(resolvedBitmap, screenWidth, screenHeight)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                when (target) {
                    WallpaperTarget.LOCK_SCREEN -> {
                        wallpaperManager.setBitmap(scaledBitmap, null, true, WallpaperManager.FLAG_LOCK)
                        Pair(true, "🔒 Papel de parede aplicado na TELA DE BLOQUEIO com sucesso!")
                    }
                    WallpaperTarget.HOME_SCREEN -> {
                        wallpaperManager.setBitmap(scaledBitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                        Pair(true, "📱 Papel de parede aplicado na TELA INICIAL com sucesso!")
                    }
                    WallpaperTarget.BOTH -> {
                        wallpaperManager.setBitmap(scaledBitmap, null, true, WallpaperManager.FLAG_LOCK or WallpaperManager.FLAG_SYSTEM)
                        Pair(true, "✨ Papel de parede aplicado em AMBAS AS TELAS com sucesso!")
                    }
                }
            } else {
                wallpaperManager.setBitmap(scaledBitmap)
                Pair(true, "Papel de parede aplicado com sucesso!")
            }
        } catch (e: Exception) {
            Pair(false, "Erro ao aplicar papel de parede: ${e.localizedMessage ?: e.message}")
        }
    }

    private fun decodeSampledBitmap(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            BitmapFactory.decodeFile(path, options)
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private fun scaleBitmapToFill(src: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val tw = if (targetWidth <= 0) 1080 else targetWidth
        val th = if (targetHeight <= 0) 1920 else targetHeight
        return if (src.width == tw && src.height == th) {
            src
        } else {
            Bitmap.createScaledBitmap(src, tw, th, true)
        }
    }

    /**
     * Generates the authentic, high-fidelity Kawaii Paçoca character lockscreen artwork
     * matching the official mascot design when no custom photo is provided.
     */
    fun createDefaultPacocaWallpaper(
        context: Context,
        title: String = "Cara de Paçoca",
        emoji: String = "🥜",
        unlockCount: Int = 1
    ): Bitmap {
        val metrics = context.resources.displayMetrics
        val width = if (metrics.widthPixels > 0) metrics.widthPixels else 1080
        val height = if (metrics.heightPixels > 0) metrics.heightPixels else 1920

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val w = width.toFloat()
        val h = height.toFloat()

        // 1. Dark bezel/backdrop
        val darkBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E1610")
        }
        canvas.drawRect(0f, 0f, w, h, darkBgPaint)

        // 2. Paçoca biscuit body
        val bodyMarginX = w * 0.035f
        val bodyMarginY = h * 0.025f
        val bodyRect = RectF(bodyMarginX, bodyMarginY, w - bodyMarginX, h - bodyMarginY)
        val cornerRadius = w * 0.45f

        val biscuitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, bodyRect.top, 0f, bodyRect.bottom,
                intArrayOf(
                    Color.parseColor("#BA8A4C"),
                    Color.parseColor("#C99955"),
                    Color.parseColor("#D6A75E"),
                    Color.parseColor("#C0904E"),
                    Color.parseColor("#A5773A")
                ),
                floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(bodyRect, cornerRadius, cornerRadius, biscuitPaint)

        // Biscuit border stroke
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (w * 0.035f).coerceAtLeast(6f)
            color = Color.parseColor("#6B481D")
        }
        canvas.drawRoundRect(bodyRect, cornerRadius, cornerRadius, borderPaint)

        // Biscuit Crumbs & Specks
        val crumbPaintDark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4E3013") }
        val crumbPaintLight = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFE5A3") }
        val rand = java.util.Random(12345)
        for (i in 0..220) {
            val cx = bodyRect.left + rand.nextFloat() * bodyRect.width()
            val cy = bodyRect.top + rand.nextFloat() * bodyRect.height()
            val r = rand.nextFloat() * (w * 0.016f) + 1.5f
            val isDark = rand.nextBoolean()
            val p = if (isDark) crumbPaintDark else crumbPaintLight
            p.alpha = (rand.nextFloat() * 80 + 30).toInt()
            canvas.drawCircle(cx, cy, r, p)
        }

        // 3. Top Golden Lock Icon & Digital Clock
        val lockSize = w * 0.065f
        val lockRect = RectF((w - lockSize) / 2f, h * 0.045f, (w + lockSize) / 2f, h * 0.045f + lockSize)
        val lockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFA000") }
        canvas.drawRoundRect(lockRect, 10f, 10f, lockPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            color = Color.WHITE
        }

        // Lock icon symbol
        textPaint.textSize = lockSize * 0.7f
        canvas.drawText("🔒", w / 2f, lockRect.bottom - (lockSize * 0.18f), textPaint)

        // Time (e.g. 16:59)
        textPaint.textSize = w * 0.17f
        textPaint.isFakeBoldText = true
        textPaint.setShadowLayer(8f, 0f, 4f, Color.parseColor("#60000000"))
        val timeSdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        canvas.drawText(timeSdf, w / 2f, h * 0.125f, textPaint)

        // Date (e.g. Sexta-feira, 14 de agosto)
        textPaint.textSize = w * 0.042f
        textPaint.isFakeBoldText = false
        textPaint.color = Color.parseColor("#FFF8E1")
        val dateSdf = java.text.SimpleDateFormat("EEEE, d 'de' MMMM", java.util.Locale("pt", "BR")).format(java.util.Date())
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale("pt", "BR")) else it.toString() }
        canvas.drawText(dateSdf, w / 2f, h * 0.16f, textPaint)

        // 4. Center Oval Lens with Golden Bevel
        val lensCenterX = w * 0.50f
        val lensCenterY = h * 0.44f
        val lensRadius = w * 0.28f

        // Lens inner radial glass gradient
        val lensBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.RadialGradient(
                lensCenterX - (lensRadius * 0.2f),
                lensCenterY - (lensRadius * 0.2f),
                lensRadius * 1.1f,
                intArrayOf(Color.parseColor("#E0E3E8"), Color.parseColor("#9E9E9E"), Color.parseColor("#616161")),
                floatArrayOf(0f, 0.65f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(lensCenterX, lensCenterY, lensRadius, lensBgPaint)

        // Lens golden bezel border
        val bevelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = w * 0.038f
            color = Color.parseColor("#B7812E")
        }
        canvas.drawCircle(lensCenterX, lensCenterY, lensRadius, bevelPaint)

        val innerGoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = w * 0.016f
            color = Color.parseColor("#FFD54F")
        }
        canvas.drawCircle(lensCenterX, lensCenterY, lensRadius - (w * 0.012f), innerGoldPaint)

        // Glass reflection arc
        val arcRect = RectF(lensCenterX - lensRadius + 8f, lensCenterY - lensRadius + 8f, lensCenterX + lensRadius - 8f, lensCenterY + lensRadius - 8f)
        val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = w * 0.04f
            color = Color.WHITE
            alpha = 70
        }
        canvas.drawArc(arcRect, 200f, 135f, false, glassPaint)

        // 5. Kawaii Facial Features (Eyebrows, Cheeks, Anime Eyes, Mouth)
        val faceY = lensCenterY + lensRadius + (h * 0.03f)
        val eyeSpacing = w * 0.32f
        val eyeRadius = w * 0.075f
        val leftEyeX = (w / 2f) - eyeSpacing
        val rightEyeX = (w / 2f) + eyeSpacing
        val eyeY = faceY + (h * 0.07f)

        // Eyebrows
        val browPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = w * 0.02f
            strokeCap = Paint.Cap.ROUND
            color = Color.parseColor("#3E2723")
        }
        val leftBrowRect = RectF(leftEyeX - eyeRadius, eyeY - (eyeRadius * 1.8f), leftEyeX + eyeRadius, eyeY - (eyeRadius * 0.8f))
        val rightBrowRect = RectF(rightEyeX - eyeRadius, eyeY - (eyeRadius * 1.8f), rightEyeX + eyeRadius, eyeY - (eyeRadius * 0.8f))
        canvas.drawArc(leftBrowRect, 190f, 130f, false, browPaint)
        canvas.drawArc(rightBrowRect, 220f, 130f, false, browPaint)

        // Pink Rosy Blush Cheeks
        val cheekPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E57373")
            alpha = 190
        }
        val cheekRadiusX = w * 0.08f
        val cheekRadiusY = h * 0.025f
        canvas.drawOval(RectF(leftEyeX - (eyeRadius * 1.6f) - cheekRadiusX, eyeY - cheekRadiusY, leftEyeX - (eyeRadius * 1.6f) + cheekRadiusX, eyeY + cheekRadiusY), cheekPaint)
        canvas.drawOval(RectF(rightEyeX + (eyeRadius * 1.6f) - cheekRadiusX, eyeY - cheekRadiusY, rightEyeX + (eyeRadius * 1.6f) + cheekRadiusX, eyeY + cheekRadiusY), cheekPaint)

        // Anime Sparkle Eyes (Dark Brown Base)
        val eyeBasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2E1A0C") }
        canvas.drawCircle(leftEyeX, eyeY, eyeRadius, eyeBasePaint)
        canvas.drawCircle(rightEyeX, eyeY, eyeRadius, eyeBasePaint)

        // Big Primary Catchlight (Upper Left)
        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawCircle(leftEyeX - (eyeRadius * 0.32f), eyeY - (eyeRadius * 0.32f), eyeRadius * 0.44f, highlightPaint)
        canvas.drawCircle(rightEyeX - (eyeRadius * 0.32f), eyeY - (eyeRadius * 0.32f), eyeRadius * 0.44f, highlightPaint)

        // Small Secondary Catchlight (Lower Right)
        canvas.drawCircle(leftEyeX + (eyeRadius * 0.35f), eyeY + (eyeRadius * 0.35f), eyeRadius * 0.22f, highlightPaint)
        canvas.drawCircle(rightEyeX + (eyeRadius * 0.35f), eyeY + (eyeRadius * 0.35f), eyeRadius * 0.22f, highlightPaint)

        // Cheerful Smile
        val mouthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2E1A0C")
            style = Paint.Style.FILL
        }
        val mouthPath = android.graphics.Path().apply {
            val mw = w * 0.15f
            val mh = h * 0.028f
            val my = eyeY + (h * 0.01f)
            moveTo((w / 2f) - mw, my)
            quadTo(w / 2f, my + (mh * 2f), (w / 2f) + mw, my)
            close()
        }
        canvas.drawPath(mouthPath, mouthPaint)

        // 6. Bottom Action Button: Desbloquear
        val btnW = w * 0.82f
        val btnH = h * 0.065f
        val btnY = h * 0.81f
        val btnRect = RectF((w - btnW) / 2f, btnY, (w + btnW) / 2f, btnY + btnH)

        val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                btnRect.left, btnRect.top, btnRect.left, btnRect.bottom,
                intArrayOf(Color.parseColor("#FF8F00"), Color.parseColor("#E65100"), Color.parseColor("#BF360C")),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(btnRect, btnH / 2f, btnH / 2f, btnPaint)

        textPaint.textSize = w * 0.048f
        textPaint.isFakeBoldText = true
        textPaint.color = Color.WHITE
        textPaint.clearShadowLayer()
        canvas.drawText("🔒 Desbloquear", w / 2f, btnY + (btnH * 0.62f), textPaint)

        // Sub pill: Desbloquear Tirando Foto
        val subBtnW = w * 0.68f
        val subBtnH = h * 0.048f
        val subBtnY = btnY + btnH + (h * 0.015f)
        val subBtnRect = RectF((w - subBtnW) / 2f, subBtnY, (w + subBtnW) / 2f, subBtnY + subBtnH)

        val subBtnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            alpha = 95
        }
        canvas.drawRoundRect(subBtnRect, subBtnH / 2f, subBtnH / 2f, subBtnPaint)

        textPaint.textSize = w * 0.035f
        textPaint.isFakeBoldText = false
        canvas.drawText("📷 Desbloquear Tirando Foto", w / 2f, subBtnY + (subBtnH * 0.64f), textPaint)

        return bitmap
    }
}
