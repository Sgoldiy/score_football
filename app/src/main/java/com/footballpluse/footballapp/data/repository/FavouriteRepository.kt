package com.footballpluse.footballapp.data.repository

import com.footballpluse.footballapp.data.local.db.FavouriteClubDao
import com.footballpluse.footballapp.data.local.db.FavouriteClubEntity
import com.footballpluse.footballapp.data.local.db.FavouriteLeagueDao
import com.footballpluse.footballapp.data.local.db.FavouriteLeagueEntity
import com.footballpluse.footballapp.data.local.db.FavouritePlayerDao
import com.footballpluse.footballapp.data.local.db.FavouritePlayerEntity
import com.footballpluse.footballapp.domain.model.FavouriteClub
import com.footballpluse.footballapp.domain.model.FavouritePlayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavouriteRepository @Inject constructor(
    private val favouriteClubDao: FavouriteClubDao,
    private val favouriteLeagueDao: FavouriteLeagueDao,
    private val favouritePlayerDao: FavouritePlayerDao
) {
    fun getFavouriteClubs(): Flow<List<FavouriteClub>> =
        favouriteClubDao.getFavouriteClubs().map { list -> list.map { it.toDomain() } }

    fun getFavouriteLeagues(): Flow<List<com.footballpluse.footballapp.domain.model.LeagueInfo>> =
        favouriteLeagueDao.getFavouriteLeagues().map { list -> 
            list.map { 
                com.footballpluse.footballapp.domain.model.LeagueInfo(
                    id = it.leagueId,
                    name = it.leagueName,
                    logo = it.logoUrl,
                    country = it.country,
                    flag = null,
                    season = null
                )
            } 
        }

    fun getFavouritePlayers(): Flow<List<FavouritePlayer>> =
        favouritePlayerDao.getFavouritePlayers().map { list -> list.map { it.toDomain() } }

    suspend fun replaceFavouriteClubs(clubs: List<FavouriteClub>) {
        favouriteClubDao.clear()
        favouriteClubDao.insertAll(clubs.map { it.toEntity() })
    }

    suspend fun replaceFavouriteLeagues(leagues: List<com.footballpluse.footballapp.domain.model.LeagueInfo>) {
        favouriteLeagueDao.clear()
        favouriteLeagueDao.insertAll(leagues.map { 
            FavouriteLeagueEntity(
                leagueId = it.id,
                leagueName = it.name,
                country = it.country,
                logoUrl = it.logo,
                addedAt = System.currentTimeMillis()
            )
        })
    }

    suspend fun replaceFavouritePlayers(players: List<FavouritePlayer>) {
        favouritePlayerDao.clear()
        favouritePlayerDao.insertAll(players.map { it.toEntity() })
    }
}

private fun FavouriteClubEntity.toDomain(): FavouriteClub = FavouriteClub(
    id = id,
    clubId = clubId,
    clubName = clubName,
    leagueId = leagueId,
    leagueName = leagueName,
    logoUrl = logoUrl,
    addedAt = addedAt
)

private fun FavouritePlayerEntity.toDomain(): FavouritePlayer = FavouritePlayer(
    id = id,
    playerId = playerId,
    playerName = playerName,
    clubId = clubId,
    clubName = clubName,
    position = position,
    photoUrl = photoUrl,
    addedAt = addedAt
)

private fun FavouriteClub.toEntity(): FavouriteClubEntity = FavouriteClubEntity(
    // Keep the DB-generated id
    id = if (id == 0) 0 else id,
    clubId = clubId,
    clubName = clubName,
    leagueId = leagueId,
    leagueName = leagueName,
    logoUrl = logoUrl,
    addedAt = addedAt
)

private fun FavouritePlayer.toEntity(): FavouritePlayerEntity = FavouritePlayerEntity(
    id = if (id == 0) 0 else id,
    playerId = playerId,
    playerName = playerName,
    clubId = clubId,
    clubName = clubName,
    position = position,
    photoUrl = photoUrl,
    addedAt = addedAt
)
