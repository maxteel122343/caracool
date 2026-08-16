package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.model.FeedComment
import com.example.data.model.FeedPost
import com.example.data.model.UnlockLog
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

object SupabaseSyncService {
    private const val TAG = "SupabaseSyncService"
    private const val PREFS_NAME = "pacoca_supabase_sync_prefs"
    private const val KEY_GUEST_USER_ID = "guest_user_id"

    private val scope = CoroutineScope(Dispatchers.IO)
    private var realtimeJob: Job? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Retorna o ID único persistente do usuário (ID autenticado ou guest_xxxx).
     */
    fun getEffectiveUserId(context: Context): String {
        val user = SupabaseAuthHelper.currentUser.value
        if (user != null && user.id.isNotBlank()) {
            return user.id
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var guestId = prefs.getString(KEY_GUEST_USER_ID, null)
        if (guestId == null) {
            guestId = "guest_" + UUID.randomUUID().toString().replace("-", "").take(12)
            prefs.edit().putString(KEY_GUEST_USER_ID, guestId).apply()
        }
        return guestId
    }

    fun isGuest(): Boolean {
        return SupabaseAuthHelper.currentUser.value == null
    }

    private fun getAuthHeaders(userToken: String? = null): Map<String, String> {
        val anonKey = SupabaseAuthHelper.getSupabaseAnonKey()
        val token = userToken ?: SupabaseAuthHelper.currentUser.value?.accessToken ?: anonKey
        return mapOf(
            "apikey" to anonKey,
            "Authorization" to "Bearer $token",
            "Content-Type" to "application/json"
        )
    }

    fun encodePhotoToBase64(context: Context, photoUriStr: String?): String? {
        if (photoUriStr.isNullOrBlank()) return null
        return try {
            val bitmap = when {
                photoUriStr.startsWith("content://") -> {
                    val uri = Uri.parse(photoUriStr)
                    context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }
                photoUriStr.startsWith("/") || photoUriStr.startsWith("file://") -> {
                    val path = photoUriStr.removePrefix("file://")
                    BitmapFactory.decodeFile(path)
                }
                else -> null
            } ?: return null

            val maxDimension = 640
            val width = bitmap.width
            val height = bitmap.height
            val scale = if (width > maxDimension || height > maxDimension) {
                val ratio = width.toFloat() / height.toFloat()
                if (ratio > 1) {
                    Pair(maxDimension, (maxDimension / ratio).toInt())
                } else {
                    Pair((maxDimension * ratio).toInt(), maxDimension)
                }
            } else {
                Pair(width, height)
            }

            val resized = if (scale.first != width || scale.second != height) {
                Bitmap.createScaledBitmap(bitmap, scale.first, scale.second, true)
            } else {
                bitmap
            }

            val outputStream = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val bytes = outputStream.toByteArray()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding photo to base64: ${e.message}")
            null
        }
    }

    fun saveBase64ToLocalCache(context: Context, postId: Long, base64Str: String?): String? {
        if (base64Str.isNullOrBlank()) return null
        return try {
            val cacheDir = File(context.cacheDir, "community_feed_cache")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val file = File(cacheDir, "post_${postId}.jpg")
            val bytes = Base64.decode(base64Str, Base64.DEFAULT)
            FileOutputStream(file).use { it.write(bytes) }
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving base64 to local cache: ${e.message}")
            null
        }
    }

    fun startRealtimeCommunityListener(context: Context, onNewPostsReceived: ((Int) -> Unit)? = null) {
        stopRealtimeCommunityListener()
        Log.d(TAG, "Starting Supabase Realtime Polling Sync listener")

        realtimeJob = scope.launch {
            while (isActive) {
                try {
                    if (SupabaseAuthHelper.isConfigured()) {
                        val currentUserId = getEffectiveUserId(context)
                        val res = syncCommunityFeedFromCloud(context, currentUserId)
                        if (res.isSuccess) {
                            val count = res.getOrDefault(0)
                            if (count > 0) {
                                onNewPostsReceived?.invoke(count)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error in realtime sync poll: ${e.message}")
                }
                delay(15000) // Poll every 15 seconds
            }
        }
    }

    fun stopRealtimeCommunityListener() {
        realtimeJob?.cancel()
        realtimeJob = null
    }

    suspend fun syncCommunityFeedFromCloud(context: Context, currentUserId: String?): Result<Int> = withContext(Dispatchers.IO) {
        if (!SupabaseAuthHelper.isConfigured()) {
            return@withContext Result.success(0)
        }

        try {
            val url = "${SupabaseAuthHelper.getSupabaseUrl()}/rest/v1/community_posts?select=*&order=timestamp.desc&limit=60"
            val headers = getAuthHeaders()
            val reqBuilder = Request.Builder().url(url)
            headers.forEach { (k, v) -> reqBuilder.addHeader(k, v) }

            val response = httpClient.newCall(reqBuilder.build()).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.w(TAG, "Supabase fetch posts error ${response.code}: $responseBody")
                return@withContext Result.failure(Exception("HTTP ${response.code}: $responseBody"))
            }

            val listType = Types.newParameterizedType(List::class.java, Map::class.java)
            val adapter = moshi.adapter<List<Map<String, Any?>>>(listType)
            val postList = adapter.fromJson(responseBody) ?: emptyList()

            val db = AppDatabase.getDatabase(context)
            val feedDao = db.feedDao()
            val myEffectiveId = currentUserId ?: getEffectiveUserId(context)

            var newCount = 0
            for (p in postList) {
                val postId = (p["id"] as? Number)?.toLong() ?: continue
                val postUserId = p["userId"] as? String ?: ""
                val isMine = postUserId.isNotBlank() && postUserId == myEffectiveId

                val existing = feedDao.getPostById(postId)
                val base64 = p["photoBase64"] as? String
                val cachedPhoto = if (!base64.isNullOrBlank()) {
                    saveBase64ToLocalCache(context, postId, base64)
                } else {
                    existing?.photoUri ?: p["photoUri"] as? String
                }

                val feedPost = FeedPost(
                    id = postId,
                    authorName = p["authorName"] as? String ?: "Membro da Comunidade",
                    authorAvatarEmoji = p["authorAvatarEmoji"] as? String ?: "🥜",
                    authorAvatarUri = p["authorAvatarUri"] as? String,
                    photoUri = cachedPhoto,
                    presetImageKey = p["presetImageKey"] as? String,
                    caption = p["caption"] as? String ?: "",
                    unlockCount = (p["unlockCount"] as? Number)?.toInt() ?: 1,
                    timestamp = (p["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    likesCount = (p["likesCount"] as? Number)?.toInt() ?: 0,
                    loveCount = (p["loveCount"] as? Number)?.toInt() ?: 0,
                    laughCount = (p["laughCount"] as? Number)?.toInt() ?: 0,
                    pacocaCount = (p["pacocaCount"] as? Number)?.toInt() ?: 0,
                    fireCount = (p["fireCount"] as? Number)?.toInt() ?: 0,
                    wowCount = (p["wowCount"] as? Number)?.toInt() ?: 0,
                    isLikedByMe = existing?.isLikedByMe ?: false,
                    myReactionEmoji = existing?.myReactionEmoji,
                    themeTag = p["themeTag"] as? String ?: "Original",
                    isUserPost = isMine || (existing?.isUserPost ?: false),
                    commentsCount = (p["commentsCount"] as? Number)?.toInt() ?: 0,
                    wallpaperSetCount = (p["wallpaperSetCount"] as? Number)?.toInt() ?: 0,
                    isWallpaperUsedByMe = existing?.isWallpaperUsedByMe ?: false,
                    recentLikersSummary = existing?.recentLikersSummary ?: "",
                    recentWallpaperUsersSummary = existing?.recentWallpaperUsersSummary ?: ""
                )

                feedDao.insertPost(feedPost)
                if (existing == null && !isMine) {
                    newCount++
                }
            }

            // Also fetch comments
            fetchCommentsFromSupabase(context)

            Log.d(TAG, "Successfully synced ${postList.size} posts from Supabase ($newCount new)")
            Result.success(newCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing feed from Supabase: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun fetchCommentsFromSupabase(context: Context) {
        try {
            val url = "${SupabaseAuthHelper.getSupabaseUrl()}/rest/v1/community_comments?select=*&order=timestamp.asc&limit=150"
            val headers = getAuthHeaders()
            val reqBuilder = Request.Builder().url(url)
            headers.forEach { (k, v) -> reqBuilder.addHeader(k, v) }

            val response = httpClient.newCall(reqBuilder.build()).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val listType = Types.newParameterizedType(List::class.java, Map::class.java)
                val adapter = moshi.adapter<List<Map<String, Any?>>>(listType)
                val commentList = adapter.fromJson(responseBody) ?: emptyList()

                val db = AppDatabase.getDatabase(context)
                val feedDao = db.feedDao()

                for (c in commentList) {
                    val commentId = (c["id"] as? Number)?.toLong() ?: continue
                    val postId = (c["postId"] as? Number)?.toLong() ?: continue
                    val feedComment = FeedComment(
                        id = commentId,
                        postId = postId,
                        authorName = c["authorName"] as? String ?: "Amigo Paçoca",
                        authorAvatarEmoji = c["authorAvatarEmoji"] as? String ?: "🥜",
                        authorAvatarUri = c["authorAvatarUri"] as? String,
                        text = c["text"] as? String ?: "",
                        timestamp = (c["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                    feedDao.insertComment(feedComment)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Comments sync error: ${e.message}")
        }
    }

    suspend fun syncOnLogin(context: Context, user: SupabaseUser): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val settings = db.settingsDao().getSettingsDirect()

            // 1. Sync User Profile
            syncProfileUpdate(
                context = context,
                userName = settings?.userName ?: user.displayName ?: "Você",
                userAvatarEmoji = settings?.userAvatarEmoji ?: "🥜",
                photoUri = settings?.userProfilePhotoUri ?: settings?.userPhotoUri ?: user.photoUrl,
                userId = user.id
            )

            // 2. Upload local posts
            val localPosts = db.feedDao().getAllPostsDirect().filter { it.isUserPost }
            for (post in localPosts) {
                syncNewPost(context, post, user.id)
            }

            // 3. Download community feed
            syncCommunityFeedFromCloud(context, user.id)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error in syncOnLogin: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun syncNewUnlock(context: Context, log: UnlockLog, userId: String? = null) = withContext(Dispatchers.IO) {
        if (!SupabaseAuthHelper.isConfigured()) return@withContext

        try {
            val effectiveUserId = userId ?: getEffectiveUserId(context)
            val url = "${SupabaseAuthHelper.getSupabaseUrl()}/rest/v1/unlock_logs"
            val bodyMap = mapOf(
                "id" to log.id,
                "userId" to effectiveUserId,
                "dateString" to log.dateString,
                "unlockNumberToday" to log.unlockNumberToday,
                "phraseSpoken" to log.phraseSpoken,
                "themeUsed" to log.themeUsed,
                "timestamp" to log.timestamp
            )

            val adapter = moshi.adapter<Map<String, Any?>>(Map::class.java)
            val json = adapter.toJson(bodyMap)

            val reqBuilder = Request.Builder()
                .url(url)
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(json.toRequestBody(jsonMediaType))

            getAuthHeaders().forEach { (k, v) -> reqBuilder.addHeader(k, v) }
            httpClient.newCall(reqBuilder.build()).execute()
        } catch (e: Exception) {
            Log.w(TAG, "Could not sync unlock to Supabase: ${e.message}")
        }
    }

    suspend fun syncProfileUpdate(
        context: Context,
        userName: String,
        userAvatarEmoji: String,
        photoUri: String?,
        userId: String? = null
    ) = withContext(Dispatchers.IO) {
        if (!SupabaseAuthHelper.isConfigured()) return@withContext

        try {
            val effectiveUserId = userId ?: getEffectiveUserId(context)
            val base64Photo = encodePhotoToBase64(context, photoUri)
            val url = "${SupabaseAuthHelper.getSupabaseUrl()}/rest/v1/users"

            val bodyMap = mapOf(
                "userId" to effectiveUserId,
                "userName" to userName,
                "userAvatarEmoji" to userAvatarEmoji,
                "userProfilePhotoUri" to photoUri,
                "photoBase64" to base64Photo,
                "updatedAt" to System.currentTimeMillis()
            )

            val adapter = moshi.adapter<Map<String, Any?>>(Map::class.java)
            val json = adapter.toJson(bodyMap)

            val reqBuilder = Request.Builder()
                .url(url)
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(json.toRequestBody(jsonMediaType))

            getAuthHeaders().forEach { (k, v) -> reqBuilder.addHeader(k, v) }
            httpClient.newCall(reqBuilder.build()).execute()
        } catch (e: Exception) {
            Log.w(TAG, "Could not sync profile to Supabase: ${e.message}")
        }
    }

    suspend fun syncUserRanking(
        context: Context,
        userId: String,
        userName: String,
        avatarEmoji: String,
        photoUri: String?,
        unlockCount: Int,
        isKool: Boolean
    ) = withContext(Dispatchers.IO) {
        if (!SupabaseAuthHelper.isConfigured()) return@withContext

        try {
            val url = "${SupabaseAuthHelper.getSupabaseUrl()}/rest/v1/community_rankings"
            val bodyMap = mapOf(
                "userId" to userId,
                "name" to userName,
                "avatarEmoji" to avatarEmoji,
                "photoUri" to photoUri,
                "unlockCount" to unlockCount,
                "isKool" to isKool,
                "lastActive" to System.currentTimeMillis()
            )

            val adapter = moshi.adapter<Map<String, Any?>>(Map::class.java)
            val json = adapter.toJson(bodyMap)

            val reqBuilder = Request.Builder()
                .url(url)
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(json.toRequestBody(jsonMediaType))

            getAuthHeaders().forEach { (k, v) -> reqBuilder.addHeader(k, v) }
            httpClient.newCall(reqBuilder.build()).execute()
        } catch (e: Exception) {
            Log.w(TAG, "Could not sync ranking to Supabase: ${e.message}")
        }
    }

    suspend fun syncNewPost(context: Context, post: FeedPost, userId: String? = null) = withContext(Dispatchers.IO) {
        if (!SupabaseAuthHelper.isConfigured()) return@withContext

        try {
            val effectiveUserId = userId ?: getEffectiveUserId(context)
            val photoBase64 = encodePhotoToBase64(context, post.photoUri)

            val url = "${SupabaseAuthHelper.getSupabaseUrl()}/rest/v1/community_posts"
            val bodyMap = mapOf(
                "id" to post.id,
                "userId" to effectiveUserId,
                "authorName" to post.authorName,
                "authorAvatarEmoji" to post.authorAvatarEmoji,
                "authorAvatarUri" to post.authorAvatarUri,
                "photoUri" to post.photoUri,
                "photoBase64" to photoBase64,
                "presetImageKey" to post.presetImageKey,
                "caption" to post.caption,
                "unlockCount" to post.unlockCount,
                "timestamp" to post.timestamp,
                "likesCount" to post.likesCount,
                "loveCount" to post.loveCount,
                "laughCount" to post.laughCount,
                "pacocaCount" to post.pacocaCount,
                "fireCount" to post.fireCount,
                "wowCount" to post.wowCount,
                "themeTag" to post.themeTag,
                "commentsCount" to post.commentsCount,
                "wallpaperSetCount" to post.wallpaperSetCount,
                "isGuest" to isGuest()
            )

            val adapter = moshi.adapter<Map<String, Any?>>(Map::class.java)
            val json = adapter.toJson(bodyMap)

            val reqBuilder = Request.Builder()
                .url(url)
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(json.toRequestBody(jsonMediaType))

            getAuthHeaders().forEach { (k, v) -> reqBuilder.addHeader(k, v) }
            val response = httpClient.newCall(reqBuilder.build()).execute()
            Log.d(TAG, "Post #${post.id} sync result: ${response.code}")
        } catch (e: Exception) {
            Log.e(TAG, "Could not sync post #${post.id} to Supabase: ${e.message}", e)
        }
    }

    suspend fun syncNewComment(
        context: Context,
        postId: Long,
        commentId: Long,
        commentText: String,
        authorName: String,
        authorEmoji: String = "🥜",
        authorAvatarUri: String? = null,
        userId: String? = null
    ) = withContext(Dispatchers.IO) {
        if (!SupabaseAuthHelper.isConfigured()) return@withContext

        try {
            val effectiveUserId = userId ?: getEffectiveUserId(context)
            val url = "${SupabaseAuthHelper.getSupabaseUrl()}/rest/v1/community_comments"

            val bodyMap = mapOf(
                "id" to commentId,
                "postId" to postId,
                "userId" to effectiveUserId,
                "authorName" to authorName,
                "authorAvatarEmoji" to authorEmoji,
                "authorAvatarUri" to authorAvatarUri,
                "text" to commentText,
                "timestamp" to System.currentTimeMillis(),
                "isGuest" to isGuest()
            )

            val adapter = moshi.adapter<Map<String, Any?>>(Map::class.java)
            val json = adapter.toJson(bodyMap)

            val reqBuilder = Request.Builder()
                .url(url)
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(json.toRequestBody(jsonMediaType))

            getAuthHeaders().forEach { (k, v) -> reqBuilder.addHeader(k, v) }
            httpClient.newCall(reqBuilder.build()).execute()
        } catch (e: Exception) {
            Log.e(TAG, "Could not sync comment to Supabase: ${e.message}", e)
        }
    }

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
    ) = withContext(Dispatchers.IO) {
        if (!SupabaseAuthHelper.isConfigured()) return@withContext

        try {
            val effectiveUserId = userId ?: getEffectiveUserId(context)
            val patchUrl = "${SupabaseAuthHelper.getSupabaseUrl()}/rest/v1/community_posts?id=eq.$postId"

            val patchBody: Map<String, Any?> = mapOf(
                "likesCount" to likesCount,
                "loveCount" to loveCount,
                "laughCount" to laughCount,
                "pacocaCount" to pacocaCount,
                "fireCount" to fireCount,
                "wowCount" to wowCount
            )

            val adapter = moshi.adapter<Map<String, Any?>>(Map::class.java)
            val json = adapter.toJson(patchBody)

            val reqBuilder = Request.Builder()
                .url(patchUrl)
                .patch(json.toRequestBody(jsonMediaType))

            getAuthHeaders().forEach { (k, v) -> reqBuilder.addHeader(k, v) }
            httpClient.newCall(reqBuilder.build()).execute()

            // Also record reaction
            val reactionUrl = "${SupabaseAuthHelper.getSupabaseUrl()}/rest/v1/community_reactions"
            val reactionBody = mapOf(
                "postId" to postId,
                "userId" to effectiveUserId,
                "userName" to userName,
                "reaction" to reaction,
                "timestamp" to System.currentTimeMillis()
            )
            val rJson = adapter.toJson(reactionBody)
            val rReq = Request.Builder()
                .url(reactionUrl)
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(rJson.toRequestBody(jsonMediaType))

            getAuthHeaders().forEach { (k, v) -> rReq.addHeader(k, v) }
            httpClient.newCall(rReq.build()).execute()
        } catch (e: Exception) {
            Log.e(TAG, "Could not sync post like to Supabase: ${e.message}", e)
        }
    }

    suspend fun syncWallpaperUsage(context: Context, postId: Long, userId: String? = null, userName: String = "Alguém") = withContext(Dispatchers.IO) {
        if (!SupabaseAuthHelper.isConfigured()) return@withContext

        try {
            val db = AppDatabase.getDatabase(context)
            val post = db.feedDao().getPostById(postId) ?: return@withContext
            val newCount = post.wallpaperSetCount + 1

            val patchUrl = "${SupabaseAuthHelper.getSupabaseUrl()}/rest/v1/community_posts?id=eq.$postId"
            val patchBody: Map<String, Any?> = mapOf("wallpaperSetCount" to newCount)

            val adapter = moshi.adapter<Map<String, Any?>>(Map::class.java)
            val json = adapter.toJson(patchBody)

            val reqBuilder = Request.Builder()
                .url(patchUrl)
                .patch(json.toRequestBody(jsonMediaType))

            getAuthHeaders().forEach { (k, v) -> reqBuilder.addHeader(k, v) }
            httpClient.newCall(reqBuilder.build()).execute()
        } catch (e: Exception) {
            Log.e(TAG, "Could not sync wallpaper usage to Supabase: ${e.message}")
        }
    }
}
