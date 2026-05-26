package com.example.footballapp.ui.screens.onboarding.flow

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.footballapp.ui.theme.DeepNavy
import com.example.footballapp.ui.theme.GlassGlowGreen

@Composable
fun OnboardingPrimaryButton(
    text: String,
    enabled: Boolean,
    loading: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = GlassGlowGreen,
            contentColor = DeepNavy,
            disabledContainerColor = GlassGlowGreen.copy(alpha = 0.40f),
            disabledContentColor = DeepNavy.copy(alpha = 0.70f)
        )
    ) {
        if (loading) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = DeepNavy,
                modifier = Modifier
                    .padding(end = 10.dp)
                    .size(18.dp)
            )
        }
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun OnboardingProgressDots(
    step: Int,
    totalDots: Int = 3,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalDots) { index ->
            val isActive = (index + 1) == step
            val size by animateDpAsState(
                targetValue = if (isActive) 10.dp else 6.dp,
                animationSpec = tween(200),
                label = "dot-size"
            )
            val color by animateColorAsState(
                targetValue = if (isActive) GlassGlowGreen else Color.White.copy(alpha = 0.25f),
                animationSpec = tween(200),
                label = "dot-color"
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(color)
                    .size(size)
            )
        }
    }
}

@Composable
fun OnboardingScreenBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .background(DeepNavy)
            .padding(horizontal = 16.dp)
    ) { content() }
}
