package com.footballpluse.footballapp.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.footballpluse.footballapp.viewmodel.ThemeViewModel

@Composable
fun SettingsScreen(
    themeViewModel: ThemeViewModel,
    onBack: () -> Unit,
    onEditPreferences: () -> Unit
) {
    val clubsVm: SettingsClubsViewModel = hiltViewModel()
    val clubs by clubsVm.clubs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0F14))
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF131620))
                .border(0.5.dp, Color(0xFF1A1E2A), RoundedCornerShape(12.dp))
                .clickable { onEditPreferences() }
                .padding(vertical = 14.dp, horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Add More Clubs",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF00E676)
            )
        }

        Spacer(Modifier.height(12.dp))
        val hideScores by clubsVm.hideScoresMode.collectAsState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF131620))
                .border(0.5.dp, Color(0xFF1A1E2A), RoundedCornerShape(12.dp))
                .padding(vertical = 12.dp, horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Spoiler-Free Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = "Hide match scores to avoid spoilers when watching replays",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8B949E)
                )
            }
            androidx.compose.material3.Switch(
                checked = hideScores,
                onCheckedChange = { clubsVm.toggleHideScoresMode(it) },
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF0D0F14),
                    checkedTrackColor = Color(0xFF00E676),
                    uncheckedThumbColor = Color(0xFF8B949E),
                    uncheckedTrackColor = Color(0xFF1E2433)
                )
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "My Favourite Clubs",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(10.dp))

        if (clubs.isEmpty()) {
            Text(
                text = "No favourite clubs yet",
                color = Color(0xFF8B949E),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        } else {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                clubs.forEach { club ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF131620))
                            .border(0.5.dp, Color(0xFF1A1E2A), RoundedCornerShape(12.dp))
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = club.logoUrl,
                            contentDescription = club.clubName,
                            modifier = Modifier.width(28.dp).height(28.dp),
                            placeholder = androidx.compose.ui.res.painterResource(com.footballpluse.footballapp.R.drawable.ic_team_placeholder),
                            error = androidx.compose.ui.res.painterResource(com.footballpluse.footballapp.R.drawable.ic_team_placeholder)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = club.clubName,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Rounded.RemoveCircleOutline,
                            contentDescription = "Remove",
                            tint = Color(0xFFFF4444),
                            modifier = Modifier
                                .clickable { clubsVm.removeClub(club.clubId) }
                                .padding(6.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = "Back",
                color = Color(0xFF00E676),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(12.dp)
            )
        }
    }
}
