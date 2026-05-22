package com.example.footballapp.ui.screens.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

data class OnboardingTeam(
    val id: Int,
    val name: String,
    val country: String,
    val logoUrl: String
)

val POPULAR_TEAMS = listOf(
    OnboardingTeam(33, "Manchester United", "England", "https://media.api-sports.io/football/teams/33.png"),
    OnboardingTeam(50, "Manchester City", "England", "https://media.api-sports.io/football/teams/50.png"),
    OnboardingTeam(40, "Liverpool", "England", "https://media.api-sports.io/football/teams/40.png"),
    OnboardingTeam(42, "Arsenal", "England", "https://media.api-sports.io/football/teams/42.png"),
    OnboardingTeam(49, "Chelsea", "England", "https://media.api-sports.io/football/teams/49.png"),
    OnboardingTeam(541, "Real Madrid", "Spain", "https://media.api-sports.io/football/teams/541.png"),
    OnboardingTeam(529, "Barcelona", "Spain", "https://media.api-sports.io/football/teams/529.png"),
    OnboardingTeam(157, "Bayern Munich", "Germany", "https://media.api-sports.io/football/teams/157.png"),
    OnboardingTeam(165, "Borussia Dortmund", "Germany", "https://media.api-sports.io/football/teams/165.png"),
    OnboardingTeam(85, "Paris Saint Germain", "France", "https://media.api-sports.io/football/teams/85.png"),
    OnboardingTeam(496, "Juventus", "Italy", "https://media.api-sports.io/football/teams/496.png"),
    OnboardingTeam(489, "AC Milan", "Italy", "https://media.api-sports.io/football/teams/489.png"),
    OnboardingTeam(492, "Inter Milan", "Italy", "https://media.api-sports.io/football/teams/492.png"),
    OnboardingTeam(530, "Atletico Madrid", "Spain", "https://media.api-sports.io/football/teams/530.png")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingPageTwo(
    viewModel: OnboardingViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val selectedTeams by viewModel.selectedTeams.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredTeams = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            POPULAR_TEAMS
        } else {
            POPULAR_TEAMS.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.country.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        
        // Header with Back Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Step 2 of 3",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(48.dp)) // Offset back button width
        }

        Spacer(modifier = Modifier.height(12.dp))
        
        // Progress indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.primary))
            Box(modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.primary))
            Box(modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "FOLLOW YOUR TEAMS",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Follow the best clubs in the world to receive live matches, stats and details.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(20.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search your club...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ),
            singleLine = true
        )
        
        // Scrollable List of Teams
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredTeams) { team ->
                val isSelected = selectedTeams.contains(team.id)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            AsyncImage(
                                model = team.logoUrl,
                                contentDescription = team.name,
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(2.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = team.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = team.country,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Button(
                            onClick = { viewModel.toggleTeam(team.id) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = if (isSelected) "FOLLOWING" else "FOLLOW",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black
            )
        ) {
            Text(
                text = if (selectedTeams.isEmpty()) "SKIP FOR NOW" else "CONTINUE",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
