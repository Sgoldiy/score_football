package com.example.footballapp.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.footballapp.data.model.PlayerProfileStatisticsResponse
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
    onNavigateToLeagueDetail: ((Int) -> Unit)? = null,
    onNavigateToPlayerProfile: (Int) -> Unit,
    onNavigateToTopPlayers: () -> Unit = {},
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
            onExplorePlayers = onNavigateToTopPlayers,
            onPlayerClick = onNavigateToPlayerProfile,
            onNavigateToLeagues = onNavigateToLeagues,
            onNavigateToLeagueDetail = onNavigateToLeagueDetail
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
    onExplorePlayers: () -> Unit,
    onPlayerClick: (Int) -> Unit,
    onNavigateToLeagues: () -> Unit,
    onNavigateToLeagueDetail: ((Int) -> Unit)? = null
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item { HomeHeader(onSearch, onFavourites, onNotifications) }

        // Live Matches Horizontal Rail
        if (state.liveMatches.isNotEmpty()) {
            item {
                LiveMatchesRail(liveMatches = state.liveMatches, onMatchClick = onMatchClick)
                Spacer(Modifier.height(24.dp))
            }
        }

        item {
            SectionLabel("Top Leagues", trailing = "All Leagues", onTrailingClick = onNavigateToLeagues)
            Spacer(Modifier.height(12.dp))
            TopLeaguesRow(
                leagues = state.topLeagues.take(8),
                onLeagueClick = { league ->
                    if (onNavigateToLeagueDetail != null) {
                        onNavigateToLeagueDetail(league.id)
                    } else {
                        onNavigateToLeagues()
                    }
                }
            )
        }

        item { Spacer(Modifier.height(28.dp)) }

        // Today's Featured Matches
        val todayMatches = (state.upcomingMatches + state.finishedMatches)
            .distinctBy { it.id }
            .take(8)
        if (todayMatches.isNotEmpty()) {
            item {
                SectionLabel("Today's Matches", trailing = "All Fixtures", onTrailingClick = {})
                Spacer(Modifier.height(12.dp))
                TodayMatchesColumn(
                    matches = todayMatches,
                    onMatchClick = onMatchClick
                )
            }
            item { Spacer(Modifier.height(28.dp)) }
        }

        if (state.topScorers.isNotEmpty()) {
            item {
                SectionLabel("Top Performers", trailing = "Stats Board", onTrailingClick = onExplorePlayers)
                Spacer(Modifier.height(12.dp))
                TopScorersRow(scorers = state.topScorers, onPlayerClick = onPlayerClick)
            }
        }

        item { Spacer(Modifier.height(28.dp)) }

        item {
            SectionLabel("Interactive Fan Zone")
            Spacer(Modifier.height(12.dp))
            HomeFanZonePoll()
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

// ─── LIVE MATCHES HORIZONTAL RAIL ──────────────────────────────────────────

@Composable
private fun LiveMatchesRail(liveMatches: List<Match>, onMatchClick: (String) -> Unit) {
    Column {
        SectionLabel("Live Matches", trailing = "See All", onTrailingClick = {})
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(liveMatches) { match ->
                LiveMatchCard(match = match, onClick = { onMatchClick(match.id.toString()) })
            }
        }
    }
}

@Composable
private fun LiveMatchCard(match: Match, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.02f))
                )
            )
            .border(1.dp, GlassGlowGreen.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = match.league.name.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(4.dp))
                LiveBadge(minute = "${match.elapsed ?: 0}")
            }
            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = match.homeTeam.logo,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = match.homeTeam.name,
                            fontSize = 12.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = match.awayTeam.logo,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = match.awayTeam.name,
                            fontSize = 12.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${match.homeScore ?: 0}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ScoreGreen
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${match.awayScore ?: 0}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ScoreGreen
                    )
                }
            }
        }
    }
}

// ─── TODAY'S MATCHES COLUMN ─────────────────────────────────────────────────

