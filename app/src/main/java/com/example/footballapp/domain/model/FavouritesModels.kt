package com.example.footballapp.domain.model

data class FavouriteClub(
    val id: Int = 0,
    val clubId: Int,
    val clubName: String,
    val leagueId: Int,
    val leagueName: String,
    val logoUrl: String,
    val addedAt: Long
)

data class FavouritePlayer(
    val id: Int = 0,
    val playerId: Int,
    val playerName: String,
    val clubId: Int,
    val clubName: String,
    val position: String,
    val photoUrl: String,
    val addedAt: Long
)
