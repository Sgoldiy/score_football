package com.example.footballapp.ui.screens.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
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
import com.example.footballapp.ui.theme.DeepNavy
import com.example.footballapp.ui.theme.GlassGlowGreen
import com.example.footballapp.ui.theme.TextSecondary

@Composable
fun FollowClubsPage(
    state: OnboardingState,
    onToggleClub: (Int) -> Unit,
    onBack: () -> Unit,
    onGetStarted: () -> Unit
) {
    val maxClubs = 10
    val selectedCount = state.followedClubIds.size

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
        }

        Text(
            text = "FOLLOW MORE",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Follow up to $maxClubs clubs across all leagues",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        // Counter
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(
                    if (selectedCount >= maxClubs) GlassGlowGreen.copy(alpha = 0.15f)
                    else Color.White.copy(alpha = 0.06f)
                )
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "$selectedCount / $maxClubs selected",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (selectedCount >= maxClubs) GlassGlowGreen else Color.White.copy(alpha = 0.7f)
            )
        }

        Spacer(Modifier.height(20.dp))

        if (state.isLoading && state.allClubs.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GlassGlowGreen)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val maxReached = state.followedClubIds.size >= 10
                val primaryClubId = state.primaryClub?.id
                val grouped = state.allClubs.groupBy { it.leagueName }
                grouped.forEach { (leagueName, clubs) ->
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            text = leagueName.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(clubs, key = { it.id }) { club ->
                        ClubGridCard(
                            club = club,
                            isSelected = state.followedClubIds.contains(club.id),
                            isLocked = club.id == primaryClubId,
                            isMaxReached = maxReached,
                            onClick = { onToggleClub(club.id) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GlassGlowGreen,
                contentColor = DeepNavy
            )
        ) {
            Text(
                text = "GET STARTED",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }
    }
}

@Composable
private fun ClubGridCard(
    club: ClubItem,
    isSelected: Boolean,
    isLocked: Boolean,
    isMaxReached: Boolean,
    onClick: () -> Unit
) {
    val dimmed = !isSelected && !isLocked && isMaxReached
    val alpha = if (dimmed) 0.4f else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .then(
                if (!isLocked) Modifier.clickable { onClick() }
                else Modifier
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) GlassGlowGreen.copy(alpha = 0.10f * alpha)
            else Color.White.copy(alpha = 0.04f * alpha)
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) GlassGlowGreen.copy(alpha = alpha)
            else Color.White.copy(alpha = 0.08f * alpha)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f * alpha)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = club.crestUrl,
                        contentDescription = club.name,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = club.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = alpha),
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
            if (isLocked) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = "Primary club",
                        tint = GlassGlowGreen,
                        modifier = Modifier.size(14.dp).align(Alignment.TopEnd)
                    )
            }
        }
    }
}

