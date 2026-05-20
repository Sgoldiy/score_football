package com.example.footballapp.ui.screens.players

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.footballapp.data.model.PlayerProfileStatisticsResponse
import com.example.footballapp.data.model.PlayerStatistics
import com.example.footballapp.data.util.ApiResult
import com.example.footballapp.ui.components.FootballLogo
import com.example.footballapp.ui.components.PlayerAvatar
import com.example.footballapp.ui.components.PremiumCard
import com.example.footballapp.ui.components.ShimmerBlock
import com.example.footballapp.ui.theme.IceBlue
import com.example.footballapp.ui.theme.LiveGreen
import com.example.footballapp.ui.theme.PitchBlack
import com.example.footballapp.ui.theme.PitchSurface
import com.example.footballapp.ui.theme.PitchSurfaceHigh
import com.example.footballapp.ui.theme.TextSecondary
import com.example.footballapp.viewmodel.PlayerProfileUiState
import com.example.footballapp.viewmodel.PlayerProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProfileScreen(
    playerId: Int,
    onBackClick: () -> Unit,
    viewModel: PlayerProfileViewModel = hiltViewModel()
) {
    LaunchedEffect(playerId) { viewModel.loadPlayer(playerId) }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = PitchBlack,
        topBar = {
            TopAppBar(
                title = {
                    val name = (uiState as? PlayerProfileUiState.Success)?.player?.player?.name ?: "Player Profile"
                    Text(name, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PitchBlack,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF071B2C), PitchBlack)))
                .padding(padding)
        ) {
            when (val state = uiState) {
                is PlayerProfileUiState.Loading -> PlayerProfileShimmer()
                is PlayerProfileUiState.Error -> PlayerProfileError(state.message)
                is PlayerProfileUiState.Success -> PlayerProfileContent(state.player, state.profileData)
            }
        }
    }
}

@Composable
private fun PlayerProfileContent(
    player: PlayerProfileStatisticsResponse,
    extra: com.example.footballapp.viewmodel.PlayerProfileData
) {
    val stats = player.statistics?.firstOrNull()
    val playerInfo = player.player
    val formattedRating = stats?.games?.rating?.let { formatPlayerRating(it) } ?: "-"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeroHeader(player, stats, formattedRating)
        }

        item {
            StatGrid(stats)
        }

        if (extra.trophies.isNotEmpty()) {
            item {
                TrophiesSection(extra.trophies)
            }
        }

        if (playerInfo?.injured == true && extra.injuries.isNotEmpty()) {
            item {
                InjurySection(extra.injuries)
            }
        }

        item {
            PositionStats(stats)
        }
    }
}

