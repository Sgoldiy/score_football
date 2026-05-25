package com.example.footballapp.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.footballapp.data.util.ApiResult
import com.example.footballapp.domain.model.LeagueInfo
import com.example.footballapp.domain.model.Match
import com.example.footballapp.ui.components.LiveBadge
import com.example.footballapp.ui.theme.*
import com.example.footballapp.viewmodel.HomeViewModel
import com.example.footballapp.viewmodel.HomeUiState

@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToFavourites: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToMatchCenter: (String) -> Unit,
    onNavigateToLeagues: () -> Unit,
    onNavigateToPlayerProfile: (Int) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is HomeUiState.Loading -> HomeLoadingShimmer()
        is HomeUiState.Error -> HomeError(state.message) { viewModel.loadHomeData() }
        is HomeUiState.Success -> HomeContent(
            state = state,
            onSearch = onNavigateToSearch,
            onFavourites = onNavigateToFavourites,
            onNotifications = onNavigateToNotifications,
            onMatchClick = onNavigateToMatchCenter,
            onExplorePlayers = { onNavigateToPlayerProfile(1) }
        )
    }
}

@Composable
private fun HomeLoadingShimmer() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { ShimmerBox(Modifier.fillMaxWidth().height(44.dp), 12.dp) }
        item { ShimmerBox(Modifier.fillMaxWidth().height(220.dp), 24.dp) }
        item { ShimmerBox(Modifier.fillMaxWidth(0.4f).height(20.dp), 8.dp) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(5) { ShimmerBox(Modifier.width(100.dp).height(40.dp), 50.dp) }
            }
        }
        item { ShimmerBox(Modifier.fillMaxWidth(0.5f).height(20.dp), 8.dp) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) { ShimmerBox(Modifier.width(140.dp).height(180.dp), 20.dp) }
            }
        }
    }
}

@Composable
private fun HomeError(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(DeepNavy),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = DangerRed, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = GlassGlowGreen, contentColor = DeepNavy)) {
                Text("Retry", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Success,
    onSearch: () -> Unit,
    onFavourites: () -> Unit,
    onNotifications: () -> Unit,
    onMatchClick: (String) -> Unit,
    onExplorePlayers: () -> Unit
) {
    val heroMatch = if (state.isLive && state.liveMatches.isNotEmpty()) {
        state.liveMatches.first()
    } else if (state.finishedMatches.isNotEmpty()) {
        state.finishedMatches.first()
    } else null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item { HomeHeader(onSearch, onFavourites, onNotifications) }

        if (heroMatch != null) {
            item { Spacer(Modifier.height(8.dp)) }
            item {
                HeroMatchCard(
                    match = heroMatch,
                    isLive = heroMatch.isLive,
                    onClick = { onMatchClick(heroMatch.id.toString()) }
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }

        item {
            SectionLabel("Top Leagues")
            Spacer(Modifier.height(12.dp))
            TopLeaguesRow(state.topLeagues.take(5), onNavigateToLeagues = {})
        }

        item { Spacer(Modifier.height(28.dp)) }

        item {
            SectionLabel("Players to Watch", trailing = "Explore", onTrailingClick = onExplorePlayers)
            Spacer(Modifier.height(12.dp))
            PlayersToWatchRow(onPlayerClick = {})
        }
    }
}

// ─── HEADER ─────────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader(
    onSearch: () -> Unit,
    onFavourites: () -> Unit,
    onNotifications: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Football Plus",
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                letterSpacing = 0.5.sp,
                color = Color.White
            )
            Text(
                text = "Live scores, stats, lineups",
                fontFamily = FontFamily.Default,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassIcon(Icons.Rounded.Search, onSearch)
            GlassIcon(Icons.Rounded.FavoriteBorder, onFavourites)
            GlassIcon(Icons.Rounded.Notifications, onNotifications)
        }
    }
}

@Composable
private fun GlassIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
    }
}

// ─── SECTION LABEL ──────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(title: String, trailing: String? = null, onTrailingClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            letterSpacing = 0.3.sp,
            color = Color.White
        )
        if (trailing != null) {
            Text(
                text = trailing,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = GlassGlowGreen.copy(alpha = 0.7f),
                modifier = Modifier.clickable { onTrailingClick?.invoke() }
            )
        }
    }
}

// ─── HERO MATCH CARD ───────────────────────────────────────────────────────

