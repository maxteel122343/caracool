package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unlock_logs")
data class UnlockLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String, // e.g. "2026-08-13"
    val unlockNumberToday: Int,
    val phraseSpoken: String,
    val themeUsed: String = "classic"
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val id: Int = 1,
    val isFirstTimeSetupCompleted: Boolean = false,
    val userWallpaperUri: String? = null,
    val userPhotoUri: String? = null, // Photo taken by user
    val userProfilePhotoUri: String? = null, // User's personal avatar profile photo
    val userAvatarEmoji: String = "👾", // User's custom avatar emoji (Default: 👾 for Cara de Cu)
    val lockscreenPhotoUri: String? = null, // Specific wallpaper for lockscreen
    val homescreenPhotoUri: String? = null, // Specific wallpaper for homescreen
    val userName: String = "Você (Cara de Cu)",
    val selectedThemeId: String = "kool",
    val unlockAudioType: String = "tts_counter", // tts_counter, tts_standard, tts_custom, sound_pop, sound_chime, sound_cyber, custom_recording
    val customUnlockText: String = "Cara de cu desbloqueado!",
    val customLockText: String = "Cara de cu bloqueado!",
    val isAiConversationalEnabled: Boolean = true,
    val isAutoPostOnUnlockEnabled: Boolean = true, // Toggle to auto post photo to community feed upon unlock (Default: true)
    val isAutoSetUnlockPhotoAsLockWallpaper: Boolean = false, // Toggle: Every unlock photo taken becomes the lockscreen wallpaper only
    val isCommunitySliderEnabled: Boolean = false, // Toggle: Carousel / slider of community users' photos as lockscreen wallpaper
    val communitySliderInterval: String = "unlock", // "unlock" (every unlock), "5_min", "30_min"
    val activeLockscreenPostId: Long? = null, // ID of featured community post on lockscreen
    val aiPersonality: String = "humorada", // humorada, energetica, carinhosa, robotica
    val customAudioPath: String? = null,
    val streakDays: Int = 1,
    val lastActiveDate: String = "",
    val isAutoDeleteOldPosts24hEnabled: Boolean = false, // Persistent posts default for everyone
    val isAutoDeleteCustomizedByUser: Boolean = false, // Track if user manually set it
    val isCaraDeKoolMode: Boolean = true, // Default: Cara de Cu (true)
    val isSafeWordMode: Boolean = true, // Safe word mode: replaces 'cu' with 'cool' (Default: true)
    val koolColorPalette: String = "universal", // "universal" (Nova Paleta Laranja/Roxo), "pacoca" (Amendoim), "nude_peach" (Cara de Kool 1), "pink_berry" (Cara de Kool 2)
    val isPhotoFrameEnabled: Boolean = true // Toggle to apply kawaii character frame on photos (Default: true)
)

@Entity(tableName = "feed_posts")
data class FeedPost(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val authorName: String,
    val authorAvatarEmoji: String = "🥜",
    val authorAvatarUri: String? = null,
    val photoUri: String? = null,
    val photoBase64: String? = null, // Compressed Base64 for global real-time cloud sharing across devices
    val presetImageKey: String? = null, // e.g. "preset_lucas", "preset_mariana", "preset_gabriel", "preset_beatriz", "user_avatar", "kool_avatar"
    val caption: String,
    val unlockCount: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val myReactionEmoji: String? = null, // "❤️", "😂", "🥜", "🔥", "😮"
    val loveCount: Int = 0,
    val laughCount: Int = 0,
    val pacocaCount: Int = 0,
    val fireCount: Int = 0,
    val wowCount: Int = 0,
    val themeTag: String = "Minha Paçoca Original",
    val isUserPost: Boolean = false,
    val commentsCount: Int = 0,
    val wallpaperSetCount: Int = 0,
    val isWallpaperUsedByMe: Boolean = false,
    val recentLikersSummary: String = "",
    val recentWallpaperUsersSummary: String = "",
    val syncStatus: String = "SYNCED", // SYNCED, PENDING, FAILED
    val isDeliveredToOthers: Boolean = true,
    val syncErrorMessage: String? = null
)

@Entity(tableName = "feed_comments")
data class FeedComment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val postId: Long,
    val authorName: String,
    val authorAvatarEmoji: String = "🥜",
    val authorAvatarUri: String? = null,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

