package com.example.footballapp.ui.screens.players

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.example.footballapp.viewmodel.TopPlayersData
import com.example.footballapp.viewmodel.TopPlayersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopPlayersScreen(
    onBackClick: () -> Unit,
    onPlayerClick: (Int) -> Unit = {},
    viewModel: TopPlayersViewModel = hiltViewModel()
) {
    val tabData by viewModel.tabData.collectAsState()
    val selectedTabIndex by viewModel.selectedTabIndex.collectAsState()
    val tabs = viewModel.tabs

    Scaffold(
        containerColor = PitchBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Top Players", fontWeight = FontWeight.Black)
                        Text(tabs[selectedTabIndex].label, color = LiveGreen, style = MaterialTheme.typography.labelMedium)
                    }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF071B2C), PitchBlack)))
                .padding(padding)
        ) {
            PremiumTabBar(
                tabs = tabs.map { it.label },
                selectedIndex = selectedTabIndex,
                onTabSelected = { viewModel.selectTab(it) }
            )

            val tabId = tabs[selectedTabIndex].id
            AnimatedContent(
                targetState = tabId,
                transitionSpec = {
                    (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                },
                label = "tab-content"
            ) { currentTabId ->
                val data = tabData[currentTabId]
                when (data) {
                    ApiResult.Loading -> TopPlayersShimmer()
                    is ApiResult.Error -> TopPlayersError(data.message)
                    is ApiResult.Success -> TopPlayersContent(data.data, onPlayerClick)
                    null -> TopPlayersShimmer()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumTabBar(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 16.dp,
        containerColor = Color.Transparent,
        contentColor = LiveGreen,
        divider = {},
        indicator = { tabPositions ->
            if (selectedIndex < tabPositions.size) {
                val tabPos = tabPositions[selectedIndex]
                Box(
                    Modifier
                        .offset(x = tabPos.left)
                        .width(tabPos.width)
                        .padding(horizontal = 12.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(listOf(LiveGreen, IceBlue))
                        )
                )
            }
        }
    ) {
        tabs.forEachIndexed { index, label ->
            val isSelected = selectedIndex == index
            Tab(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                modifier = Modifier
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                selectedContentColor = Color.White,
                unselectedContentColor = Color.White.copy(alpha = 0.4f)
            ) {
                Text(
                    text = label.uppercase(),
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.4f),
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.8.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun TopPlayersContent(data: TopPlayersData, onPlayerClick: (Int) -> Unit) {
    val heroPlayer = data.scorers.firstOrNull()
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        if (heroPlayer != null) {
            item(key = "hero") {
                HeroPlayerSection(heroPlayer, onPlayerClick)
            }
        }

        if (data.scorers.isNotEmpty()) {
            item(key = "scorers") {
                PlayerSection(
                    title = "Top scorers",
                    subtitle = "Goals this season",
                    players = data.scorers.take(20),
                    mode = "goals",
                    onPlayerClick = onPlayerClick
                )
            }
        }

        if (data.assists.isNotEmpty()) {
            item(key = "assists") {
                PlayerSection(
                    title = "Assist leaders",
                    subtitle = "Creative force",
                    players = data.assists.take(20),
                    mode = "assists",
                    onPlayerClick = onPlayerClick
                )
            }
        }

        val topRated = data.topRated()
        if (topRated.isNotEmpty()) {
            item(key = "rated") {
                PlayerSection(
                    title = "Top rated",
                    subtitle = "Highest match ratings",
                    players = topRated.take(20),
                    mode = "rating",
                    onPlayerClick = onPlayerClick
                )
            }
        }

        if (data.yellowCards.isNotEmpty()) {
            item(key = "discipline") {
                PlayerSection(
                    title = "Discipline watch",
                    subtitle = "Most yellow cards",
                    players = data.yellowCards.take(20),
                    mode = "cards",
                    onPlayerClick = onPlayerClick
                )
            }
        }
    }
}

@Composable
private fun HeroPlayerSection(player: PlayerProfileStatisticsResponse, onPlayerClick: (Int) -> Unit) {
    val stats = player.statistics?.firstOrNull()
    val formattedRating = stats?.games?.rating?.let { formatRating(it) } ?: "-"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF0A2540), Color(0xFF0D3B66), PitchBlack)
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerAvatar(
                url = player.player?.photo,
                name = player.player?.name,
                modifier = Modifier.size(120.dp),
                ringColor = LiveGreen.copy(alpha = 0.6f),
                onClick = { player.player?.id?.let(onPlayerClick) }
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "TOP SCORER",
                    color = LiveGreen,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    player.player?.name ?: "Player",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    stats?.team?.let { team ->
                        FootballLogo(team.logo, team.name, Modifier.size(20.dp), glow = LiveGreen)
                        Spacer(Modifier.width(6.dp))
                        Text(team.name ?: "", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text(
                    listOfNotNull(stats?.games?.position, player.player?.nationality).joinToString(" • "),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HeroStat("Goals", stats?.goals?.total?.toString() ?: "-", LiveGreen)
                    HeroStat("Assists", stats?.goals?.assists?.toString() ?: "-", IceBlue)
                    HeroStat("Rating", formattedRating, Color(0xFFFFD700))
                }
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, accent: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(accent.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(label, color = accent, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun PlayerSection(
    title: String,
    subtitle: String,
    players: List<PlayerProfileStatisticsResponse>,
    mode: String,
    onPlayerClick: (Int) -> Unit
) {
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(players, key = { it.player?.id ?: it.hashCode() }) { player ->
                PlayerRankCard(player, mode, onPlayerClick)
            }
        }
    }
}

@Composable
private fun PlayerRankCard(player: PlayerProfileStatisticsResponse, mode: String, onPlayerClick: (Int) -> Unit) {
    val stats = player.statistics?.firstOrNull()
    val value = when (mode) {
        "goals" -> stats?.goals?.total?.toString()
        "assists" -> stats?.goals?.assists?.toString()
        "cards" -> stats?.cards?.yellow?.toString()
        "rating" -> stats?.games?.rating?.let { formatRating(it) }
        else -> "-"
    } ?: "-"

    val ratingFloat = stats?.games?.rating?.toFloatOrNull()
    val formColor = when {
        ratingFloat != null && ratingFloat >= 7.5f -> LiveGreen
        ratingFloat != null && ratingFloat >= 6.5f -> Color(0xFFFFC857)
        ratingFloat != null -> DangerRed
        else -> TextSecondary
    }

    PremiumCard(
        modifier = Modifier.size(width = 170.dp, height = 248.dp),
        brush = Brush.linearGradient(listOf(Color(0xFF0A1E38), PitchSurfaceHigh, PitchSurface)),
        onClick = { player.player?.id?.let(onPlayerClick) }
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(8.dp))
                PlayerAvatar(
                    player.player?.photo,
                    player.player?.name,
                    Modifier.size(76.dp),
                    ringColor = formColor.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    player.player?.name ?: "Player",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FootballLogo(stats?.team?.logo, stats?.team?.name, Modifier.size(18.dp), glow = formColor)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stats?.team?.name ?: "",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(formColor)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        value,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    mode.replaceFirstChar { it.uppercase() },
                    color = formColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stats?.games?.position ?: "",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

private fun formatRating(rating: String): String {
    val floatVal = rating.toFloatOrNull() ?: return rating
    return "%.2f".format(floatVal)
}

private val DangerRed = Color(0xFFFF4D5E)

@Composable
private fun TopPlayersShimmer() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ShimmerBlock(Modifier.fillMaxWidth().height(220.dp), RoundedCornerShape(24.dp)) }
        item { ShimmerBlock(Modifier.fillMaxWidth().height(200.dp), RoundedCornerShape(24.dp)) }
        item { ShimmerBlock(Modifier.fillMaxWidth().height(200.dp), RoundedCornerShape(24.dp)) }
    }
}

@Composable
private fun TopPlayersError(message: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "No data available",
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
            Spacer(Modifier.height(8.dp))
            Text(
                "Try another competition tab",
                color = LiveGreen,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