@Composable
private fun HeroMatchCard(
    match: Match,
    isLive: Boolean,
    onClick: () -> Unit
) {
    val glowColor = if (isLive) GlassGlowGreen else GlassGlowDim
    val scoreColor = if (isLive) ScoreGreen else ScoreDim

    val infiniteTransition = rememberInfiniteTransition(label = "hero-glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = if (isLive) 1f else 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero-glow-alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.07f),
                        Color.White.copy(alpha = 0.02f)
                    )
                )
            )
            .border(1.5.dp, glowColor.copy(alpha = glowAlpha * 0.4f), RoundedCornerShape(28.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Competition row
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = match.league.logo,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp).clip(CircleShape)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = match.league.name.uppercase(),
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(Modifier.weight(1f))
                if (isLive) {
                    LiveBadge(minute = "${match.elapsed ?: 0}")
                } else {
                    Text(
                        text = match.status.short,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.35f)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Scoreboard
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TeamScoreColumn(name = match.homeTeam.name, logo = match.homeTeam.logo, alignStart = true)
                ScoreDisplay(
                    home = match.homeScore ?: 0,
                    away = match.awayScore ?: 0,
                    isLive = isLive,
                    color = scoreColor
                )
                TeamScoreColumn(name = match.awayTeam.name, logo = match.awayTeam.logo, alignStart = false)
            }

            Spacer(Modifier.height(16.dp))

            // Status line
            Text(
                text = if (isLive) "LIVE \u2022 ${match.elapsed ?: 0}\u2019" else match.status.long.uppercase(),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Medium,
                color = glowColor.copy(alpha = glowAlpha)
            )
        }
    }
}

@Composable
private fun TeamScoreColumn(name: String, logo: String?, alignStart: Boolean) {
    Column(
        horizontalAlignment = if (alignStart) Alignment.Start else Alignment.End,
        modifier = Modifier.width(IntrinsicSize.Min)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = logo,
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = name,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.8f),
            maxLines = 1,
            modifier = Modifier.width(IntrinsicSize.Max)
        )
    }
}

@Composable
private fun ScoreDisplay(home: Int, away: Int, isLive: Boolean, color: Color) {
    val pulseAlpha by if (isLive) {
        val t = rememberInfiniteTransition(label = "score-pulse")
        t.animateFloat(0.6f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "score-pulse-a")
    } else {
        remember { mutableStateOf(1f) }
    }

    Text(
        text = "${home} : ${away}",
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Black,
        fontSize = 42.sp,
        letterSpacing = 3.sp,
        color = color.copy(alpha = pulseAlpha),
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

// ─── TOP LEAGUES ───────────────────────────────────────────────────────────

@Composable
private fun TopLeaguesRow(leagues: List<LeagueInfo>, onNavigateToLeagues: () -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(leagues) { league ->
            LeaguePill(league)
        }
    }
}

@Composable
private fun LeaguePill(league: LeagueInfo) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(50.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = league.logo,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = league.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

// ─── PLAYERS TO WATCH ──────────────────────────────────────────────────────

@Composable
private fun PlayersToWatchRow(onPlayerClick: (Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(players) { player ->
            PlayerCard(player = player, onClick = { onPlayerClick(player.id) })
        }
    }
}

private data class PlayerDisplay(val id: Int, val name: String, val team: String, val goals: Int, val assists: Int, val rating: String)

private val players = listOf(
    PlayerDisplay(1, "Erling Haaland", "Manchester City", 25, 5, "8.2"),
    PlayerDisplay(2, "Mohamed Salah", "Liverpool", 19, 10, "7.9"),
    PlayerDisplay(3, "Kylian Mbapp\u00e9", "Real Madrid", 22, 8, "8.1")
)

@Composable
private fun PlayerCard(player: PlayerDisplay, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.02f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = player.name.split(" ").map { it.first() }.joinToString(""),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = player.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = player.team,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.4f),
                maxLines = 1
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PlayerStat("G", player.goals.toString(), GlassGlowGreen)
                PlayerStat("A", player.assists.toString(), GlassGlowGreen.copy(alpha = 0.7f))
                PlayerStat("R", player.rating, Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun PlayerStat(label: String, value: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = accent
        )
        Text(
            text = label,
            fontSize = 9.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.35f)
        )
    }
}

// ─── SHIMMER BOX ───────────────────────────────────────────────────────────

@Composable
private fun ShimmerBox(modifier: Modifier = Modifier, cornerRadius: androidx.compose.ui.unit.Dp = 8.dp) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.06f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "shimmer-alpha"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.White.copy(alpha = alpha))
    )
}
