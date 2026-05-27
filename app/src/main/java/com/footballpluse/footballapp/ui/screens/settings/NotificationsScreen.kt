package com.footballpluse.footballapp.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.footballpluse.footballapp.ui.theme.DeepNavy
import com.footballpluse.footballapp.ui.theme.GlassGlowGreen
import com.footballpluse.footballapp.viewmodel.NotificationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Notifications",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepNavy,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = DeepNavy
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Customize which events you want to be notified about.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )

            NotificationToggle(
                title = "Match Start",
                description = "Get notified when a match begins",
                checked = uiState.matchStart,
                onCheckedChange = { viewModel.updateNotificationSetting("match_start", it) }
            )

            NotificationToggle(
                title = "Goals",
                description = "Instant alerts for every goal",
                checked = uiState.goal,
                onCheckedChange = { viewModel.updateNotificationSetting("goal", it) }
            )

            NotificationToggle(
                title = "Half & Full Time",
                description = "Results at intervals and final whistle",
                checked = uiState.halftimeFulltime,
                onCheckedChange = { viewModel.updateNotificationSetting("halftime_fulltime", it) }
            )

            NotificationToggle(
                title = "Red Cards",
                description = "Major disciplinary events in-game",
                checked = uiState.redCard,
                onCheckedChange = { viewModel.updateNotificationSetting("red_card", it) }
            )

            NotificationToggle(
                title = "VAR Decisions",
                description = "Crucial video assistant referee checks",
                checked = uiState.varDecisions,
                onCheckedChange = { viewModel.updateNotificationSetting("var_decisions", it) }
            )

            NotificationToggle(
                title = "Lineups",
                description = "Starting XI and substitutes release",
                checked = uiState.lineupReleased,
                onCheckedChange = { viewModel.updateNotificationSetting("lineup_released", it) }
            )
        }
    }
}

@Composable
private fun NotificationToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(
                    description,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = GlassGlowGreen,
                    checkedTrackColor = GlassGlowGreen.copy(alpha = 0.3f),
                    uncheckedThumbColor = Color.White.copy(alpha = 0.4f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                )
            )
        }
    }
}
