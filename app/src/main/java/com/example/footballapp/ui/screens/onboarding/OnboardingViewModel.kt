package com.example.footballapp.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.footballapp.data.local.OnboardingDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingDataStore: OnboardingDataStore
) : ViewModel() {
    fun completeOnboarding() {
        viewModelScope.launch {
            onboardingDataStore.saveOnboardingCompleted(true)
        }
    }
}
