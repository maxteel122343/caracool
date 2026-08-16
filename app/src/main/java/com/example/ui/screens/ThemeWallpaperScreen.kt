package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import com.example.util.SafeWordHelper
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.service.SupabaseAuthHelper
import com.example.service.WallpaperTarget
import com.example.theme.PacocaThemePreset
import com.example.theme.ThemePresets
import com.example.ui.components.WallpaperRenderer
import com.example.ui.components.TardigradeMascotIcon
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeWallpaperScreen(
    viewModel: MainViewModel,
    settings: AppSettings,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var applyFeedback by remember { mutableStateOf<String?>(null) }
    val todayCount by viewModel.todayUnlockCount.collectAsState()
    val currentUser by SupabaseAuthHelper.currentUser.collectAsState()

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.saveUserPhoto(bitmap)
            applyFeedback = "Nova foto Cara de Paçoca salva e pronta para wallpaper!"
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
            Toast.makeText(context, "Permissão de câmera necessária para tirar a foto de perfil/wallpaper", Toast.LENGTH_LONG).show()
        }
    }

    val handleTakePhoto = {
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

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    viewModel.saveUserPhoto(bitmap)
                    applyFeedback = "Foto da galeria importada com sucesso!"
                }
            } catch (_: Exception) {}
        }
    }

    val activePreset = remember(settings.selectedThemeId) {
        ThemePresets.getById(settings.selectedThemeId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Imagens & Papel de Parede",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button_theme")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (settings.isCaraDeKoolMode) {
                        IconButton(
                            onClick = { viewModel.cycleKoolPalette() },
                            modifier = Modifier.testTag("cycle_kool_palette_top_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Alternar Paleta de Cores",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Live Preview of Active Wallpaper/Theme
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        WallpaperRenderer(
                            preset = activePreset,
                            userPhotoPath = settings.lockscreenPhotoUri ?: settings.userWallpaperUri ?: settings.userPhotoUri,
                            showUserPhotoMascot = true
                        )

                        // Info pill overlay
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            if (settings.isCaraDeKoolMode) {
                                TardigradeMascotIcon(size = 18.dp)
                            } else {
                                Text(text = activePreset.mascotEmoji, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (settings.isCaraDeKoolMode) "Modo: Cara de Cu" else "Tema: ${activePreset.name}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 1.5. MODO CARA DE CU / CARA DE COOL
            item {
                val appModeTitle = SafeWordHelper.getAppDisplayName(settings.isCaraDeKoolMode, settings.isSafeWordMode)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cara_de_kool_mode_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (settings.isCaraDeKoolMode) Color(0xFFFFF3E0) else Color(0xFFF5F5F5)
                    ),
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = if (settings.isCaraDeKoolMode) Color(0xFFFF6B35) else Color(0xFFE0E0E0)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Modo $appModeTitle",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (settings.isCaraDeKoolMode) "ATIVADO • Rebranding Suave e Voz Personalizada" else "Desativado • Cara de Paçoca Padrão",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFF6B35),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Switch(
                                checked = settings.isCaraDeKoolMode,
                                onCheckedChange = { viewModel.toggleCaraDeKoolMode(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFFF6B35),
                                    uncheckedThumbColor = Color(0xFFBDBDBD),
                                    uncheckedTrackColor = Color(0xFFE0E0E0)
                                ),
                                modifier = Modifier.testTag("toggle_cara_de_kool_mode")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (settings.isCaraDeKoolMode) {
                                "O app agora está no modo '$appModeTitle'! Paleta de cores moderna ativa e voz personalizada nos desbloqueios."
                            } else {
                                "Ao ativar este botão: o nome do app muda para $appModeTitle, com personalização completa e voz nos desbloqueios."
                            },
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Safe Word Mode Card (Modo Palavra Segura: Cool ao invés de Cu)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (settings.isSafeWordMode) Color(0xFFE8F5E9) else Color(0xFFFFF8E1)
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (settings.isSafeWordMode) Color(0xFFA5D6A7) else Color(0xFFFFE082)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Modo Palavra Segura (Safe Word)",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (settings.isSafeWordMode) Color(0xFF1B5E20) else Color(0xFFE65100)
                                    )
                                    Text(
                                        text = if (settings.isSafeWordMode) "Ativado: Troca 'Cu' por 'Cool' em tudo (textos, áudios e notificações)" else "Desativado: Exibe 'Cu' originalmente",
                                        fontSize = 11.sp,
                                        color = if (settings.isSafeWordMode) Color(0xFF2E7D32) else Color(0xFF795548)
                                    )
                                }

                                Switch(
                                    checked = settings.isSafeWordMode,
                                    onCheckedChange = { viewModel.toggleSafeWordMode(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF4CAF50),
                                        uncheckedThumbColor = Color(0xFFBDBDBD),
                                        uncheckedTrackColor = Color(0xFFE0E0E0)
                                    ),
                                    modifier = Modifier.testTag("toggle_safe_word_mode")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Palette Selector Section
                        Text(
                            text = "Escolher Paleta de Cores do Aplicativo:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        // 1. PALETA UNIVERSAL (Nova Paleta Padrão com #FF6B35 e #7B6CF6)
                        val isUniversalSelected = settings.koolColorPalette == "universal"
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setKoolPalette("universal")
                                }
                                .testTag("palette_universal_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUniversalSelected) Color(0xFFEFEBFF) else Color.White
                            ),
                            border = BorderStroke(
                                width = if (isUniversalSelected) 2.dp else 1.dp,
                                color = if (isUniversalSelected) Color(0xFFFF6B35) else Color(0xFFE8E8E8)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Paleta Universal (Padrão)",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1F1F1F)
                                            )
                                            if (isUniversalSelected) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFFFF6B35), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("ATIVO", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        Text(
                                            text = "Fundo #F5F5F5 • Cards #FFF • Destaque Laranja #FF6B35 • Roxo #7B6CF6",
                                            fontSize = 11.sp,
                                            color = Color(0xFF6B6B6B)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Color Swatches
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(20.dp).background(Color(0xFFFF6B35), CircleShape).border(0.5.dp, Color.Gray.copy(alpha = 0.3f), CircleShape))
                                    Box(modifier = Modifier.size(20.dp).background(Color(0xFF7B6CF6), CircleShape).border(0.5.dp, Color.Gray.copy(alpha = 0.3f), CircleShape))
                                    Box(modifier = Modifier.size(20.dp).background(Color(0xFFEFEBFF), CircleShape).border(0.5.dp, Color.Gray.copy(alpha = 0.3f), CircleShape))
                                    Box(modifier = Modifier.size(20.dp).background(Color(0xFFFFFFFF), CircleShape).border(0.5.dp, Color.Gray.copy(alpha = 0.3f), CircleShape))
                                    Box(modifier = Modifier.size(20.dp).background(Color(0xFFF5F5F5), CircleShape).border(0.5.dp, Color.Gray.copy(alpha = 0.3f), CircleShape))
                                    Box(modifier = Modifier.size(20.dp).background(Color(0xFF1F1F1F), CircleShape).border(0.5.dp, Color.Gray.copy(alpha = 0.3f), CircleShape))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 2. PALETA PAÇOCA
                        val isPacocaSelected = settings.koolColorPalette == "pacoca"
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setKoolPalette("pacoca")
                                }
                                .testTag("palette_pacoca_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPacocaSelected) Color(0xFFFFF3E0) else Color.White
                            ),
                            border = BorderStroke(
                                width = if (isPacocaSelected) 2.dp else 1.dp,
                                color = if (isPacocaSelected) Color(0xFFD87A24) else Color(0xFFE8E8E8)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Paleta Paçoca",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF3E2723)
                                            )
                                            if (isPacocaSelected) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFFD87A24), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("ATIVO", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        Text(
                                            text = "Caramelo amendoim, dourado e tons quentes de paçoca",
                                            fontSize = 11.sp,
                                            color = Color(0xFF6D4C41)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(20.dp).background(Color(0xFFD87A24), CircleShape))
                                    Box(modifier = Modifier.size(20.dp).background(Color(0xFFF5A623), CircleShape))
                                    Box(modifier = Modifier.size(20.dp).background(Color(0xFFFFE0B2), CircleShape))
                                    Box(modifier = Modifier.size(20.dp).background(Color(0xFFFFF8E7), CircleShape))
                                    Box(modifier = Modifier.size(20.dp).background(Color(0xFF3E2723), CircleShape))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 3. PALETA CARA DE KOOL / COOL (Pele & Pêssego Natural)
                        val isNudeSelected = settings.koolColorPalette == "nude_peach" || settings.koolColorPalette == "kool_1"
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setKoolPalette("nude_peach")
                                }
                                .testTag("palette_nude_peach_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isNudeSelected) Color(0xFFFFF1EB) else Color.White
                            ),
                            border = BorderStroke(
                                width = if (isNudeSelected) 2.dp else 1.dp,
                                color = if (isNudeSelected) Color(0xFFD46A7A) else Color(0xFFE8E8E8)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Paleta $appModeTitle (Pele & Pêssego)",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF3A231A)
                                            )
                                            if (isNudeSelected) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFFD46A7A), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("ATIVO", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        Text(
                                            text = "Pele pêssego, lábios terracota e nuances suaves",
                                            fontSize = 11.sp,
                                            color = Color(0xFFA06B4F)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(20.dp).background(Color(0xFFE8B89A), CircleShape))
                                    Box(modifier = Modifier.size(20.dp).background(Color(0xFFF2C9B0), CircleShape))
                                    Box(modifier = Modifier.size(20.dp).background(Color(0xFFD4A07A), CircleShape))
                                    Box(modifier = Modifier.size(20.dp).background(Color(0xFFD46A7A), CircleShape))
                                    Box(modifier = Modifier.size(20.dp).background(Color(0xFFFFF8F5), CircleShape))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 4. PALETA CARA DE KOOL / COOL 2 (Rosa Chiclete & Berry)
                        val isPinkSelected = settings.koolColorPalette == "pink_berry" || settings.koolColorPalette == "kool_2"
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setKoolPalette("pink_berry")
                                }
                                .testTag("palette_pink_berry_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPinkSelected) Color(0xFFFCE4EC) else Color.White
                            ),
                            border = BorderStroke(
                                width = if (isPinkSelected) 2.dp else 1.dp,
                                color = if (isPinkSelected) Color(0xFFE91E63) else Color(0xFFE8E8E8)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Paleta $appModeTitle 2 (Rosa Chiclete)",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF880E4F)
                                            )
                                            if (isPinkSelected) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFFE91E63), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("ATIVO", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        Text(
                                            text = "Rosa chiclete vibrante, berry marcante e tons néon",
                                            fontSize = 11.sp,
                                            color = Color(0xFFC2185B)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(20.dp).background(Color(0xFFE91E63), CircleShape))
                                    Box(modifier = Modifier.size(20.dp).background(Color(0xFF9C27B0), CircleShape))
                                    Box(modifier = Modifier.size(20.dp).background(Color(0xFFFF4081), CircleShape))
                                    Box(modifier = Modifier.size(20.dp).background(Color(0xFFFCE4EC), CircleShape))
                                    Box(modifier = Modifier.size(20.dp).background(Color(0xFF4A148C), CircleShape))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Test Voice / Action
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    if (!settings.isCaraDeKoolMode) {
                                        viewModel.toggleCaraDeKoolMode(true)
                                    } else {
                                        viewModel.testAudioPreview()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("kool_test_voice_btn")
                            ) {
                                Text(
                                    text = if (settings.isCaraDeKoolMode) "Ouvir IA Cu 🔊" else "Ativar Cara de Cu 👾",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }

            // 2. Separate System Wallpaper Application (Lockscreen vs Home Screen)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "📲 Aplicar como Papel de Parede do Dispositivo",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Escolha onde aplicar sua Cara de Paçoca: tela de bloqueio, tela inicial de apps ou ambas.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                        )

                        // Option 1: Lock Screen Only
                        Button(
                            onClick = {
                                viewModel.applyTargetedWallpaper(WallpaperTarget.LOCK_SCREEN) { success, msg ->
                                    applyFeedback = msg
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("apply_lockscreen_wallpaper_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("🔒 Definir no Papel de Parede de Desbloqueio (Lock)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Option 2: Home Screen Only
                        FilledTonalButton(
                            onClick = {
                                viewModel.applyTargetedWallpaper(WallpaperTarget.HOME_SCREEN) { success, msg ->
                                    applyFeedback = msg
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("apply_homescreen_wallpaper_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("📱 Definir na Tela Inicial (Fundo de Apps)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Option 3: Both
                        OutlinedButton(
                            onClick = {
                                viewModel.applyTargetedWallpaper(WallpaperTarget.BOTH) { success, msg ->
                                    applyFeedback = msg
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("apply_both_wallpapers_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("🔄 Definir em Ambas as Telas", fontSize = 13.sp)
                        }
                    }
                }
            }

            // 3. Automações de Desbloqueio, Feed e Slider
            item {
                Text(
                    text = "⚡ Automações & Comunidade",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Card 1: Publicação Automática no Feed
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auto_post_banner_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF8E1)
                    ),
                    border = BorderStroke(1.5.dp, Color(0xFFFFCC80)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                        .background(Color(0xFFFFE0B2), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "⚡",
                                        fontSize = 22.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "Publicação Automática no Feed",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF3E2723)
                                    )
                                    Text(
                                        text = if (settings.isAutoPostOnUnlockEnabled) "Ativado • Auto-post a cada desbloqueio" else "Desativado",
                                        fontSize = 12.sp,
                                        color = if (settings.isAutoPostOnUnlockEnabled) Color(0xFF2E7D32) else Color(0xFF8D6E63),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Switch(
                                checked = settings.isAutoPostOnUnlockEnabled,
                                onCheckedChange = { viewModel.toggleAutoPostOnUnlock(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFE65100),
                                    uncheckedThumbColor = Color(0xFFBDBDBD),
                                    uncheckedTrackColor = Color(0xFFE0E0E0)
                                ),
                                modifier = Modifier.testTag("toggle_auto_post")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Toda vez que desbloquear seu aparelho, sua foto 'Cara de Paçoca' é publicada no feed com a legenda automática 'Olha minha cara de paçoca pela ${todayCount.coerceAtLeast(1)}ª vez! 🥜'.",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = Color(0xFF6D4C41)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.publishManualPost(settings.userProfilePhotoUri ?: settings.userPhotoUri)
                                    Toast.makeText(context, "Publicando sua Cara de Paçoca no feed! 🥜✨", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFFE65100)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFE65100)
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("quick_publish_pacoca_btn")
                            ) {
                                Text(
                                    text = "Publicar foto atual agora 🥜",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Card 2: Wallpaper Slider da Comunidade
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wallpaper_slider_banner_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF3E5F5)
                    ),
                    border = BorderStroke(1.5.dp, Color(0xFFCE93D8)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                        text = if (settings.isCommunitySliderEnabled) "Ativado • Fotos de outros no seu wallpaper" else "Desativado • Ative para rodízio",
                                        fontSize = 12.sp,
                                        color = if (settings.isCommunitySliderEnabled) Color(0xFF2E7D32) else Color(0xFF7B1FA2),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Switch(
                                checked = settings.isCommunitySliderEnabled,
                                onCheckedChange = { viewModel.toggleCommunitySlider(it, settings.communitySliderInterval) },
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

                        if (settings.isCommunitySliderEnabled) {
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
                                    selected = settings.communitySliderInterval == "unlock",
                                    onClick = { viewModel.toggleCommunitySlider(true, "unlock") },
                                    label = { Text("A cada Desbloqueio", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = settings.communitySliderInterval == "5_min",
                                    onClick = { viewModel.toggleCommunitySlider(true, "5_min") },
                                    label = { Text("5 minutos", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = settings.communitySliderInterval == "30_min",
                                    onClick = { viewModel.toggleCommunitySlider(true, "30_min") },
                                    label = { Text("30 minutos", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // Card 3: Auto set unlock photo as lock wallpaper
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = "Auto-definir foto como wallpaper de bloqueio",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Toda foto tirada ao desbloquear vira apenas seu wallpaper de desbloqueio automaticamente.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.isAutoSetUnlockPhotoAsLockWallpaper,
                                onCheckedChange = { viewModel.toggleAutoSetLockWallpaper(it) },
                                modifier = Modifier.testTag("auto_lock_wallpaper_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // Card 4: Auto-deletar posts do usuário com mais de 24h do banco de dados
            item {
                val isGuest = currentUser == null
                val isAutoDeleteOn = settings.isAutoDeleteOldPosts24hEnabled
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auto_delete_24h_banner_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAutoDeleteOn) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        if (isAutoDeleteOn) Color(0xFFFFB74D) else MaterialTheme.colorScheme.outlineVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                            if (isAutoDeleteOn) Color(0xFFFFE0B2) else MaterialTheme.colorScheme.surfaceVariant,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "⏳",
                                        fontSize = 22.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "Auto-deletar Meus Posts (24h)",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAutoDeleteOn) Color(0xFF3E2723) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isAutoDeleteOn) {
                                            if (isGuest) "Ativado • Padrão Convidado" else "Ativado manualmente"
                                        } else {
                                            if (isGuest) "Desativado manualmente" else "Desativado • Padrão Logado"
                                        },
                                        fontSize = 12.sp,
                                        color = if (isAutoDeleteOn) Color(0xFFE65100) else Color(0xFF757575),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Switch(
                                checked = isAutoDeleteOn,
                                onCheckedChange = { viewModel.toggleAutoDeleteOldPosts24h(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFE65100),
                                    uncheckedThumbColor = Color(0xFFBDBDBD),
                                    uncheckedTrackColor = Color(0xFFE0E0E0)
                                ),
                                modifier = Modifier.testTag("toggle_auto_delete_24h")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (isGuest) {
                                "👤 Modo Convidado: Este toggle fica ATIVADO por padrão para apagar automaticamente do banco de dados os seus posts criados há mais de 24 horas."
                            } else {
                                "☁️ Usuário Logado (${currentUser?.email ?: "Conectado"}): Este toggle fica DESATIVADO por padrão para manter seu histórico seguro. Você pode ativá-lo se preferir limpeza periódica."
                            },
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = if (isAutoDeleteOn) Color(0xFF5D4037) else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Banco de Dados Local 🗄️",
                                fontSize = 11.sp,
                                color = Color(0xFF8D6E63),
                                fontWeight = FontWeight.SemiBold
                            )

                            OutlinedButton(
                                onClick = {
                                    viewModel.runManual24hCleanup { count ->
                                        if (count > 0) {
                                            Toast.makeText(context, "$count post(s) antigos (>24h) excluído(s) do banco!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Nenhum post seu com mais de 24h para excluir.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("manual_cleanup_24h_btn")
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Limpar +24h Agora", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 4. Camera / Gallery Action Bar
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "📸 Foto Personalizada Cara de Paçoca",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tire uma selfie ou foto para ser seu wallpaper e avatar de desbloqueio.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { handleTakePhoto() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("take_photo_theme_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tirar Foto", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("pick_gallery_theme_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Galeria", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // 5. Theme Presets Grid
            item {
                Text(
                    text = "Temas Disponíveis no App",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            ThemePresets.allPresets.forEach { preset ->
                item {
                    val isSelected = settings.selectedThemeId == preset.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateSettings { it.copy(selectedThemeId = preset.id) }
                            }
                            .testTag("preset_${preset.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(preset.backgroundBrush),
                                contentAlignment = Alignment.Center
                            ) {
                                if (preset.id == "kool") {
                                    TardigradeMascotIcon(size = 24.dp)
                                } else {
                                    Text(text = preset.mascotEmoji, fontSize = 22.sp)
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ThemePresets.getDisplayName(preset, settings.isCaraDeKoolMode),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = preset.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selecionado",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (applyFeedback != null) {
                item {
                    Text(
                        text = applyFeedback!!,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

