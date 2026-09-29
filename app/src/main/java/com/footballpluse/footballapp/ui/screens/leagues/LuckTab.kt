package com.footballpluse.footballapp.ui.screens.leagues

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.footballpluse.footballapp.data.util.ApiResult

private val CardBg = Color(0xFF161616)
private val CardBorder = Color(0xFF242424)
private val AccentGreen = Color(0xFF4ADE80)
private val AccentRed = Color(0xFFF87171)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFA0A0A0)
private val TextTertiary = Color(0xFF777777)

/**
 * Luck & xPts tab: how many points each team ACTUALLY has vs what their
 * performance DESERVED (expected points), straight from the FC table
 * view=luck payload — luck category, luckiest and unluckiest results included.
 */
@Composable
fun LuckTab(
    luck: ApiResult<LuckUiModel>,
    onTeamClick: (Int) -> Unit
) {
    when (luck) {
        is ApiResult.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentGreen)
            }
        }
        is ApiResult.Error -> {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = luck.message ?: "Luck data unavailable",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        is ApiResult.Success -> LuckContent(luck.data, onTeamClick)
    }
}

@Composable
private fun LuckContent(data: LuckUiModel, onTeamClick: (Int) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Expected points vs actual points \u2014 who has been lucky, who has been robbed",
                color = TextTertiary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
        items(data.rows, key = { it.team.id }) { row ->
            LuckRowCard(row, onTeamClick)
        }
    }
}

@Composable
private fun LuckRowCard(row: LuckRowUiModel, onTeamClick: (Int) -> Unit) {
    val luckDiff = row.luckDifference ?: 0f
    val lucky = luckDiff >= 0f
    val accent = if (lucky) AccentGreen else AccentRed
    val luckLabel = when (row.luckCategory) {
        "very_lucky" -> "Very lucky"
        "lucky" -> "Lucky"
        "unlucky" -> "Unlucky"
        "very_unlucky" -> "Very unlucky"
        else -> "Fair"
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${row.position}",
                    color = TextTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(24.dp)
                )
                AsyncImage(
                    model = row.team.logo,
                    contentDescription = row.team.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0F0F0F))
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = row.team.name,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = luckLabel,
                        color = accent,
                        fontSize = 10.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${row.points}",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "pts",
                            color = TextTertiary,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    row.expectedPoints?.let { xp ->
                        Text(
                            text = "xPts %.1f".format(xp),
                            color = accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = CardBorder, thickness = 1.dp)
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth()) {
                Text(
                    text = luckDiff.let { "%s%.1f luck".format(if (it >= 0) "+" else "", it) },
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = row.played.let { "$it played" },
                    color = TextTertiary,
                    fontSize = 11.sp
                )
                row.expectedPosition?.let { ep ->
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "exp. pos $ep",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            val luckiest = row.luckiestResult?.takeIf { it.isNotBlank() }
            val unluckiest = row.unluckiestResult?.takeIf { it.isNotBlank() }
            if (luckiest != null || unluckiest != null) {
                Spacer(Modifier.height(8.dp))
                luckiest?.let {
                    Text("\uD83C\uDF40 $it", color = AccentGreen, fontSize = 10.sp, maxLines = 1)
                }
                unluckiest?.let {
                    Text("\uD83D\uDC94 $it", color = AccentRed, fontSize = 10.sp, maxLines = 1)
                }
            }
        }
    }
}
