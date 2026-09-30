package com.footballpluse.footballapp.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.footballpluse.footballapp.R
import com.footballpluse.footballapp.ui.theme.DarkAccentGreen
import com.footballpluse.footballapp.ui.theme.DarkSurface
import com.footballpluse.footballapp.ui.theme.DeepNavy
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Branded splash: a drawn football springs in over a pulsing glow, the
 * two-tone wordmark ("Football" + "Plus") staggers in beneath it, and a thin
 * green load line sweeps once. Shown while [SplashViewModel] resolves the
 * start destination (with a minimum display time so the animation reads).
 */
@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FootballLogo()
            Spacer(Modifier.height(28.dp))
            SplashWordmark()
            Spacer(Modifier.height(22.dp))
            SplashLoadLine()
        }
    }
}

/** Ball + glow + spin accent, entering with an overshoot spring and settling rotation. */
@Composable
private fun FootballLogo() {
    val glowAlpha = remember { Animatable(0f) }
    val ballScale = remember { Animatable(0f) }
    val ballRotation = remember { Animatable(-25f) }

    // Slow breathing glow behind the ball, once the entrance allows it.
    val pulse = rememberInfiniteTransition(label = "glow")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.13f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    LaunchedEffect(Unit) {
        launch { ballScale.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium)) }
        launch { ballRotation.animateTo(0f, tween(650, easing = FastOutSlowInEasing)) }
        glowAlpha.animateTo(1f, tween(450))
    }

    Box(contentAlignment = Alignment.Center) {
        // Glow disc
        Canvas(modifier = Modifier.size(150.dp).graphicsLayer { alpha = glowAlpha.value }) {
            drawCircle(color = DarkAccentGreen.copy(alpha = pulseAlpha))
        }
        // Ball
        Canvas(
            modifier = Modifier
                .size(96.dp)
                .graphicsLayer {
                    scaleX = ballScale.value
                    scaleY = ballScale.value
                    rotationZ = ballRotation.value
                    alpha = glowAlpha.value
                }
        ) {
            val r = size.minDimension / 2f
            val c = Offset(size.width / 2f, size.height / 2f)

            // Ball body
            drawCircle(color = Color(0xFFF4F6F8), radius = r, center = c)
            drawCircle(color = Color(0xFFD9DEE5), radius = r, center = c, style = Stroke(width = r * 0.045f))

            // Center pentagon (punched through to the background colour)
            val pr = r * 0.36f
            val pentagon = (0..4).map { i ->
                val a = Math.toRadians(-90.0 + i * 72.0)
                Offset(c.x + (pr * cos(a)).toFloat(), c.y + (pr * sin(a)).toFloat())
            }
            val pentPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(pentagon[0].x, pentagon[0].y)
                for (i in 1..4) lineTo(pentagon[i].x, pentagon[i].y)
                close()
            }
            drawPath(pentPath, color = DeepNavy)

            // Spokes from each pentagon vertex toward the ball edge
            pentagon.forEach { p ->
                val dx = p.x - c.x
                val dy = p.y - c.y
                val len = kotlin.math.hypot(dx.toDouble(), dy.toDouble())
                val ex = c.x + (dx / len * (r * 0.96f)).toFloat()
                val ey = c.y + (dy / len * (r * 0.96f)).toFloat()
                drawLine(
                    color = Color(0xFF8B949E),
                    start = p,
                    end = Offset(ex, ey),
                    strokeWidth = r * 0.05f,
                    cap = StrokeCap.Round
                )
            }

            // Brand accent arc that settles with the rotation
            drawArc(
                color = DarkAccentGreen,
                startAngle = 128f,
                sweepAngle = 104f,
                useCenter = false,
                topLeft = Offset(c.x - r * 0.92f, c.y - r * 0.92f),
                size = androidx.compose.ui.geometry.Size(r * 1.84f, r * 1.84f),
                style = Stroke(width = r * 0.09f, cap = StrokeCap.Round)
            )
        }
    }
}

/** Two-tone wordmark: "Football" (white) and "Plus" (brand green), staggered. */
@Composable
private fun SplashWordmark() {
    val rowAlpha = remember { Animatable(0f) }
    val rowOffsetDp = remember { Animatable(22f) }
    val footballAlpha = remember { Animatable(0f) }
    val plusAlpha = remember { Animatable(0f) }
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        launch { rowAlpha.animateTo(1f, tween(400, delayMillis = 520)) }
        launch { rowOffsetDp.animateTo(0f, tween(420, delayMillis = 520, easing = FastOutSlowInEasing)) }
        launch { footballAlpha.animateTo(1f, tween(380, delayMillis = 540)) }
        launch { plusAlpha.animateTo(1f, tween(380, delayMillis = 740)) }
    }

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.app_name_football),
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
            modifier = Modifier.graphicsLayer {
                alpha = rowAlpha.value * footballAlpha.value
                translationY = rowOffsetDp.value * density.density
            }
        )
        Text(
            text = stringResource(R.string.app_name_plus),
            color = DarkAccentGreen,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
            modifier = Modifier.graphicsLayer {
                alpha = rowAlpha.value * plusAlpha.value
                translationY = rowOffsetDp.value * density.density
            }
        )
    }
}

/** Thin track with a green fill that sweeps in once — a quiet "loading" cue. */
@Composable
private fun SplashLoadLine() {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(500, delayMillis = 950, easing = FastOutSlowInEasing))
    }
    Canvas(
        modifier = Modifier
            .width(148.dp)
            .height(3.dp)
            .graphicsLayer { alpha = 0.9f }
    ) {
        val track = DarkSurface
        drawRoundRect(
            color = track,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
        )
        drawRoundRect(
            color = DarkAccentGreen,
            size = androidx.compose.ui.geometry.Size(size.width * progress.value, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
        )
    }
}
