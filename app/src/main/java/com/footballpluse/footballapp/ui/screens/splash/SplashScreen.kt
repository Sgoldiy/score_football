package com.footballpluse.footballapp.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
 * Simple, modern brand intro: the football logo scales in softly with one
 * expanding ring, the "Football Plus" wordmark fades up beneath it, and a
 * thin green load line sweeps once. Nothing loops — one clean entrance.
 * Shown while [SplashViewModel] resolves the start destination.
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
            IntroLogo()
            Spacer(Modifier.height(30.dp))
            IntroWordmark()
            Spacer(Modifier.height(26.dp))
            IntroLoadLine()
        }
    }
}

/** Football logo: soft scale-in + fade, with a single ring pulse expanding outward. */
@Composable
private fun IntroLogo() {
    val logoScale = remember { Animatable(0.6f) }
    val logoAlpha = remember { Animatable(0f) }
    val ring = remember { Animatable(0f) }      // 0..1 expansion of the pulse ring
    val ringAlpha = remember { Animatable(0.45f) }

    LaunchedEffect(Unit) {
        launch {
            logoScale.animateTo(
                1f,
                spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)
            )
        }
        launch { logoAlpha.animateTo(1f, tween(350, easing = FastOutSlowInEasing)) }
        launch {
            ring.animateTo(1f, tween(950, delayMillis = 150, easing = FastOutSlowInEasing))
        }
        launch {
            ringAlpha.animateTo(0f, tween(950, delayMillis = 150, easing = LinearEasing))
        }
    }

    Box(contentAlignment = Alignment.Center) {
        // One-shot expanding ring behind the logo
        Canvas(
            modifier = Modifier
                .size(150.dp)
                .graphicsLayer {
                    val s = 0.55f + 0.45f * ring.value
                    scaleX = s
                    scaleY = s
                    alpha = ringAlpha.value
                }
        ) {
            drawCircle(
                color = DarkAccentGreen,
                radius = size.minDimension / 2f,
                style = Stroke(width = 2.dp.toPx())
            )
        }
        // The logo
        Canvas(
            modifier = Modifier
                .size(88.dp)
                .graphicsLayer {
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                    alpha = logoAlpha.value
                }
        ) {
            drawFootball(size.minDimension / 2f)
        }
    }
}

/** Two-tone wordmark fading up right after the logo. */
@Composable
private fun IntroWordmark() {
    val textAlpha = remember { Animatable(0f) }
    val rise = remember { Animatable(14f) }
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        launch { textAlpha.animateTo(1f, tween(400, delayMillis = 250, easing = FastOutSlowInEasing)) }
        launch { rise.animateTo(0f, tween(420, delayMillis = 250, easing = FastOutSlowInEasing)) }
    }

    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = stringResource(R.string.app_name_football),
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
            modifier = Modifier.graphicsLayer {
                alpha = textAlpha.value
                translationY = rise.value * density.density
            }
        )
        Text(
            text = stringResource(R.string.app_name_plus),
            color = DarkAccentGreen,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
            modifier = Modifier.graphicsLayer {
                alpha = textAlpha.value
                translationY = rise.value * density.density
            }
        )
    }
}

/** Thin track with a green fill sweeping in once. */
@Composable
private fun IntroLoadLine() {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(650, delayMillis = 550, easing = FastOutSlowInEasing))
    }
    Canvas(
        modifier = Modifier
            .width(150.dp)
            .height(3.dp)
    ) {
        drawRoundRect(color = DarkSurface, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f))
        drawRoundRect(
            color = DarkAccentGreen,
            size = Size(size.width * progress.value, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
        )
    }
}

// ─── Football drawing ────────────────────────────────────────────────────────

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFootball(r: Float) {
    val c = Offset(size.width / 2f, size.height / 2f)

    // Body
    drawCircle(color = Color(0xFFF4F6F8), radius = r, center = c)
    drawCircle(color = Color(0xFFD9DEE5), radius = r, center = c, style = Stroke(width = r * 0.05f))

    // Center pentagon
    val pr = r * 0.37f
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

    // Spokes
    pentagon.forEach { p ->
        val dx = p.x - c.x
        val dy = p.y - c.y
        val len = kotlin.math.hypot(dx.toDouble(), dy.toDouble())
        drawLine(
            color = Color(0xFF8B949E),
            start = p,
            end = Offset(
                c.x + (dx / len * (r * 0.96f)).toFloat(),
                c.y + (dy / len * (r * 0.96f)).toFloat()
            ),
            strokeWidth = r * 0.055f,
            cap = StrokeCap.Round
        )
    }

    // Brand accent arc
    drawArc(
        color = DarkAccentGreen,
        startAngle = 128f,
        sweepAngle = 104f,
        useCenter = false,
        topLeft = Offset(c.x - r * 0.92f, c.y - r * 0.92f),
        size = Size(r * 1.84f, r * 1.84f),
        style = Stroke(width = r * 0.09f, cap = StrokeCap.Round)
    )
}
