package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * High-fidelity Kawaii Paçoca Lockscreen representation.
 * Serves as the official fallback template when the user hasn't taken a selfie or configured a lockscreen photo.
 */
@Composable
fun KawaiiPacocaLockscreenTemplate(
    modifier: Modifier = Modifier,
    photoUri: String? = null,
    unlockCount: Int = 1,
    timeText: String? = null,
    dateText: String? = null,
    onUnlockClick: (() -> Unit)? = null,
    onUnlockWithPhotoClick: (() -> Unit)? = null
) {
    val currentTime = remember(timeText) {
        timeText ?: SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }
    val currentDate = remember(dateText) {
        dateText ?: SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR")).format(Date())
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF1E1610)) // Dark phone bezel/backdrop
    ) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight

        // 1. Paçoca Biscuit Body with crumbs & gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(totalWidth * 0.03f)
                .clip(RoundedCornerShape(totalWidth * 0.45f))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFBA8A4C),
                            Color(0xFFC99955),
                            Color(0xFFD6A75E),
                            Color(0xFFC0904E),
                            Color(0xFFA5773A)
                        )
                    )
                )
                .border(
                    width = (totalWidth * 0.035f).coerceAtLeast(3.dp),
                    color = Color(0xFF6B481D),
                    shape = RoundedCornerShape(totalWidth * 0.45f)
                )
        ) {
            // Peanut crumb textures & specular specks
            Canvas(modifier = Modifier.fillMaxSize()) {
                val rand = Random(12345)
                val widthPx = size.width
                val heightPx = size.height

                // Draw organic biscuit crumbs
                for (i in 0..180) {
                    val cx = rand.nextFloat() * widthPx
                    val cy = rand.nextFloat() * heightPx
                    val r = rand.nextFloat() * (widthPx * 0.018f) + 1f
                    val alpha = rand.nextFloat() * 0.35f + 0.1f
                    val isDark = rand.nextBoolean()
                    val crumbColor = if (isDark) Color(0xFF4E3013) else Color(0xFFFFE5A3)

                    drawCircle(
                        color = crumbColor.copy(alpha = alpha),
                        radius = r,
                        center = Offset(cx, cy)
                    )
                }
            }

            // Main Lockscreen Content Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = totalWidth * 0.06f, vertical = totalHeight * 0.04f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Section: Lock Icon + Big Clock
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Golden Lock Icon
                    Box(
                        modifier = Modifier
                            .size((totalWidth * 0.07f).coerceIn(16.dp, 28.dp))
                            .background(Color(0xFFFFA000), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF3E2723),
                            modifier = Modifier.size((totalWidth * 0.045f).coerceIn(10.dp, 18.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(totalHeight * 0.01f))

                    // Digital Clock
                    Text(
                        text = currentTime,
                        fontSize = (totalWidth.value * 0.17f).coerceIn(28f, 68f).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-1).sp,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = currentDate,
                        fontSize = (totalWidth.value * 0.042f).coerceIn(11f, 18f).sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFF8E1),
                        textAlign = TextAlign.Center
                    )
                }

                // Middle Section: Circular Lens Viewport with Golden Bevel & Kawaii Face
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val lensSize = (totalWidth * 0.52f).coerceIn(100.dp, 220.dp)

                    // The Circular Photo / Mirror Hole
                    Box(
                        modifier = Modifier
                            .size(lensSize)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFDCDFE3),
                                        Color(0xFF9E9E9E),
                                        Color(0xFF616161)
                                    )
                                )
                            )
                            .border(
                                width = (lensSize * 0.065f).coerceAtLeast(3.dp),
                                brush = Brush.sweepGradient(
                                    listOf(
                                        Color(0xFFFFD54F),
                                        Color(0xFFB7812E),
                                        Color(0xFFFFECB3),
                                        Color(0xFF8D5B18),
                                        Color(0xFFFFD54F)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!photoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = "Foto na moldura",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            // Clean mirror glass reflection inside the viewport lens
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                Color(0xFFFFECB3).copy(alpha = 0.85f),
                                                Color(0xFFFFD54F).copy(alpha = 0.65f),
                                                Color(0xFFFFA000).copy(alpha = 0.45f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                // Subtle camera lens aperture shimmer
                                Canvas(modifier = Modifier.fillMaxSize(0.6f)) {
                                    drawCircle(
                                        brush = Brush.linearGradient(
                                            listOf(Color.White.copy(alpha = 0.4f), Color.Transparent)
                                        ),
                                        radius = size.minDimension * 0.45f
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(totalHeight * 0.015f))

                    // Kawaii Face Component (Eyes with Sparkles, Cheeks, Smile, Brows)
                    KawaiiFacialFeatures(
                        width = lensSize * 1.35f,
                        height = lensSize * 0.45f
                    )
                }

                // Bottom Section: Lockscreen Action Buttons
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Main Orange Unlock Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height((totalHeight * 0.065f).coerceIn(36.dp, 54.dp))
                            .clip(RoundedCornerShape(26.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFFFF8F00),
                                        Color(0xFFE65100),
                                        Color(0xFFBF360C)
                                    )
                                )
                            )
                            .border(1.dp, Color(0xFFFFB74D), RoundedCornerShape(26.dp))
                            .clickable(enabled = onUnlockClick != null) { onUnlockClick?.invoke() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size((totalWidth * 0.045f).coerceIn(14.dp, 20.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Desbloquear",
                                fontSize = (totalWidth.value * 0.045f).coerceIn(12f, 17f).sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(totalHeight * 0.01f))

                    // Secondary Pill: Desbloquear Tirando Foto
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height((totalHeight * 0.05f).coerceIn(28.dp, 40.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .clickable(enabled = onUnlockWithPhotoClick != null) { onUnlockWithPhotoClick?.invoke() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size((totalWidth * 0.038f).coerceIn(12.dp, 16.dp))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Desbloquear Tirando Foto",
                                fontSize = (totalWidth.value * 0.033f).coerceIn(10f, 13f).sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Detailed Kawaii Face with Anime Eyes (with double light reflections), Rosy Cheeks, Curved Eyebrows and Smile
 */
@Composable
fun KawaiiFacialFeatures(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .width(width)
            .height(height)
    ) {
        val w = size.width
        val h = size.height

        val eyeRadius = w * 0.12f
        val leftEyeCenter = Offset(w * 0.18f, h * 0.52f)
        val rightEyeCenter = Offset(w * 0.82f, h * 0.52f)

        // 1. Curved Eyebrows
        val browPaintColor = Color(0xFF3E2723)
        // Left brow
        drawArc(
            color = browPaintColor,
            startAngle = 190f,
            sweepAngle = 130f,
            useCenter = false,
            topLeft = Offset(w * 0.09f, h * 0.05f),
            size = androidx.compose.ui.geometry.Size(w * 0.18f, h * 0.35f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.024f)
        )
        // Right brow
        drawArc(
            color = browPaintColor,
            startAngle = 220f,
            sweepAngle = 130f,
            useCenter = false,
            topLeft = Offset(w * 0.73f, h * 0.05f),
            size = androidx.compose.ui.geometry.Size(w * 0.18f, h * 0.35f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.024f)
        )

        // 2. Rosy Pink Blush Cheeks
        val cheekColor = Color(0xFFE57373).copy(alpha = 0.75f)
        drawOval(
            color = cheekColor,
            topLeft = Offset(w * 0.02f, h * 0.62f),
            size = androidx.compose.ui.geometry.Size(w * 0.16f, h * 0.35f)
        )
        drawOval(
            color = cheekColor,
            topLeft = Offset(w * 0.82f, h * 0.62f),
            size = androidx.compose.ui.geometry.Size(w * 0.16f, h * 0.35f)
        )

        // 3. Anime Sparkle Eyes (Dark Brown Base)
        val eyeBaseColor = Color(0xFF2E1A0C)
        drawCircle(color = eyeBaseColor, radius = eyeRadius, center = leftEyeCenter)
        drawCircle(color = eyeBaseColor, radius = eyeRadius, center = rightEyeCenter)

        // Big Primary Catchlight (Upper Left)
        val leftHighlightCenter1 = Offset(leftEyeCenter.x - eyeRadius * 0.3f, leftEyeCenter.y - eyeRadius * 0.3f)
        val rightHighlightCenter1 = Offset(rightEyeCenter.x - eyeRadius * 0.3f, rightEyeCenter.y - eyeRadius * 0.3f)
        drawCircle(color = Color.White, radius = eyeRadius * 0.44f, center = leftHighlightCenter1)
        drawCircle(color = Color.White, radius = eyeRadius * 0.44f, center = rightHighlightCenter1)

        // Small Secondary Catchlight (Lower Right)
        val leftHighlightCenter2 = Offset(leftEyeCenter.x + eyeRadius * 0.35f, leftEyeCenter.y + eyeRadius * 0.35f)
        val rightHighlightCenter2 = Offset(rightEyeCenter.x + eyeRadius * 0.35f, rightEyeCenter.y + eyeRadius * 0.35f)
        drawCircle(color = Color.White, radius = eyeRadius * 0.22f, center = leftHighlightCenter2)
        drawCircle(color = Color.White, radius = eyeRadius * 0.22f, center = rightHighlightCenter2)

        // 4. Cheerful Open Mouth Smile
        val mouthPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.38f, h * 0.68f)
            quadraticTo(
                w * 0.50f, h * 0.98f,
                w * 0.62f, h * 0.68f
            )
            close()
        }
        drawPath(path = mouthPath, color = Color(0xFF2E1A0C))
    }
}
