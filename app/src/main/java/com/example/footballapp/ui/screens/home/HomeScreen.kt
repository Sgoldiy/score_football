package com.example.footballapp.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.footballapp.ui.components.LiveBadge
import com.example.footballapp.ui.components.MatchRow
import com.example.footballapp.ui.components.SectionHeader

@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToFavourites: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToMatchCenter: (String) -> Unit,
    onNavigateToLeagues: () -> Unit,
    onNavigateToPlayerProfile: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // A. Top App Bar
        item {
            HomeTopBar(
                onSearchClick = onNavigateToSearch,
                onFavouritesClick = onNavigateToFavourites,
                onNotificationsClick = onNavigateToNotifications
            )
        }

        // B. Live Match Hero Card
        item {
            LiveMatchHeroCard(
                onClick = { onNavigateToMatchCenter("live_1") }
            )
        }

        // C. Featured Matches Carousel
        item {
            SectionHeader(title = "Featured", actionText = "See all", onAction = {})
            FeaturedMatchesCarousel(onMatchClick = onNavigateToMatchCenter)
        }

        // D. Top Leagues Horizontal Scroll
        item {
            SectionHeader(title = "Top Leagues", actionText = "Follow", onAction = onNavigateToLeagues)
            TopLeaguesRow()
        }

        // E. Players to Watch
        item {
            SectionHeader(title = "Players to watch", actionText = "Explore", onAction = {})
            PlayersToWatchRow(onPlayerClick = onNavigateToPlayerProfile)
        }

        // F. Filter Tabs
        item {
            FilterTabs()
        }

        // G. Match List
        items(5) { // Placeholder for grouped matches
            MatchGroup(onMatchClick = onNavigateToMatchCenter)
        }
        
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun HomeTopBar(
    onSearchClick: () -> Unit,
    onFavouritesClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Football Plus",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Live scores, stats, lineups",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TopBarIcon(Icons.Rounded.Search, onSearchClick)
            TopBarIcon(Icons.Rounded.FavoriteBorder, onFavouritesClick)
            TopBarIcon(Icons.Rounded.Notifications, onNotificationsClick)
        }
    }
}

@Composable
fun TopBarIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun LiveMatchHeroCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = "", // Competition Logo
                        contentDescription = null,
                        modifier = Modifier.size(32.dp).clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Premier League", style = MaterialTheme.typography.labelLarge)
                }
                LiveBadge(minute = "64")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamHeroColumn("Arsenal", "")
                Text(
                    text = "2 - 1",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                TeamHeroColumn("Chelsea", "")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Second Half",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TeamHeroColumn(name: String, logoUrl: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(
            model = logoUrl,
            contentDescription = null,
            modifier = Modifier.size(52.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(name, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun FeaturedMatchesCarousel(onMatchClick: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(5) {
            FeaturedMatchCard(onClick = { onMatchClick("featured_$it") })
        }
    }
}

@Composable
fun FeaturedMatchCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .height(120.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("La Liga", style = MaterialTheme.typography.labelSmall, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = "", contentDescription = null, modifier = Modifier.size(32.dp))
                Text(" 21:00 ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                AsyncImage(model = "", contentDescription = null, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            Text("Upcoming", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TopLeaguesRow() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val leagues = listOf("Premier League", "La Liga", "Bundesliga", "Serie A", "Ligue 1", "Champions League")
        items(leagues) { league ->
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.Gray))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(league, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun PlayersToWatchRow(onPlayerClick: (Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(5) {
            PlayerCard(onPlayerClick = onPlayerClick)
        }
    }
}

@Composable
fun PlayerCard(onPlayerClick: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onPlayerClick(1) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = "",
            contentDescription = null,
            modifier = Modifier.size(56.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Erling Haaland", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text("Manchester City", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatItem("G", "25")
            StatItem("A", "5")
            StatItem("R", "8.2")
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Row {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(2.dp))
        Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FilterTabs() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterTab("● Live 28", isSelected = true)
        FilterTab("Upcoming 57", isSelected = false)
        FilterTab("Finished 80", isSelected = false)
    }
}

@Composable
fun FilterTab(label: String, isSelected: Boolean) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MatchGroup(onMatchClick: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.Gray))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Premier League", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Text("3 matches", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column {
                MatchRowPlaceholder(onMatchClick)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                MatchRowPlaceholder(onMatchClick)
            }
        }
    }
}

@Composable
fun MatchRowPlaceholder(onMatchClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMatchClick("match_1") }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = "", contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Arsenal", style = MaterialTheme.typography.bodyMedium)
        }
        
        Text("2 : 1", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
            Text("Chelsea", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.width(8.dp))
            AsyncImage(model = "", contentDescription = null, modifier = Modifier.size(32.dp))
        }
    }
}
