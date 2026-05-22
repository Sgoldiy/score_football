package com.example.footballapp.ui.screens.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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

data class OnboardingLeague(
    val id: Int,
    val name: String,
    val country: String,
    val logoUrl: String
)

val TOP_LEAGUES = listOf(
    OnboardingLeague(39, "Premier League", "England", "https://media.api-sports.io/football/leagues/39.png"),
    OnboardingLeague(140, "La Liga", "Spain", "https://media.api-sports.io/football/leagues/140.png"),
    OnboardingLeague(135, "Serie A", "Italy", "https://media.api-sports.io/football/leagues/135.png"),
    OnboardingLeague(78, "Bundesliga", "Germany", "https://media.api-sports.io/football/leagues/78.png"),
    OnboardingLeague(61, "Ligue 1", "France", "https://media.api-sports.io/football/leagues/61.png"),
    OnboardingLeague(2, "Champions League", "Europe", "https://media.api-sports.io/football/leagues/2.png"),
    OnboardingLeague(88, "Eredivisie", "Netherlands", "https://media.api-sports.io/football/leagues/88.png"),
    OnboardingLeague(94, "Primeira Liga", "Portugal", "https://media.api-sports.io/football/leagues/94.png"),
    OnboardingLeague(253, "MLS", "USA", "https://media.api-sports.io/football/leagues/253.png"),
    OnboardingLeague(307, "Saudi League", "Saudi Arabia", "https://media.api-sports.io/football/leagues/307.png"),
    OnboardingLeague(15, "Serie A", "Brazil", "https://media.api-sports.io/football/leagues/15.png"),
    OnboardingLeague(3, "Europa League", "Europe", "https://media.api-sports.io/football/leagues/3.png")
)

@Composable
fun OnboardingPageOne(
    viewModel: OnboardingViewModel,
    onNext: () -> Unit
) {
    val selectedLeagues by viewModel.selectedLeagues.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(36.dp))
        
        // Progress indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.primary))
            Box(modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
            Box(modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "CHOOSE YOUR LEAGUES",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Select your favorite leagues to customize your feed and track fixtures.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(TOP_LEAGUES) { league ->
                val isSelected = selectedLeagues.contains(league.id)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clickable { viewModel.toggleLeague(league.id) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AsyncImage(
                                model = league.logoUrl,
                                contentDescription = league.name,
                                modifier = Modifier
                                    .size(44.dp)
                                    .padding(4.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = league.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onNext,
            enabled = selectedLeagues.isNotEmpty(),
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
                text = "CONTINUE",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
