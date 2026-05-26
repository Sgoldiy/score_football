package com.example.footballapp.ui.screens.onboarding.flow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.footballapp.domain.model.OnboardingDefaults
import com.example.footballapp.domain.model.OnboardingLeague
import com.example.footballapp.ui.theme.DeepNavy
import com.example.footballapp.ui.theme.GlassGlowGreen
import com.example.footballapp.ui.theme.TextSecondary

@Composable
fun OnboardingLeagueScreen(
    onContinue: () -> Unit,
    viewModel: OnboardingLeagueViewModel = hiltViewModel()
) {
    val selected by viewModel.selectedLeague.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp, bottom = 24.dp)
    ) {
        OnboardingProgressDots(step = 1, totalDots = 3, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(18.dp))
        Text(text = "Pick Your League", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(6.dp))
        Text(text = "Choose the league you follow most", fontSize = 14.sp, color = Color.White.copy(alpha = 0.60f))
        Spacer(Modifier.height(18.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            viewModel.leagues.forEach { league ->
                LeagueCard(
                    league = league,
                    selected = league.id == selected.id,
                    onClick = { viewModel.selectLeague(league) }
                )
            }
        }

        OnboardingPrimaryButton(
            text = "Continue",
            enabled = true,
            onClick = onContinue
        )
    }
}

@Composable
private fun LeagueCard(
    league: OnboardingLeague,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) GlassGlowGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)
    val border = if (selected) BorderStroke(1.5.dp, GlassGlowGreen) else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = border
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = OnboardingDefaults.leagueLogoUrl(league.id),
                contentDescription = league.name,
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = league.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = league.country, fontSize = 12.sp, color = TextSecondary)
            }
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (selected) GlassGlowGreen else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = DeepNavy,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
