package com.example.footballapp.ui.screens.onboarding.flow

import com.example.footballapp.domain.model.OnboardingLeague
import com.example.footballapp.domain.model.OnboardingClub
import com.example.footballapp.domain.model.OnboardingDefaults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory onboarding state shared across onboarding screens.
 * This is intentionally not persisted until the user taps Done on the final step.
 */
@Singleton
class OnboardingSession @Inject constructor() {
    private val _selectedLeague = MutableStateFlow(OnboardingDefaults.leagues.first())
    val selectedLeague: StateFlow<OnboardingLeague> = _selectedLeague.asStateFlow()

    private val _selectedClubs = MutableStateFlow<List<OnboardingClub>>(emptyList())
    val selectedClubs: StateFlow<List<OnboardingClub>> = _selectedClubs.asStateFlow()

    fun setLeague(league: OnboardingLeague) {
        _selectedLeague.value = league
    }

    fun setClubs(clubs: List<OnboardingClub>) {
        _selectedClubs.value = clubs
    }

    fun clear() {
        _selectedLeague.value = OnboardingDefaults.leagues.first()
        _selectedClubs.value = emptyList()
    }
}
