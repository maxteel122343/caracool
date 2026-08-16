package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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

/**
 * High-fidelity Cara de Kool Mascot Avatar and Lockscreen Art.
 * Inspired by the user's custom grumpy pink monster artwork with lock.
 */
@Composable
fun KoolMascotBadge(
    modifier: Modifier = Modifier,
    sizeDp: Int = 120,
    showLock: Boolean = true
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(sizeDp.dp * 0.28f))
            .background(Color(0xFFF7EFE4)) // Soft beige clay tile base
            .border(
                width = (sizeDp.dp * 0.03f).coerceAtLeast(1.5.dp),
                color = Color(0xFFE8DCCB),
                shape = RoundedCornerShape(sizeDp.dp * 0.28f)
            )
            .padding(sizeDp.dp * 0.04f),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.tardigrade_mascot),
            contentDescription = "Mascote Cara de Cu",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * Draws the cute grumpy pink monster with wrinkles and sad/cute expression
 */
private fun drawKoolMonster(scope: DrawScope, w: Float, h: Float) {
    with(scope) {
        val cx = w * 0.50f
        val cy = h * 0.52f

        // 1. Chubby Body (Warm Rose/Pink)
        val bodyWidth = w * 0.78f
        val bodyHeight = h * 0.74f

        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFE08D9D),
                    Color(0xFFD47587),
                    Color(0xFFC0596D)
                ),
                center = Offset(cx, cy - bodyHeight * 0.1f),
                radius = bodyWidth * 0.6f
            ),
            topLeft = Offset(cx - bodyWidth / 2f, cy - bodyHeight / 2f + h * 0.06f),
            size = Size(bodyWidth, bodyHeight)
        )

        // 2. Head Shape (Upper Oval)
        val headRadius = w * 0.36f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFEA9AA8),
                    Color(0xFFD47587),
                    Color(0xFFB54C62)
                ),
                center = Offset(cx, cy - h * 0.12f),
                radius = headRadius
            ),
            radius = headRadius,
            center = Offset(cx, cy - h * 0.10f)
        )

        // 3. Belly Wrinkle Folds (Horizontal subtle curves)
        val wrinkleColor = Color(0xFF8A3043).copy(alpha = 0.45f)
        val wrinkleStroke = Stroke(width = w * 0.035f, cap = StrokeCap.Round)

        // Lower belly fold
        val p1 = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx - bodyWidth * 0.32f, cy + bodyHeight * 0.18f)
            quadraticTo(cx, cy + bodyHeight * 0.28f, cx + bodyWidth * 0.32f, cy + bodyHeight * 0.18f)
        }
        drawPath(p1, wrinkleColor, style = wrinkleStroke)

        // Mid belly fold
        val p2 = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx - bodyWidth * 0.36f, cy + bodyHeight * 0.04f)
            quadraticTo(cx, cy + bodyHeight * 0.14f, cx + bodyWidth * 0.36f, cy + bodyHeight * 0.04f)
        }
        drawPath(p2, wrinkleColor, style = wrinkleStroke)

        // 4. Grumpy Sleepy Eyes
        val eyeWidth = w * 0.18f
        val eyeHeight = h * 0.14f
        val leftEyeCenter = Offset(cx - w * 0.18f, cy - h * 0.14f)
        val rightEyeCenter = Offset(cx + w * 0.18f, cy - h * 0.14f)

        // Eyeballs (Warm cream/white)
        drawOval(
            color = Color(0xFFFAF0E6),
            topLeft = Offset(leftEyeCenter.x - eyeWidth / 2f, leftEyeCenter.y - eyeHeight / 2f),
            size = Size(eyeWidth, eyeHeight)
        )
        drawOval(
            color = Color(0xFFFAF0E6),
            topLeft = Offset(rightEyeCenter.x - eyeWidth / 2f, rightEyeCenter.y - eyeHeight / 2f),
            size = Size(eyeWidth, eyeHeight)
        )

        // Pupils (Dark coffee)
        val pupilRadius = eyeWidth * 0.26f
        drawCircle(
            color = Color(0xFF261217),
            radius = pupilRadius,
            center = Offset(leftEyeCenter.x, leftEyeCenter.y + eyeHeight * 0.1f)
        )
        drawCircle(
            color = Color(0xFF261217),
            radius = pupilRadius,
            center = Offset(rightEyeCenter.x, rightEyeCenter.y + eyeHeight * 0.1f)
        )

        // Heavy Drooping Eyelids (Pink top half covering eyeball)
        val lidPathLeft = androidx.compose.ui.graphics.Path().apply {
            moveTo(leftEyeCenter.x - eyeWidth * 0.6f, leftEyeCenter.y - eyeHeight * 0.6f)
            lineTo(leftEyeCenter.x + eyeWidth * 0.6f, leftEyeCenter.y - eyeHeight * 0.6f)
            lineTo(leftEyeCenter.x + eyeWidth * 0.6f, leftEyeCenter.y + eyeHeight * 0.05f)
            quadraticTo(leftEyeCenter.x, leftEyeCenter.y + eyeHeight * 0.15f, leftEyeCenter.x - eyeWidth * 0.6f, leftEyeCenter.y + eyeHeight * 0.05f)
            close()
        }
        drawPath(lidPathLeft, Color(0xFFD47587))

        val lidPathRight = androidx.compose.ui.graphics.Path().apply {
            moveTo(rightEyeCenter.x - eyeWidth * 0.6f, rightEyeCenter.y - eyeHeight * 0.6f)
            lineTo(rightEyeCenter.x + eyeWidth * 0.6f, rightEyeCenter.y - eyeHeight * 0.6f)
            lineTo(rightEyeCenter.x + eyeWidth * 0.6f, rightEyeCenter.y + eyeHeight * 0.05f)
            quadraticTo(rightEyeCenter.x, rightEyeCenter.y + eyeHeight * 0.15f, rightEyeCenter.x - eyeWidth * 0.6f, rightEyeCenter.y + eyeHeight * 0.05f)
            close()
        }
        drawPath(lidPathRight, Color(0xFFD47587))

        // Sad/Grumpy Eyebrows
        val browColor = Color(0xFF521C26)
        val browStroke = Stroke(width = w * 0.035f, cap = StrokeCap.Round)

        // Left Brow (slanted up towards center)
        val leftBrow = androidx.compose.ui.graphics.Path().apply {
            moveTo(leftEyeCenter.x - eyeWidth * 0.45f, leftEyeCenter.y - eyeHeight * 0.85f)
            quadraticTo(leftEyeCenter.x, leftEyeCenter.y - eyeHeight * 1.15f, leftEyeCenter.x + eyeWidth * 0.45f, leftEyeCenter.y - eyeHeight * 0.85f)
        }
        drawPath(leftBrow, browColor, style = browStroke)

        val rightBrow = androidx.compose.ui.graphics.Path().apply {
            moveTo(rightEyeCenter.x - eyeWidth * 0.45f, rightEyeCenter.y - eyeHeight * 0.85f)
            quadraticTo(rightEyeCenter.x, rightEyeCenter.y - eyeHeight * 1.15f, rightEyeCenter.x + eyeWidth * 0.45f, rightEyeCenter.y - eyeHeight * 0.85f)
        }
        drawPath(rightBrow, browColor, style = browStroke)

        // 5. Down-turned Grumpy Sad Mouth
        val mouthPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx - w * 0.16f, cy + h * 0.04f)
            quadraticTo(cx, cy - h * 0.02f, cx + w * 0.16f, cy + h * 0.04f)
        }
        drawPath(mouthPath, Color(0xFF521C26), style = Stroke(width = w * 0.045f, cap = StrokeCap.Round))

        // Tiny cute feet at bottom
        drawCircle(Color(0xFFC0596D), radius = w * 0.07f, center = Offset(cx - w * 0.22f, cy + bodyHeight * 0.48f))
        drawCircle(Color(0xFFC0596D), radius = w * 0.07f, center = Offset(cx + w * 0.22f, cy + bodyHeight * 0.48f))
    }
}

