package com.footballpluse.footballapp.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.footballpluse.footballapp.domain.model.LeagueInfo
import com.footballpluse.footballapp.ui.theme.DeepNavy
import com.footballpluse.footballapp.ui.theme.GlassGlowGreen
import com.footballpluse.footballapp.ui.theme.TextSecondary

@Composable
fun LeagueSelectionPage(
    state: OnboardingState,
    onSelectLeague: (LeagueInfo) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        Text(
            text = "YOUR LEAGUE",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Pick your favorite league to get started",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        if (state.isLoading && state.topLeagues.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GlassGlowGreen)
            }
        } else if (state.error != null && state.topLeagues.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(state.error, color = Color(0xFFE74C3C), textAlign = TextAlign.Center)
            }
        } else {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.topLeagues.forEach { league ->
                    val isSelected = state.selectedLeague?.id == league.id
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) GlassGlowGreen else Color.White.copy(alpha = 0.10f),
                        animationSpec = tween(300),
                        label = "league-border"
                    )
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) GlassGlowGreen.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.04f),
                        animationSpec = tween(300),
                        label = "league-bg"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clickable { onSelectLeague(league) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.06f)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = league.logo,
                                    contentDescription = league.name,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = league.name,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                league.country?.let {
                                    Text(
                                        text = it,
                                        fontSize = 13.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            if (isSelected) {
                                Text(
                                    text = "\u2713",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GlassGlowGreen
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onNext,
            enabled = state.selectedLeague != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GlassGlowGreen,
                contentColor = DeepNavy,
                disabledContainerColor = Color.White.copy(alpha = 0.08f),
                disabledContentColor = TextSecondary
            )
        ) {
            Text(
                text = "CONTINUE",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }
    }
}
