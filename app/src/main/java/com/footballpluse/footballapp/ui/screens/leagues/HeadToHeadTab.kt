package com.footballpluse.footballapp.ui.screens.leagues

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.footballpluse.footballapp.data.util.ApiResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeadToHeadTab(
    teams: ApiResult<List<TeamUiModel>>,
    selectedTeamA: TeamUiModel?,
    selectedTeamB: TeamUiModel?,
    h2hData: ApiResult<H2HUiModel>,
    onSelectTeamA: (TeamUiModel?) -> Unit,
    onSelectTeamB: (TeamUiModel?) -> Unit
) {
    var showTeamAPicker by remember { mutableStateOf(false) }
    var showTeamBPicker by remember { mutableStateOf(false) }
    val teamList = (teams as? ApiResult.Success)?.data ?: emptyList()

    if (showTeamAPicker) {
        TeamPickerModal(
            teams = teamList,
            onDismiss = { showTeamAPicker = false },
            onSelect = { onSelectTeamA(it); showTeamAPicker = false }
        )
    }
    if (showTeamBPicker) {
        TeamPickerModal(
            teams = teamList,
            onDismiss = { showTeamBPicker = false },
            onSelect = { onSelectTeamB(it); showTeamBPicker = false }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TeamSelectorRow(
                selectedTeamA = selectedTeamA,
                selectedTeamB = selectedTeamB,
                onSelectA = { showTeamAPicker = true },
                onSelectB = { showTeamBPicker = true }
            )
        }

        if (selectedTeamA != null && selectedTeamB != null) {
            when (h2hData) {
                is ApiResult.Loading -> {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF4ADE80))
                        }
                    }
                }
                is ApiResult.Success -> {
                    item { H2HSummaryCard(h2hData.data, selectedTeamA, selectedTeamB) }
                    item { SectionTitle("LAST MEETINGS") }
                    items(h2hData.data.lastMeetings) { meeting ->
                        PastMeetingRow(meeting, selectedTeamA.id, selectedTeamB.id)
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                    item { SectionTitle("STATS COMPARISON") }
                    items(h2hData.data.comparisonStats) { stat ->
                        StatComparisonBar(stat)
                    }
                }
                is ApiResult.Error -> {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(h2hData.message, color = Color(0xFFEF4444))
                        }
                    }
                }
            }
        } else {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Select two teams to view head-to-head stats",
                        color = Color(0xFFA0A0A0),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        letterSpacing = 0.08.sp,
        color = Color(0xFFA0A0A0),
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun TeamSelectorRow(
    selectedTeamA: TeamUiModel?,
    selectedTeamB: TeamUiModel?,
    onSelectA: () -> Unit,
    onSelectB: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TeamSelectorChip(
            team = selectedTeamA,
            placeholder = "Select Team A",
            onClick = onSelectA,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "vs",
            fontSize = 11.sp,
            color = Color(0xFF555555),
            fontWeight = FontWeight.Bold
        )

        TeamSelectorChip(
            team = selectedTeamB,
            placeholder = "Select Team B",
            onClick = onSelectB,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TeamSelectorChip(
    team: TeamUiModel?,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1E1E1E),
        border = BorderStroke(0.5.dp, Color(0xFF242424))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (team != null) {
                Box(
                    modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    coil.compose.AsyncImage(
                        model = team.logo,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = team.name,
                    fontSize = 12.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = placeholder,
                    fontSize = 12.sp,
                    color = Color(0xFFA0A0A0)
                )
            }
        }
    }
}

@Composable
fun H2HSummaryCard(
    h2h: H2HUiModel,
    teamA: TeamUiModel,
    teamB: TeamUiModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
        border = BorderStroke(0.5.dp, Color(0xFF242424))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = teamA.name.take(3).uppercase(),
                    fontSize = 11.sp,
                    color = Color(0xFFA0A0A0)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${h2h.teamAWins}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (h2h.teamAWins > h2h.teamBWins) Color(0xFF4ADE80) else Color.White
                )
                Text("Wins", fontSize = 10.sp, color = Color(0xFF555555))
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text("DRAW", fontSize = 11.sp, color = Color(0xFFA0A0A0))
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${h2h.draws}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF555555)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = teamB.name.take(3).uppercase(),
                    fontSize = 11.sp,
                    color = Color(0xFFA0A0A0)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${h2h.teamBWins}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (h2h.teamBWins > h2h.teamAWins) Color(0xFF4ADE80) else Color.White
                )
                Text("Wins", fontSize = 10.sp, color = Color(0xFF555555))
            }
        }
    }
}

@Composable
fun PastMeetingRow(
    meeting: PastMeeting,
    teamAId: Int,
    teamBId: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
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
                text = meeting.date,
                fontSize = 11.sp,
                color = Color(0xFF555555),
                modifier = Modifier.width(60.dp)
            )
            Text(
                text = meeting.score,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = meeting.competition,
                fontSize = 11.sp,
                color = Color(0xFFA0A0A0),
                modifier = Modifier.width(80.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun StatComparisonBar(stat: ComparisonStat) {
    val total = (stat.teamAValue + stat.teamBValue).takeIf { it > 0f } ?: 1f
    val aWeight = (stat.teamAValue / total).coerceIn(0.05f, 0.95f)
    val bWeight = (stat.teamBValue / total).coerceIn(0.05f, 0.95f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                stat.teamADisplay,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                stat.label,
                fontSize = 11.sp,
                color = Color(0xFFA0A0A0)
            )
            Text(
                stat.teamBDisplay,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
        Spacer(Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .weight(aWeight)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp))
                        .background(Color(0xFF4ADE80))
                )
                Spacer(Modifier.width(2.dp))
                Box(
                    modifier = Modifier
                        .weight(bWeight)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                        .background(Color(0xFF3B82F6))
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamPickerModal(
    teams: List<TeamUiModel>,
    onDismiss: () -> Unit,
    onSelect: (TeamUiModel) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161616),
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Select Team", fontWeight = FontWeight.Bold, color = Color.White)
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(teams) { team ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(team) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            coil.compose.AsyncImage(
                                model = team.logo,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = team.name,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF4ADE80))
            }
        }
    )
}
