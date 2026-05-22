package com.example.footballapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                isWinner = match.homeTeam.winner == true
            )
            Spacer(Modifier.height(4.dp))
            TeamRow(
                name = match.awayTeam.name,
                logo = match.awayTeam.logo,
                score = match.awayScore,
                isWinner = match.awayTeam.winner == true
            )
        }
    }
}

@Composable
private fun TeamRow(name: String, logo: String?, score: Int?, isWinner: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = logo,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                color = if (isWinner) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
        Text(
            text = score?.toString() ?: "-",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (isWinner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
