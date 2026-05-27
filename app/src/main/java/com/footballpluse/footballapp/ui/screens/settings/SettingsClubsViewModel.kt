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
    private val favouriteRepository: FavouriteRepository
) : ViewModel() {
    val clubs: StateFlow<List<FavouriteClub>> =
        favouriteRepository.getFavouriteClubs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun removeClub(clubId: Int) {
        viewModelScope.launch {
            val updated = clubs.value.filterNot { it.clubId == clubId }
            favouriteRepository.replaceFavouriteClubs(updated)
        }
    }
}

