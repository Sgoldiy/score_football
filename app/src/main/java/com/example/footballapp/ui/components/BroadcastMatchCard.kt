package com.example.footballapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.footballapp.domain.model.Match
import com.example.footballapp.domain.model.MatchEvent
import com.example.footballapp.ui.theme.*

@Composable
fun BroadcastMatchCard(
    match: Match,
    modifier: Modifier = Modifier,
    expandedByDefault: Boolean = false,
    onClick: () -> Unit
) {
    var expanded by remember(match.id) { mutableStateOf(expandedByDefault) }
    val isLive = match.isLive
    val scoreScale by animateFloatAsState(
        targetValue = if (isLive) 1.04f else 1f,
        animationSpec = tween(600),
        label = "score-scale"
    )

    PremiumCard(
        modifier = modifier
            .animateContentSize()
            .clickable(onClick = onClick),
        brush = Brush.linearGradient(
            listOf(
                if (isLive) Color(0xFF0E3B29) else Color(0xFF0A1E38),
                PitchSurfaceHigh,
                PitchBlack
            )
        )
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MatchHeader(match, isLive)
            ScoreRow(match, isLive, scoreScale)
            BottomActionRow(match)
        }
    }
}

@Composable
private fun MatchHeader(match: Match, isLive: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            FootballLogo(
                match.league.logo,
                match.league.name,
                Modifier.size(26.dp),
                glow = if (isLive) LiveGreen else IceBlue
            )
            Spacer(Modifier.width(8.dp))
            Text(
                match.league.name,
                color = Color.White.copy(alpha = 0.88f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isLive) {
                LivePulse(color = LiveGreen)
                Spacer(Modifier.width(5.dp))
            }
            Text(
                if (isLive) "${match.elapsed ?: ""}'" else match.status.short,
                color = if (isLive) LiveGreen else Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black
            )
            if (isLive) {
                Box(
                    modifier = Modifier
                        .padding(start = 5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(LiveGreen)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text("LIVE", color = PitchBlack, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun ScoreRow(match: Match, isLive: Boolean, scale: Float) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            TeamScoreBlock(
                name = match.homeTeam.name,
                logo = match.homeTeam.logo,
                alignStart = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Box(
            modifier = Modifier
                .graphicsLayer { scaleX = scale; scaleY = scale },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        Color.White.copy(alpha = 0.06f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${match.homeScore ?: "-"}",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = " : ",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 3.dp)
                )
                Text(
                    text = "${match.awayScore ?: "-"}",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            TeamScoreBlock(
                name = match.awayTeam.name,
                logo = match.awayTeam.logo,
                alignStart = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TeamScoreBlock(
    name: String,
    logo: String?,
    alignStart: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = if (alignStart) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!alignStart) {
            Text(
                name,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
        }
        FootballLogo(logo, name, Modifier.size(40.dp), glow = LiveGreen.copy(alpha = 0.3f))
        if (alignStart) {
            Spacer(Modifier.width(8.dp))
            Text(
                name,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomActionRow(
    match: Match
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (match.isLive) "Live action in progress" else "Match Center",
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
