package com.footballpluse.footballapp.ui.screens.onboarding.flow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballpluse.footballapp.data.local.DataStoreManager
import com.footballpluse.footballapp.data.repository.FavouriteRepository
import com.footballpluse.footballapp.data.repository.OnboardingRepository
import com.footballpluse.footballapp.domain.model.OnboardingClub
import com.footballpluse.footballapp.domain.model.OnboardingDefaults
import com.footballpluse.footballapp.domain.model.OnboardingLeague
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingClubUiState(
    val leagues: List<OnboardingLeague> = OnboardingDefaults.leagues,
    val selectedTabLeagueId: Int = leaguesFirstId(),
    val selectedClubIds: Set<Int> = emptySet(),
    val isLoadingPrefill: Boolean = false,
    val isSaving: Boolean = false
) {
    companion object {
        private fun leaguesFirstId() = OnboardingDefaults.leagues.first().id
    }
}

@HiltViewModel
class OnboardingClubViewModel @Inject constructor(
    private val session: OnboardingSession,
    private val dataStoreManager: DataStoreManager,
    private val favouriteRepository: FavouriteRepository,
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingClubUiState())
    val uiState: StateFlow<OnboardingClubUiState> = _uiState.asStateFlow()

    fun init(mode: String) {
        if (mode == "edit") {
            prefillFromSaved()
        } else {
            _uiState.value = _uiState.value.copy(selectedTabLeagueId = session.selectedLeague.value.id)
        }
    }

    private fun prefillFromSaved() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingPrefill = true)
            val savedLeagueId = dataStoreManager.favouriteLeagueId.first()
            val savedLeague = OnboardingDefaults.leagues.firstOrNull { it.id == savedLeagueId }
            if (savedLeague != null) {
                session.setLeague(savedLeague)
            }
            val savedClubs = favouriteRepository.getFavouriteClubs().first()
            session.setClubs(
                savedClubs.map { c -> OnboardingClub(clubId = c.clubId, clubName = c.clubName, leagueId = c.leagueId) }
            )
            _uiState.value = _uiState.value.copy(
                selectedTabLeagueId = savedLeagueId,
                selectedClubIds = savedClubs.map { it.clubId }.toSet(),
                isLoadingPrefill = false
            )
        }
    }

    fun onTabSelected(leagueId: Int) {
        _uiState.value = _uiState.value.copy(selectedTabLeagueId = leagueId)
    }

    fun toggleClub(club: OnboardingClub) {
        val current = _uiState.value.selectedClubIds.toMutableSet()
        if (current.contains(club.clubId)) current.remove(club.clubId) else current.add(club.clubId)
        _uiState.value = _uiState.value.copy(selectedClubIds = current)
        session.setClubs(current.toOnboardingClubs())
    }

    fun continueWithSelection() {
        session.setClubs(_uiState.value.selectedClubIds.toOnboardingClubs())
    }

    fun save(mode: String, onDone: () -> Unit) {
        val clubs = _uiState.value.selectedClubIds.toOnboardingClubs()
        if (clubs.isEmpty()) return
        val league = session.selectedLeague.value

        _uiState.value = _uiState.value.copy(isSaving = true)
        viewModelScope.launch {
            onboardingRepository.saveSelections(
                favouriteLeague = league,
                clubs = clubs,
                markOnboardingCompleted = (mode != "edit")
            )
            _uiState.value = _uiState.value.copy(isSaving = false)
            onDone()
        }
    }

    fun clubsForLeague(leagueId: Int): List<OnboardingClub> =
        OnboardingDefaults.clubsByLeagueId[leagueId].orEmpty()

    private fun Set<Int>.toOnboardingClubs(): List<OnboardingClub> {
        val byId = OnboardingDefaults.clubsByLeagueId.values.flatten().associateBy { it.clubId }
        return this.mapNotNull { id -> byId[id] }
    }
}
