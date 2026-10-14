package com.footballpluse.footballapp.ui.screens.search

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
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.footballpluse.footballapp.data.util.ApiResult
import com.footballpluse.footballapp.ui.theme.PitchBlack
import com.footballpluse.footballapp.ui.theme.PitchSurface
import com.footballpluse.footballapp.ui.theme.TextSecondary
import com.footballpluse.footballapp.viewmodel.SearchUiState
import com.footballpluse.footballapp.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onLeagueClick: (Int) -> Unit = {},
    onTeamClick: (Int) -> Unit = {},
    onPlayerClick: (Int) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.searchQuery.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = { viewModel.onQueryChanged(it) },
                        placeholder = { Text("Search teams, leagues, players...", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color.White,
                            focusedTextColor = Color.White
                        ),
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = TextSecondary) },
                        singleLine = true
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PitchBlack)
            )
        },
        containerColor = PitchBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState) {
                is SearchUiState.Idle -> {
                    if (recentSearches.isNotEmpty()) {
                        RecentSearches(
                            searches = recentSearches,
                            onSearchClick = { viewModel.onQueryChanged(it) },
                            onClear = { viewModel.clearRecentSearches() }
                        )
                    } else {
                        SearchSuggestions(onSearchClick = { viewModel.onQueryChanged(it) })
                    }
                }
                is SearchUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
                is SearchUiState.Success -> {
                    val data = (uiState as SearchUiState.Success)
                    SearchResultList(
                        teams = data.teams,
                        leagues = data.leagues,
                        players = data.players,
                        onLeagueClick = onLeagueClick,
                        onTeamClick = onTeamClick,
                        onPlayerClick = onPlayerClick
                    )
                }
                is SearchUiState.Error -> {
                    val error = (uiState as SearchUiState.Error)
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(error.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentSearches(
    searches: List<String>,
    onSearchClick: (String) -> Unit,
    onClear: () -> Unit
) {
    Column(Modifier.padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Recent Searches",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onClear) {
                Text("Clear", color = TextSecondary)
            }
        }
        Spacer(Modifier.height(8.dp))
        searches.forEach { search ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSearchClick(search) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(search, color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
private fun SearchSuggestions(onSearchClick: (String) -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Text(
            "Trending Searches",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))
        val trendingTags = listOf("Real Madrid", "Arsenal", "Barcelona", "Haaland", "Mbappé", "Bellingham", "Man City")
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(trendingTags) { tag ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(PitchSurface)
                        .border(1.dp, Color(0xFF00E676).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .clickable { onSearchClick(tag) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = tag,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Popular Leagues",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        val popular = listOf("Premier League", "La Liga", "Bundesliga", "Serie A", "Champions League")
        popular.forEach { name ->
            SuggestionItem(name = name, onClick = { onSearchClick(name) })
        }
    }
}

@Composable
private fun SuggestionItem(name: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PitchSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(name, color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
    }
}

@Composable
private fun SearchResultList(
    teams: List<com.footballpluse.footballapp.domain.model.TeamInfo>,
    leagues: List<com.footballpluse.footballapp.domain.model.LeagueInfo>,
    players: List<com.footballpluse.footballapp.domain.model.PlayerInfo> = emptyList(),
    onLeagueClick: (Int) -> Unit,
    onTeamClick: (Int) -> Unit,
    onPlayerClick: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (players.isNotEmpty()) {
            item {
                Text(
                    "Players",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(players) { player ->
                SearchResultItem(
                    name = player.name,
                    logo = player.photo,
                    subtitle = listOfNotNull(
                        player.type?.takeIf { it.isNotBlank() },
                        player.nationality?.takeIf { it.isNotBlank() },
                        player.age?.let { "$it yrs" }
                    ).joinToString(" \u00B7 "),
                    onClick = { onPlayerClick(player.id) }
                )
            }
        }

        if (teams.isNotEmpty()) {
            item {
                Text(
                    "Teams",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(teams) { team ->
                SearchResultItem(
                    name = team.name,
                    logo = team.logo,
                    subtitle = team.country,
                    onClick = { onTeamClick(team.id) }
                )
            }
        }

        if (leagues.isNotEmpty()) {
            item {
                Text(
                    "Leagues",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(leagues) { league ->
                SearchResultItem(
                    name = league.name,
                    logo = league.logo,
                    subtitle = league.country,
                    onClick = { onLeagueClick(league.id) }
                )
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    name: String,
    logo: String?,
    subtitle: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(PitchSurface),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = logo,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                placeholder = androidx.compose.ui.res.painterResource(com.footballpluse.footballapp.R.drawable.ic_placeholder),
                error = androidx.compose.ui.res.painterResource(com.footballpluse.footballapp.R.drawable.ic_placeholder)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(name, color = Color.White, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