@Composable
private fun TodayMatchesColumn(matches: List<Match>, onMatchClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        matches.forEach { match ->
            val isFinished = match.status.short in listOf("FT", "AET", "PEN")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMatchClick(match.id.toString()) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.04f)
                ),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.07f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // League logo
                    AsyncImage(
                        model = match.league.logo,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))

                    // Home team
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = match.homeTeam.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(8.dp))
                        AsyncImage(
                            model = match.homeTeam.logo,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Score / Time
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isFinished || match.isLive) {
                            val scoreColor = when {
                                match.isLive -> GlassGlowGreen
                                else -> Color.White
                            }
                            Text(
                                text = "${match.homeScore ?: 0} - ${match.awayScore ?: 0}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = scoreColor,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            val timeLabel = remember(match.timestamp) {
                                try {
                                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                        .format(java.util.Date(match.timestamp * 1000L))
                                } catch (e: Exception) { "-" }
                            }
                            Text(
                                text = timeLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Away team
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        AsyncImage(
                            model = match.awayTeam.logo,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = match.awayTeam.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Live badge
                    if (match.isLive) {
                        Spacer(Modifier.width(8.dp))
                        LiveBadge(minute = "${match.elapsed ?: 0}")
                    } else if (isFinished) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "FT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.35f)
                        )
                    }
                }
            }
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
private fun TopLeaguesRow(leagues: List<LeagueInfo>, onLeagueClick: (LeagueInfo) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(leagues) { league ->
            LeaguePill(league) { onLeagueClick(league) }
        }
    }
}

@Composable
private fun LeaguePill(league: LeagueInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(50.dp))
            .clickable { onClick() }
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

// ─── TOP SCORERS / PERFORMERS ROW ──────────────────────────────────────────

@Composable
private fun TopScorersRow(
    scorers: List<PlayerProfileStatisticsResponse>,
    onPlayerClick: (Int) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(scorers) { entry ->
            val player = entry.player
            val stats = entry.statistics?.firstOrNull()
            if (player != null) {
                Box(
                    modifier = Modifier
                        .width(260.dp)
                        .height(120.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.02f))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                        .clickable { onPlayerClick(player.id) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Side: Image with radial background glow and distinct green border
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(GlassGlowGreen.copy(alpha = 0.25f), Color.Transparent),
                                        radius = 120f
                                    )
                                )
                                .border(1.5.dp, GlassGlowGreen.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = player.photo,
                                contentDescription = player.name,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        // Right Side: Details and Stat Badges
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = (player.name ?: "Player").uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = stats?.team?.name ?: "Unknown Team",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(8.dp))
                            
                            // Compact Stat Badges Row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val goals = stats?.goals?.total ?: 0
                                val assists = stats?.goals?.assists ?: 0
                                val rating = stats?.games?.rating?.let { "%.1f".format(it.toFloatOrNull() ?: 0f) } ?: "-"

                                CompactStatBadge(label = "G", value = "$goals", color = GlassGlowGreen)
                                CompactStatBadge(label = "A", value = "$assists", color = IceBlue)
                                CompactStatBadge(label = "R", value = rating, color = SignalAmber)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactStatBadge(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label: ",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = value,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
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

@Composable
private fun HomeFanZonePoll() {
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = BorderStroke(1.dp, GlassGlowGreen.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(GlassGlowGreen)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "DAILY PREDICTION POLL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GlassGlowGreen,
                    letterSpacing = 1.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Who is your favorite to lift the European Golden Shoe this season?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(16.dp))
            
            val options = listOf(
                "Erling Haaland" to 42,
                "Harry Kane" to 28,
                "Kylian Mbappé" to 20,
                "Robert Lewandowski" to 10
            )
            
            options.forEachIndexed { index, (optionText, percentage) ->
                val isSelected = selectedOption == index
                val hasVoted = selectedOption != null
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) GlassGlowGreen.copy(alpha = 0.15f)
                            else Color.White.copy(alpha = 0.03f)
                        )
                        .border(
                            1.dp,
                            if (isSelected) GlassGlowGreen.copy(alpha = 0.5f)
                            else Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            if (selectedOption == null) {
                                selectedOption = index
                            }
                        }
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (hasVoted) {
                            // Animated progress bar backdrop
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .fillMaxWidth(percentage / 100f)
                                    .background(
                                        if (isSelected) GlassGlowGreen.copy(alpha = 0.08f)
                                        else Color.White.copy(alpha = 0.02f)
                                    )
                            )
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = GlassGlowGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    text = optionText,
                                    color = if (isSelected) GlassGlowGreen else Color.White.copy(alpha = 0.85f),
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                            if (hasVoted) {
                                Text(
                                    text = "$percentage%",
                                    color = if (isSelected) GlassGlowGreen else Color.White.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