/**
 * Lockscreen representation for "Cara de Kool" mode
 */
@Composable
fun KawaiiKoolLockscreenTemplate(
    modifier: Modifier = Modifier,
    photoUri: String? = null,
    unlockCount: Int = 1,
    timeText: String? = null,
    dateText: String? = null,
    palette: String = "universal",
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

    val (frameBg, gradientColors, innerBorderColor, lockTint, subtitleColor, badgeBg, badgeBorder, buttonTextColor, buttonPhotoBg) = remember(palette) {
        when (palette) {
            "pacoca" -> LockscreenPaletteColors(
                frameBg = Color(0xFF2A1810),
                gradient = listOf(Color(0xFFFFE0B2), Color(0xFFFFA726), Color(0xFFFB8C00), Color(0xFFD87A24), Color(0xFF5D4037)),
                innerBorder = Color(0xFF3E2723),
                lockTint = Color(0xFFFFE082),
                subtitle = Color(0xFFFFF8E1),
                badgeBg = Color(0xFF8D6E63),
                badgeBorder = Color(0xFFFFD54F),
                buttonText = Color(0xFF4E342E),
                buttonPhotoBg = Color(0xFFD87A24)
            )
            "nude_peach", "kool_1" -> LockscreenPaletteColors(
                frameBg = Color(0xFF2E1B1B),
                gradient = listOf(Color(0xFFFDEAE2), Color(0xFFF5B7B1), Color(0xFFD46A7A), Color(0xFFA04050), Color(0xFF4A1E24)),
                innerBorder = Color(0xFF3A1E24),
                lockTint = Color(0xFFFFCCBC),
                subtitle = Color(0xFFFFF8F5),
                badgeBg = Color(0xFF6D214F),
                badgeBorder = Color(0xFFF8BBD0),
                buttonText = Color(0xFF4A1E24),
                buttonPhotoBg = Color(0xFFD46A7A)
            )
            "pink_berry", "kool_2" -> LockscreenPaletteColors(
                frameBg = Color(0xFF2C0F1E),
                gradient = listOf(Color(0xFFF48FB1), Color(0xFFEC407A), Color(0xFFD81B60), Color(0xFFC2185B), Color(0xFF880E4F)),
                innerBorder = Color(0xFF6B1736),
                lockTint = Color(0xFFFF80AB),
                subtitle = Color(0xFFFCE4EC),
                badgeBg = Color(0xFF880E4F),
                badgeBorder = Color(0xFFFF80AB),
                buttonText = Color(0xFF880E4F),
                buttonPhotoBg = Color(0xFFFF4081)
            )
            else -> LockscreenPaletteColors( // "universal" (Laranja & Roxo Padrão)
                frameBg = Color(0xFF1B1A24),
                gradient = listOf(Color(0xFFFF8A50), Color(0xFFFF6B35), Color(0xFFE85B24), Color(0xFF7B6CF6), Color(0xFF4A148C)),
                innerBorder = Color(0xFF2B1B48),
                lockTint = Color(0xFFFFD180),
                subtitle = Color(0xFFFFF3E0),
                badgeBg = Color(0xFF53389E),
                badgeBorder = Color(0xFFFFB74D),
                buttonText = Color(0xFF311B92),
                buttonPhotoBg = Color(0xFFFF6B35)
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .background(frameBg)
    ) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(totalWidth * 0.03f)
                .clip(RoundedCornerShape(totalWidth * 0.45f))
                .background(Brush.verticalGradient(colors = gradientColors))
                .border(
                    width = (totalWidth * 0.035f).coerceAtLeast(3.dp),
                    color = innerBorderColor,
                    shape = RoundedCornerShape(totalWidth * 0.45f)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = totalWidth * 0.08f, vertical = totalHeight * 0.04f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Lock & Time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .background(Color(0x55000000), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Bloqueado",
                        tint = lockTint,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "CARA DE CU 👾",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(totalHeight * 0.015f))

                Text(
                    text = currentTime,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )

                Text(
                    text = currentDate,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = subtitleColor
                )

                Spacer(modifier = Modifier.height(totalHeight * 0.02f))

                // Center Frame / Mascot
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (!photoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                .data(photoUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Foto de Bloqueio Cara de Cu",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(24.dp))
                        )
                    } else {
                        KoolMascotBadge(
                            sizeDp = 170,
                            showLock = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(totalHeight * 0.02f))

                // Unlock counter badge
                Box(
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(20.dp))
                        .border(1.5.dp, badgeBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Desbloqueio #$unlockCount hoje!",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(totalHeight * 0.02f))

                // Unlock action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (onUnlockClick != null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .background(Color.White, RoundedCornerShape(16.dp))
                                .clickable { onUnlockClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Desbloquear",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = buttonTextColor
                            )
                        }
                    }

                    if (onUnlockWithPhotoClick != null) {
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .height(46.dp)
                                .background(buttonPhotoBg, RoundedCornerShape(16.dp))
                                .clickable { onUnlockWithPhotoClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Foto de Cu 👾",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class LockscreenPaletteColors(
    val frameBg: Color,
    val gradient: List<Color>,
    val innerBorder: Color,
    val lockTint: Color,
    val subtitle: Color,
    val badgeBg: Color,
    val badgeBorder: Color,
    val buttonText: Color,
    val buttonPhotoBg: Color
)
