package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.theme.PacocaThemePreset

@Composable
fun WallpaperRenderer(
    preset: PacocaThemePreset,
    userPhotoPath: String?,
    modifier: Modifier = Modifier,
    showUserPhotoMascot: Boolean = true
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(preset.backgroundBrush)
    ) {
        // If user provided a custom photo and we're in custom theme mode, display it full bleed
        if (preset.isCustomUserPhoto && !userPhotoPath.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(userPhotoPath)
                    .crossfade(true)
                    .build(),
                contentDescription = "Papel de parede Cara de Paçoca",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Dim overlay for readable text
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )
        }

        // Floating "Cara de Paçoca" Badge / Avatar if user photo exists and not full screen
        if (showUserPhotoMascot && !preset.isCustomUserPhoto && !userPhotoPath.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(160.dp)
                    .border(4.dp, Color(0xFFFBBF24), CircleShape)
                    .padding(4.dp)
                    .clip(CircleShape)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(userPhotoPath)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Sua Cara de Paçoca",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
