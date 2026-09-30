package com.footballpluse.footballapp.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import kotlin.random.Random

/**
 * The single branded splash. Everything animates continuously so the screen
 * always feels alive: a bouncing, spinning football with squash-and-stretch
 * and a rotating dashed orbit ring, drifting sparkles, a shimmering two-tone
 * wordmark ("Football" + "Plus") and a sweeping green load line. Shown while
 * [SplashViewModel] resolves the start destination (2 s minimum display).
 */
@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy),
        contentAlignment = Alignment.Center
    ) {
        SparkleField()

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BouncingFootball()
            Spacer(Modifier.height(34.dp))
            SplashWordmark()
            Spacer(Modifier.height(24.dp))
            SplashLoadLine()
        }
    }
}

// ─── Ball ─────────────────────────────────────────────────────────────────────

/** Bounce baseline (above center) and the ball's rest height. */
private const val BOUNCE_DROP_DP = 46f
private const val BALL_SIZE_DP = 84

/**
 * Football that drops in with a bounce, then keeps bouncing in place with
 * squash-and-stretch, continuous spin, and a rotating dashed orbit ring.
 */
@Composable
private fun BouncingFootball() {
    val drop = remember { Animatable(-320f) }          // px offset from above
    val bounce = remember { Animatable(0f) }           // -1..1 phase, drives height + squash
    val spin = remember { Animatable(0f) }             // continuous degrees
    val dropSquash = remember { Animatable(1f) }       // 1 = round; <1 wide, >1 tall on impact

    val pulse = rememberInfiniteTransition(label = "glow")
    val glowAlpha by pulse.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.26f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse),
        label = "glowAlpha"
    )

    LaunchedEffect(Unit) {
        launch {
            // Entrance: fall with ease-in, squash on "landing", then hand over
            // to the endless bounce loop.
            drop.animateTo(0f, tween(420, easing = CubicBezierEasing(0.55f, 0f, 1f, 0.45f)))
            dropSquash.animateTo(0.72f, tween(90, easing = FastOutSlowInEasing))
            dropSquash.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMediumLow))
            bounce.animateTo(
                1f,
                tween(620, easing = CubicBezierEasing(0.28f, 0f, 0.6f, 1f))
            )
            while (true) {
                bounce.animateTo(
                    -1f,
                    tween(560, easing = CubicBezierEasing(0.35f, 0f, 0.65f, 1f))
                )
                bounce.animateTo(
                    1f,
                    tween(560, easing = CubicBezierEasing(0.35f, 0f, 0.65f, 1f))
                )
            }
        }
        launch {
            // Spin: fast at first (settling rotation), then a steady roll.
            spin.animateTo(360f, tween(900, easing = FastOutSlowInEasing))
            while (true) {
                spin.animateTo(spin.value + 360f, tween(2400, easing = LinearEasing))
            }
        }
    }

    val density = LocalDensity.current
    val dropPx = with(density) { BOUNCE_DROP_DP.dp.toPx() }

    Box(contentAlignment = Alignment.BottomCenter) {
        // Pulsing glow under the ball
        Canvas(
            modifier = Modifier
                .size(150.dp)
        ) {
            drawCircle(color = DarkAccentGreen.copy(alpha = glowAlpha * 0.35f), radius = size.minDimension / 2f)
        }
        // Dashed orbit ring rotating opposite the spin
        val orbit = rememberInfiniteTransition(label = "orbit")
        val orbitAngle by orbit.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing)),
            label = "orbitAngle"
        )
        Canvas(
            modifier = Modifier
                .size(124.dp)
                .graphicsLayer { rotationZ = orbitAngle }
        ) {
            val r = size.minDimension / 2f
            val seg = 13f
            var angle = 0f
            while (angle < 360f) {
                drawArc(
                    color = DarkAccentGreen.copy(alpha = 0.5f),
                    startAngle = angle,
                    sweepAngle = seg,
                    useCenter = false,
                    topLeft = Offset(r * 0.06f, r * 0.06f),
                    size = Size(size.width * 0.88f, size.height * 0.88f),
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )
                angle += seg + 16f
            }
        }
        // The ball
        Canvas(
            modifier = Modifier
                .size(BALL_SIZE_DP.dp)
                .graphicsLayer {
                    // Flight height: rise as |bounce| grows, squash at the ground (b = 0).
                    translationY = drop.value - (bounce.value * bounce.value) * dropPx * 0.42f
                    val b = bounce.value
                    val squash = 1f + 0.14f * (1f - kotlin.math.abs(b)) * dropSquash.value
                    scaleX = squash
                    scaleY = 2f - squash
                    rotationZ = spin.value % 360f
                }
        ) {
            drawFootball(size.minDimension / 2f)
        }
    }
}

