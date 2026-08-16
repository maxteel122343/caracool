package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AppSettings
import com.example.service.PacocaFrameHelper
import com.example.ui.theme.NaturalBorderLight
import com.example.ui.theme.NaturalTextSecondary
import com.example.util.SafeWordHelper
import kotlinx.coroutines.launch

@Composable
fun UserProfileDialog(
    settings: AppSettings,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String, photoUri: String?) -> Unit,
    onToggleFrame: ((Boolean) -> Unit)? = null
) {
    val isCuMode = settings.isCaraDeKoolMode
    val isSafeWord = settings.isSafeWordMode
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val defaultDisplayName = SafeWordHelper.getDefaultUserName(isCuMode, isSafeWord)

    var nameState by remember {
        mutableStateOf(
            if (settings.userName.isBlank() ||
                settings.userName == "Você (Cara de Paçoca)" ||
                settings.userName == "Você (Cara de Cu)" ||
                settings.userName == "Você (Cara de Kool)" ||
                settings.userName == "Você (Cara de Cool)") {
                defaultDisplayName
            } else {
                settings.userName
            }
        )
    }

    var photoUriState by remember { mutableStateOf(settings.userProfilePhotoUri ?: settings.userPhotoUri) }
    var isFrameEnabledState by remember { mutableStateOf(settings.isPhotoFrameEnabled) }
    var isProcessingFace by remember { mutableStateOf(false) }

    // Process photo with ML Kit and Face Filter (or clean photo if frame is disabled)
    fun processPhotoWithPacocaFrame(bitmap: Bitmap) {
        coroutineScope.launch {
            isProcessingFace = true
            try {
                val framedPath = PacocaFrameHelper.processAndApplyPacocaFrame(
                    context = context,
                    sourceBitmap = bitmap,
                    isKoolMode = isCuMode,
                    isFrameEnabled = isFrameEnabledState
                )
                photoUriState = framedPath
                val appLabel = SafeWordHelper.getAppDisplayName(isCuMode, isSafeWord)
                Toast.makeText(context, "$appLabel gerada com sucesso! ✨", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao processar foto: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isProcessingFace = false
            }
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            processPhotoWithPacocaFrame(bitmap)
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
            Toast.makeText(context, "Permissão de câmera necessária para foto de perfil", Toast.LENGTH_LONG).show()
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

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isProcessingFace = true
                try {
                    val framedPath = PacocaFrameHelper.processAndApplyPacocaFrame(
                        context = context,
                        sourceUri = uri,
                        isKoolMode = isCuMode,
                        isFrameEnabled = isFrameEnabledState
                    )
                    photoUriState = framedPath
                    val appLabel = SafeWordHelper.getAppDisplayName(isCuMode, isSafeWord)
                    Toast.makeText(context, "$appLabel gerada da galeria! ✨", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Erro ao processar imagem: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessingFace = false
                }
            }
        }
    }

    val dialogTitle = if (isCuMode) {
        if (isSafeWord) "Meu Perfil Cara de Cool" else "Meu Perfil Cara de Cu"
    } else {
        "Meu Perfil Paçoca"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(28.dp))
                .testTag("user_profile_dialog"),
            color = Color.White,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row (Universal Theme, Clean title without emoji)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dialogTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F1F1F)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color(0xFF757575)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Avatar / Profile Photo Preview (Universal Warm Theme with clean Person Icon)
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFFB74D), Color(0xFFFF6B35))
                            )
                        )
                        .border(3.5.dp, Color(0xFFFFE0B2), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val currentPhoto = photoUriState

                    if (isProcessingFace) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                    } else if (!currentPhoto.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(currentPhoto)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Foto de perfil",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar do Usuário",
                            tint = Color.White,
                            modifier = Modifier.size(62.dp)
                        )
                    }
                }

                if (isProcessingFace) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Detectando rosto e gerando foto de perfil... ✨",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B35)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Frame Toggle Switch Card (Clean Universal Theme, without peanut emoji)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF8E7)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color(0xFFFFE0B2)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFFF6B35),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Moldura de Paçoca na Foto",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF212121)
                                )
                                Text(
                                    text = if (isFrameEnabledState) "Moldura padrão ativada ao tirar foto" else "Foto limpa sem moldura ao tirar foto",
                                    fontSize = 11.sp,
                                    color = Color(0xFF6B6B6B)
                                )
                            }
                        }
                        Switch(
                            checked = isFrameEnabledState,
                            onCheckedChange = { checked ->
                                isFrameEnabledState = checked
                                onToggleFrame?.invoke(checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFFF6B35),
                                uncheckedThumbColor = Color(0xFFBDBDBD),
                                uncheckedTrackColor = Color(0xFFE0E0E0)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Photo Action Buttons (Camera / Gallery) - Universal Theme
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { handleTakePhoto() },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("profile_take_photo_btn"),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCC80)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE65100))
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Câmera", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("profile_gallery_btn"),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalBorderLight),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF424242))
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Galeria", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (photoUriState != null) {
                        IconButton(
                            onClick = { photoUriState = null },
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(14.dp))
                        ) {
                            Text("🗑️", fontSize = 16.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // User Name Text Field (Universal Theme)
                OutlinedTextField(
                    value = nameState,
                    onValueChange = { nameState = it },
                    label = { Text("Seu Nome / Apelido", fontSize = 13.sp) },
                    placeholder = { Text(defaultDisplayName) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B35),
                        unfocusedBorderColor = NaturalBorderLight
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Info summary pill
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8F9FA)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡 Sua foto e nome aparecerão automaticamente em todos os seus posts, comentários e curtidas na Comunidade!",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = Color(0xFF616161)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Save Action Button (Universal Orange Theme)
                Button(
                    onClick = {
                        val finalName = if (nameState.trim().isBlank()) defaultDisplayName else nameState.trim()
                        onSave(finalName, settings.userAvatarEmoji, photoUriState)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_profile_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B35)
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salvar Perfil", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
