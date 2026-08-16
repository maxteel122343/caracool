package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.AppSettings
import com.example.ui.theme.SoftPeachBackground
import com.example.ui.theme.SoftPeachBorder
import com.example.ui.theme.SoftPeachCard
import com.example.ui.theme.SoftPeachCardVariant
import com.example.ui.theme.SoftRoseAccent
import com.example.ui.theme.SoftRosePrimary
import com.example.ui.theme.SoftRosePrimaryContainer
import com.example.ui.theme.SoftTextMuted
import com.example.ui.theme.SoftTextPrimary
import com.example.ui.theme.SoftTextSecondary
import com.example.ui.viewmodel.MainViewModel
import com.example.util.FaceWarpFilterHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceWarpFilterScreen(
    viewModel: MainViewModel,
    settings: AppSettings,
    onBack: () -> Unit,
    onNavigateToFeed: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var filteredBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var distortionIntensity by remember { mutableFloatStateOf(0.75f) } // 0.0 to 1.0
    var isProcessing by remember { mutableStateOf(false) }
    var faceDetected by remember { mutableStateOf(false) }
    var focalPoint by remember { mutableStateOf<PointF?>(null) }
    var customRadius by remember { mutableStateOf<Float?>(null) }
    var imageDisplaySize by remember { mutableStateOf(IntSize.Zero) }

    var showPostDialog by remember { mutableStateOf(false) }
    var postCaption by remember { mutableStateOf("") }
    var showTargetIndicator by remember { mutableStateOf(false) }

    // Function to re-apply warp distortion filter
    val updateFilteredImage: () -> Unit = {
        val src = sourceBitmap
        if (src != null) {
            coroutineScope.launch {
                isProcessing = true
                val warped = FaceWarpFilterHelper.applyCaraDeCuWarp(
                    src = src,
                    intensity = distortionIntensity,
                    focalPoint = focalPoint,
                    customRadius = customRadius
                )
                filteredBitmap = warped
                isProcessing = false
            }
        }
    }

    // Function to load a new image and detect face
    val loadNewBitmap: (Bitmap) -> Unit = { newBmp ->
        sourceBitmap = newBmp
        coroutineScope.launch {
            isProcessing = true
            val detection = FaceWarpFilterHelper.detectFaceFocalPoint(newBmp)
            focalPoint = PointF(detection.centerX, detection.centerY)
            customRadius = detection.radius
            faceDetected = detection.faceDetected

            val warped = FaceWarpFilterHelper.applyCaraDeCuWarp(
                src = newBmp,
                intensity = distortionIntensity,
                focalPoint = focalPoint,
                customRadius = customRadius
            )
            filteredBitmap = warped
            isProcessing = false
        }
    }

    // Initial load: Load user's profile photo, existing photo or fallback avatar
    LaunchedEffect(Unit) {
        val currentPhotoPath = settings.userProfilePhotoUri ?: settings.userPhotoUri
        if (!currentPhotoPath.isNullOrBlank() && File(currentPhotoPath).exists()) {
            try {
                val loaded = BitmapFactory.decodeFile(currentPhotoPath)
                if (loaded != null) {
                    loadNewBitmap(loaded)
                    return@LaunchedEffect
                }
            } catch (_: Exception) {}
        }
        val defaultAvatar = FaceWarpFilterHelper.createSampleFaceBitmap(isKoolMode = settings.isCaraDeKoolMode)
        loadNewBitmap(defaultAvatar)
    }

    // Activity launchers for Camera and Gallery
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        loadNewBitmap(bmp)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao carregar imagem: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp: Bitmap? ->
        bmp?.let { loadNewBitmap(it) }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch()
        } else {
            Toast.makeText(context, "Permissão de câmera necessária", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleCameraClick() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Re-apply filter when intensity changes
    LaunchedEffect(distortionIntensity, focalPoint) {
        if (sourceBitmap != null) {
            updateFilteredImage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Filtro Cara de Cu",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = SoftTextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "🍑", fontSize = 18.sp)
                        }
                        Text(
                            text = "Distorção facial e biquinho em tempo real",
                            fontSize = 11.5.sp,
                            color = SoftTextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = SoftTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val defaultAvatar = FaceWarpFilterHelper.createSampleFaceBitmap(isKoolMode = settings.isCaraDeKoolMode)
                            loadNewBitmap(defaultAvatar)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "Rosto Padrão",
                            tint = SoftRosePrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SoftPeachBackground,
                    titleContentColor = SoftTextPrimary
                )
            )
        },
        containerColor = SoftPeachBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Source Selection Buttons (Camera / Gallery)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SoftPeachCard),
                border = BorderStroke(1.dp, SoftPeachBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { handleCameraClick() },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("filter_camera_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftRosePrimary,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tirar Foto", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("filter_gallery_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SoftTextPrimary
                        ),
                        border = BorderStroke(1.dp, SoftRoseAccent),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp), tint = SoftRosePrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Galeria", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Photo Preview & Interactive Touch Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .testTag("filter_preview_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SoftPeachCardVariant),
                border = BorderStroke(1.5.dp, SoftPeachBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { imageDisplaySize = it }
                        .pointerInput(sourceBitmap) {
                            detectTapGestures { offset ->
                                val src = sourceBitmap
                                if (src != null && imageDisplaySize.width > 0 && imageDisplaySize.height > 0) {
                                    val bmpX = (offset.x / imageDisplaySize.width.toFloat()) * src.width
                                    val bmpY = (offset.y / imageDisplaySize.height.toFloat()) * src.height
                                    focalPoint = PointF(bmpX, bmpY)
                                    showTargetIndicator = true
                                    Toast.makeText(context, "Centro do efeito ajustado!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val displayBitmap = filteredBitmap ?: sourceBitmap

                    if (displayBitmap != null) {
                        Image(
                            bitmap = displayBitmap.asImageBitmap(),
                            contentDescription = "Preview com Filtro Cara de Cu",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        CircularProgressIndicator(
                            color = SoftRosePrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Processing Overlay Indicator
                    if (isProcessing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp
                            )
                        }
                    }

                    // Face Detected Badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.90f),
                        border = BorderStroke(1.dp, SoftPeachBorder),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (faceDetected) Icons.Default.Check else Icons.Default.CenterFocusStrong,
                                contentDescription = null,
                                tint = if (faceDetected) Color(0xFF43A047) else SoftRosePrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (faceDetected) "Rosto Detectado ✨" else "Toque para centralizar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SoftTextPrimary
                            )
                        }
                    }

                    // Filter Intensity Watermark
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.90f),
                        border = BorderStroke(1.dp, SoftPeachBorder),
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = "🍑 ${(distortionIntensity * 100).roundToInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftRosePrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Intensity Slider & Controls Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SoftPeachCard),
                border = BorderStroke(1.dp, SoftPeachBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                tint = SoftRosePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Intensidade da Distorção",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SoftTextPrimary
                            )
                        }

                        Text(
                            text = when {
                                distortionIntensity < 0.35f -> "Fraco"
                                distortionIntensity < 0.70f -> "Médio"
                                distortionIntensity < 0.92f -> "Forte"
                                else -> "Extremo (Cara de Cu Total 🍑)"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftRosePrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Slider with - and + step buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                distortionIntensity = (distortionIntensity - 0.05f).coerceAtLeast(0f)
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color.White, CircleShape)
                                .border(1.dp, SoftPeachBorder, CircleShape)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Diminuir", tint = SoftTextPrimary, modifier = Modifier.size(16.dp))
                        }

                        Slider(
                            value = distortionIntensity,
                            onValueChange = { distortionIntensity = it },
                            valueRange = 0f..1f,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp)
                                .testTag("filter_intensity_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = SoftRosePrimary,
                                activeTrackColor = SoftRoseAccent,
                                inactiveTrackColor = SoftPeachBorder
                            )
                        )

                        IconButton(
                            onClick = {
                                distortionIntensity = (distortionIntensity + 0.05f).coerceAtMost(1f)
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color.White, CircleShape)
                                .border(1.dp, SoftPeachBorder, CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Aumentar", tint = SoftTextPrimary, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Intensity Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterPresetChip(
                            label = "Leve",
                            percentage = "30%",
                            isSelected = (distortionIntensity in 0.25f..0.35f),
                            onClick = { distortionIntensity = 0.30f },
                            modifier = Modifier.weight(1f)
                        )
                        FilterPresetChip(
                            label = "Médio",
                            percentage = "60%",
                            isSelected = (distortionIntensity in 0.55f..0.65f),
                            onClick = { distortionIntensity = 0.60f },
                            modifier = Modifier.weight(1f)
                        )
                        FilterPresetChip(
                            label = "Forte",
                            percentage = "85%",
                            isSelected = (distortionIntensity in 0.80f..0.90f),
                            onClick = { distortionIntensity = 0.85f },
                            modifier = Modifier.weight(1f)
                        )
                        FilterPresetChip(
                            label = "Total 🍑",
                            percentage = "100%",
                            isSelected = distortionIntensity >= 0.95f,
                            onClick = { distortionIntensity = 1.0f },
                            modifier = Modifier.weight(1.1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Actions: Post to Feed, Save, Profile, Share
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // PRIMARY: Post to Community Feed
                Button(
                    onClick = {
                        postCaption = "Olha minha cara de cu com o filtro do app! 🍑✨"
                        showPostDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("filter_post_feed_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SoftRosePrimary,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Postar no Feed da Comunidade 🚀",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Secondary Action Row: Save & Use as Profile
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val bmp = filteredBitmap ?: sourceBitmap
                            if (bmp != null) {
                                coroutineScope.launch(Dispatchers.IO) {
                                    val file = File(context.filesDir, "cara_de_cu_${System.currentTimeMillis()}.jpg")
                                    FileOutputStream(file).use { out ->
                                        bmp.compress(Bitmap.CompressFormat.JPEG, 95, out)
                                    }
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Foto salva com sucesso! 💾", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = SoftTextPrimary
                        ),
                        border = BorderStroke(1.dp, SoftPeachBorder)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp), tint = SoftRosePrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salvar Foto", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val bmp = filteredBitmap ?: sourceBitmap
                            if (bmp != null) {
                                coroutineScope.launch(Dispatchers.IO) {
                                    val file = File(context.filesDir, "user_profile_filtered_${System.currentTimeMillis()}.jpg")
                                    FileOutputStream(file).use { out ->
                                        bmp.compress(Bitmap.CompressFormat.JPEG, 95, out)
                                    }
                                    val path = file.absolutePath
                                    viewModel.updateUserProfile(
                                        name = settings.userName,
                                        emoji = "🍑",
                                        photoUri = path
                                    )
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Foto de perfil atualizada! 👤", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = SoftTextPrimary
                        ),
                        border = BorderStroke(1.dp, SoftPeachBorder)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp), tint = SoftRosePrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Usar no Perfil", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Share Button
                OutlinedButton(
                    onClick = {
                        val bmp = filteredBitmap ?: sourceBitmap
                        if (bmp != null) {
                            coroutineScope.launch(Dispatchers.IO) {
                                val file = File(context.cacheDir, "share_cara_de_cu.jpg")
                                FileOutputStream(file).use { out ->
                                    bmp.compress(Bitmap.CompressFormat.JPEG, 95, out)
                                }
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Olha a minha Cara de Cu feita no app! 🍑 Baixe o app Cara de Cu / Cara de Paçoca!")
                                }
                                withContext(Dispatchers.Main) {
                                    context.startActivity(Intent.createChooser(shareIntent, "Compartilhar Filtro Cara de Cu"))
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SoftPeachCard,
                        contentColor = SoftTextPrimary
                    ),
                    border = BorderStroke(1.dp, SoftPeachBorder)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = SoftRosePrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Compartilhar com Amigos", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Post to Feed Dialog
    if (showPostDialog) {
        AlertDialog(
            onDismissRequest = { showPostDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🍑", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Postar no Feed", fontWeight = FontWeight.Bold, color = SoftTextPrimary)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Escreva uma legenda divertida para a sua foto com o Filtro Cara de Cu:",
                        fontSize = 13.sp,
                        color = SoftTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = postCaption,
                        onValueChange = { postCaption = it },
                        placeholder = { Text("Ex: Olha minha cara de cu hoje! 🍑😂", color = SoftTextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SoftRosePrimary,
                            unfocusedBorderColor = SoftPeachBorder,
                            focusedTextColor = SoftTextPrimary,
                            unfocusedTextColor = SoftTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bmp = filteredBitmap ?: sourceBitmap
                        if (bmp != null) {
                            coroutineScope.launch(Dispatchers.IO) {
                                val file = File(context.filesDir, "feed_cara_de_cu_${System.currentTimeMillis()}.jpg")
                                FileOutputStream(file).use { out ->
                                    bmp.compress(Bitmap.CompressFormat.JPEG, 95, out)
                                }
                                val path = file.absolutePath
                                withContext(Dispatchers.Main) {
                                    viewModel.publishManualPost(path, postCaption)
                                    showPostDialog = false
                                    onNavigateToFeed()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftRosePrimary)
                ) {
                    Text("Publicar Agora 🚀", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPostDialog = false }) {
                    Text("Cancelar", color = SoftTextSecondary)
                }
            },
            containerColor = SoftPeachBackground,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun FilterPresetChip(
    label: String,
    percentage: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) SoftRosePrimary else Color.White)
            .border(
                1.dp,
                if (isSelected) SoftRosePrimary else SoftPeachBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else SoftTextPrimary,
                maxLines = 1
            )
            Text(
                text = percentage,
                fontSize = 9.sp,
                color = if (isSelected) Color.White.copy(alpha = 0.85f) else SoftTextSecondary
            )
        }
    }
}
