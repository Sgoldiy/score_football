package com.footballpluse.footballapp.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val uid: String,
    val username: String,
    val displayUsername: String,
    val favoriteLeague: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val onboardingComplete: Boolean = false
)
