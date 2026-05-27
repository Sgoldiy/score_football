package com.footballpluse.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballpluse.footballapp.data.model.Country
import com.footballpluse.footballapp.data.model.LeagueResponse
import com.footballpluse.footballapp.data.repository.LeagueRepository
import com.footballpluse.footballapp.data.util.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LeaguesViewModel @Inject constructor(
    private val repository: LeagueRepository
) : ViewModel() {

    private val _leaguesState = MutableStateFlow<ApiResult<LeaguesData>>(ApiResult.Loading)
    val leaguesState: StateFlow<ApiResult<LeaguesData>> = _leaguesState

    init {
        loadLeaguesData()
    }

    fun loadLeaguesData() {
        viewModelScope.launch {
            _leaguesState.value = ApiResult.Loading
            try {
                // Fetch leagues primarily
                val leaguesResult = repository.getLeagues()
                
                if (leaguesResult is ApiResult.Success) {
                    // Fetch others optionally/in background to not block leagues
                    val countriesResult = try { repository.getCountries() } catch (e: Exception) { ApiResult.Error(e.message ?: "") }
                    val seasonsResult = try { repository.getSeasons() } catch (e: Exception) { ApiResult.Error(e.message ?: "") }

                    val countries = (countriesResult as? ApiResult.Success)?.data ?: emptyList()
                    val seasons = (seasonsResult as? ApiResult.Success)?.data ?: emptyList()

                    _leaguesState.value = ApiResult.Success(
                        LeaguesData(leaguesResult.data, countries, seasons)
                    )
                } else if (leaguesResult is ApiResult.Error) {
                    _leaguesState.value = ApiResult.Error(leaguesResult.message)
                }
            } catch (e: Exception) {
                _leaguesState.value = ApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }
}

data class LeaguesData(
    val leagues: List<LeagueResponse>,
    val countries: List<Country>,
    val seasons: List<Int>
)
