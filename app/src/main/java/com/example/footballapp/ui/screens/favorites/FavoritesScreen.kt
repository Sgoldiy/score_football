package com.example.footballapp.ui.screens.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.footballapp.ui.components.InfoPill
import com.example.footballapp.ui.components.PremiumCard
import com.example.footballapp.ui.components.SectionTitle
import com.example.footballapp.ui.theme.TextSecondary

@Composable
fun FavoritesScreen(onBackClick: () -> Unit) {
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
                    SectionTitle("Favorites", trailing = "Follow system")
                    Text(
                        "Favorite teams, leagues, match reminders, and recent searches now have a production UI surface ready for Room-backed persistence.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        items(3) { index ->
            PremiumCard(Modifier.fillMaxWidth()) {
                Column {
                    Text("Favorite collection ${index + 1}", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                    Text("Live alerts, fixture shortcuts, and team follow states", color = TextSecondary)
                    InfoPill("Manage")
                }
            }
        }
    }
}
