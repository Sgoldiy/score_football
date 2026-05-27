package com.example.footballapp.ui.screens.search

import androidx.compose.foundation.background
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.footballapp.data.util.ApiResult
import com.example.footballapp.ui.theme.PitchBlack
import com.example.footballapp.ui.theme.PitchSurface
import com.example.footballapp.ui.theme.TextSecondary
import com.example.footballapp.viewmodel.SearchUiState
import com.example.footballapp.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onLeagueClick: (Int) -> Unit = {},
    onTeamClick: (Int) -> Unit = {},
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
                        placeholder = { Text("Search teams or leagues...", color = TextSecondary) },
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
                        SearchSuggestions()
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
                        onLeagueClick = onLeagueClick,
                        onTeamClick = onTeamClick
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
private fun SearchSuggestions() {
    Column(Modifier.padding(16.dp)) {
        Text(
            "Popular Leagues",
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        val popular = listOf("Premier League", "La Liga", "Champions League", "Serie A")
        popular.forEach { name ->
            SuggestionItem(name)
        }
    }
}

@Composable
private fun SuggestionItem(name: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
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
        Text(name, color = Color.White.copy(alpha = 0.8f))
    }
}

@Composable
private fun SearchResultList(
    teams: List<com.example.footballapp.domain.model.TeamInfo>,
    leagues: List<com.example.footballapp.domain.model.LeagueInfo>,
    onLeagueClick: (Int) -> Unit,
    onTeamClick: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
                modifier = Modifier.size(32.dp)
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
