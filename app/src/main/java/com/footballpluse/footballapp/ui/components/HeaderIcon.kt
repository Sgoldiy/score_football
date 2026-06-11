package com.footballpluse.footballapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun HeaderIcon(
    icon: ImageVector,
    onClick: () -> Unit = {},
    badgeCount: Int = 0
) {
    Box(contentAlignment = Alignment.TopEnd) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFF131620), CircleShape)
                .border(0.5.dp, Color(0xFF1A1E2A), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 18.dp),
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFCCCCCC),
                modifier = Modifier.size(18.dp)
            )
        }
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(0xFF00E676), CircleShape)
                    .offset(x = (-2).dp, y = 2.dp)
            )
        }
    }
}