// ─── Wordmark ────────────────────────────────────────────────────────────────

/** Two-tone wordmark with staggered rise-in and a moving light glint. */
@Composable
private fun SplashWordmark() {
    val footballAlpha = remember { Animatable(0f) }
    val plusAlpha = remember { Animatable(0f) }
    val rise = remember { Animatable(26f) }
    val density = LocalDensity.current

    // Shimmer glint sweeping under the text, on a loop.
    val glint = rememberInfiniteTransition(label = "glint")
    val glintX by glint.animateFloat(
        initialValue = -1.2f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            tween(1900, delayMillis = 700, easing = LinearEasing)
        ),
        label = "glintX"
    )

    LaunchedEffect(Unit) {
        launch { footballAlpha.animateTo(1f, tween(360, delayMillis = 300)) }
        launch { plusAlpha.animateTo(1f, tween(360, delayMillis = 480)) }
        launch { rise.animateTo(0f, tween(430, delayMillis = 300, easing = FastOutSlowInEasing)) }
    }

    Box {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.app_name_football),
                color = Color.White,
                fontSize = 31.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = footballAlpha.value
                    translationY = rise.value * density.density
                }
            )
            Text(
                text = stringResource(R.string.app_name_plus),
                color = DarkAccentGreen,
                fontSize = 31.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = plusAlpha.value
                    translationY = rise.value * density.density
                }
            )
        }
        // Glint bar sweeping beneath the wordmark
        Canvas(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .width(190.dp)
                .height(3.dp)
                .graphicsLayer { alpha = 0.85f }
        ) {
            drawRoundRect(color = DarkSurface, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f))
            val glintW = size.width * 0.32f
            val x = glintX * size.width
            drawRoundRect(
                color = DarkAccentGreen,
                topLeft = Offset(x, 0f),
                size = Size(glintW, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
            )
        }
    }
}

// ─── Sparkles ────────────────────────────────────────────────────────────────

/**
 * A fixed field of drifting green/white specks behind the content. Seeded
 * once so the pattern is stable across recompositions.
 */
@Composable
private fun SparkleField() {
    val rng = remember { Random(20260930) }
    val sparkles = remember {
        List(26) {
            Sparkle(
                x = rng.nextFloat(),
                y = rng.nextFloat(),
                radius = 1.4f + rng.nextFloat() * 2.6f,
                phase = rng.nextFloat() * 2f,
                speed = 0.35f + rng.nextFloat() * 0.5f,
                green = rng.nextBoolean()
            )
        }
    }
    val t = rememberInfiniteTransition(label = "sparkles")
    val time by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Reverse),
        label = "time"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        sparkles.forEach { s ->
            val cycle = (time * s.speed + s.phase) % 2f
            val alpha = (1f - kotlin.math.abs(cycle - 1f)).coerceIn(0f, 1f) * 0.45f
            val drift = (cycle - 1f) * size.height * 0.035f
            drawCircle(
                color = (if (s.green) DarkAccentGreen else Color.White).copy(alpha = alpha),
                radius = s.radius,
                center = Offset(s.x * size.width, (s.y + drift / size.height) * size.height)
            )
        }
    }
}

private data class Sparkle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val phase: Float,
    val speed: Float,
    val green: Boolean
)

// ─── Load line ───────────────────────────────────────────────────────────────

/** Green fill sweeping across a thin track, once, right after the wordmark. */
@Composable
private fun SplashLoadLine() {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(600, delayMillis = 700, easing = FastOutSlowInEasing))
    }
    Canvas(
        modifier = Modifier
            .width(150.dp)
            .height(3.dp)
            .graphicsLayer { alpha = 0.9f }
    ) {
        drawRoundRect(color = DarkSurface, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f))
        drawRoundRect(
            color = DarkAccentGreen,
            size = Size(size.width * progress.value, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
        )
    }
}

// ─── Football drawing (shared) ───────────────────────────────────────────────

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
