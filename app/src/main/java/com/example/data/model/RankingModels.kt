package com.example.data.model

data class RankedUser(
    val rank: Int = 1,
    val userId: String = "",
    val name: String,
    val avatarEmoji: String = "🥜",
    val photoUri: String? = null,
    val unlockCount: Int = 0,
    val isCurrentUser: Boolean = false,
    val badgeTitle: String = "",
    val isKool: Boolean = false,
    val streakDays: Int = 1,
    val presetImageKey: String? = null
)
