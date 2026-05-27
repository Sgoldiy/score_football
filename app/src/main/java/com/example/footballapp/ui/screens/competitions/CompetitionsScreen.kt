package com.example.footballapp.ui.screens.competitions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.footballapp.ui.theme.PitchBlack

private data class CompetitionCard(
    val id: String,
    val leagueId: Int,
    val name: String,
    val season: String,
    val emoji: String,
    val bgColor: Color,
    val accentColor: Color
)

private val COMPETITIONS = listOf(
    CompetitionCard(
        "pl", 39, "Premier League", "2024/25", "\uD83C\uDFC6",
        Color(0xFF4C1D95), Color(0xFFA78BFA)
    ),
    CompetitionCard(
        "ucl", 2, "Champions League", "2024/25", "\uD83C\uDFC6",
        Color(0xFF1E3A8A), Color(0xFF60A5FA)
    ),
    CompetitionCard(
        "laliga", 140, "La Liga", "2024/25", "\uD83C\uDFC6",
        Color(0xFF7F1D1D), Color(0xFFF87171)
    ),
    CompetitionCard(
        "bundesliga", 78, "Bundesliga", "2024/25", "\uD83C\uDFC6",
        Color(0xFF1C1C1E), Color(0xFFEF4444)
    ),
    CompetitionCard(
        "seriea", 135, "Serie A", "2024/25", "\uD83C\uDFC6",
        Color(0xFF0F172A), Color(0xFF3B82F6)
    ),
    CompetitionCard(
        "ligue1", 61, "Ligue 1", "2024/25", "\uD83C\uDFC6",
        Color(0xFF134E4A), Color(0xFF2DD4BF)
    ),
    CompetitionCard(
        "worldcup", 1, "FIFA World Cup", "2026", "\uD83C\uDF0D",
        Color(0xFF451A03), Color(0xFFF59E0B)
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompetitionsScreen(
    onBackClick: () -> Unit,
    onCompetitionClick: (leagueId: Int, season: Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Competitions", fontWeight = FontWeight.Bold, color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PitchBlack)
            )
        },
        containerColor = PitchBlack
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(COMPETITIONS, key = { it.id }) { comp ->
                CompetitionCardItem(
                    card = comp,
                    onClick = { onCompetitionClick(comp.leagueId, if (comp.id == "worldcup") 2026 else 2024) }
                )
            }
        }
    }
}

@Composable
private fun CompetitionCardItem(
    card: CompetitionCard,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = card.bgColor),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = card.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = card.accentColor
                    ) {
                        Text(
                            text = card.season,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "View Standings \u2192",
                        color = card.accentColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = card.emoji, fontSize = 24.sp)
            }
        }
    }
}
