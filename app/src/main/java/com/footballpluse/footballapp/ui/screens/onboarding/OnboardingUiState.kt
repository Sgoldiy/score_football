package com.footballpluse.footballapp.ui.screens.onboarding

import com.footballpluse.footballapp.domain.model.LeagueInfo

data class OnboardingState(
    val step: Int = 1,
    val isLoading: Boolean = false,
    val error: String? = null,
    val topLeagues: List<LeagueInfo> = emptyList(),
    val selectedLeague: LeagueInfo? = null,
    val leagueClubs: List<ClubItem> = emptyList(),
    val primaryClub: ClubItem? = null,
    val allClubs: List<ClubItem> = emptyList(),
    val followedClubIds: Set<Int> = emptySet()
)

data class ClubItem(
    val id: Int,
    val name: String,
    val crestUrl: String?,
    val leagueName: String = ""
)

data class OnboardingUiState(
    val username: String = "",
    val usernameStatus: UsernameStatus = UsernameStatus.Idle,
    val suggestions: List<String> = emptyList(),
    val selectedLeague: League? = null,
    val selectedClubs: List<Club> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed class UsernameStatus {
    data object Idle : UsernameStatus()
    data object Typing : UsernameStatus()
    data object Checking : UsernameStatus()
    data class Available(val username: String) : UsernameStatus()
    data class Taken(val username: String) : UsernameStatus()
    data class Invalid(val reason: String) : UsernameStatus()
}

sealed class NavigationEvent {
    data object GoToLeagueScreen : NavigationEvent()
}

data class League(
    val id: String,
    val name: String,
    val country: String,
    val emoji: String,
    val logoUrl: String? = null
)

data class Club(
    val id: String,
    val name: String,
    val leagueId: String,
    val logoUrl: String? = null,
    val rank: Int = 0
)

sealed class OnboardingEvent {
    data object NavigateToClubs : OnboardingEvent()
    data object NavigateToLeague : OnboardingEvent()
    data object NavigateToHome : OnboardingEvent()
    data class ShowSnackbar(val message: String) : OnboardingEvent()
}
