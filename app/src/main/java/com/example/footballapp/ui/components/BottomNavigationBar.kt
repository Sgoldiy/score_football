package com.example.footballapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.footballapp.navigation.Screen
import com.example.footballapp.ui.theme.ElectricBlue
import com.example.footballapp.ui.theme.IceBlue
import com.example.footballapp.ui.theme.LiveGreen
import com.example.footballapp.ui.theme.PitchBlack
import com.example.footballapp.ui.theme.PitchSurfaceHigh
import com.example.footballapp.ui.theme.TextSecondary

data class BottomNavItem(
    val label: String,
    val route: String,
    val icon: ImageVector
)

@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit
) {
    val items = listOf(
        BottomNavItem("Home", Screen.Home.route, Icons.Default.Home),
        BottomNavItem("Fixtures", Screen.Fixtures.route, Icons.Default.DateRange),
        BottomNavItem("Leagues", Screen.Leagues.route, Icons.Default.Star)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glassmorphism Background with Blur
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(35.dp),
                    ambientColor = ElectricBlue.copy(alpha = 0.2f),
                    spotColor = ElectricBlue.copy(alpha = 0.3f)
                )
                .clip(RoundedCornerShape(35.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(35.dp)
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else TextSecondary,
                    animationSpec = tween(400),
                    label = "nav-color"
                )
                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.2f else 1f,
                    animationSpec = tween(400),
                    label = "nav-scale"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onItemClick(item) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isSelected) {
                            // Animated Glow behind icon
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                ElectricBlue.copy(alpha = 0.2f),
                                                Color.Transparent
                                            )
                                        ),
                                        CircleShape
                                    )
                            )
                        }
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = contentColor,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                        )
                    }
                    if (isSelected) {
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(LiveGreen)
                        )
                    }
                }
            }
        }
    }
}
