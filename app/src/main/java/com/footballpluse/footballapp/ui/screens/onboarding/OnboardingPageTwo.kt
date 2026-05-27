package com.footballpluse.footballapp.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import com.footballpluse.footballapp.ui.theme.DeepNavy
import com.footballpluse.footballapp.ui.theme.GlassGlowGreen
import com.footballpluse.footballapp.ui.theme.TextSecondary

@Composable
fun ClubSelectionPage(
    state: OnboardingState,
    onSelectClub: (ClubItem) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "2 of 3",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Spacer(Modifier.width(48.dp))
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "YOUR CLUB",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Pick your primary club from ${state.selectedLeague?.name ?: ""}",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        if (state.isLoading && state.leagueClubs.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GlassGlowGreen)
            }
        } else {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.leagueClubs.forEach { club ->
                    val isSelected = state.primaryClub?.id == club.id
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) GlassGlowGreen else Color.White.copy(alpha = 0.10f),
                        animationSpec = tween(300),
                        label = "club-border"
                    )
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) GlassGlowGreen.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.04f),
                        animationSpec = tween(300),
                        label = "club-bg"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clickable { onSelectClub(club) },
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
                                    model = club.crestUrl,
                                    contentDescription = club.name,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = club.name,
                                modifier = Modifier.weight(1f),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
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
            enabled = state.primaryClub != null,
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
