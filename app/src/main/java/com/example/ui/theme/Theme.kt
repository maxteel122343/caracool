package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 1. Paleta Universal (Nova Paleta Padrão do Usuário)
val UniversalColorScheme = lightColorScheme(
    primary = UniversalPrimary,             // #FF6B35 (Laranja)
    onPrimary = Color.White,
    primaryContainer = UniversalSecondaryContainer, // #EFEBFF (Roxo suave / lilás)
    onPrimaryContainer = UniversalSecondary,        // #7B6CF6
    secondary = UniversalSecondary,                 // #7B6CF6
    onSecondary = Color.White,
    secondaryContainer = UniversalSecondaryContainer,
    onSecondaryContainer = UniversalSecondary,
    tertiary = UniversalSecondary,
    onTertiary = Color.White,
    background = UniversalBackground,               // #F5F5F5
    onBackground = UniversalTextPrimary,            // #1F1F1F
    surface = UniversalCard,                        // #FFFFFF
    onSurface = UniversalTextPrimary,               // #1F1F1F
    surfaceVariant = UniversalCardVariant,          // #FFFFFF / #FAFAFA
    onSurfaceVariant = UniversalTextSecondary,      // #6B6B6B
    outline = UniversalBorder                       // #E8E8E8
)

// 2. Paleta Paçoca (Amendoim / Caramelo)
val PacocaColorScheme = lightColorScheme(
    primary = PacocaPrimary,
    onPrimary = Color.White,
    primaryContainer = PacocaSecondaryContainer,
    onPrimaryContainer = PacocaPrimary,
    secondary = PacocaSecondary,
    onSecondary = Color.White,
    secondaryContainer = PacocaSecondaryContainer,
    onSecondaryContainer = PacocaPrimary,
    tertiary = PacocaSecondary,
    onTertiary = Color.White,
    background = PacocaBackground,
    onBackground = PacocaTextPrimary,
    surface = PacocaCard,
    onSurface = PacocaTextPrimary,
    surfaceVariant = PacocaCard,
    onSurfaceVariant = PacocaTextSecondary,
    outline = PacocaBorder
)

// 3. Paleta Cara de Kool (Pele & Pêssego Natural)
val Kool1ColorScheme = lightColorScheme(
    primary = Kool1Primary,
    onPrimary = Color.White,
    primaryContainer = Kool1SecondaryContainer,
    onPrimaryContainer = Kool1Primary,
    secondary = Kool1Secondary,
    onSecondary = Color.White,
    secondaryContainer = Kool1SecondaryContainer,
    onSecondaryContainer = Kool1Primary,
    tertiary = Kool1Secondary,
    onTertiary = Color.White,
    background = Kool1Background,
    onBackground = Kool1TextPrimary,
    surface = Kool1Card,
    onSurface = Kool1TextPrimary,
    surfaceVariant = Kool1Card,
    onSurfaceVariant = Kool1TextSecondary,
    outline = Kool1Border
)

// 4. Paleta Cara de Kool 2 (Rosa Chiclete & Berry)
val Kool2ColorScheme = lightColorScheme(
    primary = Kool2Primary,
    onPrimary = Color.White,
    primaryContainer = Kool2SecondaryContainer,
    onPrimaryContainer = Kool2Primary,
    secondary = Kool2Secondary,
    onSecondary = Color.White,
    secondaryContainer = Kool2SecondaryContainer,
    onSecondaryContainer = Kool2Primary,
    tertiary = Kool2Secondary,
    onTertiary = Color.White,
    background = Kool2Background,
    onBackground = Kool2TextPrimary,
    surface = Kool2Card,
    onSurface = Kool2TextPrimary,
    surfaceVariant = Kool2Card,
    onSurfaceVariant = Kool2TextSecondary,
    outline = Kool2Border
)

@Composable
fun CaraDePacocaTheme(
    isKoolTheme: Boolean = false,
    koolPalette: String = "universal",
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val activeColorScheme = when (koolPalette) {
        "pacoca" -> PacocaColorScheme
        "nude_peach", "kool_1" -> Kool1ColorScheme
        "pink_berry", "kool_2" -> Kool2ColorScheme
        "universal" -> UniversalColorScheme
        else -> UniversalColorScheme
    }

    MaterialTheme(
        colorScheme = activeColorScheme,
        typography = Typography,
        content = content
    )
}