@Composable
private fun HeroHeader(
    player: PlayerProfileStatisticsResponse,
    stats: PlayerStatistics?,
    formattedRating: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(28.dp))
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF0A2540), Color(0xFF0D3B66), Color(0xFF030B17))
                    )
                )
        )

        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PlayerAvatar(
                url = player.player?.photo,
                name = player.player?.name,
                modifier = Modifier.size(140.dp),
                ringColor = LiveGreen.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                player.player?.name ?: "Player",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                stats?.team?.let { team ->
                    FootballLogo(team.logo, team.name, Modifier.size(22.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(team.name ?: "", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                listOfNotNull(
                    player.player?.nationality,
                    stats?.games?.position,
                    player.player?.age?.let { "$it yrs" }
                ).joinToString(" • "),
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HeroStatBig("Games", stats?.games?.appearances?.toString() ?: "-", Color.White)
                HeroStatBig("Goals", stats?.goals?.total?.toString() ?: "-", LiveGreen)
                HeroStatBig("Assists", stats?.goals?.assists?.toString() ?: "-", IceBlue)
                HeroStatBig("Rating", formattedRating, Color(0xFFFFD700))
            }
        }
    }
}

@Composable
private fun HeroStatBig(label: String, value: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black
        )
        Text(label, color = accent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatGrid(stats: PlayerStatistics?) {
    if (stats == null) return

    PremiumCard(
        brush = Brush.linearGradient(listOf(PitchSurfaceHigh, PitchSurface))
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Season Statistics", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatSectionLabel("ATTACK")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCell("Goals", "${stats.goals?.total ?: 0}", Modifier.weight(1f), accent = LiveGreen)
                    StatCell("Assists", "${stats.goals?.assists ?: 0}", Modifier.weight(1f), accent = IceBlue)
                    StatCell("Shots/On", "${stats.shots?.total ?: 0}/${stats.shots?.on ?: 0}", Modifier.weight(1f))
                }
                
                StatSectionLabel("CREATIVITY")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCell("Key Passes", "${stats.passes?.key ?: 0}", Modifier.weight(1f))
                    StatCell("Pass Acc.", "${stats.passes?.accuracy?.let { "$it%" } ?: "0%"}", Modifier.weight(1f))
                    StatCell("Dribbles", "${stats.dribbles?.success ?: 0}", Modifier.weight(1f))
                }

                StatSectionLabel("DEFENSE")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCell("Tackles", "${stats.tackles?.total ?: 0}", Modifier.weight(1f))
                    StatCell("Interceptions", "${stats.tackles?.interceptions ?: 0}", Modifier.weight(1f))
                    StatCell("Duels Won", "${stats.duels?.won ?: 0}", Modifier.weight(1f))
                }

                StatSectionLabel("DISCIPLINE")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCell("Yellows", "${stats.cards?.yellow ?: 0}", Modifier.weight(1f), accent = Color(0xFFFFC857))
                    StatCell("Reds", "${stats.cards?.red ?: 0}", Modifier.weight(1f), accent = Color(0xFFFF4D5E))
                    StatCell("Fouls", "${stats.fouls?.committed ?: 0}", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatSectionLabel(label: String) {
    Text(
        label,
        color = TextSecondary,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = Color.White
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = accent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, maxLines = 1)
    }
}

@Composable
private fun PositionStats(stats: PlayerStatistics?) {
    if (stats == null) return
    val hasDuelStats = stats.duels?.total != null
    val hasSubStats = stats.substitutes?.`in` != null

    if (!hasDuelStats && !hasSubStats) return

    PremiumCard(
        brush = Brush.linearGradient(listOf(PitchSurfaceHigh, PitchSurface))
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Game Impact", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasDuelStats) {
                    StatCell("Duels Won", "${stats.duels?.won ?: "-"}", Modifier.weight(1f))
                    StatCell("Duels Total", "${stats.duels?.total ?: "-"}", Modifier.weight(1f))
                }
                if (hasSubStats) {
                    StatCell("Sub In", "${stats.substitutes?.`in` ?: "-"}", Modifier.weight(1f))
                    StatCell("Sub Out", "${stats.substitutes?.out ?: "-"}", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TrophiesSection(trophies: List<com.example.footballapp.data.model.PlayerTrophy>) {
    PremiumCard(
        brush = Brush.linearGradient(listOf(PitchSurfaceHigh, PitchSurface))
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Honours", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                trophies.take(12).forEach { trophy ->
                    TrophyBadge(trophy)
                }
            }
        }
    }
}

@Composable
private fun TrophyBadge(trophy: com.example.footballapp.data.model.PlayerTrophy) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFD700).copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            trophy.place ?: "🏆",
            color = Color(0xFFFFD700),
            fontSize = 24.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            trophy.league ?: "",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            trophy.season ?: "",
            color = TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun InjurySection(injuries: List<com.example.footballapp.data.model.Injury>) {
    PremiumCard(
        brush = Brush.linearGradient(listOf(Color(0xFF3D0A0A), PitchSurface))
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Injury History", color = DangerRed, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
            injuries.forEach { injury ->
                Text(
                    "${injury.fixture?.date?.take(10) ?: ""} • ${injury.player?.reason ?: "Unknown"}",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun formatPlayerRating(rating: String): String {
    val floatVal = rating.toFloatOrNull() ?: return rating
    return "%.2f".format(floatVal)
}

private val DangerRed = Color(0xFFFF4D5E)

@Composable
private fun PlayerProfileShimmer() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ShimmerBlock(Modifier.fillMaxWidth().height(320.dp), RoundedCornerShape(28.dp)) }
        item { ShimmerBlock(Modifier.fillMaxWidth().height(200.dp), RoundedCornerShape(24.dp)) }
    }
}

@Composable
private fun PlayerProfileError(message: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Player data unavailable",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
