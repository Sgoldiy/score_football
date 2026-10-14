package com.footballpluse.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballpluse.footballapp.data.repository.FavouriteRepository
import com.footballpluse.footballapp.data.util.ApiResult
import com.footballpluse.footballapp.domain.model.FavouritePlayer
import com.footballpluse.footballapp.domain.model.PlayerDetail
import com.footballpluse.footballapp.domain.repository.FootballRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: FootballRepository,
    private val favouriteRepository: FavouriteRepository
) : ViewModel() {

    private val _playerState = MutableStateFlow<ApiResult<PlayerDetail>>(ApiResult.Loading)
    val playerState: StateFlow<ApiResult<PlayerDetail>> = _playerState

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite

    fun loadPlayerData(playerId: Int, season: Int) {
        viewModelScope.launch {
            _playerState.value = ApiResult.Loading
            _playerState.value = repository.getPlayerDetail(playerId, season)
        }
        viewModelScope.launch {
            favouriteRepository.isPlayerFavorite(playerId).collect {
                _isFavorite.value = it
            }
        }
    }

    fun toggleFavoritePlayer(detail: PlayerDetail) {
        viewModelScope.launch {
            val p = detail.info
            val team = detail.stats.firstOrNull()?.team
            if (_isFavorite.value) {
                favouriteRepository.removeFavouritePlayer(p.id)
            } else {
                favouriteRepository.addFavouritePlayer(
                    FavouritePlayer(
                        id = 0,
                        playerId = p.id,
                        playerName = p.name,
                        clubId = team?.id ?: 0,
                        clubName = team?.name ?: "",
                        position = p.type ?: "",
                        photoUrl = p.photo ?: ""
                    )
                )
            }
        }
    }
}
