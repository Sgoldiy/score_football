package com.example.footballapp.ui.screens.onboarding

import androidx.compose.runtime.*

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    var currentPage by remember { mutableStateOf(1) }

    when (currentPage) {
        1 -> OnboardingPageOne(onNext = { currentPage = 2 })
        2 -> OnboardingPageTwo(onNext = { currentPage = 3 })
        3 -> OnboardingPageThree(onGetStarted = onComplete)
    }
}
