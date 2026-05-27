package com.example.footballapp.ui.components

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.footballapp.R
import com.example.footballapp.domain.model.Match

data class CompetitionTheme(
    val backgroundStart: Color,
    val backgroundEnd: Color,
    val accentColor: Color,
    val emoji: String
)

val defaultTheme = CompetitionTheme(Color(0xFF1F2937), Color(0xFF374151), Color(0xFF9CA3AF), "⚽")

val competitionThemes = mapOf(
    39  to CompetitionTheme(Color(0xFF3B0764), Color(0xFF6D28D9), Color(0xFFC4B5FD), "🏴󠁧󠁢󠁥󠁮󠁧󠁿"),
    140 to CompetitionTheme(Color(0xFF7F1D1D), Color(0xFFB91C1C), Color(0xFFFCA5A5), "🇪🇸"),
    78  to CompetitionTheme(Color(0xFF18181B), Color(0xFF3F3F46), Color(0xFFFF6B6B), "🇩🇪"),
    135 to CompetitionTheme(Color(0xFF0C1445), Color(0xFF1E3A8A), Color(0xFF93C5FD), "🇮🇹"),
    61  to CompetitionTheme(Color(0xFF052E16), Color(0xFF065F46), Color(0xFF6EE7B7), "🇫🇷"),
    2   to CompetitionTheme(Color(0xFF172554), Color(0xFF1D4ED8), Color(0xFFBAE6FD), "⭐"),
    1   to CompetitionTheme(Color(0xFF451A03), Color(0xFF92400E), Color(0xFFFDE68A), "🌍")
)

@Composable
fun CompetitionCard(
    leagueId: Int,
    leagueName: String,
    logoUrl: String?,
    season: Int,
    onClick: () -> Unit
) {
    val theme = competitionThemes[leagueId] ?: defaultTheme

    Card(
        modifier = Modifier
            .width(200.dp)
            .height(220.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(theme.backgroundStart, theme.backgroundEnd)
                    )
                )
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.3f)
            ) {
                val seasonLabel = if (season == 2026) "2026"
                                  else "$season/${(season + 1).toString().takeLast(2)}"
                Text(
                    text = seasonLabel,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = theme.accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.Center)
                    .offset(y = (-16).dp)
                    .background(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (logoUrl != null) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = leagueName,
                        modifier = Modifier.size(52.dp),
                        placeholder = painterResource(R.drawable.ic_placeholder),
                        error = painterResource(R.drawable.ic_error)
                    )
                } else {
                    Text(text = theme.emoji, fontSize = 32.sp)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                        )
                    )
                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp, top = 20.dp)
            ) {
                Text(
                    text = leagueName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Standings",
                        color = theme.accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = theme.accentColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FeaturedLiveCard(match: Match) {
    val minute = match.elapsed ?: 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(110.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF0F2027))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.align(Alignment.TopStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFFEF4444), CircleShape)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "LIVE  ${minute}'",
                    color = Color(0xFFEF4444),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(
                        model = match.homeTeam.logo,
                        contentDescription = match.homeTeam.name,
                        modifier = Modifier.size(36.dp),
                        placeholder = painterResource(R.drawable.ic_placeholder),
                        error = painterResource(R.drawable.ic_error)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = match.homeTeam.name.take(12),
                        color = Color.White,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "${match.homeScore ?: 0}  –  ${match.awayScore ?: 0}",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(
                        model = match.awayTeam.logo,
                        contentDescription = match.awayTeam.name,
                        modifier = Modifier.size(36.dp),
                        placeholder = painterResource(R.drawable.ic_placeholder),
                        error = painterResource(R.drawable.ic_error)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = match.awayTeam.name.take(12),
                        color = Color.White,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun StatPill(icon: String, label: String, containerColor: Color, contentColor: Color) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(icon, color = contentColor, fontSize = 12.sp)
            Text(label, color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}
