package com.example.footballapp.ui.screens.leagues

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.footballapp.data.model.LeagueResponse
import com.example.footballapp.data.util.ApiResult
import com.example.footballapp.ui.components.FootballLogo
import com.example.footballapp.ui.components.InfoPill
import com.example.footballapp.ui.components.PremiumCard
import com.example.footballapp.ui.components.ShimmerBlock
import com.example.footballapp.ui.theme.IceBlue
import com.example.footballapp.ui.theme.LiveGreen
import com.example.footballapp.ui.theme.PitchBlack
import com.example.footballapp.ui.theme.PitchSurface
import com.example.footballapp.ui.theme.PitchSurfaceHigh
import com.example.footballapp.ui.theme.TextSecondary
import com.example.footballapp.viewmodel.LeaguesData
import com.example.footballapp.viewmodel.LeaguesViewModel

@Composable
fun LeaguesScreen(
    onBackClick: () -> Unit,
    viewModel: LeaguesViewModel = hiltViewModel()
) {
    val state by viewModel.leaguesState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A192F), PitchBlack)))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                LeagueHero()
            }

            when (val current = state) {
                ApiResult.Loading -> {
                    items(6) {
                        Box(Modifier.padding(horizontal = 16.dp)) {
                            ShimmerBlock(Modifier.fillMaxWidth().height(100.dp))
                        }
                    }
                }
                is ApiResult.Error -> {
                    item {
                        Box(Modifier.padding(16.dp)) {
                            PremiumCard(Modifier.fillMaxWidth()) {
                                Text(current.message, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                is ApiResult.Success -> {
                    item {
                        CoverageSummary(current.data)
                    }
                    items(current.data.leagues.take(80), key = { it.league?.id ?: it.hashCode() }) { league ->
                        Box(Modifier.padding(horizontal = 16.dp)) {
                            LeagueRow(league)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeagueHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF003366), Color(0xFF0A192F))
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                "Leagues",
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                "Explore world football competitions",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun CoverageSummary(data: LeaguesData) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryMetricCard("Leagues", data.leagues.size.toString(), Modifier.weight(1f))
        SummaryMetricCard("Countries", data.countries.size.toString(), Modifier.weight(1f))
        SummaryMetricCard("Active", data.leagues.count { it.seasons.orEmpty().any { s -> s.current == true } }.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun SummaryMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    PremiumCard(
        modifier = modifier,
        brush = Brush.linearGradient(listOf(PitchSurfaceHigh, PitchSurface))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LeagueRow(response: LeagueResponse) {
    val league = response.league
    val currentSeason = response.seasons.orEmpty().firstOrNull { it.current == true } ?: response.seasons.orEmpty().maxByOrNull { it.year }
    
    PremiumCard(
        modifier = Modifier.fillMaxWidth(),
        brush = Brush.linearGradient(listOf(PitchSurfaceHigh.copy(alpha = 0.5f), PitchSurface.copy(alpha = 0.5f)))
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(12.dp)
            ) {
                FootballLogo(
                    url = league?.logo,
                    contentDescription = league?.name,
                    modifier = Modifier.fillMaxSize(),
                    glow = IceBlue.copy(alpha = 0.2f)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    league?.name ?: "Competition",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    listOfNotNull(response.country?.name, currentSeason?.year?.toString()).joinToString(" • "),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val coverage = currentSeason?.coverage
                    if (coverage?.standings == true) CoverageBadge("TABLE", LiveGreen)
                    if (coverage?.fixtures?.events == true) CoverageBadge("EVENTS", IceBlue)
                    if (coverage?.players == true) CoverageBadge("DATA", Color.White)
                }
            }
        }
    }
}

@Composable
private fun CoverageBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text,
            color = color,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black
        )
    }
}
