package com.example.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class PacocaThemePreset(
    val id: String,
    val name: String,
    val description: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val backgroundBrush: Brush,
    val accentColor: Color,
    val isCustomUserPhoto: Boolean = false,
    val mascotEmoji: String = "🥜"
)

object ThemePresets {
    val Classic = PacocaThemePreset(
        id = "classic",
        name = "Minha Paçoca Original",
        description = "Tons naturais suaves de amendoim, creme e caramelo artesanal.",
        primaryColor = Color(0xFF5D4037),
        secondaryColor = Color(0xFF8D6E63),
        backgroundBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFDF8F3),
                Color(0xFFEFEBE9),
                Color(0xFFD7CCC8)
            )
        ),
        accentColor = Color(0xFFFFB74D),
        mascotEmoji = "🥜"
    )

    val FestaJunina = PacocaThemePreset(
        id = "festajunina",
        name = "Festa de São João",
        description = "Alegria da fogueira, bandeirinhas e doce de amendoim caipira.",
        primaryColor = Color(0xFFEA580C),
        secondaryColor = Color(0xFFC2410C),
        backgroundBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF7C2D12),
                Color(0xFF9A3412),
                Color(0xFFEA580C)
            )
        ),
        accentColor = Color(0xFFFACC15),
        mascotEmoji = "🔥"
    )

    val UserCustom = PacocaThemePreset(
        id = "custom_photo",
        name = "Minha Foto Cara de Paçoca",
        description = "Sua foto de câmera/galeria personalizada com moldura e efeitos.",
        primaryColor = Color(0xFFD97706),
        secondaryColor = Color(0xFF1C1917),
        backgroundBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1C1917),
                Color(0xFF292524),
                Color(0xFF44403C)
            )
        ),
        accentColor = Color(0xFFF59E0B),
        isCustomUserPhoto = true,
        mascotEmoji = "📸"
    )

    val CaraDeKool = PacocaThemePreset(
        id = "kool",
        name = "Modo Cara de Cu",
        description = "Estética Rosa & Berry marcante, mascote Tardígrado e moldura de paçoca.",
        primaryColor = Color(0xFFE91E63),
        secondaryColor = Color(0xFF880E4F),
        backgroundBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFF0F5),
                Color(0xFFFCE4EC),
                Color(0xFFF8BBD0)
            )
        ),
        accentColor = Color(0xFFFF4081),
        mascotEmoji = "👾"
    )

    val allPresets = listOf(Classic, CaraDeKool, FestaJunina, UserCustom)

    fun getById(id: String): PacocaThemePreset {
        return allPresets.find { it.id == id } ?: Classic
    }

    fun getDisplayName(preset: PacocaThemePreset, isCuMode: Boolean): String {
        return if (preset.id == "custom_photo") {
            if (isCuMode) "Minha Foto Cara de Cu" else "Minha Foto Cara de Paçoca"
        } else {
            preset.name
        }
    }
}
