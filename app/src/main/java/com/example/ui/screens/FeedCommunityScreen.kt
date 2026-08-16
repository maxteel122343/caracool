package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.example.util.SafeWordHelper
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.example.R
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AppSettings
import com.example.data.model.FeedComment
import com.example.data.model.FeedPost
import com.example.ui.components.KawaiiKoolLockscreenTemplate
import com.example.ui.components.KawaiiPacocaLockscreenTemplate
import com.example.ui.components.SyncLogViewerDialog
import com.example.ui.components.TutorialGuideDialog
import com.example.ui.components.UserProfileDialog
import com.example.ui.components.TardigradeMascotIcon
import com.example.ui.theme.NaturalBorderLight
import com.example.ui.theme.NaturalTextPrimary
import com.example.ui.theme.NaturalTextSecondary
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedCommunityScreen(
    viewModel: MainViewModel,
    settings: AppSettings,
    onBack: () -> Unit
) {
    val posts by viewModel.feedPostsState.collectAsState()
    val todayCount by viewModel.todayUnlockCount.collectAsState()
    val context = LocalContext.current

    var showProfileDialog by remember { mutableStateOf(false) }
    var showTutorialDialog by remember { mutableStateOf(false) }
    var showSyncLogsDialog by remember { mutableStateOf(false) }
    var showLockscreenPreview by remember { mutableStateOf(false) }
    var showNewPostDialog by remember { mutableStateOf(false) }
    var selectedPostForComments by remember { mutableStateOf<FeedPost?>(null) }
    var selectedPhotoPathForNewPost by remember { mutableStateOf<String?>(settings.userProfilePhotoUri ?: settings.userPhotoUri) }

    if (showSyncLogsDialog) {
        SyncLogViewerDialog(
            viewModel = viewModel,
            onDismiss = { showSyncLogsDialog = false }
        )
    }

    if (showTutorialDialog) {
        TutorialGuideDialog(
            settings = settings,
            viewModel = viewModel,
            onDismiss = { showTutorialDialog = false },
            onOpenLockscreenPreview = { showLockscreenPreview = true }
        )
    }

    if (showLockscreenPreview) {
        LockScreenView(
            viewModel = viewModel,
            settings = settings,
            onClose = { showLockscreenPreview = false }
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

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val savedPath = viewModel.saveUserPhoto(bitmap)
            selectedPhotoPathForNewPost = savedPath
            showNewPostDialog = true
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch()
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao abrir câmera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Permissão de câmera necessária para tirar fotos para a Comunidade", Toast.LENGTH_LONG).show()
        }
    }

    val handleCameraLaunch = {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            try {
                cameraLauncher.launch()
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao abrir câmera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoPathForNewPost = uri.toString()
            showNewPostDialog = true
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("feed_community_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Top Header
        item {
            FeedHeaderSection(
                postsCount = posts.size,
                settings = settings,
                onProfileClick = { showProfileDialog = true },
                onTutorialClick = { showTutorialDialog = true },
                onOpenSyncLogs = { showSyncLogsDialog = true },
                onRefreshClick = { viewModel.refreshFeedFromCloud() },
                onNewPostClick = {
                    selectedPhotoPathForNewPost = settings.userProfilePhotoUri ?: settings.userPhotoUri
                    showNewPostDialog = true
                },
                onCameraClick = { handleCameraLaunch() }
            )
        }

        // Section Title
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (settings.isCaraDeKoolMode) "FEED KOOL EM TEMPO REAL" else "FEED EM TEMPO REAL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = if (settings.isCaraDeKoolMode) Color(0xFFC2185B) else Color(0xFF8D6E63)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                if (settings.isCaraDeKoolMode) Color(0xFFFCE4EC) else Color(0xFFE65100).copy(alpha = 0.15f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${posts.size} posts",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (settings.isCaraDeKoolMode) Color(0xFFC2185B) else Color(0xFFE65100)
                        )
                    }
                }

                Text(
                    text = "Atualizado agora 🟢",
                    fontSize = 11.sp,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Posts List
        if (posts.isEmpty()) {
            item {
                EmptyFeedState(
                    settings = settings,
                    onPostNow = {
                        selectedPhotoPathForNewPost = settings.userProfilePhotoUri ?: settings.userPhotoUri
                        showNewPostDialog = true
                    }
                )
            }
        } else {
            items(
                items = posts,
                key = { it.id }
            ) { post ->
                val isFeaturedLockscreen = post.id == settings.activeLockscreenPostId
                FeedPostCard(
                    post = post,
                    viewModel = viewModel,
                    settings = settings,
                    isFeaturedLockscreen = isFeaturedLockscreen,
                    onOpenComments = { selectedPostForComments = post },
                    onOpenSyncLogs = { showSyncLogsDialog = true },
                    onSetAsLockWallpaper = {
                        viewModel.setCommunityPostAsLockWallpaper(post)
                    },
                    onShare = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            val shareMsg = if (settings.isCaraDeKoolMode || post.themeTag.contains("Kool", ignoreCase = true) || post.themeTag.contains("Cu", ignoreCase = true)) {
                                val appLabel = SafeWordHelper.getAppDisplayName(isCuMode = true, isSafeWordMode = settings.isSafeWordMode)
                                val cuWord = if (settings.isSafeWordMode) "cara de cool" else "cara de cu"
                                "Olha a $cuWord de ${post.authorName}! Desbloqueio #${post.unlockCount}: \"${post.caption}\" ✨ Baixe o app $appLabel!"
                            } else {
                                "Olha a cara de paçoca de ${post.authorName}! Desbloqueio #${post.unlockCount}: \"${post.caption}\" 🥜 Baixe o app Cara de Paçoca!"
                            }
                            putExtra(Intent.EXTRA_TEXT, shareMsg)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Compartilhar"))
                    },
                    onDelete = {
                        viewModel.deleteFeedPost(post.id)
                    }
                )
            }
        }
    }

    // New Post Dialog
    if (showNewPostDialog) {
        NewPostDialog(
            todayCount = todayCount,
            initialPhotoUri = selectedPhotoPathForNewPost,
            settings = settings,
            onDismiss = { showNewPostDialog = false },
            onPublish = { photoPath, caption ->
                viewModel.publishManualPost(photoPath, caption)
                showNewPostDialog = false
            },
            onPickCamera = { handleCameraLaunch() },
            onPickGallery = { galleryLauncher.launch("image/*") }
        )
    }

    // Comments Bottom Sheet
    selectedPostForComments?.let { post ->
        CommentsBottomSheet(
            post = post,
            viewModel = viewModel,
            settings = settings,
            onDismiss = { selectedPostForComments = null }
        )
    }
}

