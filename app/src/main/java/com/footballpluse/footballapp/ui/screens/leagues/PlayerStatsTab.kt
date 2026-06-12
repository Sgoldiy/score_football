package com.footballpluse.footballapp.ui.screens.leagues

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.footballpluse.footballapp.data.util.ApiResult

@Composable
fun PlayerStatsTab(
    topScorers: ApiResult<List<PlayerStatUiModel>>,
    topAssists: ApiResult<List<PlayerStatUiModel>>,
    topYellowCards: ApiResult<List<PlayerStatUiModel>>,
    topRedCards: ApiResult<List<PlayerStatUiModel>>
) {
    var selectedCategory by remember { mutableStateOf(StatCategory.GOALS) }

    Column(modifier = Modifier.fillMaxSize()) {
        StatCategoryChips(
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it }
        )

        Spacer(Modifier.height(8.dp))

        StatsSummaryGrid(
            topScorers = topScorers,
            topAssists = topAssists,
            onCategoryClick = { selectedCategory = it }
        )

        Spacer(Modifier.height(12.dp))

        val activeResult = when (selectedCategory) {
            StatCategory.GOALS -> topScorers
            StatCategory.ASSISTS -> topAssists
            StatCategory.CARDS -> topYellowCards
            else -> topScorers
        }

        when (activeResult) {
            is ApiResult.Loading -> {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF4ADE80))
                }
            }
            is ApiResult.Success -> {
                val players = activeResult.data
                if (players.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No statistical records found", color = Color(0xFFA0A0A0))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(players) { _, player ->
                            PlayerStatRow(
                                player = player,
                                isSelected = false
                            )
                        }
                    }
                }
            }
            is ApiResult.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(activeResult.message, color = Color(0xFFEF4444))
                }
            }
        }
    }
}

@Composable
fun StatCategoryChips(
    selectedCategory: StatCategory,
    onCategorySelected: (StatCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCategory.entries.forEach { category ->
            val isSelected = category == selectedCategory
            Surface(
                onClick = { onCategorySelected(category) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) Color(0xFF14532D) else Color(0xFF1E1E1E),
                border = BorderStroke(
                    0.5.dp,
                    if (isSelected) Color(0xFF4ADE80).copy(alpha = 0.5f) else Color(0xFF242424)
                )
            ) {
                Text(
                    text = category.label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected) Color(0xFF4ADE80) else Color(0xFFA0A0A0),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun StatsSummaryGrid(
    topScorers: ApiResult<List<PlayerStatUiModel>>,
    topAssists: ApiResult<List<PlayerStatUiModel>>,
    onCategoryClick: (StatCategory) -> Unit
) {
    val topScorer = (topScorers as? ApiResult.Success)?.data?.firstOrNull()
    val topAssist = (topAssists as? ApiResult.Success)?.data?.firstOrNull()

    if (topScorer == null && topAssist == null) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatsSummaryCard(
            modifier = Modifier.weight(1f),
            label = "Top Scorer",
            playerName = topScorer?.playerName ?: "-",
            value = "${topScorer?.statValue ?: 0} goals",
            clubName = topScorer?.clubName ?: "",
            onClick = { onCategoryClick(StatCategory.GOALS) }
        )
        StatsSummaryCard(
            modifier = Modifier.weight(1f),
            label = "Most Assists",
            playerName = topAssist?.playerName ?: "-",
            value = "${topAssist?.statValue ?: 0} assists",
            clubName = topAssist?.clubName ?: "",
            onClick = { onCategoryClick(StatCategory.ASSISTS) }
        )
    }
}

@Composable
private fun StatsSummaryCard(
    modifier: Modifier = Modifier,
    label: String,
    playerName: String,
    value: String,
    clubName: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
        border = BorderStroke(0.5.dp, Color(0xFF242424))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                fontSize = 11.sp,
                letterSpacing = 0.08.sp,
                color = Color(0xFFA0A0A0)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = playerName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4ADE80)
            )
            Text(
                text = clubName,
                fontSize = 11.sp,
                color = Color(0xFFA0A0A0),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PlayerStatRow(
    player: PlayerStatUiModel,
    isSelected: Boolean
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (isSelected) player.progressFraction else player.progressFraction,
        animationSpec = tween(600),
        label = "stat-progress"
    )

    val rankColor = when (player.rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> Color(0xFFA0A0A0)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
        border = BorderStroke(0.5.dp, Color(0xFF242424))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${player.rank}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = rankColor,
                modifier = Modifier.width(28.dp)
            )

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(getAvatarColor(player.playerName)),
                contentAlignment = Alignment.Center
            ) {
                if (player.avatarUrl != null) {
                    AsyncImage(
                        model = player.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp).clip(CircleShape)
                    )
                } else {
                    Text(
                        text = player.playerName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifBlank { "P" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.playerName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = player.clubName,
                    fontSize = 11.sp,
                    color = Color(0xFFA0A0A0),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.width(50.dp)
            ) {
                Text(
                    text = "${player.statValue}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.End
                )
                if (player.secondaryStatLabel.isNotBlank()) {
                    Text(
                        text = player.secondaryStatLabel,
                        fontSize = 9.sp,
                        color = Color(0xFF555555),
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        StatProgressBar(
            progress = animatedProgress,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 50.dp, end = 66.dp, bottom = 8.dp)
        )
    }
}

@Composable
fun StatProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = 0.05f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF4ADE80))
        )
    }
}

private val avatarColors = listOf(
    Color(0xFF4ADE80), Color(0xFF3B82F6), Color(0xFFF59E0B),
    Color(0xFFEF4444), Color(0xFF8B5CF6), Color(0xFFEC4899),
    Color(0xFF14B8A6), Color(0xFFF97316)
)

private fun getAvatarColor(name: String): Color {
    val index = kotlin.math.abs(name.hashCode()) % avatarColors.size
    return avatarColors[index]
}
