package com.example.footballapp.ui.screens.leagues

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.footballapp.data.model.LeagueResponse
import com.example.footballapp.data.util.ApiResult
import com.example.footballapp.ui.components.MatchRowShimmer
import com.example.footballapp.ui.components.SectionHeader
import com.example.footballapp.viewmodel.LeaguesViewModel

@Composable
fun LeaguesScreen(
    onNavigateToLeagueDetail: (Int) -> Unit,
    viewModel: LeaguesViewModel = hiltViewModel()
) {
    val leaguesState by viewModel.leaguesState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SectionHeader(title = "All Leagues")
        
        when (val state = leaguesState) {
            is ApiResult.Loading -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(16.dp)) {
                    items(10) { MatchRowShimmer() }
                }
            }
            is ApiResult.Success -> {
                LeaguesList(state.data.leagues, onNavigateToLeagueDetail)
            }
            is ApiResult.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun LeaguesList(leagues: List<LeagueResponse>, onLeagueClick: (Int) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(leagues) { leagueItem ->
            LeagueItem(leagueItem, onClick = { leagueItem.league?.id?.let(onLeagueClick) })
        }
    }
}

@Composable
fun LeagueItem(leagueItem: LeagueResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = leagueItem.league?.logo,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = leagueItem.league?.name ?: "Unknown League",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = leagueItem.country?.name ?: "International",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