@Composable
fun FeedHeaderSection(
    postsCount: Int,
    settings: AppSettings,
    onProfileClick: () -> Unit,
    onTutorialClick: () -> Unit,
    onOpenSyncLogs: () -> Unit,
    onRefreshClick: () -> Unit,
    onNewPostClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("feed_header_section"),
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Title & Profile / Refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val feedTitle = if (settings.isCaraDeKoolMode) {
                        if (settings.isSafeWordMode) "Feed Cara de Cool" else "Feed Cara de Cu"
                    } else "Feed da Comunidade"
                    val feedSubtitle = if (settings.isCaraDeKoolMode) {
                        if (settings.isSafeWordMode) "CARAS DE COOL EM TEMPO REAL" else "CARAS DE CU EM TEMPO REAL"
                    } else "CARAS DE PAÇOCA EM TEMPO REAL"

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (settings.isCaraDeKoolMode) {
                            TardigradeMascotIcon(size = 24.dp, showLock = true)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = feedTitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = feedSubtitle,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Refresh from cloud button
                    IconButton(
                        onClick = onRefreshClick,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            )
                            .size(36.dp)
                            .testTag("feed_refresh_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sincronizar Feed com a Nuvem",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Profile Avatar Button in Header
                    val profilePhotoPath = settings.userProfilePhotoUri ?: settings.userPhotoUri

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                            .clickable { onProfileClick() }
                            .testTag("feed_header_profile_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!profilePhotoPath.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(profilePhotoPath)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Foto de perfil",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            if (settings.isCaraDeKoolMode) {
                                TardigradeMascotIcon(size = 22.dp)
                            } else {
                                Text(
                                    text = settings.userAvatarEmoji.ifBlank { "🥜" },
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // Row 2: Action Bar (Postar, Tirar Foto, Tutorial, Logs)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Postar Button (Primary)
                Button(
                    onClick = onNewPostClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(38.dp)
                        .testTag("feed_new_post_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Postar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                // Camera Button
                FilledTonalButton(
                    onClick = onCameraClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("feed_camera_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Câmera", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }

                // Tutorial Button
                FilledTonalButton(
                    onClick = onTutorialClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("feed_tutorial_btn")
                ) {
                    Text("Tutorial", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }

                // Sync Logs Hub Button
                FilledTonalButton(
                    onClick = onOpenSyncLogs,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(0.9f)
                        .height(38.dp)
                        .testTag("feed_sync_logs_btn")
                ) {
                    Text("Logs", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun WallpaperSliderBannerCard(
    isSliderEnabled: Boolean,
    interval: String,
    onToggle: (Boolean) -> Unit,
    onIntervalChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("wallpaper_slider_banner_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF3E5F5)
        ),
        border = BorderStroke(1.5.dp, Color(0xFFCE93D8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFFE1BEE7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🔄",
                            fontSize = 22.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Wallpaper Slider da Comunidade",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4A148C)
                        )
                        Text(
                            text = if (isSliderEnabled) "Ativado • Fotos de outros no seu wallpaper" else "Desativado • Ative para rodízio",
                            fontSize = 12.sp,
                            color = if (isSliderEnabled) Color(0xFF2E7D32) else Color(0xFF7B1FA2),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Switch(
                    checked = isSliderEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF8E24AA),
                        uncheckedThumbColor = Color(0xFFBDBDBD),
                        uncheckedTrackColor = Color(0xFFE0E0E0)
                    ),
                    modifier = Modifier.testTag("toggle_community_slider")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Fotos de outros usuários aparecem no seu wallpaper de desbloqueio com áudio exclusivo contendo o nome do autor e a contagem de desbloqueios!",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = Color(0xFF4A148C)
            )

            if (isSliderEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Tempo de Troca:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6A1B9A)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = interval == "unlock",
                        onClick = { onIntervalChange("unlock") },
                        label = { Text("A cada Desbloqueio", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = interval == "5_min",
                        onClick = { onIntervalChange("5_min") },
                        label = { Text("5 minutos", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = interval == "30_min",
                        onClick = { onIntervalChange("30_min") },
                        label = { Text("30 minutos", fontSize = 11.sp) }
                    )
                }
            }
        }
    }
}

@Composable
fun AutoPostBannerCard(
    isAutoPostEnabled: Boolean,
    todayCount: Int,
    settings: AppSettings,
    onToggle: (Boolean) -> Unit,
    onQuickPostNow: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("auto_post_banner_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            1.5.dp,
            MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (settings.isCaraDeKoolMode) "👾" else "⚡",
                            fontSize = 22.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (settings.isCaraDeKoolMode) "Publicação Automática no Feed Cu" else "Publicação Automática no Feed",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isAutoPostEnabled) "Ativado • Auto-post a cada desbloqueio" else "Desativado",
                            fontSize = 12.sp,
                            color = if (isAutoPostEnabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Switch(
                    checked = isAutoPostEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = Color(0xFFBDBDBD),
                        uncheckedTrackColor = Color(0xFFE0E0E0)
                    ),
                    modifier = Modifier.testTag("toggle_auto_post")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (settings.isCaraDeKoolMode) {
                    val appModeLabel = SafeWordHelper.getAppDisplayName(settings.isCaraDeKoolMode, settings.isSafeWordMode)
                    val modeCaption = if (settings.isSafeWordMode) "Olha minha cara de cool pela" else "Olha minha cara de cu pela"
                    "Toda vez que desbloquear seu aparelho, sua foto '$appModeLabel' é publicada no feed com a legenda automática '$modeCaption ${todayCount.coerceAtLeast(1)}ª vez!'."
                } else {
                    "Toda vez que desbloquear seu aparelho, sua foto 'Cara de Paçoca' é publicada no feed com a legenda automática 'Olha minha cara de paçoca pela ${todayCount.coerceAtLeast(1)}ª vez!'."
                },
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onQuickPostNow,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("quick_publish_pacoca_btn")
                ) {
                    Text(
                        text = if (settings.isCaraDeKoolMode) "Publicar foto atual agora 👾" else "Publicar foto atual agora 🥜",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun FeedPostCard(
    post: FeedPost,
    viewModel: MainViewModel,
    settings: AppSettings,
    isFeaturedLockscreen: Boolean = false,
    onOpenComments: () -> Unit,
    onOpenSyncLogs: () -> Unit = {},
    onSetAsLockWallpaper: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val commentsFlow: Flow<List<FeedComment>> = remember(post.id) { viewModel.getCommentsForPost(post.id) }
    val commentsList: List<FeedComment> by commentsFlow.collectAsState(initial = emptyList())
    var inlineCommentText by remember { mutableStateOf("") }
    var showInlineComments by remember { mutableStateOf(false) }
    var showReactionPicker by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("feed_post_card_${post.id}"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            if (isFeaturedLockscreen) 2.dp else 1.dp,
            if (isFeaturedLockscreen) Color(0xFFE65100) else NaturalBorderLight
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFeaturedLockscreen) 4.dp else 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (isFeaturedLockscreen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = if (settings.isCaraDeKoolMode) "👾" else "🌟", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Papel de Parede de Desbloqueio Ativo",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            val isKoolPost = settings.isCaraDeKoolMode || post.themeTag.contains("Kool", ignoreCase = true) || post.themeTag.contains("Cu", ignoreCase = true) || post.presetImageKey == "kool_avatar"

            val displayAuthorName = if (post.isUserPost && settings.isCaraDeKoolMode) {
                if (post.authorName == "Você (Cara de Paçoca)" || post.authorName == "Cara de Paçoca" || post.authorName.isBlank()) {
                    SafeWordHelper.getDefaultUserName(isCuMode = true, isSafeWordMode = settings.isSafeWordMode)
                } else post.authorName
            } else post.authorName

            val displayEmoji = if (settings.isCaraDeKoolMode && (post.authorAvatarEmoji == "🥜" || post.authorAvatarEmoji == "💖" || post.authorAvatarEmoji.isBlank())) {
                "👾"
            } else post.authorAvatarEmoji

            // Post Header (Author info)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Avatar
                    AuthorAvatarView(
                        authorName = displayAuthorName,
                        authorEmoji = displayEmoji,
                        avatarUri = post.authorAvatarUri
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = displayAuthorName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (settings.isCaraDeKoolMode) Color(0xFF880E4F) else NaturalTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (post.isUserPost) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "VOCÊ",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatTimeAgo(post.timestamp),
                                fontSize = 12.sp,
                                color = NaturalTextSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "•",
                                fontSize = 10.sp,
                                color = NaturalTextSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (settings.isCaraDeKoolMode && post.isUserPost && post.themeTag == "Clássico") "Modo Cara de Kool" else post.themeTag,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Unlock count chip badge
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${post.unlockCount}ª vez",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                if (post.isUserPost) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Excluir post",
                            tint = Color(0xFF9E9E9E),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Photo Focus Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .height(280.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (settings.isCaraDeKoolMode) Color(0xFFFFF0F5) else Color(0xFFF5F5F5))
            ) {
                PostPhotoDisplay(post = post)

                // Watermark pill on top-left of photo
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    val badgeTitle = if (isKoolPost) {
                        if (settings.isSafeWordMode) "Cara de Cool #${post.unlockCount}" else "Cara de Cu #${post.unlockCount}"
                    } else "🥜 Cara de Paçoca #${post.unlockCount}"
                    Text(
                        text = badgeTitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Caption Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val rawCaption = if (settings.isCaraDeKoolMode && post.isUserPost && post.caption.contains("cara de paçoca", ignoreCase = true)) {
                    post.caption.replace("cara de paçoca", if (settings.isSafeWordMode) "cara de cool" else "cara de cu", ignoreCase = true).replace("🥜", "✨")
                } else post.caption
                val captionText = SafeWordHelper.formatSafeWord(rawCaption, settings.isSafeWordMode)

                Text(
                    text = captionText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (settings.isCaraDeKoolMode) Color(0xFF4A148C) else Color(0xFF3E2723),
                    lineHeight = 22.sp
                )
            }

            // --- FACEBOOK-STYLE FACEPILE BALLOONS ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                // Balloon 1: Who Liked (Shows photo of person who liked)
                if (post.likesCount > 0) {
                    FacebookLikeBubble(
                        likesCount = post.likesCount,
                        summaryText = post.recentLikersSummary,
                        isLikedByMe = post.isLikedByMe,
                        myEmoji = settings.userAvatarEmoji,
                        myPhotoUri = if (post.isLikedByMe) {
                            settings.userProfilePhotoUri ?: settings.userPhotoUri
                        } else {
                            post.authorAvatarUri ?: post.photoUri
                        }
                    )
                }

                // Balloon 2: Who is using this as Lockscreen Wallpaper
                if (post.wallpaperSetCount > 0 || isFeaturedLockscreen) {
                    Spacer(modifier = Modifier.height(4.dp))
                    FacebookWallpaperBubble(
                        count = post.wallpaperSetCount.coerceAtLeast(if (isFeaturedLockscreen) 1 else 0),
                        summaryText = if (isFeaturedLockscreen) "Você e outros ${post.wallpaperSetCount} usam no desbloqueio" else post.recentWallpaperUsersSummary,
                        myPhotoUri = settings.userProfilePhotoUri
                    )
                }

                // Sync Delivery Status Tag / Diagnostic Indicator
                Spacer(modifier = Modifier.height(6.dp))
                if (post.isUserPost) {
                    val isSynced = post.syncStatus == "SYNCED"
                    val isFailed = post.syncStatus == "FAILED"
                    val syncBg = when {
                        isSynced -> Color(0xFFE8F5E9)
                        isFailed -> Color(0xFFFFEBEE)
                        else -> Color(0xFFFFF8E1)
                    }
                    val syncColor = when {
                        isSynced -> Color(0xFF2E7D32)
                        isFailed -> Color(0xFFD32F2F)
                        else -> Color(0xFFF57F17)
                    }
                    val syncLabel = when {
                        isSynced -> "🟢 Foto Sincronizada na Nuvem • Visível aos outros"
                        isFailed -> "🔴 Falha no envio • Toque p/ ver log & reenviar"
                        else -> "⏳ Enviando foto para o Firestore..."
                    }

                    Surface(
                        onClick = onOpenSyncLogs,
                        shape = RoundedCornerShape(8.dp),
                        color = syncBg,
                        border = BorderStroke(1.dp, syncColor.copy(alpha = 0.4f)),
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = syncLabel,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = syncColor
                            )
                            if (isFailed) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Reenviar ↺",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFB71C1C)
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        onClick = onOpenSyncLogs,
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE3F2FD),
                        border = BorderStroke(1.dp, Color(0xFF90CAF9)),
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "☁️ Recebido da Comunidade via Nuvem Firestore",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1565C0)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = Color(0xFFF0EAE6),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Interaction Bar (Like with popup, Set Wallpaper, Comments, Share)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like Button with Reaction Picker Popup
                Box {
                    // Floating Emoji Picker
                    if (showReactionPicker) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White,
                            shadowElevation = 8.dp,
                            border = BorderStroke(1.dp, Color(0xFFFFCC80)),
                            modifier = Modifier
                                .offset(y = (-48).dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("❤️", "😂", "🔥", "😮").forEach { emoji ->
                                    Surface(
                                        onClick = {
                                            viewModel.reactToPost(post.id, emoji)
                                            showReactionPicker = false
                                        },
                                        shape = CircleShape,
                                        color = if (post.myReactionEmoji == emoji) {
                                            if (settings.isCaraDeKoolMode) Color(0xFFFCE4EC) else Color(0xFFFFE0B2)
                                        } else Color.Transparent,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(text = emoji, fontSize = 20.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Like Button
                    FilledTonalButton(
                        onClick = {
                            if (post.isLikedByMe) {
                                viewModel.reactToPost(post.id, post.myReactionEmoji ?: "❤️")
                            } else {
                                viewModel.reactToPost(post.id, "❤️")
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (post.isLikedByMe) {
                                if (settings.isCaraDeKoolMode) Color(0xFFFCE4EC) else Color(0xFFFFE0B2)
                            } else {
                                if (settings.isCaraDeKoolMode) Color(0xFFFFF0F5) else Color(0xFFF5F5F5)
                            },
                            contentColor = if (post.isLikedByMe) {
                                if (settings.isCaraDeKoolMode) Color(0xFFE91E63) else Color(0xFFE65100)
                            } else {
                                Color(0xFF616161)
                            }
                        ),
                        modifier = Modifier.testTag("like_btn_${post.id}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (post.isLikedByMe) (post.myReactionEmoji ?: "❤️") else "❤️",
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (post.isLikedByMe) {
                                    when (post.myReactionEmoji) {
                                        "😂" -> "Rindo"
                                        "🔥" -> "Top"
                                        "😮" -> "Uau"
                                        else -> "Curtiu"
                                    }
                                } else {
                                    "Curtir"
                                },
                                fontSize = 12.sp,
                                fontWeight = if (post.isLikedByMe) FontWeight.Bold else FontWeight.Medium
                            )
                            if (post.likesCount > 0) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "(${post.likesCount})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            // Reaction trigger icon
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { showReactionPicker = !showReactionPicker }
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✨",
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                // Set as Lockscreen Wallpaper Button
                FilledTonalButton(
                    onClick = onSetAsLockWallpaper,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isFeaturedLockscreen) {
                            if (settings.isCaraDeKoolMode) Color(0xFFFCE4EC) else Color(0xFFFFE0B2)
                        } else {
                            if (settings.isCaraDeKoolMode) Color(0xFFFFF0F5) else Color(0xFFEFEBE9)
                        },
                        contentColor = if (isFeaturedLockscreen) {
                            if (settings.isCaraDeKoolMode) Color(0xFFC2185B) else Color(0xFFE65100)
                        } else {
                            if (settings.isCaraDeKoolMode) Color(0xFF880E4F) else Color(0xFF4E342E)
                        }
                    ),
                    modifier = Modifier.testTag("set_lockscreen_btn_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Definir wallpaper de desbloqueio",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isFeaturedLockscreen) "No Desbloqueio ✓" else "Desbloqueio",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Comment Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showInlineComments = !showInlineComments }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("comment_button_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Comentários",
                            tint = NaturalTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val totalComments = post.commentsCount + commentsList.size
                        Text(
                            text = "$totalComments",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = NaturalTextPrimary
                        )
                    }

                    // Share Button
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartilhar",
                            tint = NaturalTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Inline Comments Section
            AnimatedVisibility(visible = showInlineComments || commentsList.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (settings.isCaraDeKoolMode) Color(0xFFFFF8FA) else Color(0xFFFAF7F5))
                        .padding(14.dp)
                ) {
                    if (commentsList.isNotEmpty()) {
                        Text(
                            text = "Comentários (${commentsList.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (settings.isCaraDeKoolMode) Color(0xFFC2185B) else Color(0xFF8D6E63),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        val previewComments = commentsList.take(3)
                        for (cmt in previewComments) {
                            CommentRow(comment = cmt)
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        if (commentsList.size > 3) {
                            TextButton(
                                onClick = onOpenComments,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = "Ver todos os ${commentsList.size} comentários...",
                                    fontSize = 12.sp,
                                    color = if (settings.isCaraDeKoolMode) Color(0xFFE91E63) else Color(0xFFE65100),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Inline Add Comment
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Avatar next to comment box
                        val userPhoto = settings.userProfilePhotoUri ?: settings.userPhotoUri
                        val userBmp = remember(userPhoto) {
                            if (!userPhoto.isNullOrBlank()) {
                                val f = File(userPhoto)
                                if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
                            } else null
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (userBmp != null) {
                                Image(
                                    bitmap = userBmp.asImageBitmap(),
                                    contentDescription = "Meu Avatar",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.app_mascot_user_icon),
                                    contentDescription = "Meu Avatar",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(2.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            value = inlineCommentText,
                            onValueChange = { inlineCommentText = it },
                            placeholder = { Text("Escreva um comentário...", fontSize = 12.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("inline_comment_input_${post.id}"),
                            shape = RoundedCornerShape(20.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = if (settings.isCaraDeKoolMode) Color(0xFFE91E63) else Color(0xFFE65100),
                                unfocusedBorderColor = NaturalBorderLight
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (inlineCommentText.isNotBlank()) {
                                    viewModel.addCommentToPost(post.id, inlineCommentText)
                                    inlineCommentText = ""
                                }
                            })
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (inlineCommentText.isNotBlank()) {
                                    viewModel.addCommentToPost(post.id, inlineCommentText)
                                    inlineCommentText = ""
                                }
                            },
                            modifier = Modifier
                                .background(
                                    if (settings.isCaraDeKoolMode) Color(0xFFE91E63) else Color(0xFFE65100),
                                    CircleShape
                                )
                                .size(40.dp)
                                .testTag("send_comment_btn_${post.id}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- FACEBOOK STYLE SOCIAL FACEPILES ---

@Composable
fun FacebookLikeBubble(
    likesCount: Int,
    summaryText: String,
    isLikedByMe: Boolean,
    myEmoji: String,
    myPhotoUri: String?
) {
    if (likesCount <= 0) return

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFF3E0),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo of the person who liked with small heart badge (Replaces the standalone heart icon)
            Box(
                modifier = Modifier.size(26.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!myPhotoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = myPhotoUri,
                        contentDescription = "Foto da pessoa que curtiu",
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, Color(0xFFE65100), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFCC80)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(myEmoji.ifBlank { "🌸" }, fontSize = 12.sp)
                    }
                }

                // Tiny heart badge overlay in bottom-right corner
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("❤️", fontSize = 7.sp)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            val displayText = when {
                isLikedByMe && likesCount == 1 -> "Você curtiu esta publicação!"
                isLikedByMe && likesCount > 1 -> "Você e mais ${likesCount - 1} pessoa(s) curtiram"
                likesCount == 1 -> "1 curtida"
                else -> "$likesCount curtidas"
            }

            Text(
                text = displayText,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF6D4C41),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun FacebookWallpaperBubble(
    count: Int,
    summaryText: String,
    myPhotoUri: String?
) {
    if (count <= 0) return

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFEDE7F6),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF7B1FA2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            val displayText = if (summaryText.isNotBlank() && !summaryText.contains("Gabriel")) {
                summaryText
            } else if (count == 1) {
                "1 pessoa usando como wallpaper de bloqueio"
            } else {
                "$count pessoas usando como wallpaper de bloqueio"
            }

            Text(
                text = displayText,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4A148C),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun PostPhotoDisplay(
    post: FeedPost
) {
    val context = LocalContext.current
    val photoUri = post.photoUri

    val isLocalFileValid = remember(photoUri) {
        if (photoUri.isNullOrBlank()) false
        else if (photoUri.startsWith("http://") || photoUri.startsWith("https://") || photoUri.startsWith("content://") || photoUri.startsWith("data:image")) true
        else {
            val file = File(photoUri.removePrefix("file://"))
            file.exists() && file.length() > 0
        }
    }

    if (isLocalFileValid && !photoUri.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(photoUri)
                .crossfade(true)
                .build(),
            contentDescription = "Foto do post",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    } else if (post.themeTag.contains("Kool", ignoreCase = true) || post.themeTag.contains("Cu", ignoreCase = true) || post.presetImageKey == "kool_avatar") {
        KawaiiKoolLockscreenTemplate(
            modifier = Modifier.fillMaxSize(),
            photoUri = null,
            unlockCount = post.unlockCount.coerceAtLeast(1),
            palette = "universal"
        )
    } else {
        // High-Fidelity Kawaii Paçoca Lockscreen Standard Format with Cheerful Mascot Avatar
        KawaiiPacocaLockscreenTemplate(
            modifier = Modifier.fillMaxSize(),
            photoUri = null,
            unlockCount = post.unlockCount.coerceAtLeast(1)
        )
    }
}

@Composable
fun AuthorAvatarView(
    authorName: String,
    authorEmoji: String,
    avatarUri: String?
) {
    val isKool = authorEmoji == "💖" || authorName.contains("Kool", ignoreCase = true)
    if (!avatarUri.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(avatarUri)
                .crossfade(true)
                .build(),
            contentDescription = authorName,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .border(2.dp, if (isKool) Color(0xFFF48FB1) else Color(0xFFFFB74D), CircleShape),
            contentScale = ContentScale.Crop
        )
        return
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isKool || authorEmoji == "💖" || authorEmoji == "👾") {
            TardigradeMascotIcon(size = 28.dp)
        } else {
            Text(
                text = authorEmoji.ifBlank { "🥜" },
                fontSize = 22.sp
            )
        }
    }
}

@Composable
fun CommentRow(comment: FeedComment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Avatar in comment
        if (!comment.authorAvatarUri.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(comment.authorAvatarUri)
                    .crossfade(true)
                    .build(),
                contentDescription = comment.authorName,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .padding(end = 4.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.app_mascot_user_icon),
                contentDescription = comment.authorName,
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 4.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.authorName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NaturalTextPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formatTimeAgo(comment.timestamp),
                    fontSize = 10.sp,
                    color = NaturalTextSecondary
                )
            }
            Text(
                text = comment.text,
                fontSize = 12.sp,
                color = Color(0xFF4E342E),
                lineHeight = 16.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    post: FeedPost,
    viewModel: MainViewModel,
    settings: AppSettings,
    onDismiss: () -> Unit
) {
    val commentsFlow: Flow<List<FeedComment>> = remember(post.id) { viewModel.getCommentsForPost(post.id) }
    val comments: List<FeedComment> by commentsFlow.collectAsState(initial = emptyList())
    var newCommentText by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Comentários",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = post.caption,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(modifier = Modifier.height(12.dp))

            if (comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum comentário ainda. Seja o primeiro a comentar!",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .height(260.dp)
                ) {
                    items(items = comments, key = { it.id }) { comment ->
                        CommentRow(comment = comment)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newCommentText,
                    onValueChange = { newCommentText = it },
                    placeholder = { Text("Deixe seu comentário divertido...", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (settings.isCaraDeKoolMode) Color(0xFFE91E63) else Color(0xFFE65100),
                        unfocusedBorderColor = NaturalBorderLight
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (newCommentText.isNotBlank()) {
                            viewModel.addCommentToPost(post.id, newCommentText)
                            newCommentText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (settings.isCaraDeKoolMode) Color(0xFFE91E63) else Color(0xFFE65100)
                    ),
                    shape = CircleShape,
                    contentPadding = PaddingValues(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun NewPostDialog(
    todayCount: Int,
    initialPhotoUri: String?,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onPublish: (String?, String) -> Unit,
    onPickCamera: () -> Unit,
    onPickGallery: () -> Unit
) {
    var caption by remember {
        mutableStateOf(
            if (settings.isCaraDeKoolMode) {
                if (settings.isSafeWordMode) "Olha minha cara de cool pela ${todayCount}ª vez!" else "Olha minha cara de cu pela ${todayCount}ª vez!"
            } else {
                "Olha minha cara de paçoca pela ${todayCount}ª vez!"
            }
        )
    }
    var selectedPhoto by remember { mutableStateOf(initialPhotoUri) }

    val dialogTitle = if (settings.isCaraDeKoolMode) {
        if (settings.isSafeWordMode) "Publicar no Feed Cool" else "Publicar no Feed Cu"
    } else "Publicar no Feed"

    val sharePrompt = if (settings.isCaraDeKoolMode) {
        if (settings.isSafeWordMode) "Compartilhe sua cara de cool com a comunidade:" else "Compartilhe sua cara de cu com a comunidade:"
    } else "Compartilhe sua cara de paçoca com a comunidade:"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dialogTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (settings.isCaraDeKoolMode) MaterialTheme.colorScheme.primary else NaturalTextPrimary
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = sharePrompt,
                    fontSize = 13.sp,
                    color = NaturalTextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Photo preview or selector
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (settings.isCaraDeKoolMode) Color(0xFFFCE4EC) else Color(0xFFFFF3E0))
                        .border(
                            1.dp,
                            if (settings.isCaraDeKoolMode) Color(0xFFF48FB1) else Color(0xFFFFCC80),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!selectedPhoto.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(selectedPhoto)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Prévia",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (settings.isCaraDeKoolMode) "👾✨" else "😋🥜", fontSize = 42.sp)
                            Text(
                                if (settings.isCaraDeKoolMode) "Mascote Cara de Cu" else "Mascote Cara de Paçoca",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (settings.isCaraDeKoolMode) Color(0xFFC2185B) else Color(0xFFE65100)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = onPickCamera) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (settings.isCaraDeKoolMode) Color(0xFFE91E63) else Color(0xFFE65100))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tirar Foto", fontSize = 12.sp, color = if (settings.isCaraDeKoolMode) Color(0xFFE91E63) else Color(0xFFE65100))
                    }

                    TextButton(onClick = onPickGallery) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (settings.isCaraDeKoolMode) Color(0xFFE91E63) else Color(0xFFE65100))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Galeria", fontSize = 12.sp, color = if (settings.isCaraDeKoolMode) Color(0xFFE91E63) else Color(0xFFE65100))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Legenda Automática:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NaturalTextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (settings.isCaraDeKoolMode) Color(0xFFE91E63) else Color(0xFFE65100)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onPublish(selectedPhoto, caption) },
                colors = ButtonDefaults.buttonColors(containerColor = if (settings.isCaraDeKoolMode) Color(0xFFE91E63) else Color(0xFFE65100))
            ) {
                Text("Publicar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = NaturalTextSecondary)
            }
        }
    )
}

@Composable
fun EmptyFeedState(
    settings: AppSettings,
    onPostNow: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (settings.isCaraDeKoolMode) {
            TardigradeMascotIcon(size = 76.dp, showLock = true)
        } else {
            Text("🥜", fontSize = 48.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        val emptyTitle = if (settings.isCaraDeKoolMode) {
            if (settings.isSafeWordMode) "Nenhuma cara de cool postada ainda!" else "Nenhuma cara de cu postada ainda!"
        } else "Nenhuma cara de paçoca postada ainda!"
        val emptySubtitle = if (settings.isCaraDeKoolMode) {
            val modeWord = if (settings.isSafeWordMode) "Cara de Cool" else "Cara de Cu"
            "Desbloqueie o celular ou poste a primeira foto para começar o feed de $modeWord!"
        } else "Desbloqueie o celular ou poste a primeira foto para começar o feed!"

        Text(
            text = emptyTitle,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = emptySubtitle,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Button(
            onClick = onPostNow,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Publicar Foto")
        }
    }
}

fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / (1000 * 60)
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "Agora mesmo"
        minutes < 60 -> "há ${minutes}m"
        hours < 24 -> "há ${hours}h"
        days < 7 -> "há ${days}d"
        else -> {
            val sdf = SimpleDateFormat("dd/MM 'às' HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
