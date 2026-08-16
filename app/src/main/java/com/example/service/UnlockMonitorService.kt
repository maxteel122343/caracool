package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.audio.AudioVoiceManager
import com.example.data.database.AppDatabase
import com.example.data.model.AppSettings
import com.example.data.model.FeedPost
import com.example.data.model.UnlockLog
import com.example.theme.ThemePresets
import com.example.util.SafeWordHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class UnlockMonitorService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var audioVoiceManager: AudioVoiceManager
    private var screenReceiver: BroadcastReceiver? = null
    private var isReceiverRegistered = false

    companion object {
        private const val TAG = "UnlockMonitorService"
        const val CHANNEL_SERVICE_ID = "cara_de_pacoca_service_channel"
        const val CHANNEL_UNLOCK_ALERT_ID = "cara_de_pacoca_unlock_alerts"
        const val FOREGROUND_NOTIFICATION_ID = 1001
        const val UNLOCK_ALERT_NOTIFICATION_ID = 1002

        const val ACTION_TRIGGER_TEST_UNLOCK = "com.example.caradepacoca.TRIGGER_TEST_UNLOCK"

        private val _unlockEvents = MutableSharedFlow<UnlockEvent>(extraBufferCapacity = 10)
        val unlockEvents: SharedFlow<UnlockEvent> = _unlockEvents.asSharedFlow()

        data class UnlockEvent(
            val countToday: Int,
            val phrase: String,
            val photoPath: String? = null,
            val timestamp: Long = System.currentTimeMillis()
        )

        fun start(context: Context) {
            val intent = Intent(context, UnlockMonitorService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting foreground service: ${e.message}", e)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, UnlockMonitorService::class.java)
            try {
                context.stopService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping service: ${e.message}", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "UnlockMonitorService onCreate")
        audioVoiceManager = AudioVoiceManager.getInstance(applicationContext)

        createNotificationChannels()
        NotificationHelper.createChannels(applicationContext)
        startForeground(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification(0))

        registerScreenBroadcastReceiver()
        updateNotificationCount()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TRIGGER_TEST_UNLOCK -> handleUnlockOccurred(isTest = true)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerScreenBroadcastReceiver() {
        if (isReceiverRegistered) return

        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_USER_PRESENT -> {
                        Log.d(TAG, "⚡ ACTION_USER_PRESENT received (Real device unlocked by user)!")
                        handleUnlockOccurred(isTest = false)
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        Log.d(TAG, "Screen turned ON")
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        Log.d(TAG, "Screen turned OFF (Locked)")
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            priority = 1000
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(screenReceiver, filter, RECEIVER_EXPORTED)
            } else {
                registerReceiver(screenReceiver, filter)
            }
            isReceiverRegistered = true
            Log.d(TAG, "Screen broadcast receiver registered successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register screen receiver: ${e.message}", e)
        }
    }

    private fun handleUnlockOccurred(isTest: Boolean = false) {
        serviceScope.launch {
            var wakeLock: PowerManager.WakeLock? = null
            try {
                val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "caradepacoca:unlock_wakelock"
                )?.apply {
                    acquire(8000L) // 8 seconds max wake lock
                }

                val db = AppDatabase.getDatabase(applicationContext)
                val unlockDao = db.unlockDao()
                val settingsDao = db.settingsDao()
                val feedDao = db.feedDao()

                val settings = settingsDao.getSettingsDirect() ?: AppSettings()
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                val currentCount = unlockDao.getTodayUnlockCount(today) + 1
                val ordinal = audioVoiceManager.toPortugueseOrdinal(currentCount)

                // 1. Capture front camera selfie automatically
                val capturedPhotoPath = FrontCameraHelper.captureFrontSelfie(
                    context = applicationContext,
                    userName = settings.userName,
                    unlockCount = currentCount
                )

                // Update settings with latest photo if taken
                if (!capturedPhotoPath.isNullOrBlank()) {
                    val updatedSettings = settings.copy(
                        userPhotoUri = capturedPhotoPath,
                        userWallpaperUri = capturedPhotoPath
                    )
                    settingsDao.insertOrUpdate(updatedSettings)

                    // Auto set as Lockscreen Wallpaper if enabled
                    if (settings.isAutoSetUnlockPhotoAsLockWallpaper) {
                        WallpaperHelper.applyWallpaper(
                            context = applicationContext,
                            target = WallpaperTarget.LOCK_SCREEN,
                            filePath = capturedPhotoPath
                        )
                    }
                }

                // Determine greeting / phrase incorporating the user's name
                val rawPhrase = if (settings.isAiConversationalEnabled) {
                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val timeGreeting = when (hour) {
                        in 5..11 -> "Bom dia"
                        in 12..17 -> "Boa tarde"
                        else -> "Boa noite"
                    }
                    GeminiAiService.generateConversationalGreeting(
                        userName = settings.userName,
                        unlockCount = currentCount,
                        ordinalString = ordinal,
                        personality = settings.aiPersonality,
                        timeOfDayGreeting = timeGreeting,
                        isKoolMode = settings.isCaraDeKoolMode
                    )
                } else {
                    audioVoiceManager.formatUnlockPhrase(
                        userName = settings.userName,
                        countToday = currentCount,
                        audioType = settings.unlockAudioType,
                        customText = settings.customUnlockText,
                        isAiNatural = false,
                        isKoolMode = settings.isCaraDeKoolMode,
                        isSafeWordMode = settings.isSafeWordMode
                    )
                }
                val phrase = SafeWordHelper.formatSafeWord(rawPhrase, settings.isSafeWordMode)

                // 2. Record unlock in Room Database
                val log = UnlockLog(
                    dateString = today,
                    unlockNumberToday = currentCount,
                    phraseSpoken = phrase,
                    themeUsed = settings.selectedThemeId
                )
                unlockDao.insertUnlockLog(log)

                // 3. Play audio / voice
                audioVoiceManager.playUnlockAudio(
                    userName = settings.userName,
                    countToday = currentCount,
                    audioType = settings.unlockAudioType,
                    customText = settings.customUnlockText,
                    isAiNatural = settings.isAiConversationalEnabled,
                    customAudioUri = settings.customAudioPath,
                    coroutineScope = serviceScope,
                    isKoolMode = settings.isCaraDeKoolMode,
                    isSafeWordMode = settings.isSafeWordMode
                )

                // 4. Vibrate device
                vibrateDevice()

                // 5. Auto-post to community feed if enabled
                if (settings.isAutoPostOnUnlockEnabled) {
                    val cuWord = if (settings.isSafeWordMode) "cara de cool" else "cara de cu"
                    val autoCaption = if (settings.isCaraDeKoolMode) {
                        "Olha a $cuWord do ${settings.userName} pela ${currentCount}ª vez!"
                    } else {
                        "Olha a cara de paçoca do ${settings.userName} pela ${currentCount}ª vez!"
                    }
                    val themePreset = ThemePresets.getById(settings.selectedThemeId)
                    val userAvatar = settings.userProfilePhotoUri ?: capturedPhotoPath ?: settings.userPhotoUri
                    val userEmoji = if (settings.isCaraDeKoolMode) "👾" else settings.userAvatarEmoji.ifBlank { "🥜" }
                    val newPost = FeedPost(
                        id = System.currentTimeMillis(),
                        authorName = settings.userName,
                        authorAvatarEmoji = userEmoji,
                        authorAvatarUri = userAvatar,
                        photoUri = capturedPhotoPath ?: settings.userPhotoUri,
                        presetImageKey = if (capturedPhotoPath == null && settings.userPhotoUri == null) (if (settings.isCaraDeKoolMode) "kool_avatar" else "user_avatar") else null,
                        caption = autoCaption,
                        unlockCount = currentCount,
                        timestamp = System.currentTimeMillis(),
                        likesCount = 0,
                        isLikedByMe = false,
                        themeTag = if (settings.isCaraDeKoolMode) "Modo Cara de Cu" else themePreset.name,
                        isUserPost = true,
                        commentsCount = 0,
                        wallpaperSetCount = 0,
                        isWallpaperUsedByMe = false,
                        recentLikersSummary = "",
                        recentWallpaperUsersSummary = ""
                    )
                    val insertedId = feedDao.insertPost(newPost)
                    val savedPost = newPost.copy(id = if (insertedId > 0) insertedId else newPost.id)
                    try {
                        SupabaseSyncService.syncNewPost(applicationContext, savedPost)
                    } catch (_: Exception) {}
                }

                // 6. Rotate Community Wallpaper if enabled
                if (settings.isCommunitySliderEnabled && settings.communitySliderInterval == "unlock") {
                    val communityPosts = feedDao.getAllPostsDirect().filter { !it.isUserPost && !it.photoUri.isNullOrBlank() }
                    if (communityPosts.isNotEmpty()) {
                        val randomPost = communityPosts.random()
                        WallpaperHelper.applyWallpaper(
                            context = applicationContext,
                            target = WallpaperTarget.LOCK_SCREEN,
                            filePath = randomPost.photoUri
                        )
                        val isCu = settings.isCaraDeKoolMode
                        val changeTitle = "✨ Seu wallpaper mudou!"
                        val changeMsg = if (isCu) {
                            "Você é agora cara de cu da ${randomPost.authorName}! Parabéns cara de cu!"
                        } else {
                            "Você é agora cara de paçoca da ${randomPost.authorName}! Parabéns cara de paçoca!"
                        }
                        val spokenMsg = if (isCu) {
                            "Seu wallpaper mudou! Você é agora a cara de cu da ${randomPost.authorName}! Parabéns, cara de cu!"
                        } else {
                            "Seu wallpaper mudou! Você é agora a cara de paçoca da ${randomPost.authorName}! Parabéns, cara de paçoca!"
                        }
                        // Trigger voice and notification for wallpaper change
                        NotificationHelper.sendSocialNotificationWithVoice(
                            context = applicationContext,
                            title = changeTitle,
                            message = changeMsg,
                            spokenVoicePhrase = spokenMsg
                        )
                    }
                }

                // 7. Update Foreground Notification & Post Heads-Up Alert with audio
                val formattedPhrase = SafeWordHelper.formatSafeWord(phrase, settings.isSafeWordMode)
                updateForegroundNotification(currentCount, settings.isCaraDeKoolMode, settings.isSafeWordMode)
                postUnlockHeadsUpNotification(currentCount, formattedPhrase, settings.isCaraDeKoolMode, settings.isSafeWordMode)

                // 8. Emit internal event for ViewModel
                _unlockEvents.emit(UnlockEvent(currentCount, formattedPhrase, capturedPhotoPath))

                Log.d(TAG, "Unlock #${currentCount} processed successfully: $phrase")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing unlock event: ${e.message}", e)
            } finally {
                try {
                    if (wakeLock?.isHeld == true) {
                        wakeLock.release()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun vibrateDevice() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                val effect = VibrationEffect.createWaveform(longArrayOf(0, 100, 60, 120), -1)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 60, 120), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(180L)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration failed: ${e.message}")
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Ongoing Service Channel (Low importance)
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                "Monitor de Desbloqueio",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantém o monitor de desbloqueio ativo em segundo plano."
                setShowBadge(false)
            }

            // Unlock Alerts Channel (High importance with heads-up pop)
            val alertsChannel = NotificationChannel(
                CHANNEL_UNLOCK_ALERT_ID,
                "Alertas de Desbloqueio",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações instantâneas ao desbloquear o aparelho."
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager?.createNotificationChannel(serviceChannel)
            notificationManager?.createNotificationChannel(alertsChannel)
        }
    }

    private fun buildForegroundNotification(unlockCount: Int, isKoolMode: Boolean = false, isSafeWordMode: Boolean = true): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val testIntent = Intent(this, UnlockMonitorService::class.java).apply {
            action = ACTION_TRIGGER_TEST_UNLOCK
        }
        val pendingTestIntent = PendingIntent.getService(
            this,
            1,
            testIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val countText = if (unlockCount > 0) "$unlockCount desbloqueios hoje" else "Pronto para o próximo desbloqueio"
        val titleText = if (isKoolMode) {
            if (isSafeWordMode) "Cara de Cool Ativo" else "Cara de Cu Ativo"
        } else "Cara de Paçoca Ativo"

        return NotificationCompat.Builder(this, CHANNEL_SERVICE_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle(titleText)
            .setContentText(countText)
            .setSubText("IA & Desbloqueio")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_play, "🔊 Testar Voz", pendingTestIntent)
            .addAction(android.R.drawable.ic_menu_today, "📱 Abrir App", pendingIntent)
            .build()
    }

    private fun updateForegroundNotification(unlockCount: Int, isKoolMode: Boolean = false, isSafeWordMode: Boolean = true) {
        val notification = buildForegroundNotification(unlockCount, isKoolMode, isSafeWordMode)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(FOREGROUND_NOTIFICATION_ID, notification)
    }

    private fun postUnlockHeadsUpNotification(unlockCount: Int, phrase: String, isKoolMode: Boolean = false, isSafeWordMode: Boolean = true) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            2,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val titleText = if (isKoolMode) {
            if (isSafeWordMode) "Cara de Cool #${unlockCount}" else "Cara de Cu #${unlockCount}"
        } else "Cara de Paçoca #${unlockCount}"

        val safePhrase = SafeWordHelper.formatSafeWord(phrase, isSafeWordMode)

        val notification = NotificationCompat.Builder(this, CHANNEL_UNLOCK_ALERT_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle(titleText)
            .setContentText(safePhrase)
            .setStyle(NotificationCompat.BigTextStyle().bigText(safePhrase))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(UNLOCK_ALERT_NOTIFICATION_ID, notification)
    }

    private fun updateNotificationCount() {
        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val count = db.unlockDao().getTodayUnlockCount(today)
                val settings = db.settingsDao().getSettingsDirect()
                val isKool = settings?.isCaraDeKoolMode ?: false
                val isSafe = settings?.isSafeWordMode ?: true
                updateForegroundNotification(count, isKool, isSafe)
            } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "UnlockMonitorService onDestroy")
        try {
            if (isReceiverRegistered && screenReceiver != null) {
                unregisterReceiver(screenReceiver)
                isReceiverRegistered = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver: ${e.message}")
        }
        serviceJob.cancel()
    }
}
