package com.example.footballapp.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.footballapp.ui.theme.IceBlue
import com.example.footballapp.ui.theme.LiveGreen
import com.example.footballapp.ui.theme.PitchLine
import com.example.footballapp.ui.theme.PitchSurface
import com.example.footballapp.ui.theme.PitchSurfaceHigh
import com.example.footballapp.ui.theme.TextSecondary

@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    brush: Brush = Brush.linearGradient(
        listOf(PitchSurfaceHigh, PitchSurface, Color(0xFF09110E))
    ),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onClick() }
    } else Modifier

    Surface(
        modifier = modifier.then(clickModifier),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        shadowElevation = 10.dp
    ) {
        Box(
            modifier = Modifier
                .background(brush)
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
fun LivePulse(
    modifier: Modifier = Modifier,
    color: Color = LiveGreen
) {
    val transition = rememberInfiniteTransition(label = "live-pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(780), RepeatMode.Reverse),
        label = "live-pulse-alpha"
    )
    Box(
        modifier = modifier
            .size(9.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

@Composable
fun ShimmerBlock(
    modifier: Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(18.dp)
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "shimmer-alpha"
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.05f),
                        Color.White.copy(alpha = 0.12f * alpha),
                        Color.White.copy(alpha = 0.04f)
                    )
                )
            )
    )
}

@Composable
fun SectionTitle(
    title: String,
    trailing: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = LiveGreen
            )
        }
    }
}

@Composable
fun TeamCrestName(
    name: String,
    logo: String?,
    modifier: Modifier = Modifier,
    crestSize: Int = 50
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(crestSize.dp)
                .shadow(18.dp, CircleShape, ambientColor = LiveGreen.copy(alpha = 0.18f), spotColor = LiveGreen.copy(alpha = 0.18f))
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = logo,
                contentDescription = name,
                modifier = Modifier.size((crestSize - 12).dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = name.ifBlank { "Team" },
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun FootballLogo(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    glow: Color = LiveGreen,
    contentScale: ContentScale = ContentScale.Fit
) {
    Box(
        modifier = modifier
            .shadow(16.dp, CircleShape, ambientColor = glow.copy(alpha = 0.18f), spotColor = glow.copy(alpha = 0.18f))
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.1f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNullOrBlank()) {
            Text("FC", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
        } else {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentScale = contentScale
            )
        }
    }
}

@Composable
fun PlayerAvatar(
    url: String?,
    name: String?,
    modifier: Modifier = Modifier,
    ringColor: Color = Color.White.copy(alpha = 0.18f),
    onClick: (() -> Unit)? = null
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onClick() }
    } else Modifier

    Box(
        modifier = modifier.then(clickModifier)
            .clip(CircleShape)
            .background(PitchSurfaceHigh)
            .border(1.dp, ringColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNullOrBlank()) {
            Text(
                text = name.orEmpty().split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifBlank { "P" },
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelMedium
            )
        } else {
            AsyncImage(
                model = url,
                contentDescription = name,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun InfoPill(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color = LiveGreen
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(100.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text, color = accent, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun StatComparisonBar(
    label: String,
    home: Float,
    away: Float,
    homeText: String,
    awayText: String,
    modifier: Modifier = Modifier
) {
    val total = (home + away).takeIf { it > 0f } ?: 1f
    val homeWeight = (home / total).coerceIn(0.05f, 0.95f)
    val awayWeight = (away / total).coerceIn(0.05f, 0.95f)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                homeText,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                label.uppercase(),
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
            Text(
                awayText,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Color.White.copy(alpha = 0.05f))
        ) {
            Box(
                Modifier
                    .weight(homeWeight)
                    .height(6.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(LiveGreen.copy(alpha = 0.7f), LiveGreen)
                        )
                    )
            )
            Spacer(Modifier.width(2.dp))
            Box(
                Modifier
                    .weight(awayWeight)
                    .height(6.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(IceBlue, IceBlue.copy(alpha = 0.7f))
                        )
                    )
            )
        }
    }
}
