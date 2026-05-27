package com.footballpluse.footballapp.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.footballpluse.footballapp.ui.theme.DeepNavy
import com.footballpluse.footballapp.ui.theme.GlassGlowGreen
import com.footballpluse.footballapp.ui.theme.TextSecondary

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
    ) {
        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                slideInHorizontally(
                    animationSpec = tween(350),
                    initialOffsetX = { fullWidth -> direction * fullWidth }
                ) + fadeIn(tween(300)) togetherWith
                slideOutHorizontally(
                    animationSpec = tween(350),
                    targetOffsetX = { fullWidth -> -direction * fullWidth }
                ) + fadeOut(tween(300))
            },
            label = "onboarding-step"
        ) { step ->
            when (step) {
                1 -> LeagueSelectionPage(
                    state = state,
                    onSelectLeague = { league ->
                        viewModel.selectLeague(league)
                    },
                    onNext = {
                        viewModel.goToStep(2)
                        viewModel.loadLeagueClubs()
                    }
                )
                2 -> ClubSelectionPage(
                    state = state,
                    onSelectClub = { club -> viewModel.setPrimaryClub(club) },
                    onBack = { viewModel.goToStep(1) },
                    onNext = {
                        viewModel.goToStep(3)
                        viewModel.loadAllClubs()
                    }
                )
                3 -> FollowClubsPage(
                    state = state,
                    onToggleClub = { viewModel.toggleFollowClub(it) },
                    onBack = { viewModel.goToStep(2) },
                    onGetStarted = { viewModel.completeOnboarding(onFinish) }
                )
            }
        }

        StepDots(
            currentStep = state.step,
            totalSteps = 3,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        )
    }
}

@Composable
private fun StepDots(
    currentStep: Int,
    totalSteps: Int = 3,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            val isActive = index < currentStep
            val isCurrent = index + 1 == currentStep
            Box(
                modifier = Modifier
                    .width(if (isCurrent) 28.dp else 8.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isActive) GlassGlowGreen
                        else Color.White.copy(alpha = 0.15f)
                    )
            )
        }
    }
}
