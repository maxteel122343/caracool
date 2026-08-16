package com.example.data.repository

import android.content.Context
import com.example.data.dao.FeedDao
import com.example.data.dao.SettingsDao
import com.example.data.dao.UnlockDao
import com.example.data.model.AppSettings
import com.example.data.model.FeedComment
import com.example.data.model.FeedPost
import com.example.data.model.UnlockLog
import com.example.service.SupabaseSyncService
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppRepository(
    private val unlockDao: UnlockDao,
    private val settingsDao: SettingsDao,
    private val feedDao: FeedDao,
    private val context: Context? = null
) {
    val settingsFlow: Flow<AppSettings?> = settingsDao.getSettings()
    val allLogsFlow: Flow<List<UnlockLog>> = unlockDao.getAllLogs()
    val totalUnlocksFlow: Flow<Int> = unlockDao.getTotalUnlockCount()
    val feedPostsFlow: Flow<List<FeedPost>> = feedDao.getAllPosts()

    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getLogsForDate(dateString: String): Flow<List<UnlockLog>> {
        return unlockDao.getLogsForDate(dateString)
    }

    suspend fun getTodayUnlockCount(): Int {
        return unlockDao.getTodayUnlockCount(getTodayDateString())
    }

    suspend fun recordUnlock(phraseSpoken: String, themeUsed: String): Pair<Int, UnlockLog> {
        val today = getTodayDateString()
        val currentCount = unlockDao.getTodayUnlockCount(today)
        val newCount = currentCount + 1
        val log = UnlockLog(
            dateString = today,
            unlockNumberToday = newCount,
            phraseSpoken = phraseSpoken,
            themeUsed = themeUsed
        )
        val id = unlockDao.insertUnlockLog(log)
        val savedLog = log.copy(id = id)

        // Sincroniza desbloqueio com Supabase (se context estiver disponível)
        if (context != null) {
            try {
                SupabaseSyncService.syncNewUnlock(context, savedLog)
            } catch (_: Exception) {
            }
        }

        return Pair(newCount, savedLog)
    }

    suspend fun getSettings(): AppSettings {
        return settingsDao.getSettingsDirect() ?: AppSettings()
    }

    suspend fun getSettingsDirect(): AppSettings? {
        return settingsDao.getSettingsDirect()
    }

    suspend fun updateSettings(settings: AppSettings) {
        settingsDao.insertOrUpdate(settings)
    }

    suspend fun clearHistory() {
        unlockDao.clearAllLogs()
    }

    // --- Feed & Community Operations com Sincronização Automática com Firestore ---

    /**
     * Cria uma publicação no Feed da Comunidade:
     * 1. Salva no banco local Room com ID único global baseado em timestamp.
     * 2. Faz o upload imediato para o Firestore (para usuário logado ou convidado).
     */
    suspend fun createFeedPost(
        authorName: String,
        authorAvatarEmoji: String = "🥜",
        authorAvatarUri: String? = null,
        photoUri: String? = null,
        presetImageKey: String? = null,
        caption: String,
        unlockCount: Int,
        themeTag: String = "Minha Paçoca Original",
        isUserPost: Boolean = true
    ): FeedPost {
        val uniquePostId = System.currentTimeMillis()
        val post = FeedPost(
            id = uniquePostId,
            authorName = authorName,
            authorAvatarEmoji = authorAvatarEmoji,
            authorAvatarUri = authorAvatarUri,
            photoUri = photoUri,
            presetImageKey = presetImageKey,
            caption = caption,
            unlockCount = unlockCount,
            timestamp = uniquePostId,
            likesCount = 0,
            isLikedByMe = false,
            themeTag = themeTag,
            isUserPost = isUserPost,
            commentsCount = 0,
            wallpaperSetCount = 0,
            isWallpaperUsedByMe = false,
            recentLikersSummary = "",
            recentWallpaperUsersSummary = ""
        )
        val id = feedDao.insertPost(post)
        val savedPost = post.copy(id = if (id > 0) id else uniquePostId)

        // Upload imediato para o Supabase
        if (context != null) {
            try {
                SupabaseSyncService.syncNewPost(context, savedPost)
            } catch (_: Exception) {
            }
        }

        return savedPost
    }

    /**
     * Alterna curtida em um post e sincroniza imediatamente no Room e no Supabase.
     */
    suspend fun togglePostLike(postId: Long, userName: String = "Você") {
        val post = feedDao.getPostById(postId) ?: return
        val newLiked = !post.isLikedByMe
        val newCount = if (newLiked) post.likesCount + 1 else (post.likesCount - 1).coerceAtLeast(0)
        val newLoveCount = if (newLiked) post.loveCount + 1 else (post.loveCount - 1).coerceAtLeast(0)
        val newLikersSummary = if (newLiked) {
            if (newCount > 1) "$userName e mais ${newCount - 1} pessoa(s)" else "$userName curtiu"
        } else {
            if (newCount > 0) "$newCount curtida(s)" else ""
        }
        val updatedPost = post.copy(
            isLikedByMe = newLiked,
            likesCount = newCount,
            myReactionEmoji = if (newLiked) (post.myReactionEmoji ?: "❤️") else null,
            loveCount = newLoveCount,
            recentLikersSummary = newLikersSummary
        )
        feedDao.updatePost(updatedPost)

        // Sincroniza imediatamente no Supabase
        if (context != null) {
            try {
                SupabaseSyncService.syncPostLike(
                    context = context,
                    postId = postId,
                    userName = userName,
                    likesCount = updatedPost.likesCount,
                    reaction = updatedPost.myReactionEmoji ?: "❤️",
                    loveCount = updatedPost.loveCount,
                    laughCount = updatedPost.laughCount,
                    pacocaCount = updatedPost.pacocaCount,
                    fireCount = updatedPost.fireCount,
                    wowCount = updatedPost.wowCount
                )
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Registra reação emoji ao post e sincroniza imediatamente no Room e no Supabase.
     */
    suspend fun reactToPost(postId: Long, reactionEmoji: String, userName: String = "Você") {
        val post = feedDao.getPostById(postId) ?: return
        val prevReaction = post.myReactionEmoji

        var love = post.loveCount
        var laugh = post.laughCount
        var pacoca = post.pacocaCount
        var fire = post.fireCount
        var wow = post.wowCount

        // Decrementa reação anterior se houver
        if (prevReaction != null) {
            when (prevReaction) {
                "❤️" -> love = (love - 1).coerceAtLeast(0)
                "😂" -> laugh = (laugh - 1).coerceAtLeast(0)
                "🥜" -> pacoca = (pacoca - 1).coerceAtLeast(0)
                "🔥" -> fire = (fire - 1).coerceAtLeast(0)
                "😮" -> wow = (wow - 1).coerceAtLeast(0)
            }
        }

        val newReaction = if (prevReaction == reactionEmoji) null else reactionEmoji
        if (newReaction != null) {
            when (newReaction) {
                "❤️" -> love += 1
                "😂" -> laugh += 1
                "🥜" -> pacoca += 1
                "🔥" -> fire += 1
                "😮" -> wow += 1
            }
        }

        val totalLikes = love + laugh + pacoca + fire + wow
        val newLikersSummary = if (newReaction != null) {
            if (totalLikes > 1) "$userName e mais ${totalLikes - 1} pessoa(s)" else "$userName reagiu $newReaction"
        } else {
            if (totalLikes > 0) "$totalLikes reações" else ""
        }

        val updatedPost = post.copy(
            myReactionEmoji = newReaction,
            isLikedByMe = newReaction != null,
            likesCount = totalLikes,
            loveCount = love,
            laughCount = laugh,
            pacocaCount = pacoca,
            fireCount = fire,
            wowCount = wow,
            recentLikersSummary = newLikersSummary
        )
        feedDao.updatePost(updatedPost)

        // Sincroniza imediatamente no Supabase
        if (context != null) {
            try {
                SupabaseSyncService.syncPostLike(
                    context = context,
                    postId = postId,
                    userName = userName,
                    likesCount = totalLikes,
                    reaction = newReaction ?: "🥜",
                    loveCount = love,
                    laughCount = laugh,
                    pacocaCount = pacoca,
                    fireCount = fire,
                    wowCount = wow
                )
            } catch (_: Exception) {
            }
        }
    }

    suspend fun markPostWallpaperUsed(postId: Long, userName: String = "Você") {
        val post = feedDao.getPostById(postId) ?: return
        val newCount = post.wallpaperSetCount + 1
        val newSummary = "$userName e outros $newCount usaram como wallpaper"
        val updated = post.copy(
            wallpaperSetCount = newCount,
            isWallpaperUsedByMe = true,
            recentWallpaperUsersSummary = newSummary
        )
        feedDao.updatePost(updated)

        if (context != null) {
            try {
                SupabaseSyncService.syncWallpaperUsage(context, postId, userName = userName)
            } catch (_: Exception) {
            }
        }
    }

    suspend fun getPostById(postId: Long): FeedPost? {
        return feedDao.getPostById(postId)
    }

    fun getCommentsForPost(postId: Long): Flow<List<FeedComment>> {
        return feedDao.getCommentsForPost(postId)
    }

    /**
     * Adiciona um comentário a um post:
     * 1. Salva no banco local Room com ID único global.
     * 2. Faz o upload imediato para o Supabase no documento do post.
     */
    suspend fun addComment(
        postId: Long,
        authorName: String,
        text: String,
        authorAvatarEmoji: String = "🥜",
        authorAvatarUri: String? = null
    ): FeedComment {
        val commentId = System.currentTimeMillis()
        val comment = FeedComment(
            id = commentId,
            postId = postId,
            authorName = authorName,
            authorAvatarEmoji = authorAvatarEmoji,
            authorAvatarUri = authorAvatarUri,
            text = text,
            timestamp = commentId
        )
        val id = feedDao.insertComment(comment)
        feedDao.incrementCommentsCount(postId)
        val savedComment = comment.copy(id = if (id > 0) id else commentId)

        // Sincroniza imediatamente no Supabase
        if (context != null) {
            try {
                SupabaseSyncService.syncNewComment(
                    context = context,
                    postId = postId,
                    commentId = savedComment.id,
                    commentText = text,
                    authorName = authorName,
                    authorEmoji = authorAvatarEmoji,
                    authorAvatarUri = authorAvatarUri
                )
            } catch (_: Exception) {
            }
        }

        return savedComment
    }

    suspend fun updateProfile(
        userName: String,
        userAvatarEmoji: String,
        userProfilePhotoUri: String?
    ) {
        val current = getSettings()
        val oldName = current.userName
        val updated = current.copy(
            userName = userName.ifBlank { "Você (Cara de Paçoca)" },
            userAvatarEmoji = userAvatarEmoji.ifBlank { "🥜" },
            userProfilePhotoUri = userProfilePhotoUri,
            userPhotoUri = userProfilePhotoUri ?: current.userPhotoUri,
            userWallpaperUri = userProfilePhotoUri ?: current.userWallpaperUri,
            lockscreenPhotoUri = userProfilePhotoUri ?: current.lockscreenPhotoUri,
            selectedThemeId = if (userProfilePhotoUri != null) "custom_photo" else current.selectedThemeId
        )
        updateSettings(updated)

        // Update existing user posts and comments in local database
        try {
            feedDao.updateUserPostsAuthorInfo(
                newName = updated.userName,
                newEmoji = updated.userAvatarEmoji,
                newAvatarUri = updated.userProfilePhotoUri
            )
            feedDao.updateCommentsAuthorInfo(
                oldName = oldName,
                newName = updated.userName,
                newEmoji = updated.userAvatarEmoji,
                newAvatarUri = updated.userProfilePhotoUri
            )
        } catch (_: Exception) {}

        if (context != null) {
            try {
                SupabaseSyncService.syncProfileUpdate(
                    context = context,
                    userName = updated.userName,
                    userAvatarEmoji = updated.userAvatarEmoji,
                    photoUri = updated.userProfilePhotoUri
                )
            } catch (_: Exception) {
            }
        }
    }

    suspend fun deletePost(postId: Long) {
        feedDao.deletePost(postId)
        feedDao.deleteCommentsForPost(postId)
        if (context != null) {
            try {
                SupabaseSyncService.syncDeletePost(context, postId)
            } catch (_: Exception) {}
        }
    }

    suspend fun cleanOldUserPosts24h(): Int {
        val cutoff = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
        val deletedCount = feedDao.deleteUserPostsOlderThan(cutoff)
        if (deletedCount > 0) {
            feedDao.deleteOrphanedComments()
        }
        if (context != null) {
            try {
                SupabaseSyncService.syncDeleteOldPosts24h(context, cutoff)
            } catch (_: Exception) {}
        }
        return deletedCount
    }

    suspend fun ensureInitialFeedPosts() {
        // Remove permanently all fake dummy/preset posts from local database
        feedDao.deletePresetFakePosts()
        feedDao.clearAllPresetDummyPosts()
    }
}
