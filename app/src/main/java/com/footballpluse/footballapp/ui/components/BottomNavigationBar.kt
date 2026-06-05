package com.footballpluse.footballapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.footballpluse.footballapp.navigation.Screen

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit
) {
    val items = listOf(
        BottomNavItem(Screen.Home.route, Icons.Rounded.Home, "Home"),
        BottomNavItem(Screen.Fixtures.route, Icons.Rounded.CalendarToday, "Fixtures"),
        BottomNavItem(Screen.Leagues.route, Icons.Rounded.EmojiEvents, "Leagues"),
        BottomNavItem(Screen.TopPlayers.route, Icons.Rounded.BarChart, "Stats"),
        BottomNavItem(Screen.Favourites.route, Icons.Outlined.StarBorder, "Favorites")
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFF131620))
            .border(width = 1.dp, color = Color(0xFF1A1E2A))
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        onClick = { onItemClick(item) }
                    )
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (item.route == Screen.Favourites.route && isSelected) Icons.Filled.Star else item.icon,
                    contentDescription = item.label,
                    tint = if (isSelected) Color(0xFF00E676) else Color(0xFF8B949E),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.label,
                    color = if (isSelected) Color(0xFF00E676) else Color(0xFF8B949E),
                    fontSize = 9.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676))
                    )
                } else {
                    Box(modifier = Modifier.size(3.dp)) // Empty space to preserve height alignment
                }
            }
        }
    }
}
