package com.footballpluse.footballapp.ui.screens.players

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.footballpluse.footballapp.data.model.PlayerProfileStatisticsResponse
import com.footballpluse.footballapp.data.util.ApiResult
import com.footballpluse.footballapp.ui.components.FootballLogo
import com.footballpluse.footballapp.ui.components.PlayerAvatar
import com.footballpluse.footballapp.ui.components.PremiumCard
import com.footballpluse.footballapp.ui.components.ShimmerBlock
import com.footballpluse.footballapp.ui.theme.IceBlue
import com.footballpluse.footballapp.ui.theme.LiveGreen
import com.footballpluse.footballapp.ui.theme.PitchBlack
import com.footballpluse.footballapp.ui.theme.PitchSurface
import com.footballpluse.footballapp.ui.theme.PitchSurfaceHigh
import com.footballpluse.footballapp.ui.theme.TextSecondary
import com.footballpluse.footballapp.viewmodel.TopPlayersData
import com.footballpluse.footballapp.viewmodel.TopPlayersViewModel

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
    var selectedPosition by remember { mutableStateOf("All") }
    val positions = listOf("All", "Forwards", "Midfielders", "Defenders", "Goalkeepers")
    
    val filteredScorers = data.scorers.filter { matchesPosition(it, selectedPosition) }
    val filteredAssists = data.assists.filter { matchesPosition(it, selectedPosition) }
    val filteredRated = data.topRated().filter { matchesPosition(it, selectedPosition) }
    val filteredYellow = data.yellowCards.filter { matchesPosition(it, selectedPosition) }

    val heroPlayer = filteredScorers.firstOrNull()
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(key = "position_filters") {
            PositionFilterRail(
                positions = positions,
                selectedPosition = selectedPosition,
                onSelect = { selectedPosition = it }
            )
        }

        if (filteredScorers.isNotEmpty()) {
            item(key = "comparative_chart") {
                ComparativePerformanceChart(players = filteredScorers)
            }
        }

        if (heroPlayer != null) {
            item(key = "hero") {
                HeroPlayerSection(heroPlayer, onPlayerClick)
            }
        }

        if (filteredScorers.isNotEmpty()) {
            item(key = "scorers") {
                PlayerSection(
                    title = "Top scorers",
                    subtitle = "Goals this season",
                    players = filteredScorers.take(20),
                    mode = "goals",
                    onPlayerClick = onPlayerClick
                )
            }
        }

        if (filteredAssists.isNotEmpty()) {
            item(key = "assists") {
                PlayerSection(
                    title = "Assist leaders",
                    subtitle = "Creative force",
                    players = filteredAssists.take(20),
                    mode = "assists",
                    onPlayerClick = onPlayerClick
                )
            }
        }

        if (filteredRated.isNotEmpty()) {
            item(key = "rated") {
                PlayerSection(
                    title = "Top rated",
                    subtitle = "Highest match ratings",
                    players = filteredRated.take(20),
                    mode = "rating",
                    onPlayerClick = onPlayerClick
                )
            }
        }

        if (filteredYellow.isNotEmpty()) {
            item(key = "discipline") {
                PlayerSection(
                    title = "Discipline watch",
                    subtitle = "Most yellow cards",
                    players = filteredYellow.take(20),
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
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = label,
            color = accent,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            softWrap = false
        )
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
        
        Spacer(Modifier.height(12.dp))
        
        if (players.size >= 3) {
            TopPlayersPodium(players = players, mode = mode, onPlayerClick = onPlayerClick)
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(players.drop(3), key = { "${mode}_${it.player?.id ?: it.hashCode()}" }) { player ->
                    PlayerRankCard(player, mode, onPlayerClick)
                }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(players, key = { "${mode}_${it.player?.id ?: it.hashCode()}" }) { player ->
                    PlayerRankCard(player, mode, onPlayerClick)
                }
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

private fun matchesPosition(player: PlayerProfileStatisticsResponse, filter: String): Boolean {
    if (filter == "All") return true
    val pos = player.statistics?.firstOrNull()?.games?.position ?: return false
    return when (filter) {
        "Forwards" -> pos.equals("Attacker", ignoreCase = true)
        "Midfielders" -> pos.equals("Midfielder", ignoreCase = true)
        "Defenders" -> pos.equals("Defender", ignoreCase = true)
        "Goalkeepers" -> pos.equals("Goalkeeper", ignoreCase = true)
        else -> false
    }
}

@Composable
private fun PositionFilterRail(
    positions: List<String>,
    selectedPosition: String,
    onSelect: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(positions, key = { it }) { pos ->
            val isSelected = pos == selectedPosition
            val borderGlow = if (isSelected) LiveGreen else Color.White.copy(alpha = 0.15f)
            val bg = if (isSelected) LiveGreen.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.02f)
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .border(1.dp, borderGlow, RoundedCornerShape(12.dp))
                    .clickable { onSelect(pos) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = pos,
                    color = if (isSelected) LiveGreen else Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ComparativePerformanceChart(players: List<PlayerProfileStatisticsResponse>) {
    val top5 = players.take(5)
    if (top5.isEmpty()) return

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Top 5 Performance Chart", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text("Comparative Goals vs Assists Analysis", color = TextSecondary, fontSize = 11.sp)
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(LiveGreen))
                        Spacer(Modifier.width(4.dp))
                        Text("Goals", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(IceBlue))
                        Spacer(Modifier.width(4.dp))
                        Text("Assists", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            val maxVal = top5.maxOfOrNull {
                val stats = it.statistics?.firstOrNull()
                val g = stats?.goals?.total ?: 0
                val a = stats?.goals?.assists ?: 0
                maxOf(g, a, 1)
            } ?: 1
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    val barWidth = 14.dp.toPx()
                    val groupGap = 24.dp.toPx()
                    val totalGroups = top5.size
                    
                    val groupWidth = (canvasWidth - (totalGroups - 1) * groupGap) / totalGroups
                    
                    val baselineY = canvasHeight - 30.dp.toPx()
                    val chartHeight = canvasHeight - 40.dp.toPx()
                    
                    // Draw grid lines
                    val gridLineCount = 3
                    for (i in 1..gridLineCount) {
                        val fraction = i.toFloat() / (gridLineCount + 1).toFloat()
                        val y = baselineY - fraction * chartHeight
                        drawLine(
                            color = Color.White.copy(alpha = 0.04f),
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(canvasWidth, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }
                    
                    drawLine(
                        color = Color.White.copy(alpha = 0.1f),
                        start = androidx.compose.ui.geometry.Offset(0f, baselineY),
                        end = androidx.compose.ui.geometry.Offset(canvasWidth, baselineY),
                        strokeWidth = 1.dp.toPx()
                    )
                    
                    top5.forEachIndexed { index, player ->
                        val stats = player.statistics?.firstOrNull()
                        val goals = stats?.goals?.total ?: 0
                        val assists = stats?.goals?.assists ?: 0
                        
                        val goalsBarHeight = (goals.toFloat() / maxVal.toFloat()) * chartHeight
                        val assistsBarHeight = (assists.toFloat() / maxVal.toFloat()) * chartHeight
                        
                        val groupStartX = index * (groupWidth + groupGap)
                        
                        val goalsStartX = groupStartX + (groupWidth / 2f) - barWidth - 2.dp.toPx()
                        val assistsStartX = groupStartX + (groupWidth / 2f) + 2.dp.toPx()
                        
                        if (goalsBarHeight > 0) {
                            drawRoundRect(
                                color = LiveGreen,
                                topLeft = androidx.compose.ui.geometry.Offset(goalsStartX, baselineY - goalsBarHeight),
                                size = androidx.compose.ui.geometry.Size(barWidth, goalsBarHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                        
                        if (assistsBarHeight > 0) {
                            drawRoundRect(
                                color = IceBlue,
                                topLeft = androidx.compose.ui.geometry.Offset(assistsStartX, baselineY - assistsBarHeight),
                                size = androidx.compose.ui.geometry.Size(barWidth, assistsBarHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    top5.forEach { player ->
                        val lastName = player.player?.name?.split(" ")?.lastOrNull() ?: "Player"
                        Text(
                            text = lastName,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopPlayersPodium(
    players: List<PlayerProfileStatisticsResponse>,
    mode: String,
    onPlayerClick: (Int) -> Unit
) {
    val p1 = players.getOrNull(0) ?: return
    val p2 = players.getOrNull(1) ?: return
    val p3 = players.getOrNull(2) ?: return
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        PodiumCol(
            player = p2,
            rank = 2,
            mode = mode,
            color = Color(0xFFC0C0C0),
            modifier = Modifier.weight(1f),
            onPlayerClick = onPlayerClick
        )
        
        PodiumCol(
            player = p1,
            rank = 1,
            mode = mode,
            color = Color(0xFFFFD700),
            modifier = Modifier.weight(1.1f),
            onPlayerClick = onPlayerClick
        )
        
        PodiumCol(
            player = p3,
            rank = 3,
            mode = mode,
            color = Color(0xFFCD7F32),
            modifier = Modifier.weight(1f),
            onPlayerClick = onPlayerClick
        )
    }
}

@Composable
private fun PodiumCol(
    player: PlayerProfileStatisticsResponse,
    rank: Int,
    mode: String,
    color: Color,
    modifier: Modifier = Modifier,
    onPlayerClick: (Int) -> Unit
) {
    val stats = player.statistics?.firstOrNull()
    val value = when (mode) {
        "goals" -> stats?.goals?.total?.toString()
        "assists" -> stats?.goals?.assists?.toString()
        "cards" -> stats?.cards?.yellow?.toString()
        "rating" -> stats?.games?.rating?.let { formatRating(it) }
        else -> "-"
    } ?: "-"

    val colHeight = when (rank) {
        1 -> 180.dp
        2 -> 155.dp
        else -> 140.dp
    }
    
    val medal = when (rank) {
        1 -> "👑"
        2 -> "🥈"
        else -> "🥉"
    }

    Card(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
            .height(colHeight)
            .clickable { player.player?.id?.let(onPlayerClick) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    PlayerAvatar(
                        url = player.player?.photo,
                        name = player.player?.name,
                        modifier = Modifier.size(if (rank == 1) 56.dp else 46.dp),
                        ringColor = color.copy(alpha = 0.6f)
                    )
                    Box(
                        modifier = Modifier
                            .offset(x = 4.dp, y = 4.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = medal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = player.player?.name ?: "Player",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stats?.team?.name ?: "",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = value,
                    color = color,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
                Text(
                    text = when (mode) {
                        "goals" -> "Goals"
                        "assists" -> "Assists"
                        "cards" -> "Cards"
                        "rating" -> "Rating"
                        else -> ""
                    },
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
