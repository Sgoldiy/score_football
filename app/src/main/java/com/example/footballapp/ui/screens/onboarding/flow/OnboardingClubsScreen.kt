package com.example.footballapp.ui.screens.onboarding.flow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.footballapp.domain.model.OnboardingClub
import com.example.footballapp.domain.model.OnboardingDefaults
import com.example.footballapp.ui.theme.DeepNavy
import com.example.footballapp.ui.theme.GlassGlowGreen

@Composable
fun OnboardingClubsScreen(
    mode: String,
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: OnboardingClubViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(mode) { viewModel.init(mode) }

    val selectedCount = state.selectedClubIds.size
    val continueEnabled = selectedCount > 0
    val continueText = when {
        state.isSaving -> "Saving..."
        continueEnabled && selectedCount == 1 -> "Continue with 1 club"
        continueEnabled -> "Continue with $selectedCount clubs"
        else -> "Select at least 1 club"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onBack() }
                    .padding(6.dp)
            )
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                OnboardingProgressDots(step = 2, totalDots = 2)
                Spacer(Modifier.height(10.dp))
                Text(text = "Pick Your Clubs", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text(text = "Select the clubs you support", fontSize = 14.sp, color = Color.White.copy(alpha = 0.60f))
            }
        }

        Spacer(Modifier.height(14.dp))

        LeagueTabRow(
            leagues = state.leagues,
            selectedLeagueId = state.selectedTabLeagueId,
            selectedClubIds = state.selectedClubIds,
            onTabSelected = { viewModel.onTabSelected(it) }
        )

        Spacer(Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(viewModel.clubsForLeague(state.selectedTabLeagueId)) { club ->
                ClubCard(
                    club = club,
                    selected = state.selectedClubIds.contains(club.clubId),
                    onToggle = { viewModel.toggleClub(club) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        OnboardingPrimaryButton(
            text = continueText,
            enabled = continueEnabled,
            loading = state.isSaving,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            onClick = { viewModel.save(mode = mode, onDone = onDone) }
        )
    }
}

@Composable
private fun LeagueTabRow(
    leagues: List<com.example.footballapp.domain.model.OnboardingLeague>,
    selectedLeagueId: Int,
    selectedClubIds: Set<Int>,
    onTabSelected: (Int) -> Unit
) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leagues.forEach { league ->
            val selected = league.id == selectedLeagueId
            val count = viewModelSelectedCountForLeague(league.id, selectedClubIds)
            Box(
                modifier = Modifier
                    .clickable { onTabSelected(league.id) }
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box {
                        Text(
                            text = league.shortName,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) Color.White else Color.White.copy(alpha = 0.50f),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        if (count > 0) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(GlassGlowGreen)
                                    .align(Alignment.TopEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = count.toString(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepNavy
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(if (selected) GlassGlowGreen else Color.Transparent)
                    )
                }
            }
        }
    }
}

private fun viewModelSelectedCountForLeague(leagueId: Int, selectedClubIds: Set<Int>): Int {
    val clubs = OnboardingDefaults.clubsByLeagueId[leagueId].orEmpty()
    val clubIds = clubs.map { it.clubId }.toSet()
    return selectedClubIds.count { clubIds.contains(it) }
}

@Composable
private fun ClubCard(
    club: OnboardingClub,
    selected: Boolean,
    onToggle: () -> Unit
) {
    val bg = if (selected) GlassGlowGreen.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f)
    val border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, GlassGlowGreen) else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onToggle() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = border ?: androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .padding(10.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(GlassGlowGreen)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = OnboardingDefaults.clubLogoUrl(club.clubId),
                    contentDescription = club.clubName,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(64.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = club.clubName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}
