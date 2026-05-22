package com.example.footballapp.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.footballapp.data.local.AppSettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val appSettingsDataStore: AppSettingsDataStore
) : ViewModel() {

    private val _navigationEvent = MutableSharedFlow<SplashNavigation>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    init {
        checkOnboardingStatus()
    }

    private fun checkOnboardingStatus() {
        viewModelScope.launch {
            delay(2000) // Show splash for 2 seconds
            val isCompleted = appSettingsDataStore.isOnboardingCompleted.first()
            if (isCompleted) {
                _navigationEvent.emit(SplashNavigation.NavigateToHome)
            } else {
                _navigationEvent.emit(SplashNavigation.NavigateToOnboarding)
            }
        }
    }
}

sealed class SplashNavigation {
    object NavigateToOnboarding : SplashNavigation()
    object NavigateToHome : SplashNavigation()
}
