package com.example.footballapp.data.repository

import com.example.footballapp.data.local.DataStoreManager
import com.example.footballapp.domain.model.FavouriteClub
import com.example.footballapp.domain.model.OnboardingClub
import com.example.footballapp.domain.model.OnboardingLeague
import com.example.footballapp.domain.model.OnboardingDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingRepository @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val favouriteRepository: FavouriteRepository
) {
    suspend fun saveSelections(
        favouriteLeague: OnboardingLeague,
        clubs: List<OnboardingClub>,
        markOnboardingCompleted: Boolean
    ) {
        val now = System.currentTimeMillis()
        val favouriteClubs = clubs.map { club ->
            FavouriteClub(
                clubId = club.clubId,
                clubName = club.clubName,
                leagueId = club.leagueId,
                leagueName = OnboardingDefaults.leagues.firstOrNull { it.id == club.leagueId }?.name ?: "",
                logoUrl = OnboardingDefaults.clubLogoUrl(club.clubId),
                addedAt = now
            )
        }

        withContext(Dispatchers.IO) {
            coroutineScope {
                val ds = async {
                    dataStoreManager.saveFavouriteLeague(favouriteLeague.id, favouriteLeague.name)
                    if (markOnboardingCompleted) {
                        dataStoreManager.saveOnboardingCompleted(true)
                    }
                }
                val clubsJob = async { favouriteRepository.replaceFavouriteClubs(favouriteClubs) }

                ds.await()
                clubsJob.await()
            }
        }
    }
}
