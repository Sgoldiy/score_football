package com.example.footballapp.ui.screens.details

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
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
import coil.compose.AsyncImage
import com.example.footballapp.data.util.ApiResult
import com.example.footballapp.domain.model.*
import com.example.footballapp.ui.components.*
import com.example.footballapp.ui.theme.*
import com.example.footballapp.viewmodel.FixtureDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchCenterScreen(
    matchId: String,
    onBackClick: () -> Unit,
    onPlayerClick: (Int) -> Unit = {},
    viewModel: FixtureDetailViewModel = hiltViewModel()
) {
    val detailState by viewModel.detailState.collectAsState()

    LaunchedEffect(matchId) {
        matchId.toIntOrNull()?.let { viewModel.loadFixtureDetails(it) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Match Center", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        (detailState as? ApiResult.Success)?.data?.match?.league?.let {
                            Text(it.name, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = PitchBlack,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = listOf("Overview", "Lineups", "H2H", "Chat")

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(PitchBlack, PitchSurfaceHigh)
                    )
                )
        ) {
            when (val state = detailState) {
                is ApiResult.Loading -> {
                    Column(Modifier.padding(16.dp)) {
                        MatchRowShimmer()
                        Spacer(Modifier.height(16.dp))
                        CardShimmer()
                    }
                }
                is ApiResult.Success -> {
                    val data = state.data
                    Column {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            contentColor = LiveGreen,
                            divider = {},
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = LiveGreen
                                )
                            }
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = {
                                        Text(
                                            title,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            when (selectedTab) {
                                0 -> { // Overview
                                    item {
                                        BroadcastMatchCard(
                                            match = data.match,
                                            expandedByDefault = true,
                                            onClick = {}
                                        )
                                    }
                                    
                                    item {
                                        MatchAttackMomentumGraph(events = data.events, homeTeamId = data.match.homeTeam.id)
                                    }
                                    
                                    // Stadium & Referee Details
                                    item {
                                        VenueRefereeCard(venue = data.venue, referee = data.referee)
                                    }

                                    // Player of the Match (POTM) Hero Card
                                    val allPlayers = data.players.flatMap { it.players }
                                    val potmPlayer = allPlayers.maxByOrNull { it.rating?.toFloatOrNull() ?: 0f }
                                    if (potmPlayer != null && (potmPlayer.rating?.toFloatOrNull() ?: 0f) > 0f) {
                                        val potmTeam = data.players.find { ts -> ts.players.any { p -> p.id == potmPlayer.id } }
                                        val isHomePotm = potmTeam?.teamId == data.match.homeTeam.id
                                        val potmTeamName = if (isHomePotm) data.match.homeTeam.name else data.match.awayTeam.name
                                        val potmTeamLogo = if (isHomePotm) data.match.homeTeam.logo else data.match.awayTeam.logo
                                        item {
                                            PlayerOfTheMatchCard(
                                                player = potmPlayer,
                                                teamName = potmTeamName,
                                                teamLogo = potmTeamLogo,
                                                onPlayerClick = onPlayerClick
                                            )
                                        }
                                    }

                                    // Timeline Events List
                                    if (data.events.isNotEmpty()) {
                                        item {
                                            LiveEventsTimeline(
                                                events = data.events,
                                                homeTeamId = data.match.homeTeam.id,
                                                awayTeamId = data.match.awayTeam.id
                                            )
                                        }
                                    }

                                    // Match Comparative Statistics
                                    if (data.stats.isNotEmpty()) {
                                        item {
                                            MatchStatsSection(
                                                stats = data.stats,
                                                homeTeamId = data.match.homeTeam.id,
                                                awayTeamId = data.match.awayTeam.id
                                            )
                                        }
                                    }

                                    // Match Player Ratings Section
                                    if (data.players.isNotEmpty()) {
                                        item {
                                            MatchPlayerRatingsSection(
                                                playersData = data.players,
                                                homeTeamName = data.match.homeTeam.name,
                                                awayTeamName = data.match.awayTeam.name,
                                                onPlayerClick = onPlayerClick
                                            )
                                        }
                                    }
                                }
                                1 -> { // Lineups
                                    if (data.lineups == null) {
                                        item {
                                            EmptyStateMessage("Lineups not available yet")
                                        }
                                    } else {
                                        // Tactical Pitch Formation Display
                                        item {
                                            FootballPitch(lineups = data.lineups, onPlayerClick = onPlayerClick)
                                        }
                                        
                                        item { Spacer(Modifier.height(12.dp)) }
                                        
                                        // Starting XI and Subs Listing
                                        item { 
                                            Column {
                                                LineupSection(data.lineups.home) 
                                                Spacer(Modifier.height(16.dp))
                                                LineupSection(data.lineups.away)
                                            }
                                        }
                                    }
                                }
                                2 -> { // H2H & Predictions / Odds
                                    item {
                                        WhoWillWinPoll(viewModel = viewModel)
                                    }

                                    // Predictions Summary
                                    if (data.prediction != null) {
                                        item {
                                            PredictionsSection(prediction = data.prediction)
                                        }
                                    }

                                    // Bookmaker Odds Summary
                                    if (data.odds.isNotEmpty()) {
                                        item {
                                            OddsSection(odds = data.odds)
                                        }
                                    }

                                    if (data.headToHead.isEmpty()) {
                                        item {
                                            EmptyStateMessage("No head-to-head history found")
                                        }
                                    } else {
                                        item {
                                            Text(
                                                text = "Past Encounters",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 15.sp,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )
                                        }
                                        items(data.headToHead) { match ->
                                            MatchRow(match = match, onClick = { /* Navigate */ })
                                        }
                                    }
                                }
                                3 -> { // Chat
                                    item {
                                        MatchChatSection(viewModel = viewModel)
                                    }
                                }
                            }
                        }
                    }
                }
                is ApiResult.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

// ─── OVERVIEW STADIUM & OFFICIAL CARD ────────────────────────────────────────

@Composable
private fun VenueRefereeCard(venue: VenueInfo?, referee: String?) {
    if (venue == null && referee == null) return
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.HomeWork, contentDescription = null, tint = GlassGlowGreen, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Match Details", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
            }
            Spacer(Modifier.height(14.dp))
            if (venue != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Stadium: ", color = TextSecondary, fontSize = 13.sp)
                    Text(listOfNotNull(venue.name, venue.city).joinToString(", "), color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
                venue.capacity?.let {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Capacity: ", color = TextSecondary, fontSize = 13.sp)
                        Text(String.format("%,d", it), color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    }
                }
            }
            if (!referee.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Referee: ", color = TextSecondary, fontSize = 13.sp)
                    Text(referee, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
            }
        }
    }
}

// ─── COMPARATIVE STATS METERS ───────────────────────────────────────────────

@Composable
private fun MatchStatsSection(stats: List<MatchStat>, homeTeamId: Int, awayTeamId: Int) {
    if (stats.isEmpty()) return
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.BarChart, contentDescription = null, tint = GlassGlowGreen, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Match Statistics", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
            }
            Spacer(Modifier.height(16.dp))
            
            val groupedStats = stats.groupBy { it.type }
            groupedStats.forEach { (type, items) ->
                val homeValStr = items.find { it.teamId == homeTeamId }?.value ?: "0"
                val awayValStr = items.find { it.teamId == awayTeamId }?.value ?: "0"
                
                val homeVal = homeValStr.replace("%", "").toFloatOrNull() ?: 0f
                val awayVal = awayValStr.replace("%", "").toFloatOrNull() ?: 0f
                
                StatMeter(
                    label = type,
                    homeStr = homeValStr,
                    awayStr = awayValStr,
                    homeVal = homeVal,
                    awayVal = awayVal
                )
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun StatMeter(label: String, homeStr: String, awayStr: String, homeVal: Float, awayVal: Float) {
    val total = homeVal + awayVal
    val homeRatio = if (total > 0) homeVal / total else 0.5f
    val awayRatio = if (total > 0) awayVal / total else 0.5f
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(homeStr, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            Text(awayStr, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(homeRatio.coerceAtLeast(0.01f))
                    .background(LiveGreen)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(awayRatio.coerceAtLeast(0.01f))
                    .background(IceBlue)
            )
        }
    }
}

// ─── LIVE EVENTS TIMELINE ───────────────────────────────────────────────────

@Composable
private fun LiveEventsTimeline(events: List<MatchEvent>, homeTeamId: Int, awayTeamId: Int) {
    if (events.isEmpty()) return
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.History, contentDescription = null, tint = GlassGlowGreen, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Match Timeline", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
            }
            Spacer(Modifier.height(16.dp))
            
            val sortedEvents = events.sortedBy { it.time }
            sortedEvents.forEachIndexed { index, event ->
                val isHome = event.teamId == homeTeamId
                EventTimelineRow(event = event, isHome = isHome, isLast = index == sortedEvents.lastIndex)
            }
        }
    }
}

