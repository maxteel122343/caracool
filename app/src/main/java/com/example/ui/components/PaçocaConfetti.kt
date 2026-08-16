package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

data class Particle(
    val startX: Float,
    val startY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val color: Color,
    val size: Float,
    val isPeanut: Boolean,
    val rotationSpeed: Float
)

@Composable
fun PaçocaConfetti(
    isVisible: Boolean,
    onFinished: () -> Unit = {}
) {
    if (!isVisible) return

    val progress = remember { Animatable(0f) }
    val particles = remember {
        val colors = listOf(
            Color(0xFFF59E0B), // Peanut Gold
            Color(0xFFD97706), // Paçoca Amber
            Color(0xFF78350F), // Peanut Brown
            Color(0xFFFEF3C7), // Peanut Cream
            Color(0xFFF43F5E), // Strawberry festive
            Color(0xFF10B981)  // Mint green
        )
        List(40) {
            Particle(
                startX = 0.5f,
                startY = 0.6f,
                velocityX = (Random.nextFloat() - 0.5f) * 1.8f,
                velocityY = -(0.5f + Random.nextFloat() * 1.2f),
                color = colors[Random.nextInt(colors.size)],
                size = 14f + Random.nextFloat() * 20f,
                isPeanut = Random.nextBoolean(),
                rotationSpeed = (Random.nextFloat() - 0.5f) * 720f
            )
        }
    }

    LaunchedEffect(isVisible) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing)
        )
        onFinished()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val t = progress.value
        val gravity = 2.2f * t * t
        val alpha = (1f - t).coerceIn(0f, 1f)

        particles.forEach { p ->
            val curX = (p.startX + p.velocityX * t) * size.width
            val curY = (p.startY + p.velocityY * t + gravity) * size.height
            val rot = p.rotationSpeed * t

            rotate(rot, pivot = Offset(curX, curY)) {
                if (p.isPeanut) {
                    // Draw peanut oval shape
                    drawOval(
                        color = p.color.copy(alpha = alpha),
                        topLeft = Offset(curX - p.size / 2, curY - p.size / 3),
                        size = Size(p.size, p.size * 0.6f)
                    )
                } else {
                    // Draw square paçoca crumb
                    drawRect(
                        color = p.color.copy(alpha = alpha),
                        topLeft = Offset(curX - p.size / 2, curY - p.size / 2),
                        size = Size(p.size, p.size * 0.8f)
                    )
                }
            }
        }
    }
}
