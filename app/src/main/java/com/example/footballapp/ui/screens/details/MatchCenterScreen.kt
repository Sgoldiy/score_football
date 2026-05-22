package com.example.footballapp.ui.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
        val tabs = listOf("Overview", "Lineups", "H2H")

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
                                }
                                1 -> { // Lineups
                                    if (data.lineups == null) {
                                        item {
                                            EmptyStateMessage("Lineups not available yet")
                                        }
                                    } else {
                                        item { 
                                            Column {
                                                LineupSection(data.lineups.home) 
                                                Spacer(Modifier.height(16.dp))
                                                LineupSection(data.lineups.away)
                                            }
                                        }
                                    }
                                }
                                2 -> { // H2H
                                    if (data.headToHead.isEmpty()) {
                                        item {
                                            EmptyStateMessage("No head-to-head history found")
                                        }
                                    } else {
                                        items(data.headToHead) { match ->
                                            MatchRow(match = match, onClick = { /* Navigate to another match? */ })
                                        }
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
