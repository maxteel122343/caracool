package com.example.service

import android.content.Context
import com.example.data.model.FeedPost
import com.example.data.model.UnlockLog

/**
 * Bridge de compatibilidade que delega para [SupabaseSyncService].
 */
object FirestoreSyncService {
    fun getEffectiveUserId(context: Context): String = SupabaseSyncService.getEffectiveUserId(context)
    fun isGuest(): Boolean = SupabaseSyncService.isGuest()

    fun startRealtimeCommunityListener(context: Context, onNewPostsReceived: ((Int) -> Unit)? = null) {
        SupabaseSyncService.startRealtimeCommunityListener(context, onNewPostsReceived)
    }

    fun stopRealtimeCommunityListener() {
        SupabaseSyncService.stopRealtimeCommunityListener()
    }

    suspend fun syncCommunityFeedFromCloud(context: Context, currentUserId: String?) =
        SupabaseSyncService.syncCommunityFeedFromCloud(context, currentUserId)

    suspend fun syncOnLogin(context: Context, user: SupabaseUser) =
        SupabaseSyncService.syncOnLogin(context, user)

    suspend fun syncNewUnlock(context: Context, log: UnlockLog, userId: String? = null) =
        SupabaseSyncService.syncNewUnlock(context, log, userId)

    suspend fun syncProfileUpdate(
        context: Context,
        userName: String,
        userAvatarEmoji: String,
        photoUri: String?,
        userId: String? = null
    ) = SupabaseSyncService.syncProfileUpdate(context, userName, userAvatarEmoji, photoUri, userId)

    suspend fun syncUserRanking(
        context: Context,
        userId: String,
        userName: String,
        avatarEmoji: String,
        photoUri: String?,
        unlockCount: Int,
        isKool: Boolean
    ) = SupabaseSyncService.syncUserRanking(context, userId, userName, avatarEmoji, photoUri, unlockCount, isKool)

    suspend fun syncNewPost(context: Context, post: FeedPost, userId: String? = null) =
        SupabaseSyncService.syncNewPost(context, post, userId)

    suspend fun syncNewComment(
        context: Context,
        postId: Long,
        commentId: Long,
        commentText: String,
        authorName: String,
        authorEmoji: String = "🥜",
        authorAvatarUri: String? = null,
        userId: String? = null
    ) = SupabaseSyncService.syncNewComment(
        context = context,
        postId = postId,
        commentId = commentId,
        commentText = commentText,
        authorName = authorName,
        authorEmoji = authorEmoji,
        authorAvatarUri = authorAvatarUri,
        userId = userId
    )

    suspend fun syncPostLike(
        context: Context,
        postId: Long,
        userName: String,
        likesCount: Int,
        reaction: String = "🥜",
        loveCount: Int = 0,
        laughCount: Int = 0,
        pacocaCount: Int = 0,
        fireCount: Int = 0,
        wowCount: Int = 0,
        userId: String? = null
    ) = SupabaseSyncService.syncPostLike(
        context = context,
        postId = postId,
        userName = userName,
        likesCount = likesCount,
        reaction = reaction,
        loveCount = loveCount,
        laughCount = laughCount,
        pacocaCount = pacocaCount,
        fireCount = fireCount,
        wowCount = wowCount,
        userId = userId
    )

    suspend fun syncWallpaperUsage(context: Context, postId: Long, userId: String? = null, userName: String = "Alguém") =
        SupabaseSyncService.syncWallpaperUsage(context, postId, userId, userName)
}