@Composable
private fun EventTimelineRow(event: MatchEvent, isHome: Boolean, isLast: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopEnd) {
            if (isHome) {
                EventDetail(event = event, alignEnd = true)
            }
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${event.time}'",
                    color = LiveGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(Color.White.copy(alpha = 0.08f))
                )
            }
        }
        
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopStart) {
            if (!isHome) {
                EventDetail(event = event, alignEnd = false)
            }
        }
    }
}

@Composable
private fun EventDetail(event: MatchEvent, alignEnd: Boolean) {
    val alignment = if (alignEnd) Alignment.End else Alignment.Start
    val icon = when (event.type.lowercase()) {
        "goal" -> "⚽"
        "card" -> if (event.detail.lowercase().contains("red")) "🟥" else "🟨"
        "subst" -> "🔄"
        else -> "🔹"
    }
    Column(horizontalAlignment = alignment) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!alignEnd) {
                Text(icon, fontSize = 12.sp)
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = event.playerName ?: "Player",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            if (alignEnd) {
                Spacer(Modifier.width(6.dp))
                Text(icon, fontSize = 12.sp)
            }
        }
        Text(
            text = listOfNotNull(event.detail, event.assistName?.let { "asst: $it" }).joinToString(" - "),
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─── TACTICAL PITCH FORMATION Lineup ─────────────────────────────────────────

@Composable
private fun FootballPitch(lineups: MatchLineups, onPlayerClick: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0F3B20))
            .border(1.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            drawRect(
                color = Color.White.copy(alpha = 0.2f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
            
            drawLine(
                color = Color.White.copy(alpha = 0.2f),
                start = androidx.compose.ui.geometry.Offset(0f, height / 2f),
                end = androidx.compose.ui.geometry.Offset(width, height / 2f),
                strokeWidth = 2.dp.toPx()
            )
            
            drawCircle(
                color = Color.White.copy(alpha = 0.2f),
                radius = 48.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(width / 2f, height / 2f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
            
            drawRect(
                color = Color.White.copy(alpha = 0.2f),
                topLeft = androidx.compose.ui.geometry.Offset(width * 0.2f, 0f),
                size = androidx.compose.ui.geometry.Size(width * 0.6f, height * 0.15f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
            
            drawRect(
                color = Color.White.copy(alpha = 0.2f),
                topLeft = androidx.compose.ui.geometry.Offset(width * 0.2f, height * 0.85f),
                size = androidx.compose.ui.geometry.Size(width * 0.6f, height * 0.15f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        }
        
        val homeLineup = lineups.home
        val awayLineup = lineups.away
        
        homeLineup.startXI.forEach { player ->
            val gridCoords = parseGrid(player.grid) ?: return@forEach
            val xRatio = gridCoords.second / 6f
            val yRatio = 0.5f + (gridCoords.first / 11f)
            
            PlayerNode(
                player = player,
                xRatio = xRatio,
                yRatio = yRatio,
                shirtColor = LiveGreen,
                textColor = DeepNavy,
                onPlayerClick = onPlayerClick
            )
        }
        
        awayLineup.startXI.forEach { player ->
            val gridCoords = parseGrid(player.grid) ?: return@forEach
            val xRatio = (6 - gridCoords.second) / 6f
            val yRatio = 0.5f - (gridCoords.first / 11f)
            
            PlayerNode(
                player = player,
                xRatio = xRatio,
                yRatio = yRatio,
                shirtColor = IceBlue,
                textColor = DeepNavy,
                onPlayerClick = onPlayerClick
            )
        }
    }
}

private fun parseGrid(grid: String?): Pair<Int, Int>? {
    if (grid.isNullOrBlank()) return null
    val parts = grid.split(":")
    val row = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val col = parts.getOrNull(1)?.toIntOrNull() ?: return null
    return Pair(row, col)
}

@Composable
private fun BoxScope.PlayerNode(
    player: LineupPlayer,
    xRatio: Float,
    yRatio: Float,
    shirtColor: Color,
    textColor: Color,
    onPlayerClick: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset(
                x = (xRatio * 300f).dp,
                y = (yRatio * 360f).dp
            )
            .clickable { onPlayerClick(player.id) }
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(shirtColor)
                .border(1.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                player.number.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = textColor
            )
        }
        Text(
            player.name.split(" ").lastOrNull() ?: player.name,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

// ─── LINEUP TEXT LIST SECTION ───────────────────────────────────────────────

@Composable
private fun LineupSection(teamLineup: TeamLineup) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TeamCrestName(
                name = teamLineup.team.name,
                logo = teamLineup.team.logo,
                modifier = Modifier.weight(1f)
            )
            Text(
                teamLineup.formation ?: "",
                style = MaterialTheme.typography.labelLarge,
                color = LiveGreen,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text("Starting XI", style = MaterialTheme.typography.titleSmall, color = Color.White)
        Spacer(Modifier.height(8.dp))
        
        teamLineup.startXI.forEach { player ->
            PlayerRow(player)
        }
        
        if (teamLineup.substitutes.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Substitutes", style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.6f))
            Spacer(Modifier.height(8.dp))
            teamLineup.substitutes.forEach { player ->
                PlayerRow(player, isSub = true)
            }
        }
    }
}

@Composable
private fun PlayerRow(player: LineupPlayer, isSub: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSub) Color.White.copy(alpha = 0.1f) else LiveGreen.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                player.number.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (isSub) Color.White.copy(alpha = 0.6f) else LiveGreen,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            player.name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSub) Color.White.copy(alpha = 0.6f) else Color.White,
            modifier = Modifier.weight(1f)
        )
        Text(
            player.position,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.4f)
        )
    }
}

// ─── PREDICTIONS & ODDS CARD ────────────────────────────────────────────────

@Composable
private fun PredictionsSection(prediction: MatchPrediction) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Analytics, contentDescription = null, tint = GlassGlowGreen, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Win Probability Predictions", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
            }
            Spacer(Modifier.height(20.dp))
            
            val homePct = prediction.homePercent?.replace("%", "")?.toFloatOrNull() ?: 33.3f
            val drawPct = prediction.drawPercent?.replace("%", "")?.toFloatOrNull() ?: 33.3f
            val awayPct = prediction.awayPercent?.replace("%", "")?.toFloatOrNull() ?: 33.3f
            
            val total = homePct + drawPct + awayPct
            val homeSweep = if (total > 0f) (homePct / total) * 360f else 120f
            val drawSweep = if (total > 0f) (drawPct / total) * 360f else 120f
            val awaySweep = if (total > 0f) (awayPct / total) * 360f else 120f

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(140.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 12.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    
                    // Draw Home arc
                    drawArc(
                        color = LiveGreen,
                        startAngle = -90f,
                        sweepAngle = homeSweep,
                        useCenter = false,
                        style = stroke
                    )
                    
                    // Draw Draw arc
                    drawArc(
                        color = Color(0xFFFFC107),
                        startAngle = -90f + homeSweep,
                        sweepAngle = drawSweep,
                        useCenter = false,
                        style = stroke
                    )
                    
                    // Draw Away arc
                    drawArc(
                        color = IceBlue,
                        startAngle = -90f + homeSweep + drawSweep,
                        sweepAngle = awaySweep,
                        useCenter = false,
                        style = stroke
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = prediction.advice?.split(" ").orEmpty().take(3).joinToString(" ").uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(100.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProbabilityItem(label = "Home", value = prediction.homePercent ?: "33%", color = LiveGreen)
                ProbabilityItem(label = "Draw", value = prediction.drawPercent ?: "33%", color = Color(0xFFFFC107))
                ProbabilityItem(label = "Away", value = prediction.awayPercent ?: "33%", color = IceBlue)
            }
        }
    }
}

