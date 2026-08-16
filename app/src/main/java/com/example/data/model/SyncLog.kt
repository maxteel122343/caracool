package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SyncLogLevel {
    SUCCESS,
    INFO,
    WARNING,
    ERROR
}

enum class SyncCategory {
    PHOTO_SYNC,        // Sincronização de fotos / upload de mídia
    FEED_PROPAGATION,  // Confirmação de recebimento por outros usuários no feed
    REALTIME_STREAM,   // Listener em tempo real do Firestore
    ERROR_DIAGNOSTIC,  // Erros de rede, permissões, exceções
    AUTH_CLOUD,        // Autenticação, UID e modo convidado
    SYSTEM_DIAGNOSTIC  // Testes de integridade do sistema
}

@Entity(tableName = "sync_logs")
data class SyncLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val level: String = "INFO", // SUCCESS, INFO, WARNING, ERROR
    val category: String = "PHOTO_SYNC", // PHOTO_SYNC, FEED_PROPAGATION, REALTIME_STREAM, ERROR_DIAGNOSTIC, AUTH_CLOUD, SYSTEM_DIAGNOSTIC
    val title: String,
    val message: String,
    val technicalDetails: String? = null,
    val postId: Long? = null,
    val photoUri: String? = null,
    val isDeliveredToOthers: Boolean = false,
    val receiversCount: Int = 0,
    val errorCode: String? = null
)

data class SyncHealthStatus(
    val isOnline: Boolean = true,
    val isFirestoreConnected: Boolean = true,
    val realtimeListenerActive: Boolean = false,
    val activeUserId: String = "",
    val isGuestUser: Boolean = true,
    val lastPhotoSyncStatus: String = "Nenhuma foto enviada recentemente",
    val lastPhotoSyncSuccess: Boolean = true,
    val lastPhotoSyncTimestamp: Long? = null,
    val totalPostsSynced: Int = 0,
    val totalOtherUsersPostsReceived: Int = 0,
    val totalErrorsCount: Int = 0,
    val lastErrorTitle: String? = null,
    val lastErrorMessage: String? = null,
    val lastErrorTimestamp: Long? = null
)
