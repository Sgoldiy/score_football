package com.footballpluse.footballapp.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_clubs")
data class FavoriteClubEntity(
    @PrimaryKey val clubId: String,
    val clubName: String,
    val leagueId: String,
    val logoUrl: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)
