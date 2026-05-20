package com.example.footballapp.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.footballapp.data.model.FixtureResponse
import com.example.footballapp.data.model.League
import com.example.footballapp.data.model.PlayerProfileStatisticsResponse
import com.example.footballapp.ui.components.BroadcastMatchCard
import com.example.footballapp.ui.components.FootballLogo
import com.example.footballapp.ui.components.InfoPill
import com.example.footballapp.ui.components.PlayerAvatar
import com.example.footballapp.ui.components.PremiumCard
import com.example.footballapp.ui.components.ShimmerBlock
import com.example.footballapp.ui.theme.*
import com.example.footballapp.viewmodel.HomeUiState
import com.example.footballapp.viewmodel.HomeViewModel
import kotlinx.coroutines.delay

private enum class MatchTab(val label: String) {
    Live("Live"),
    Upcoming("Upcoming"),
    Finished("Finished")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToFixtures: () -> Unit,
    onNavigateToLeagues: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTopPlayers: () -> Unit,
    onNavigateToMatchDetails: (String) -> Unit,
    onNavigateToPlayerProfile: (Int) -> Unit = {}
) {
    val uiState by viewModel.homeState.collectAsState()

    Scaffold(
        containerColor = PitchBlack,
        topBar = {
            Box {
                // Glassmorphism effect for Top Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .blur(20.dp)
                        .background(PitchBlack.copy(alpha = 0.4f))
                )
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Football Plus",
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                "Live scores, stats, lineups",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { onNavigateToFixtures() }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                        }
                        IconButton(onClick = { onNavigateToFavorites() }) {
                            Icon(Icons.Default.Favorite, contentDescription = "Favorites", tint = Color.White)
                        }
                        IconButton(onClick = { onNavigateToSettings() }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0A192F), // Deep Dark Blue
                            PitchBlack,
                            PitchBlack
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                HomeUiState.Loading -> HomeShimmer()
                is HomeUiState.Error -> HomeError(state.message, onRetry = viewModel::fetchHomeContent)
                is HomeUiState.Success -> HomeContent(
                    state = state,
                    onLeagueClick = onNavigateToLeagues,
                    onTopPlayersClick = onNavigateToTopPlayers,
                    onMatchClick = { onNavigateToMatchDetails(it.toString()) },
                    onPlayerClick = onNavigateToPlayerProfile
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Success,
    onLeagueClick: () -> Unit,
    onTopPlayersClick: () -> Unit,
    onMatchClick: (Int) -> Unit,
    onPlayerClick: (Int) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = MatchTab.entries

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            HeroMatchCarousel(
                matches = state.featuredMatches.take(8),
                onMatchClick = onMatchClick
            )
        }

        item {
            PremiumLeagueSelector(
                leagues = state.topLeagues.take(12),
                onClick = onLeagueClick
            )
        }

        item {
            if (state.topPlayers.isNotEmpty()) {
                PremiumPlayersSection(state.topPlayers.take(10), onTopPlayersClick, onPlayerClick)
            }
        }

        item {
            SegmentedMatchTabs(
                tabs = tabs,
                selectedIndex = selectedTab,
                counts = listOf(state.liveMatches.size, state.upcomingMatches.size, state.finishedMatches.size),
                onTabSelected = { selectedTab = it }
            )
        }

        item {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    (slideInVertically { it * 50 } + fadeIn()).togetherWith(slideOutVertically { -it * 50 } + fadeOut())
                },
                label = "match-list"
            ) { targetTab ->
                val visibleMatches = when (tabs[targetTab]) {
                    MatchTab.Live -> state.liveMatches
                    MatchTab.Upcoming -> state.upcomingMatches
                    MatchTab.Finished -> state.finishedMatches
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (visibleMatches.isEmpty()) {
                        EmptyState(tabs[targetTab].label)
                    } else {
                        visibleMatches.take(24).forEach { match ->
                            BroadcastMatchCard(
                                match = match,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { match.fixture?.id?.let(onMatchClick) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedMatchTabs(
    tabs: List<MatchTab>,
    selectedIndex: Int,
    counts: List<Int>,
    onTabSelected: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PitchSurfaceHigh)
            .padding(4.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedIndex == index
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                    label = "tab-bg"
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(LiveGreen)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = tab.label,
                        color = if (isSelected) Color.White else TextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = if (isSelected) 13.sp else 12.sp
                    )
                    if (counts.getOrElse(index) { 0 } > 0) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) LiveGreen.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${counts[index]}",
                                color = if (isSelected) LiveGreen else TextSecondary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumLeagueSelector(
    leagues: List<League>,
    onClick: () -> Unit
) {
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Top leagues",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                "Follow",
                color = LiveGreen,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onClick)
            )
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(leagues, key = { it.id }) { league ->
                PremiumLeagueChip(league)
            }
        }
    }
}

@Composable
private fun PremiumLeagueChip(league: League) {
    val gradient = when (league.id) {
        39 -> Brush.linearGradient(listOf(PremierLeaguePurple, PremierLeaguePink))
        140 -> Brush.linearGradient(listOf(LaLigaRed, LaLigaOrange))
        135 -> Brush.linearGradient(listOf(SerieABlue, ElectricBlue))
        78 -> Brush.linearGradient(listOf(BundesligaRed, Color.Black))
        61 -> Brush.linearGradient(listOf(Color(0xFF004170), Ligue1Yellow))
        2 -> Brush.linearGradient(listOf(UCLDarkBlue, UCLGold))
        3 -> Brush.linearGradient(listOf(Color(0xFF003399), Color(0xFF00BFFF)))
        848 -> Brush.linearGradient(listOf(Color(0xFF004D40), Color(0xFF00C853)))
        1 -> Brush.linearGradient(listOf(WorldCupGold, Color.Black))
        4 -> Brush.linearGradient(listOf(Color(0xFF003399), Color(0xFFFFD700)))
        9 -> Brush.linearGradient(listOf(Color(0xFFD52B1E), Color(0xFF003DA5)))
        else -> Brush.linearGradient(listOf(PitchSurfaceHigh, PitchSurface))
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = {})
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FootballLogo(
            url = league.logo,
            contentDescription = league.name,
            modifier = Modifier.size(32.dp),
            glow = Color.White.copy(alpha = 0.3f)
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                league.name ?: "League",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (league.country != null) {
                Text(
                    league.country.uppercase(),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun PremiumPlayersSection(
    players: List<PlayerProfileStatisticsResponse>,
    onViewAll: () -> Unit,
    onPlayerClick: (Int) -> Unit
) {
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Players to watch",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                "Explore",
                color = LiveGreen,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onViewAll)
            )
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(players, key = { it.player?.id ?: it.hashCode() }) { player ->
                PlayerSpotlightCard(player, onPlayerClick)
            }
        }
    }
}

@Composable
private fun PlayerSpotlightCard(player: PlayerProfileStatisticsResponse, onPlayerClick: (Int) -> Unit) {
    val stats = player.statistics?.firstOrNull()
    val ratingFloat = stats?.games?.rating?.toFloatOrNull()
    val formColor = when {
        ratingFloat != null && ratingFloat >= 7.5f -> LiveGreen
        ratingFloat != null && ratingFloat >= 6.5f -> Color(0xFFFFC857)
        else -> TextSecondary
    }
    val formattedRating = ratingFloat?.let { "%.2f".format(it) } ?: "-"

    PremiumCard(
        modifier = Modifier.width(164.dp),
        brush = Brush.linearGradient(listOf(Color(0xFF0A1E38), PitchSurfaceHigh, PitchSurface)),
        onClick = { player.player?.id?.let(onPlayerClick) }
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PlayerAvatar(
                url = player.player?.photo,
                name = player.player?.name,
                modifier = Modifier.size(68.dp),
                ringColor = formColor.copy(alpha = 0.45f)
            )
            Text(
                player.player?.name ?: "Player",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                stats?.team?.name ?: "",
                color = TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniStat("G", stats?.goals?.total?.toString() ?: "-")
                MiniStat("A", stats?.goals?.assists?.toString() ?: "-")
                MiniStat("R", formattedRating)
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
        Text(
            label,
            color = TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun HeroMatchCarousel(
    matches: List<FixtureResponse>,
    onMatchClick: (Int) -> Unit
) {
    if (matches.isEmpty()) {
        EmptyState("Matches")
        return
    }

    val listState = rememberLazyListState()
    LaunchedEffect(matches.size) {
        if (matches.size > 1) {
            while (true) {
                delay(4_500)
                val next = (listState.firstVisibleItemIndex + 1) % matches.size
                listState.animateScrollToItem(next)
            }
        }
    }
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(end = 16.dp)
    ) {
        items(matches, key = { it.fixture?.id ?: it.hashCode() }) { match ->
            BroadcastMatchCard(
                match = match,
                modifier = Modifier.width(328.dp),
                expandedByDefault = true,
                onClick = { match.fixture?.id?.let(onMatchClick) }
            )
        }
    }
}

@Composable
private fun EmptyState(label: String) {
    PremiumCard(
        modifier = Modifier.fillMaxWidth(),
        brush = Brush.linearGradient(listOf(PitchSurface, PitchSurfaceHigh))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "No $label matches",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Check another tab",
                color = TextSecondary,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun HomeShimmer() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ShimmerBlock(Modifier.fillMaxWidth().height(240.dp), RoundedCornerShape(24.dp)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(3) { ShimmerBlock(Modifier.weight(1f).height(48.dp), RoundedCornerShape(18.dp)) }
            }
        }
        items(4) { ShimmerBlock(Modifier.fillMaxWidth().height(180.dp), RoundedCornerShape(24.dp)) }
    }
}

@Composable
private fun HomeError(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        PremiumCard(brush = Brush.linearGradient(listOf(PitchSurfaceHigh, PitchSurface))) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Unable to load scores",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(Modifier.height(16.dp))
                InfoPill(
                    "Retry",
                    modifier = Modifier.clickable(onClick = onRetry)
                )
            }
        }
    }
}
