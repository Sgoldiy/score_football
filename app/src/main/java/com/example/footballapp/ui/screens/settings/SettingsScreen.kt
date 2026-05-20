package com.example.footballapp.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.footballapp.ui.components.PremiumCard
import com.example.footballapp.ui.components.SectionTitle
import com.example.footballapp.ui.theme.TextSecondary

@Composable
fun SettingsScreen(onBackClick: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 28.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PremiumCard(Modifier.fillMaxWidth()) {
                Column {
                    SectionTitle("Settings", trailing = "Match alerts")
                    Text(
                        "Notification reminders, dynamic theme, offline cache, and live update preferences.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        items(listOf("Goal notifications", "Kickoff reminders", "Dark football theme", "Offline cache")) { label ->
            PremiumCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(label, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                        Text("Preference shell ready for persistence", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(checked = label != "Offline cache", onCheckedChange = { })
                }
            }
        }
    }
}
