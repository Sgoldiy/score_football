package com.footballpluse.footballapp.ui.screens.onboarding.flow

import androidx.lifecycle.ViewModel
import com.footballpluse.footballapp.domain.model.OnboardingDefaults
import com.footballpluse.footballapp.domain.model.OnboardingLeague
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class OnboardingLeagueViewModel @Inject constructor(
    private val session: OnboardingSession
) : ViewModel() {
    val leagues: List<OnboardingLeague> = OnboardingDefaults.leagues
    val selectedLeague: StateFlow<OnboardingLeague> = session.selectedLeague

    fun selectLeague(league: OnboardingLeague) {
        session.setLeague(league)
    }
}

