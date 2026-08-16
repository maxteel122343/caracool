package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.util.SafeWordHelper
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.theme.ThemePresets
import com.example.ui.components.AuthAccountDialog
import com.example.ui.components.HomeUnlockRankingCard
import com.example.ui.components.PaçocaConfetti
import com.example.ui.components.TutorialGuideDialog
import com.example.ui.components.UserProfileDialog
import com.example.ui.components.WallpaperRenderer
import com.example.ui.components.TardigradeMascotIcon
import com.example.ui.components.DownloadApkDialog
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Sync
import com.example.ui.theme.NaturalAccentDark
import com.example.ui.theme.NaturalAccentGold
import com.example.ui.theme.NaturalBorderDarker
import com.example.ui.theme.NaturalBorderLight
import com.example.ui.theme.NaturalBorderSubtle
import com.example.ui.theme.NaturalTextMuted
import com.example.ui.theme.NaturalTextPrimary
import com.example.ui.theme.NaturalTextSecondary
import com.example.ui.viewmodel.MainViewModel
import java.io.File
import android.graphics.BitmapFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    settings: AppSettings,
    onNavigateToAudio: () -> Unit,
    onNavigateToThemes: () -> Unit,
    onNavigateToStats: () -> Unit,
    onLaunchScreenProtector: () -> Unit,
    onNavigateToFeed: () -> Unit = {}
) {
    val todayCount by viewModel.todayUnlockCount.collectAsState()
    val totalCount by viewModel.totalUnlocksState.collectAsState()
    val showCelebration by viewModel.showUnlockCelebration.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val rankedUsers by viewModel.rankedUsersState.collectAsState()
    val currentRankingPeriod by viewModel.rankingPeriod.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showDownloadApkDialog by remember { mutableStateOf(false) }
    var showTutorialDialog by remember(settings.isFirstTimeSetupCompleted) {
        mutableStateOf(!settings.isFirstTimeSetupCompleted)
    }

    LaunchedEffect(statusMsg) {
        statusMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    if (showDownloadApkDialog) {
        DownloadApkDialog(
            onDismiss = { showDownloadApkDialog = false }
        )
    }

    if (showTutorialDialog) {
        TutorialGuideDialog(
            settings = settings,
            viewModel = viewModel,
            onDismiss = {
                viewModel.completeFirstTimeSetup()
                showTutorialDialog = false
            },
            onOpenLockscreenPreview = onLaunchScreenProtector
        )
    }

    if (showAuthDialog) {
        AuthAccountDialog(
            viewModel = viewModel,
            currentUser = currentUser,
            onDismiss = { showAuthDialog = false }
        )
    }

    if (showProfileDialog) {
        UserProfileDialog(
            settings = settings,
            onDismiss = { showProfileDialog = false },
            onSave = { name, emoji, photoUri ->
                viewModel.updateUserProfile(name, emoji, photoUri)
            },
            onToggleFrame = { enabled ->
                viewModel.setPhotoFrameEnabled(enabled)
            }
        )
    }

    val activePreset = remember(settings.selectedThemeId) {
        ThemePresets.getById(settings.selectedThemeId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Column {
                            val appTitle = SafeWordHelper.getAppDisplayName(settings.isCaraDeKoolMode, settings.isSafeWordMode)
                            Text(
                                text = appTitle,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 19.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (currentUser != null) "NUVEM CONECTADA" else "MODO CONVIDADO",
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                letterSpacing = 1.sp,
                                maxLines = 1,
                                color = if (currentUser != null) Color(0xFF2E7D32) else MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                },
                actions = {
                    // Palette Switcher (When Kool mode is active)
                    if (settings.isCaraDeKoolMode) {
                        IconButton(
                            onClick = { viewModel.cycleKoolPalette() },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Trocar Paleta de Cores",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    // Download APK Button
                    IconButton(
                        onClick = { showDownloadApkDialog = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Baixar APK",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Tutorial Guide Button
                    IconButton(
                        onClick = { showTutorialDialog = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Tutorial",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Google Login / Account Status Button (Replaced Cloud Icon)
                    IconButton(
                        onClick = { showAuthDialog = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (currentUser != null) Color(0xFFE8F5E9) else Color(0xFFF1F3F4))
                                .border(
                                    1.dp,
                                    if (currentUser != null) Color(0xFFA5D6A7) else Color(0xFFDADCE0),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = "G",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = if (currentUser != null) Color(0xFF2E7D32) else Color(0xFF4285F4)
                                )
                            }
                        }
                    }

                    val profilePhotoPath = settings.userProfilePhotoUri ?: settings.userPhotoUri

                    Box(
                        modifier = Modifier
                            .padding(start = 2.dp, end = 12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable { showProfileDialog = true }
                            .testTag("user_profile_header_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!profilePhotoPath.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(profilePhotoPath)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Foto de Perfil",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Perfil",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 0. Hero Welcome Card with Universal Elegant Theme (Pure uniform grey background)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF1F3F5)
                        ),
                        border = BorderStroke(
                            1.dp,
                            Color(0xFFE2E4E8)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Universal 3D Lock/Security Icon Badge
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFE4E7EB))
                                    .border(
                                        1.dp,
                                        Color(0xFFD0D5DD),
                                        RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                val bannerTitle = SafeWordHelper.getAppDisplayName(settings.isCaraDeKoolMode, settings.isSafeWordMode)
                                Text(
                                    text = "Bem-vindo ao $bannerTitle",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.5.sp,
                                    color = Color(0xFF212529)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Seu assistente e desbloqueio inteligente de tela.",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = Color(0xFF6C757D)
                                )
                            }
                        }
                    }
                }

                // 1. Hero Wallpaper & Screen Protector Preview
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        shape = RoundedCornerShape(32.dp),
                        border = BorderStroke(4.dp, Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            WallpaperRenderer(
                                preset = activePreset,
                                userPhotoPath = settings.userWallpaperUri ?: settings.userPhotoUri,
                                showUserPhotoMascot = true
                            )

                            // Semi-transparent bottom gradient scrim
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                                        )
                                    )
                            )

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "WALLPAPER ATUAL",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = "'${ThemePresets.getDisplayName(activePreset, settings.isCaraDeKoolMode)}'",
                                            color = Color.White,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    // "EM USO" pill badge
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.primary,
                                                RoundedCornerShape(20.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "EM USO",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Button 1: Solid Primary (Orange)
                                    Button(
                                        onClick = onLaunchScreenProtector,
                                        modifier = Modifier
                                            .weight(1.1f)
                                            .height(42.dp)
                                            .testTag("launch_lock_screen_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("Troca Preset", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Button 2: Soft Purple Secondary (Tutorial)
                                    FilledTonalButton(
                                        onClick = { showTutorialDialog = true },
                                        modifier = Modifier
                                            .weight(0.9f)
                                            .height(42.dp)
                                            .testTag("home_tutorial_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("🎓", fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Tutorial", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Button 3: Soft Purple Lock Action
                                    FilledTonalButton(
                                        onClick = { viewModel.performUnlock() },
                                        modifier = Modifier
                                            .size(42.dp)
                                            .testTag("quick_unlock_test_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LockOpen,
                                            contentDescription = "Testar Desbloqueio Rápido",
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Daily Unlock Counter Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "HOJE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$todayCount Desbloqueios",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .height(36.dp)
                                            .padding(horizontal = 4.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary,
                                                RoundedCornerShape(18.dp)
                                            )
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "ON",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.testAudioPreview() },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                CircleShape
                                            )
                                            .testTag("preview_sound_fab")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Ouvir frase do desbloqueio",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Background detector indicator
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.background,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary,
                                                CircleShape
                                            )
                                    )
                                    Text(
                                        text = if (settings.isCaraDeKoolMode) {
                                            "Monitor em 2º plano ativo: fala a voz de Cu e conta ao desbloquear o aparelho!"
                                        } else {
                                            "Monitor em 2º plano ativo: fala a voz e conta ao desbloquear o aparelho de verdade!"
                                        },
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Spoken phrase & Voice mode overview
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.RecordVoiceOver,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "FRASE DE DESBLOQUEIO",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "COMANDO OK",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier
                                        .clickable(onClick = onNavigateToAudio)
                                        .padding(4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            val lastPhrase = viewModel.lastSpokenPhrase.value ?: ""
                            val displayedPhrase = run {
                                val processed = if (settings.isCaraDeKoolMode) {
                                    lastPhrase.replace("cara de paçoca", "cara de cu", ignoreCase = true)
                                        .replace("paçoca", "cu", ignoreCase = true)
                                        .replace("cara de kool", "cara de cu", ignoreCase = true)
                                        .replace("kool", "cu", ignoreCase = true)
                                } else {
                                    lastPhrase
                                }
                                SafeWordHelper.formatSafeWord(processed, settings.isSafeWordMode)
                            }
                            Text(
                                text = "\"$displayedPhrase\"",
                                fontSize = 14.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }

                // 4. Ranking de Desbloqueios da Comunidade com Fotos de Perfil
                item {
                    HomeUnlockRankingCard(
                        rankedUsers = rankedUsers,
                        currentPeriod = currentRankingPeriod,
                        settings = settings,
                        onPeriodChange = { period -> viewModel.setRankingPeriod(period) },
                        onPerformUnlock = { viewModel.performUnlock() },
                        onNavigateToFeed = onNavigateToFeed
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Confetti celebration animation
            PaçocaConfetti(
                isVisible = showCelebration,
                onFinished = { viewModel.dismissCelebration() }
            )
        }
    }
}

@Composable
fun ActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badge: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, NaturalBorderSubtle),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFFFFE0B2), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = badge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NaturalTextSecondary,
                    modifier = Modifier
                        .background(
                            Color(0xFFEFEBE9),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = NaturalTextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = NaturalTextSecondary
            )
        }
    }
}

