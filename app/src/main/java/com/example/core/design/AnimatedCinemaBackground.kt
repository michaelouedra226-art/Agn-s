package com.example.core.design

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.sin
import kotlin.random.Random

data class CinemaParticle(
    val initialX: Float,
    val initialY: Float,
    val radius: Float,
    val speed: Float,
    val alphaOffset: Float
)

@Composable
fun AnimatedCinemaBackground(
    modifier: Modifier = Modifier,
    isGenerating: Boolean = false,
    hasError: Boolean = false,
    particlesEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CinemaAtmosphere")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isGenerating) 12000 else 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "MeshPhase"
    )

    val particles = remember {
        List(35) {
            CinemaParticle(
                initialX = Random.nextFloat(),
                initialY = Random.nextFloat(),
                radius = Random.nextFloat() * 1.8f + 1f,
                speed = Random.nextFloat() * 0.15f + 0.05f,
                alphaOffset = Random.nextFloat() * (Math.PI * 2).toFloat()
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Layer 0: Base void
            drawRect(color = CinColors.BgVoid)

            // Layer 1: Moving Mesh Gradient Orbs
            val orb1X = width * (0.35f + 0.25f * sin(phase))
            val orb1Y = height * (0.3f + 0.2f * sin(phase * 0.8f))
            val orb1Color = when {
                hasError -> CinColors.Danger.copy(alpha = 0.22f)
                isGenerating -> CinColors.AccentPink.copy(alpha = 0.25f)
                else -> CinColors.AccentViolet.copy(alpha = 0.22f)
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orb1Color, Color.Transparent),
                    center = Offset(orb1X, orb1Y),
                    radius = width * 0.75f
                ),
                radius = width * 0.75f,
                center = Offset(orb1X, orb1Y)
            )

            val orb2X = width * (0.7f - 0.25f * sin(phase * 1.2f))
            val orb2Y = height * (0.75f - 0.25f * sin(phase * 0.9f))
            val orb2Color = if (isGenerating) CinColors.AccentViolet.copy(alpha = 0.20f) else CinColors.AccentCyan.copy(alpha = 0.15f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orb2Color, Color.Transparent),
                    center = Offset(orb2X, orb2Y),
                    radius = width * 0.8f
                ),
                radius = width * 0.8f,
                center = Offset(orb2X, orb2Y)
            )

            // Layer 2: Floating Particles / Cinema Dust
            if (particlesEnabled) {
                particles.forEach { p ->
                    val curY = ((p.initialY - (phase / (2 * Math.PI).toFloat()) * p.speed * 4f) % 1f + 1f) % 1f
                    val curX = p.initialX + 0.03f * sin(phase + p.alphaOffset)
                    val alpha = (0.15f + 0.25f * ((sin(phase * 2f + p.alphaOffset) + 1f) / 2f)).coerceIn(0.05f, 0.45f)
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = p.radius,
                        center = Offset(curX * width, curY * height)
                    )
                }
            }

            // Layer 3: Subtle cinema vignette
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color(0x66000000)),
                    center = Offset(width / 2f, height / 2f),
                    radius = width.coerceAtLeast(height) * 0.85f
                )
            )
        }

        content()
    }
}
