package com.example.ui.viewmodel

import android.app.Application
import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioVoiceManager
import com.example.data.database.AppDatabase
import com.example.data.model.AppSettings
import com.example.data.model.FeedComment
import com.example.data.model.FeedPost
import com.example.data.model.RankedUser
import com.example.data.model.UnlockLog
import com.example.data.repository.AppRepository
import com.example.service.GeminiAiService
import com.example.service.NotificationHelper
import com.example.service.PacocaFrameHelper
import com.example.service.SupabaseAuthHelper
import com.example.service.SupabaseSyncService
import com.example.service.SupabaseUser
import com.example.service.SyncLogger
import com.example.data.model.SyncHealthStatus
import com.example.data.model.SyncLog
import com.example.service.UnlockMonitorService
import com.example.service.WallpaperHelper
import com.example.service.WallpaperTarget
import com.example.theme.ThemePresets
import com.example.util.SafeWordHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = AppRepository(db.unlockDao(), db.settingsDao(), db.feedDao(), application)
    val audioManager = AudioVoiceManager.getInstance(application)

    val settingsState: StateFlow<AppSettings> = repository.settingsFlow
        .map { it ?: AppSettings() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    val allLogsState: StateFlow<List<UnlockLog>> = repository.allLogsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalUnlocksState: StateFlow<Int> = repository.totalUnlocksFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val feedPostsState: StateFlow<List<com.example.data.model.FeedPost>> = repository.feedPostsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val _todayUnlockCount = MutableStateFlow(0)
    val todayUnlockCount: StateFlow<Int> = _todayUnlockCount.asStateFlow()

    private val _rankingPeriod = MutableStateFlow("today") // "today" or "all_time"
    val rankingPeriod: StateFlow<String> = _rankingPeriod.asStateFlow()

    fun setRankingPeriod(period: String) {
        _rankingPeriod.value = period
    }

    val rankedUsersState: StateFlow<List<RankedUser>> = kotlinx.coroutines.flow.combine(
        feedPostsState,
        settingsState,
        todayUnlockCount,
        totalUnlocksState,
        _rankingPeriod
    ) { posts, settings, todayCount, totalCount, period ->
        buildRankingList(posts, settings, todayCount, totalCount, period)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun buildRankingList(
        posts: List<FeedPost>,
        settings: AppSettings,
        todayCount: Int,
        totalCount: Int,
        period: String
    ): List<RankedUser> {
        val isKool = settings.isCaraDeKoolMode
        val isToday = period == "today"

        // Base community members
        val communitySeed = listOf(
            RankedUser(
                name = "Mariana Paçoca",
                avatarEmoji = "🥜",
                photoUri = null,
                unlockCount = if (isToday) 28 else 342,
                badgeTitle = "Mestre Paçoca 👑",
                isCurrentUser = false,
                isKool = false
            ),
            RankedUser(
                name = "Lucas Silva",
                avatarEmoji = "🚀",
                photoUri = null,
                unlockCount = if (isToday) 22 else 289,
                badgeTitle = "Desbloqueador Pro 🔥",
                isCurrentUser = false,
                isKool = false
            ),
            RankedUser(
                name = "Beatriz Kawaii",
                avatarEmoji = "✨",
                photoUri = null,
                unlockCount = if (isToday) 19 else 245,
                badgeTitle = "Top Membro ✨",
                isCurrentUser = false,
                isKool = false
            ),
            RankedUser(
                name = "Gabriel Gamer",
                avatarEmoji = "🎮",
                photoUri = null,
                unlockCount = if (isToday) 15 else 198,
                badgeTitle = "Gamer Ativo ⚡",
                isCurrentUser = false,
                isKool = false
            ),
            RankedUser(
                name = "Camila Tardígrado",
                avatarEmoji = "👾",
                photoUri = null,
                unlockCount = if (isToday) 12 else 176,
                badgeTitle = "Cara de Cu Fan 👾",
                isCurrentUser = false,
                isKool = true
            ),
            RankedUser(
                name = "Rodrigo Santos",
                avatarEmoji = "😎",
                photoUri = null,
                unlockCount = if (isToday) 8 else 120,
                badgeTitle = "Amante da Paçoca 🥜",
                isCurrentUser = false,
                isKool = false
            )
        )

        val userList = mutableListOf<RankedUser>()

        // 1. Current user
        val userUnlock = if (isToday) todayCount else totalCount.coerceAtLeast(todayCount)
        val userName = if (settings.userName.isNotBlank()) settings.userName else (if (isKool) "Você (Cara de Cu)" else "Você (Cara de Paçoca)")
        val userEmoji = if (isKool) "👾" else settings.userAvatarEmoji.ifBlank { "🥜" }
        val userPhoto = settings.userProfilePhotoUri ?: settings.userPhotoUri

        val currentUserEntry = RankedUser(
            name = userName,
            avatarEmoji = userEmoji,
            photoUri = userPhoto,
            unlockCount = userUnlock,
            isCurrentUser = true,
            badgeTitle = if (isKool) "Meu Perfil Cu 👾" else "Meu Perfil 🥜",
            isKool = isKool
        )
        userList.add(currentUserEntry)

        // 2. Community posts from Feed / Cloud
        val groupedByAuthor = posts.filter { !it.isUserPost && it.authorName.isNotBlank() }
            .groupBy { it.authorName }

        for ((author, authorPosts) in groupedByAuthor) {
            val maxUnlock = authorPosts.maxOfOrNull { it.unlockCount } ?: 1
            val firstPost = authorPosts.first()
            val adjustedCount = if (isToday) maxUnlock else maxUnlock * 7 + 10
            val isAuthorKool = firstPost.themeTag.contains("Kool", ignoreCase = true) || firstPost.themeTag.contains("Cu", ignoreCase = true) || firstPost.authorAvatarEmoji == "👾"
            userList.add(
                RankedUser(
                    name = author,
                    avatarEmoji = firstPost.authorAvatarEmoji.ifBlank { if (isAuthorKool) "👾" else "🥜" },
                    photoUri = firstPost.authorAvatarUri ?: firstPost.photoUri,
                    unlockCount = adjustedCount,
                    isCurrentUser = false,
                    badgeTitle = if (isAuthorKool) "Comunidade Cu 👾" else "Comunidade Paçoca 🥜",
                    isKool = isAuthorKool
                )
            )
        }

        // 3. Add seed users if not already present
        for (seed in communitySeed) {
            if (userList.none { it.name.equals(seed.name, ignoreCase = true) }) {
                userList.add(seed)
            }
        }

        // Sort descending by unlockCount and assign rank numbers
        val sorted = userList.sortedByDescending { it.unlockCount }
        return sorted.mapIndexed { index, item ->
            val rankNum = index + 1
            val dynamicBadge = when (rankNum) {
                1 -> if (isKool) "Rei do Cu 👑👾" else "Rei da Paçoca 👑🥜"
                2 -> if (isKool) "Vice-Campeão Cu 🥈" else "Vice-Campeão Paçoca 🥈"
                3 -> if (isKool) "Bronze Supremo 🥉" else "Bronze Paçoca 🥉"
                else -> item.badgeTitle
            }
            item.copy(rank = rankNum, badgeTitle = dynamicBadge)
        }
    }

    private val _lastSpokenPhrase = MutableStateFlow("Cara de cu desbloqueado!")
    val lastSpokenPhrase: StateFlow<String> = _lastSpokenPhrase.asStateFlow()

    private val _activeCommunityLockPost = MutableStateFlow<FeedPost?>(null)
    val activeCommunityLockPost: StateFlow<FeedPost?> = _activeCommunityLockPost.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isScreenProtectorActive = MutableStateFlow(false)
    val isScreenProtectorActive: StateFlow<Boolean> = _isScreenProtectorActive.asStateFlow()

    private val _showUnlockCelebration = MutableStateFlow(false)
    val showUnlockCelebration: StateFlow<Boolean> = _showUnlockCelebration.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val currentUser: StateFlow<SupabaseUser?> = SupabaseAuthHelper.currentUser
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    val syncLogsState: StateFlow<List<SyncLog>> = SyncLogger.syncLogs
    val syncHealthState: StateFlow<SyncHealthStatus> = SyncLogger.healthStatus

    init {
        SupabaseAuthHelper.init(application)
        SyncLogger.init(application)
        refreshTodayCount()
        viewModelScope.launch {
            repository.ensureInitialFeedPosts()
            updateActiveCommunityLockPost()
            
            // Only clean 24h old posts if explicitly enabled by user
            val s = repository.getSettingsDirect() ?: AppSettings()
            if (s.isAutoDeleteOldPosts24hEnabled) {
                repository.cleanOldUserPosts24h()
            }

            // Sincroniza feed global do Supabase e inicia listener em tempo real
            val currentUser = SupabaseAuthHelper.currentUser.value
            SupabaseSyncService.syncCommunityFeedFromCloud(getApplication(), currentUser?.id)
            SupabaseSyncService.startRealtimeCommunityListener(getApplication())
        }

        // Automatic Cloud Sync if user is already logged in or state changes
        viewModelScope.launch {
            SupabaseAuthHelper.currentUser.collect { user ->
                if (user != null) {
                    performCloudSync()
                } else {
                    SupabaseSyncService.syncCommunityFeedFromCloud(getApplication(), null)
                    SupabaseSyncService.startRealtimeCommunityListener(getApplication())
                    val s = repository.getSettingsDirect() ?: AppSettings()
                    if (s.isAutoDeleteOldPosts24hEnabled) {
                        repository.cleanOldUserPosts24h()
                    }
                }
            }
        }

        // Listen for real device unlock events detected by the Foreground Service
        viewModelScope.launch {
            UnlockMonitorService.unlockEvents.collect { event ->
                _todayUnlockCount.value = event.countToday
                _lastSpokenPhrase.value = event.phrase
                _showUnlockCelebration.value = true
                refreshTodayCount()
                updateActiveCommunityLockPost()
            }
        }
        // Periodic Community Wallpaper Slider loop (for 5 min, 30 min intervals)
        viewModelScope.launch {
            while (true) {
                val settings = settingsState.value
                val delayMs = when (settings.communitySliderInterval) {
                    "5_min" -> 5 * 60 * 1000L
                    "30_min" -> 30 * 60 * 1000L
                    else -> 60 * 1000L
                }
                kotlinx.coroutines.delay(delayMs)
                if (settings.isCommunitySliderEnabled && settings.communitySliderInterval != "unlock") {
                    val communityPosts = feedPostsState.value.filter { !it.isUserPost }
                    if (communityPosts.isNotEmpty()) {
                        val currentActiveId = _activeCommunityLockPost.value?.id
                        val otherPosts = communityPosts.filter { it.id != currentActiveId }
                        val nextPost = if (otherPosts.isNotEmpty()) otherPosts.random() else communityPosts.first()
                        _activeCommunityLockPost.value = nextPost

                        // Apply to system lockscreen if permissible
                        if (!nextPost.photoUri.isNullOrBlank()) {
                            WallpaperHelper.applyWallpaper(
                                context = getApplication(),
                                target = WallpaperTarget.LOCK_SCREEN,
                                filePath = nextPost.photoUri
                            )
                        }
                    }
                }
            }
        }
    }

    private fun updateActiveCommunityLockPost() {
        val settings = settingsState.value
        val posts = feedPostsState.value
        if (settings.activeLockscreenPostId != null) {
            _activeCommunityLockPost.value = posts.find { it.id == settings.activeLockscreenPostId }
        } else if (settings.isCommunitySliderEnabled && posts.isNotEmpty()) {
            _activeCommunityLockPost.value = posts.firstOrNull { !it.isUserPost } ?: posts.first()
        } else {
            _activeCommunityLockPost.value = null
        }
    }

    fun refreshTodayCount() {
        viewModelScope.launch {
            _todayUnlockCount.value = repository.getTodayUnlockCount()
        }
    }

    fun setScreenProtectorActive(active: Boolean) {
        _isScreenProtectorActive.value = active
        if (active) {
            val settings = settingsState.value
            audioManager.playLockAudio(settings.customLockText, viewModelScope)
            // If community slider is on, pick next random or sequential post
            if (settings.isCommunitySliderEnabled) {
                val posts = feedPostsState.value.filter { !it.isUserPost }
                if (posts.isNotEmpty()) {
                    val randomPost = posts.random()
                    _activeCommunityLockPost.value = randomPost
                }
            }
        }
    }

    /**
     * Executes the unlock action: increments count, logs to DB, plays TTS/audio, vibrates, triggers confetti
     */
    fun performUnlock(capturedSelfieBitmap: Bitmap? = null, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            val settings = settingsState.value
            val currentCount = repository.getTodayUnlockCount() + 1
            _todayUnlockCount.value = currentCount

            // If a selfie was taken on unlock, process through PacocaFrameHelper and save it
            var latestPhotoPath = settings.userPhotoUri
            if (capturedSelfieBitmap != null) {
                try {
                    val framedPath = PacocaFrameHelper.processAndApplyPacocaFrame(
                        context = getApplication(),
                        sourceBitmap = capturedSelfieBitmap,
                        isKoolMode = settings.isCaraDeKoolMode
                    )
                    latestPhotoPath = framedPath
                } catch (e: Exception) {
                    latestPhotoPath = saveUserPhotoInternal(capturedSelfieBitmap)
                }
                updateSettings {
                    it.copy(
                        userPhotoUri = latestPhotoPath,
                        userWallpaperUri = latestPhotoPath,
                        lockscreenPhotoUri = latestPhotoPath,
                        userProfilePhotoUri = latestPhotoPath,
                        selectedThemeId = if (settings.isCaraDeKoolMode) "kool" else "custom_photo"
                    )
                }
            }

            // Auto set as Lockscreen Wallpaper only if toggle is enabled
            if (settings.isAutoSetUnlockPhotoAsLockWallpaper && !latestPhotoPath.isNullOrBlank()) {
                WallpaperHelper.applyWallpaper(
                    context = getApplication(),
                    target = WallpaperTarget.LOCK_SCREEN,
                    filePath = latestPhotoPath
                )
            }

            val ordinal = audioManager.toPortugueseOrdinal(currentCount)

            // Check if active wallpaper is from another community user
            val communityPost = if (settings.isCommunitySliderEnabled || settings.activeLockscreenPostId != null) {
                _activeCommunityLockPost.value
            } else null

            // Determine unlock phrase (Community user special phrase, AI conversational, counter or custom)
            val rawPhrase = if (communityPost != null && !communityPost.isUserPost) {
                if (settings.isCaraDeKoolMode) {
                    "Cara de cu da ${communityPost.authorName} desbloqueado pela ${communityPost.unlockCount}ª vez, e contando! 👾"
                } else {
                    "Cara de paçoca da ${communityPost.authorName} desbloqueado pela ${communityPost.unlockCount}ª vez, e contando! 🥜"
                }
            } else if (settings.isAiConversationalEnabled) {
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
                audioManager.formatUnlockPhrase(
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

            _lastSpokenPhrase.value = phrase

            // Record in Room Database and sync to Firestore
            repository.recordUnlock(phrase, settings.selectedThemeId)

            // Auto-post photo to community feed if enabled
            if (settings.isAutoPostOnUnlockEnabled) {
                val cuWord = if (settings.isSafeWordMode) "cara de cool" else "cara de cu"
                val autoCaption = if (settings.isCaraDeKoolMode) {
                    "Olha a $cuWord do ${settings.userName} pela ${currentCount}ª vez! ✨"
                } else {
                    "Olha a cara de paçoca do ${settings.userName} pela ${currentCount}ª vez! 🥜"
                }
                val themePreset = ThemePresets.getById(settings.selectedThemeId)
                val userAvatar = settings.userProfilePhotoUri ?: latestPhotoPath
                val userEmoji = if (settings.isCaraDeKoolMode) "👾" else settings.userAvatarEmoji.ifBlank { "🥜" }
                repository.createFeedPost(
                    authorName = settings.userName,
                    authorAvatarEmoji = userEmoji,
                    authorAvatarUri = userAvatar,
                    photoUri = latestPhotoPath,
                    presetImageKey = if (latestPhotoPath == null) (if (settings.isCaraDeKoolMode) "kool_avatar" else "user_avatar") else null,
                    caption = autoCaption,
                    unlockCount = currentCount,
                    themeTag = if (settings.isCaraDeKoolMode) (if (settings.isSafeWordMode) "Modo Cara de Cool" else "Modo Cara de Cu") else themePreset.name,
                    isUserPost = true
                )
            }

            // Play audio / voice
            if (communityPost != null && !communityPost.isUserPost) {
                audioManager.speak(phrase)
            } else if (settings.isAiConversationalEnabled) {
                audioManager.speak(phrase)
            } else {
                audioManager.playUnlockAudio(
                    userName = settings.userName,
                    countToday = currentCount,
                    audioType = settings.unlockAudioType,
                    customText = settings.customUnlockText,
                    isAiNatural = false,
                    customAudioUri = settings.customAudioPath,
                    coroutineScope = viewModelScope,
                    isKoolMode = settings.isCaraDeKoolMode,
                    isSafeWordMode = settings.isSafeWordMode
                )
            }

            // Haptic vibration
            vibrateDevice()

            // Trigger celebration
            _showUnlockCelebration.value = true
            _isScreenProtectorActive.value = false

            onComplete?.invoke()
        }
    }

    fun dismissCelebration() {
        _showUnlockCelebration.value = false
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun updateSettings(update: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            val current = settingsState.value
            val modified = update(current)
            repository.updateSettings(modified)
            updateActiveCommunityLockPost()
        }
    }

    fun completeQuickSetup(photoPath: String?, audioType: String, customPhrase: String?) {
        viewModelScope.launch {
            val current = settingsState.value
            val updated = current.copy(
                isFirstTimeSetupCompleted = true,
                userPhotoUri = photoPath ?: current.userPhotoUri,
                userWallpaperUri = photoPath ?: current.userWallpaperUri,
                lockscreenPhotoUri = photoPath ?: current.lockscreenPhotoUri,
                selectedThemeId = if (photoPath != null) "custom_photo" else "classic",
                unlockAudioType = audioType,
                customUnlockText = customPhrase ?: current.customUnlockText
            )
            repository.updateSettings(updated)

            // Auto apply to lockscreen in quick setup
            if (!photoPath.isNullOrBlank()) {
                WallpaperHelper.applyWallpaper(
                    context = getApplication(),
                    target = WallpaperTarget.LOCK_SCREEN,
                    filePath = photoPath
                )
            }

            _statusMessage.value = "Configuração do Cara de Paçoca concluída com sucesso!"
        }
    }

    private fun saveUserPhotoInternal(bitmap: Bitmap): String {
        val file = File(getApplication<Application>().filesDir, "cara_de_pacoca_photo_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        val path = file.absolutePath
        updateSettings {
            it.copy(
                userPhotoUri = path,
                userWallpaperUri = path,
                lockscreenPhotoUri = path,
                selectedThemeId = "custom_photo"
            )
        }
        return path
    }

    fun saveUserPhoto(bitmap: Bitmap): String {
        return saveUserPhotoInternal(bitmap)
    }

    fun applyTargetedWallpaper(
        target: WallpaperTarget,
        customBitmap: Bitmap? = null,
        customPath: String? = null,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val settings = settingsState.value
            val path = customPath ?: settings.lockscreenPhotoUri ?: settings.userWallpaperUri ?: settings.userPhotoUri

            val (success, message) = if (customBitmap != null) {
                WallpaperHelper.applyWallpaper(getApplication(), target, bitmap = customBitmap)
            } else if (!path.isNullOrBlank()) {
                WallpaperHelper.applyWallpaper(getApplication(), target, filePath = path)
            } else {
                val defaultBitmap = WallpaperHelper.createDefaultPacocaWallpaper(
                    context = getApplication(),
                    title = "Cara de Paçoca",
                    emoji = "🥜",
                    unlockCount = _todayUnlockCount.value.coerceAtLeast(1)
                )
                WallpaperHelper.applyWallpaper(getApplication(), target, bitmap = defaultBitmap)
            }

            if (success) {
                updateSettings { curr ->
                    when (target) {
                        WallpaperTarget.LOCK_SCREEN -> curr.copy(lockscreenPhotoUri = path)
                        WallpaperTarget.HOME_SCREEN -> curr.copy(homescreenPhotoUri = path)
                        WallpaperTarget.BOTH -> curr.copy(lockscreenPhotoUri = path, homescreenPhotoUri = path, userWallpaperUri = path)
                    }
                }
            }

            withContext(Dispatchers.Main) {
                onResult(success, message)
            }
        }
    }

    fun setCommunityPostAsLockWallpaper(post: FeedPost, onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            val path = post.photoUri
            val (success, message) = if (!path.isNullOrBlank() && File(path).exists()) {
                WallpaperHelper.applyWallpaper(getApplication(), WallpaperTarget.LOCK_SCREEN, filePath = path)
            } else {
                val bannerBitmap = WallpaperHelper.createDefaultPacocaWallpaper(
                    context = getApplication(),
                    title = post.authorName,
                    emoji = post.authorAvatarEmoji,
                    unlockCount = post.unlockCount
                )
                WallpaperHelper.applyWallpaper(getApplication(), WallpaperTarget.LOCK_SCREEN, bitmap = bannerBitmap)
            }

            if (success) {
                val settings = settingsState.value
                repository.markPostWallpaperUsed(post.id, settings.userName)
                updateSettings {
                    it.copy(
                        activeLockscreenPostId = post.id,
                        lockscreenPhotoUri = post.photoUri
                    )
                }
                _activeCommunityLockPost.value = post
                vibrateShort()
                _statusMessage.value = "🔒 Foto de ${post.authorName} definida como seu Wallpaper de Desbloqueio!"
            }

            withContext(Dispatchers.Main) {
                onResult?.invoke(success, message)
            }
        }
    }

    fun toggleCommunitySlider(enabled: Boolean, interval: String = "unlock") {
        updateSettings {
            it.copy(
                isCommunitySliderEnabled = enabled,
                communitySliderInterval = interval,
                activeLockscreenPostId = if (!enabled) null else it.activeLockscreenPostId
            )
        }
        _statusMessage.value = if (enabled) {
            "🔄 Rodízio de Fotos da Comunidade no Wallpaper de Desbloqueio ativado!"
        } else {
            "Rodízio de fotos desativado. Seu wallpaper padrão foi restaurado."
        }
    }

    fun toggleAutoSetLockWallpaper(enabled: Boolean) {
        updateSettings { it.copy(isAutoSetUnlockPhotoAsLockWallpaper = enabled) }
        _statusMessage.value = if (enabled) {
            "⚡ Ativado: Toda foto tirada ao desbloquear virará seu wallpaper de bloqueio!"
        } else {
            "Auto-wallpaper no desbloqueio desativado."
        }
    }

    fun toggleAutoPostOnUnlock(enabled: Boolean) {
        updateSettings { it.copy(isAutoPostOnUnlockEnabled = enabled) }
        _statusMessage.value = if (enabled) {
            "📸 Publicação automática no Feed da Comunidade ativada!"
        } else {
            "Publicação automática no Feed desativada."
        }
    }

    fun reactToPost(postId: Long, emoji: String) {
        viewModelScope.launch {
            val settings = settingsState.value
            repository.reactToPost(postId, emoji, settings.userName)
            vibrateShort()
        }
    }

    fun updateUserProfile(name: String, emoji: String, photoUri: String?) {
        viewModelScope.launch {
            repository.updateProfile(name, emoji, photoUri)
            _statusMessage.value = "Perfil atualizado com sucesso! 🥜"
        }
    }

    fun processAndApplyPacocaFrame(
        context: android.content.Context,
        sourceUri: Uri,
        onComplete: ((String?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val isKool = settingsState.value.isCaraDeKoolMode
                val isFrameEnabled = settingsState.value.isPhotoFrameEnabled
                val framedPath = PacocaFrameHelper.processAndApplyPacocaFrame(
                    context = context,
                    sourceUri = sourceUri,
                    isKoolMode = isKool,
                    isFrameEnabled = isFrameEnabled
                )
                updateUserProfile(
                    name = settingsState.value.userName,
                    emoji = if (isKool) "👾" else settingsState.value.userAvatarEmoji,
                    photoUri = framedPath
                )
                _statusMessage.value = if (isKool) "Cara de Cu gerada com sucesso! 👾✨" else "Cara de Paçoca gerada com sucesso! 🥜✨"
                onComplete?.invoke(framedPath)
            } catch (e: Exception) {
                _statusMessage.value = "Erro ao processar foto: ${e.localizedMessage}"
                onComplete?.invoke(null)
            }
        }
    }

    fun setPhotoFrameEnabled(enabled: Boolean) {
        updateSettings {
            it.copy(isPhotoFrameEnabled = enabled)
        }
        _statusMessage.value = if (enabled) "Moldura de Paçoca ativada nas fotos 🥜" else "Moldura desativada nas fotos 📸"
    }

    fun toggleSafeWordMode(enabled: Boolean) {
        updateSettings {
            val isCu = it.isCaraDeKoolMode
            val currentName = it.userName
            val newName = if (isCu) {
                if (enabled) {
                    if (currentName == "Você (Cara de Cu)") "Você (Cara de Cool)" else currentName
                } else {
                    if (currentName == "Você (Cara de Cool)") "Você (Cara de Cu)" else currentName
                }
            } else currentName
            it.copy(
                isSafeWordMode = enabled,
                userName = newName
            )
        }
        val label = if (enabled) "Modo Palavra Segura ativado! (Cool ativado)" else "Modo Palavra Segura desativado! (Cu ativado)"
        _statusMessage.value = label
    }

    fun toggleCaraDeKoolMode(enabled: Boolean) {
        updateSettings {
            val currentName = it.userName
            val isSafe = it.isSafeWordMode
            val newName = if (enabled) {
                if (currentName.contains("Paçoca", ignoreCase = true) || currentName.isBlank()) {
                    if (isSafe) "Você (Cara de Cool)" else "Você (Cara de Cu)"
                } else currentName
            } else {
                if (currentName == "Você (Cara de Cu)" || currentName == "Você (Cara de Kool)" || currentName == "Você (Cara de Cool)") "Você (Cara de Paçoca)" else currentName
            }
            val newEmoji = if (enabled) {
                if (it.userAvatarEmoji == "🥜" || it.userAvatarEmoji == "💖" || it.userAvatarEmoji.isBlank()) "👾" else it.userAvatarEmoji
            } else {
                if (it.userAvatarEmoji == "👾" || it.userAvatarEmoji == "💖") "🥜" else it.userAvatarEmoji
            }
            it.copy(
                isCaraDeKoolMode = enabled,
                userName = newName,
                userAvatarEmoji = newEmoji,
                selectedThemeId = if (enabled) "kool" else if (it.selectedThemeId == "kool") "classic" else it.selectedThemeId
            )
        }
        val isSafe = settingsState.value.isSafeWordMode
        val modeWord = if (isSafe) "Cara de Cool" else "Cara de Cu"
        _statusMessage.value = if (enabled) "Modo $modeWord ativado! ✨" else "Modo Cara de Paçoca ativado! ✨"
        if (enabled) {
            audioManager.speak("Modo $modeWord ativado! Agora você é uma $modeWord!", pitch = 1.15f)
        } else {
            audioManager.speak("Modo Cara de Paçoca ativado! De volta à paçoca!", pitch = 1.0f)
        }
    }

    fun setKoolPalette(palette: String) {
        updateSettings {
            it.copy(koolColorPalette = palette)
        }
        val paletteName = when (palette) {
            "universal" -> "Paleta Universal (Laranja & Roxo)"
            "pacoca" -> "Paleta Paçoca"
            "nude_peach", "kool_1" -> "Paleta Cara de Kool (Pele & Pêssego)"
            "pink_berry", "kool_2" -> "Paleta Cara de Kool 2 (Rosa Chiclete)"
            else -> "Paleta Universal"
        }
        _statusMessage.value = "Paleta alterada para: $paletteName 🎨"
    }

    fun cycleKoolPalette() {
        val current = settingsState.value.koolColorPalette
        val next = when (current) {
            "universal" -> "pacoca"
            "pacoca" -> "nude_peach"
            "nude_peach", "kool_1" -> "pink_berry"
            else -> "universal"
        }
        setKoolPalette(next)
    }

    fun togglePostLike(postId: Long) {
        viewModelScope.launch {
            val settings = settingsState.value
            repository.togglePostLike(postId, settings.userName)
            vibrateShort()
        }
    }

    fun getCommentsForPost(postId: Long): Flow<List<FeedComment>> {
        return repository.getCommentsForPost(postId)
    }

    fun addCommentToPost(postId: Long, commentText: String) {
        if (commentText.isBlank()) return
        viewModelScope.launch {
            val settings = settingsState.value
            val authorName = settings.userName
            val authorEmoji = settings.userAvatarEmoji
            val authorAvatar = settings.userProfilePhotoUri ?: settings.userPhotoUri

            repository.addComment(
                postId = postId,
                authorName = authorName,
                text = commentText.trim(),
                authorAvatarEmoji = authorEmoji,
                authorAvatarUri = authorAvatar
            )
            vibrateShort()
        }
    }

    fun refreshFeedFromCloud() {
        viewModelScope.launch {
            repository.ensureInitialFeedPosts()
            val user = SupabaseAuthHelper.currentUser.value
            val result = SupabaseSyncService.syncCommunityFeedFromCloud(getApplication(), user?.id)
            if (result.isSuccess) {
                _statusMessage.value = "Feed sincronizado com sucesso! 🔄"
            } else {
                _statusMessage.value = "Feed carregado do armazenamento local."
            }
        }
    }

    fun toggleAutoDeleteOldPosts24h(enabled: Boolean) {
        updateSettings {
            it.copy(
                isAutoDeleteOldPosts24hEnabled = enabled,
                isAutoDeleteCustomizedByUser = true
            )
        }
        if (enabled) {
            viewModelScope.launch {
                val deleted = repository.cleanOldUserPosts24h()
                if (deleted > 0) {
                    _statusMessage.value = "Limpeza realizada: $deleted post(s) com mais de 24h excluído(s)."
                } else {
                    _statusMessage.value = "Auto-limpeza de 24h ativada."
                }
            }
        } else {
            _statusMessage.value = "Auto-limpeza de 24h desativada."
        }
    }

    fun runManual24hCleanup(onResult: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val deleted = repository.cleanOldUserPosts24h()
            _statusMessage.value = if (deleted > 0) {
                "Limpeza concluída: $deleted post(s) excluído(s)."
            } else {
                "Nenhum post com mais de 24 horas encontrado."
            }
            onResult?.invoke(deleted)
        }
    }

    fun startAudioRecording() {
        val file = File(getApplication<Application>().filesDir, "custom_pacoca_audio.mp4")
        if (audioManager.startRecording(file.absolutePath)) {
            _isRecording.value = true
        }
    }

    fun stopAudioRecording() {
        val path = audioManager.stopRecording()
        _isRecording.value = false
        if (path != null) {
            updateSettings { it.copy(customAudioPath = path, unlockAudioType = "custom_recording") }
            _statusMessage.value = "Áudio gravado com sucesso!"
        }
    }

    fun testAudioPreview() {
        val settings = settingsState.value
        val count = _todayUnlockCount.value.coerceAtLeast(1)
        audioManager.playUnlockAudio(
            userName = settings.userName,
            countToday = count,
            audioType = settings.unlockAudioType,
            customText = settings.customUnlockText,
            isAiNatural = settings.isAiConversationalEnabled,
            customAudioUri = settings.customAudioPath,
            coroutineScope = viewModelScope
        )
    }

    fun previewAudioPreset(audioType: String, customText: String) {
        val settings = settingsState.value
        val count = _todayUnlockCount.value.coerceAtLeast(1)
        audioManager.playUnlockAudio(
            userName = settings.userName,
            countToday = count,
            audioType = audioType,
            customText = customText,
            isAiNatural = false,
            customAudioUri = settings.customAudioPath,
            coroutineScope = viewModelScope
        )
    }

    /**
     * Preview voice phrases for social interactions in settings without posting fake system notifications
     */
    fun triggerSocialVoiceInteraction(type: String, authorName: String = "Romário") {
        viewModelScope.launch {
            val spokenPhrase = when (type) {
                "like" -> "$authorName curtiu sua cara de paçoca! 🥜"
                "comment" -> "$authorName comentou na sua cara de paçoca!"
                "wallpaper" -> "$authorName usou sua cara de paçoca como wallpaper!"
                "rotation" -> "Seu wallpaper mudou! Você é agora a cara de paçoca da $authorName! Parabéns, cara de paçoca! 🥜"
                else -> "Notificação de Cara de Paçoca!"
            }
            audioManager.playUnlockAudio(
                userName = settingsState.value.userName,
                countToday = 1,
                audioType = "ai_phrase",
                customText = spokenPhrase,
                isAiNatural = true,
                customAudioUri = null,
                coroutineScope = viewModelScope
            )
            _statusMessage.value = "Prévia de áudio: \"$spokenPhrase\""
        }
    }

    fun applySystemWallpaper(onResult: (Boolean, String) -> Unit) {
        applyTargetedWallpaper(WallpaperTarget.BOTH, onResult = onResult)
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearHistory()
            _todayUnlockCount.value = 0
            _statusMessage.value = "Histórico reiniciado!"
        }
    }

    fun publishManualPost(photoUri: String?, customCaption: String? = null) {
        viewModelScope.launch {
            val settings = settingsState.value
            val isKool = settings.isCaraDeKoolMode
            val count = _todayUnlockCount.value.coerceAtLeast(1)
            val caption = customCaption?.takeIf { it.isNotBlank() }
                ?: if (isKool) "Olha minha cara de cu pela ${count}ª vez!" else "Olha minha cara de paçoca pela ${count}ª vez!"
            val themePreset = ThemePresets.getById(settings.selectedThemeId)

            val avatarPhoto = settings.userProfilePhotoUri ?: photoUri ?: settings.userPhotoUri
            val userEmoji = if (isKool) "👾" else settings.userAvatarEmoji.ifBlank { "🥜" }
            val authorName = if (isKool && (settings.userName == "Você (Cara de Paçoca)" || settings.userName.isBlank() || settings.userName == "Você (Cara de Kool)")) "Você (Cara de Cu)" else settings.userName

            repository.createFeedPost(
                authorName = authorName,
                authorAvatarEmoji = userEmoji,
                authorAvatarUri = avatarPhoto,
                photoUri = photoUri ?: settings.userPhotoUri,
                presetImageKey = if (photoUri == null && settings.userPhotoUri == null) (if (isKool) "kool_avatar" else "user_avatar") else null,
                caption = caption,
                unlockCount = count,
                themeTag = if (isKool) "Modo Cara de Cu" else themePreset.name,
                isUserPost = true
            )
            _statusMessage.value = if (isKool) "Sua foto foi postada no Feed com sucesso!" else "Sua cara de paçoca foi postada no Feed com sucesso!"
        }
    }

    fun deleteFeedPost(postId: Long) {
        viewModelScope.launch {
            repository.deletePost(postId)
        }
    }

    fun signInWithGoogle(context: android.content.Context, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = SupabaseAuthHelper.signInWithGoogle(context)
            if (result.isSuccess) {
                val user = result.getOrNull()
                if (user != null) {
                    performCloudSync()
                }
                _statusMessage.value = "Conectado com Google no Supabase! ☁️"
                onComplete(true, null)
            } else {
                _isSyncing.value = false
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Falha ao autenticar com Google"
                onComplete(false, errorMsg)
            }
        }
    }

    fun signInWithEmail(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = SupabaseAuthHelper.signInWithEmail(getApplication(), email, password)
            if (result.isSuccess) {
                performCloudSync()
                _statusMessage.value = "Login no Supabase realizado com sucesso! ☁️"
                onComplete(true, null)
            } else {
                _isSyncing.value = false
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "E-mail ou senha incorretos"
                onComplete(false, errorMsg)
            }
        }
    }

    fun signUpWithEmail(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = SupabaseAuthHelper.signUpWithEmail(getApplication(), email, password)
            if (result.isSuccess) {
                performCloudSync()
                _statusMessage.value = "Conta criada com sucesso no Supabase! ☁️"
                onComplete(true, null)
            } else {
                _isSyncing.value = false
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Não foi possível criar a conta"
                onComplete(false, errorMsg)
            }
        }
    }

    fun completeFirstTimeSetup() {
        updateSettings { it.copy(isFirstTimeSetupCompleted = true) }
    }

    fun signOut() {
        SupabaseAuthHelper.signOut(getApplication())
        _statusMessage.value = "Você agora está no Modo Convidado"
    }

    fun performCloudSync(onComplete: ((Boolean) -> Unit)? = null) {
        val user = SupabaseAuthHelper.currentUser.value ?: return
        viewModelScope.launch {
            _isSyncing.value = true
            val result = SupabaseSyncService.syncOnLogin(getApplication(), user)
            _isSyncing.value = false
            if (result.isSuccess) {
                _statusMessage.value = "Supabase sincronizado com sucesso! ☁️"
                onComplete?.invoke(true)
            } else {
                onComplete?.invoke(false)
            }
        }
    }

    fun forceSyncAll(onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            _isSyncing.value = true
            val user = SupabaseAuthHelper.currentUser.value
            if (user != null) {
                SupabaseSyncService.syncOnLogin(getApplication(), user)
            } else {
                SupabaseSyncService.syncCommunityFeedFromCloud(getApplication(), null)
            }
            SupabaseSyncService.startRealtimeCommunityListener(getApplication())
            _isSyncing.value = false
            _statusMessage.value = "Feed e Supabase sincronizados com sucesso! 🔄"
            onComplete?.invoke(true)
        }
    }

    fun retryPostSync(postId: Long) {
        viewModelScope.launch {
            val post = repository.getPostById(postId) ?: return@launch
            _statusMessage.value = "Reenviando post #${postId} para o Supabase..."
            SupabaseSyncService.syncNewPost(getApplication(), post)
        }
    }

    fun runSyncDiagnosticTest(): kotlinx.coroutines.flow.Flow<Pair<String, Boolean>> {
        return SyncLogger.runFullDiagnosticTest(getApplication())
    }

    fun exportSyncLogs(): String {
        return SyncLogger.exportLogsAsText()
    }

    fun clearSyncLogs() {
        SyncLogger.clearLogs(getApplication())
        _statusMessage.value = "Histórico de logs de sincronização limpo!"
    }

    private fun vibrateDevice() {
        vibrateShort()
    }

    private fun vibrateShort() {
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(VibratorManager::class.java)
                manager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Vibrator::class.java)
                @Suppress("DEPRECATION")
                vibrator?.vibrate(40)
            }
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        SupabaseSyncService.stopRealtimeCommunityListener()
        audioManager.release()
    }
}
