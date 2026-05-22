package com.example.footballapp.ui.screens.onboarding

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.footballapp.ui.screens.onboarding.OnboardingPageOne
import com.example.footballapp.ui.screens.onboarding.OnboardingPageTwo
import com.example.footballapp.ui.screens.onboarding.OnboardingPageThree

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var currentPage by remember { mutableStateOf(1) }

    when (currentPage) {
        1 -> OnboardingPageOne(
            viewModel = viewModel,
            onNext = { currentPage = 2 }
        )
        2 -> OnboardingPageTwo(
            viewModel = viewModel,
            onNext = { currentPage = 3 },
            onBack = { currentPage = 1 }
        )
        3 -> OnboardingPageThree(
            viewModel = viewModel,
            onGetStarted = {
                viewModel.completeOnboarding()
                onFinish()
            },
            onBack = { currentPage = 2 }
        )
    }
}