@Composable
private fun ProbabilityItem(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Column {
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(label, color = TextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun PlayerOfTheMatchCard(
    player: PlayerPerformance,
    teamName: String,
    teamLogo: String?,
    onPlayerClick: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.3f)), // Gold border glow!
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayerClick(player.id) }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.TopEnd)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFFFD700).copy(alpha = 0.15f), Color.Transparent)
                        )
                    )
            )
            
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(2.dp, Color(0xFFFFD700), CircleShape)
                    ) {
                        AsyncImage(
                            model = player.photo,
                            contentDescription = player.name,
                            modifier = Modifier.size(64.dp).clip(CircleShape)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .offset(x = 4.dp, y = 4.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD700)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = PitchBlack,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                
                Spacer(Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PLAYER OF THE MATCH",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = player.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (teamLogo != null) {
                            AsyncImage(model = teamLogo, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = "$teamName • ${player.position}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
                
                player.rating?.let { r ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFD700).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = r,
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchPlayerRatingsSection(
    playersData: List<PlayerMatchStats>,
    homeTeamName: String,
    awayTeamName: String,
    onPlayerClick: (Int) -> Unit
) {
    if (playersData.isEmpty()) return
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Star, contentDescription = null, tint = GlassGlowGreen, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Match Player Ratings", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
            }
            Spacer(Modifier.height(16.dp))
            
            playersData.forEach { teamStats ->
                val isHome = teamStats.teamId == playersData.firstOrNull()?.teamId
                val teamName = if (isHome) homeTeamName else awayTeamName
                
                Text(
                    text = teamName,
                    color = LiveGreen,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                
                val sortedPlayers = teamStats.players.sortedByDescending { it.rating?.toFloatOrNull() ?: 0f }
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sortedPlayers.forEach { player ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.02f))
                                .clickable { onPlayerClick(player.id) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f))
                                ) {
                                    AsyncImage(model = player.photo, contentDescription = player.name, modifier = Modifier.size(32.dp).clip(CircleShape))
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(player.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                    Text(player.position, color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (player.goals > 0) {
                                    Text("⚽".repeat(player.goals), fontSize = 11.sp)
                                }
                                if (player.assists > 0) {
                                    Text("🅰️".repeat(player.assists), fontSize = 11.sp)
                                }
                                
                                val ratingVal = player.rating?.toFloatOrNull() ?: 0f
                                val ratingColor = when {
                                    ratingVal >= 8.0f -> GlassGlowGreen
                                    ratingVal >= 7.0f -> Color(0xFFFFC107)
                                    ratingVal > 0f -> Color(0xFFF44336)
                                    else -> TextSecondary
                                }
                                if (player.rating != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(ratingColor.copy(alpha = 0.15f))
                                            .border(0.5.dp, ratingColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = player.rating,
                                            color = ratingColor,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun OddsSection(odds: List<MatchOdd>) {
    if (odds.isEmpty()) return
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.MonetizationOn, contentDescription = null, tint = GlassGlowGreen, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Bookmaker Odds", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
            }
            Spacer(Modifier.height(12.dp))
            odds.take(2).forEach { odd ->
                Text(
                    text = odd.bookmaker,
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    odd.values.take(3).forEach { valOdd ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(valOdd.value, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                                Text(valOdd.odd, color = LiveGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun EmptyStateMessage(message: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MatchAttackMomentumGraph(events: List<MatchEvent>, homeTeamId: Int) {
    val pressurePoints = remember(events) {
        val list = FloatArray(90)
        for (i in 0..89) {
            list[i] = (kotlin.math.sin(i * 0.2f) * 10f + kotlin.math.cos(i * 0.08f) * 6f).toFloat()
        }
        events.forEach { e ->
            val min = e.time.coerceIn(0, 89)
            val weight = when (e.type.lowercase()) {
                "goal" -> 65f
                "card" -> if (e.detail.lowercase().contains("red")) -40f else -15f
                "subst" -> 10f
                else -> 15f
            }
            val isHome = e.teamId == homeTeamId
            val multiplier = if (isHome) 1f else -1f
            val radius = if (e.type.lowercase() == "goal") 6 else 3
            for (offset in -radius..radius) {
                val targetMin = min + offset
                if (targetMin in 0..89) {
                    val distanceFactor = 1f - (kotlin.math.abs(offset).toFloat() / radius.toFloat())
                    list[targetMin] += weight * distanceFactor * multiplier
                }
            }
        }
        for (i in 0..89) {
            list[i] = list[i].coerceIn(-80f, 80f)
        }
        list
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Timeline, contentDescription = null, tint = LiveGreen, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Attack Momentum Graph", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                    Text("Live team pressure peaks timeline", color = TextSecondary, fontSize = 11.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(LiveGreen))
                        Spacer(Modifier.width(4.dp))
                        Text("Home", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(IceBlue))
                        Spacer(Modifier.width(4.dp))
                        Text("Away", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                    }
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val canvasHeight = size.height
                    val baseline = canvasHeight / 2f
                    
                    drawLine(
                        color = Color.White.copy(alpha = 0.1f),
                        start = androidx.compose.ui.geometry.Offset(0f, baseline),
                        end = androidx.compose.ui.geometry.Offset(width, baseline),
                        strokeWidth = 1.dp.toPx()
                    )
                    
                    val barCount = 90
                    val spacing = width / barCount
                    
                    pressurePoints.forEachIndexed { i, pressure ->
                        val x = i * spacing
                        val magnitude = (pressure / 80f) * (canvasHeight / 2f)
                        val color = if (pressure >= 0) LiveGreen else IceBlue
                        
                        drawLine(
                            color = color.copy(alpha = 0.85f),
                            start = androidx.compose.ui.geometry.Offset(x, baseline),
                            end = androidx.compose.ui.geometry.Offset(x, baseline - magnitude),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0'", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("45'", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("90'", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun WhoWillWinPoll(
    viewModel: FixtureDetailViewModel
) {
    val userVote by viewModel.userVote.collectAsState()
    val percentages by viewModel.pollPercentages.collectAsState()
    
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.EmojiPeople, contentDescription = null, tint = LiveGreen, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Who Will Win?", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
            }
            
            Spacer(Modifier.height(14.dp))
            
            if (userVote == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    VoteButton(label = "Home Win", onClick = { viewModel.submitVote(0) }, modifier = Modifier.weight(1f))
                    VoteButton(label = "Draw", onClick = { viewModel.submitVote(1) }, modifier = Modifier.weight(1f))
                    VoteButton(label = "Away Win", onClick = { viewModel.submitVote(2) }, modifier = Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    PollResultRow(label = "Home Win", percent = percentages.first, color = LiveGreen, isSelected = userVote == 0)
                    PollResultRow(label = "Draw", percent = percentages.second, color = Color(0xFFFFC107), isSelected = userVote == 1)
                    PollResultRow(label = "Away Win", percent = percentages.third, color = IceBlue, isSelected = userVote == 2)
                }
            }
        }
    }
}

@Composable
private fun VoteButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun PollResultRow(label: String, percent: Int, color: Color, isSelected: Boolean) {
    val bgGlow = if (isSelected) color.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.02f)
    val border = if (isSelected) color.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.06f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgGlow)
            .border(0.5.dp, border, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                if (isSelected) {
                    Spacer(Modifier.width(6.dp))
                    Text("• Voted", color = color, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
            Text("$percent%", color = color, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percent / 100f)
                    .background(color)
            )
        }
    }
}

@Composable
private fun MatchChatSection(viewModel: FixtureDetailViewModel) {
    val comments by viewModel.comments.collectAsState()
    var textVal by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Forum, contentDescription = null, tint = LiveGreen, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Live Fan Commentary", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
        }
        
        Spacer(Modifier.height(14.dp))
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .border(0.5.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                reverseLayout = true
            ) {
                items(comments.asReversed()) { msg ->
                    if (msg.isSystem) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = msg.text,
                                color = TextSecondary.copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.02f))
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = msg.username,
                                    color = LiveGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                val timeStr = formatChatTime(msg.timestamp)
                                Text(
                                    text = timeStr,
                                    color = TextSecondary,
                                    fontSize = 9.sp
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(text = msg.text, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val emojis = listOf("⚽", "🔥", "🟥", "😮", "👏", "🎉")
            emojis.forEach { emoji ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.04f))
                        .clickable {
                            viewModel.sendComment("$emoji Reaction!", "MyReaction")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 16.sp)
                }
            }
        }
        
        Spacer(Modifier.height(10.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = textVal,
                onValueChange = { textVal = it },
                placeholder = { Text("Say something...", color = TextSecondary, fontSize = 12.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.06f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
                maxLines = 2
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(LiveGreen)
                    .clickable {
                        if (textVal.isNotBlank()) {
                            viewModel.sendComment(textVal, "User_Guest")
                            textVal = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Send, contentDescription = "Send", tint = PitchBlack, modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun formatChatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

