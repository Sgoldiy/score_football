package com.example.footballapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.footballapp.domain.model.Match
import com.example.footballapp.ui.theme.LiveGreen

@Composable
fun MatchRow(
    match: Match,
    onClick: (String) -> Unit
) {
    val isLive = match.isLive
    val isFinished = match.status.short == "FT"
    
    val homeScore = match.homeScore ?: 0
    val awayScore = match.awayScore ?: 0
    val homeWon = isFinished && homeScore > awayScore
    val awayWon = isFinished && awayScore > homeScore
    val isDraw = isFinished && homeScore == awayScore

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(match.id.toString()) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time or Status
        Column(
            modifier = Modifier.width(45.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLive) {
                LivePulse()
                Text(
                    text = "${match.elapsed ?: ""}'",
                    style = MaterialTheme.typography.labelSmall,
                    color = LiveGreen,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = match.status.short,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
        
        Spacer(Modifier.width(12.dp))
        
        // Teams and Score
        Column(modifier = Modifier.weight(1f)) {
            TeamRow(
                name = match.homeTeam.name,
                logo = match.homeTeam.logo,
                score = match.homeScore,
                isWinner = homeWon,
                isLoser = isFinished && !isDraw && awayWon
            )
            Spacer(Modifier.height(6.dp))
            TeamRow(
                name = match.awayTeam.name,
                logo = match.awayTeam.logo,
                score = match.awayScore,
                isWinner = awayWon,
                isLoser = isFinished && !isDraw && homeWon
            )
        }
        
        Spacer(Modifier.width(12.dp))
        
        // Chevron clickability indicator
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = "Details",
            tint = Color.White.copy(alpha = 0.25f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun TeamRow(
    name: String,
    logo: String?,
    score: Int?,
    isWinner: Boolean,
    isLoser: Boolean
) {
    val opacity = if (isLoser) 0.45f else 1.0f
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = logo,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer { alpha = opacity }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                color = if (isWinner) Color.White else Color.White.copy(alpha = opacity * 0.85f)
            )
        }
        Text(
            text = score?.toString() ?: "-",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isWinner) FontWeight.ExtraBold else FontWeight.Bold,
            color = if (isWinner) LiveGreen else Color.White.copy(alpha = opacity)
        )
    }
}
