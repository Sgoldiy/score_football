package com.footballpluse.footballapp.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballpluse.footballapp.data.repository.FavouriteRepository
import com.footballpluse.footballapp.domain.model.FavouriteClub
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsClubsViewModel @Inject constructor(
    private val favouriteRepository: FavouriteRepository,
    private val appSettingsDataStore: com.footballpluse.footballapp.data.local.AppSettingsDataStore
) : ViewModel() {
    val clubs: StateFlow<List<FavouriteClub>> =
        favouriteRepository.getFavouriteClubs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val hideScoresMode: StateFlow<Boolean> =
        appSettingsDataStore.hideScoresMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun toggleHideScoresMode(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsDataStore.saveHideScoresMode(enabled)
        }
    }

    fun removeClub(clubId: Int) {
        viewModelScope.launch {
            val updated = clubs.value.filterNot { it.clubId == clubId }
            favouriteRepository.replaceFavouriteClubs(updated)
        }
    }
}

