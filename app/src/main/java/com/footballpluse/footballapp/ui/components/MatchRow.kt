package com.footballpluse.footballapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import com.footballpluse.footballapp.domain.model.Match
import com.footballpluse.footballapp.ui.screens.home.FormDotsRow
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MatchRow(
    match: Match,
    homeForm: String = "",
    awayForm: String = "",
    onClick: (String) -> Unit
) {
    val isLive = match.isLive
    val isFinished = match.status.short in listOf("FT", "AET", "PEN")
    val isLiveOrFinished = isLive || isFinished

    val homeScore = match.homeScore
    val awayScore = match.awayScore
    val homeWon = isLiveOrFinished && homeScore != null && awayScore != null && homeScore > awayScore
    val awayWon = isLiveOrFinished && homeScore != null && awayScore != null && awayScore > homeScore
    val homeLost = isLiveOrFinished && homeScore != null && awayScore != null && homeScore < awayScore
    val awayLost = isLiveOrFinished && homeScore != null && awayScore != null && awayScore < homeScore

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(match.id.toString()) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status Badge Column (Left, fixed width 42dp)
        Box(
            modifier = Modifier.width(42.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLive -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1A0A0A))
                            .border(0.5.dp, Color(0xFF3A1212), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${match.elapsed ?: 0}'",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.W600,
                            color = Color(0xFFFF4444)
                        )
                    }
                }
                match.status.short == "FT" -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1E2230))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "FT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.W500,
                            color = Color(0xFF555555)
                        )
                    }
                }
                match.status.short == "PST" -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF0D1A10))
                            .border(0.5.dp, Color(0xFF1A3A22), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PST",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.W500,
                            color = Color(0xFF00E676)
                        )
                    }
                }
                else -> {
                    val timeLabel = remember(match.timestamp) {
                        try { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(match.timestamp * 1000L)) }
                        catch (e: Exception) { "--:--" }
                    }
                    Text(
                        text = timeLabel,
                        fontSize = 11.sp,
                        color = Color(0xFF555555),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Teams Stack Column (Center, expands to fill space)
        Column(modifier = Modifier.weight(1f)) {
            TeamRow(
                name = match.homeTeam.name,
                logoUrl = match.homeTeam.logo,
                score = homeScore,
                form = homeForm,
                isWinner = homeWon,
                isLoser = homeLost,
                isLiveOrFinished = isLiveOrFinished
            )
            Spacer(modifier = Modifier.height(6.dp))
            TeamRow(
                name = match.awayTeam.name,
                logoUrl = match.awayTeam.logo,
                score = awayScore,
                form = awayForm,
                isWinner = awayWon,
                isLoser = awayLost,
                isLiveOrFinished = isLiveOrFinished
            )
        }
    }
}

@Composable
private fun TeamBadge(logoUrl: String?, teamName: String) {
    var isError by remember { mutableStateOf(false) }
    if (logoUrl.isNullOrBlank() || isError) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E2433)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = teamName.take(1).uppercase(),
                color = Color(0xFF888888),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        AsyncImage(
            model = logoUrl,
            contentDescription = teamName,
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape),
            onError = { isError = true }
        )
    }
}

@Composable
private fun TeamRow(
    name: String,
    logoUrl: String?,
    score: Int?,
    form: String,
    isWinner: Boolean,
    isLoser: Boolean,
    isLiveOrFinished: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left part: badge and text column
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            TeamBadge(logoUrl = logoUrl, teamName = name)
            Spacer(modifier = Modifier.width(7.dp))
            Column {
                Text(
                    text = name,
                    fontSize = 13.sp,
                    fontWeight = if (isWinner) FontWeight.W600 else FontWeight.W400,
                    color = when {
                        isWinner -> Color(0xFFFFFFFF)
                        isLoser -> Color(0xFF444444)
                        else -> Color(0xFFAAAAAA)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                FormDotsRow(
                    form = form,
                    modifier = Modifier.padding(start = 0.dp),
                    dotSize = 7.dp,
                    gap = 4.dp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right part: Score column (fixed width 26dp, end aligned)
        Box(
            modifier = Modifier.width(26.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            val scoreText = if (isLiveOrFinished) score?.toString() ?: "0" else "—"
            val scoreColor = when {
                !isLiveOrFinished -> Color(0xFF333333)
                isWinner -> Color(0xFFFFFFFF)
                isLoser -> Color(0xFF444444)
                else -> Color(0xFFFFFFFF) // Draw score
            }
            val scoreWeight = when {
                !isLiveOrFinished -> FontWeight.W400
                isWinner -> FontWeight.W700
                isLoser -> FontWeight.W400
                else -> FontWeight.W500 // Draw score
            }
            Text(
                text = scoreText,
                fontSize = 13.sp,
                fontWeight = scoreWeight,
                color = scoreColor,
                textAlign = TextAlign.End
            )
        }
    }
}
