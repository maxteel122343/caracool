package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppSettings
import com.example.data.model.FeedComment
import com.example.data.model.FeedPost
import com.example.data.model.UnlockLog
import kotlinx.coroutines.flow.Flow

@Dao
interface UnlockDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnlockLog(log: UnlockLog): Long

    @Query("SELECT * FROM unlock_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<UnlockLog>>

    @Query("SELECT * FROM unlock_logs ORDER BY timestamp DESC")
    suspend fun getAllLogsDirect(): List<UnlockLog>

    @Query("SELECT * FROM unlock_logs WHERE dateString = :dateString ORDER BY timestamp DESC")
    fun getLogsForDate(dateString: String): Flow<List<UnlockLog>>

    @Query("SELECT COUNT(*) FROM unlock_logs WHERE dateString = :dateString")
    suspend fun getTodayUnlockCount(dateString: String): Int

    @Query("SELECT COUNT(*) FROM unlock_logs")
    fun getTotalUnlockCount(): Flow<Int>

    @Query("DELETE FROM unlock_logs")
    suspend fun clearAllLogs()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getSettings(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettingsDirect(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: AppSettings)
}

@Dao
interface FeedDao {
    @Query("SELECT * FROM feed_posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<FeedPost>>

    @Query("SELECT * FROM feed_posts ORDER BY timestamp DESC")
    suspend fun getAllPostsDirect(): List<FeedPost>

    @Query("SELECT * FROM feed_posts WHERE id = :postId")
    suspend fun getPostById(postId: Long): FeedPost?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: FeedPost): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPosts(posts: List<FeedPost>)

    @Update
    suspend fun updatePost(post: FeedPost)

    @Query("UPDATE feed_posts SET likesCount = :newCount, isLikedByMe = :isLiked WHERE id = :postId")
    suspend fun updateLike(postId: Long, isLiked: Boolean, newCount: Int)

    @Query("UPDATE feed_posts SET commentsCount = commentsCount + 1 WHERE id = :postId")
    suspend fun incrementCommentsCount(postId: Long)

    @Query("DELETE FROM feed_posts WHERE id = :postId")
    suspend fun deletePost(postId: Long)

    @Query("DELETE FROM feed_comments WHERE postId = :postId")
    suspend fun deleteCommentsForPost(postId: Long)

    @Query("UPDATE feed_posts SET authorName = :newName, authorAvatarEmoji = :newEmoji, authorAvatarUri = :newAvatarUri WHERE isUserPost = 1")
    suspend fun updateUserPostsAuthorInfo(newName: String, newEmoji: String, newAvatarUri: String?)

    @Query("UPDATE feed_comments SET authorName = :newName, authorAvatarEmoji = :newEmoji, authorAvatarUri = :newAvatarUri WHERE authorName = :oldName")
    suspend fun updateCommentsAuthorInfo(oldName: String, newName: String, newEmoji: String, newAvatarUri: String?)

    @Query("DELETE FROM feed_posts WHERE isUserPost = 0 AND id NOT IN (:validIds)")
    suspend fun deleteRemotePostsNotIn(validIds: List<Long>)

    @Query("DELETE FROM feed_posts WHERE id IN (101, 102, 103, 104) OR presetImageKey LIKE 'preset_%' OR authorName IN ('Mariana Paçoca', 'Lucas Gamer', 'Lucas Paçoca', 'Beatriz Kawaii', 'Beatriz Paçoca', 'Camila Tardígrado', 'Gabriel Paçoca')")
    suspend fun deletePresetFakePosts()

    @Query("DELETE FROM feed_posts WHERE isUserPost = 0 AND photoBase64 IS NULL AND (presetImageKey IS NOT NULL OR authorName IN ('Mariana Paçoca', 'Lucas Gamer', 'Lucas Paçoca', 'Beatriz Kawaii', 'Beatriz Paçoca', 'Camila Tardígrado', 'Gabriel Paçoca'))")
    suspend fun clearAllPresetDummyPosts()

    @Query("SELECT COUNT(*) FROM feed_posts")
    suspend fun getPostCount(): Int

    @Query("DELETE FROM feed_posts WHERE isUserPost = 0")
    suspend fun clearNonRealPosts()

    @Query("DELETE FROM feed_posts WHERE isUserPost = 1 AND timestamp < :cutoffTimestamp")
    suspend fun deleteUserPostsOlderThan(cutoffTimestamp: Long): Int

    @Query("DELETE FROM feed_comments WHERE postId NOT IN (SELECT id FROM feed_posts)")
    suspend fun deleteOrphanedComments(): Int

    @Query("SELECT * FROM feed_comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsForPost(postId: Long): Flow<List<FeedComment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: FeedComment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllComments(comments: List<FeedComment>)
}

