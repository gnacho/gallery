package app.galeria.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val GalleryDarkColors = darkColorScheme(
    primary = Color(0xFF6B9BFF),
    onPrimary = Color.White,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF17181B),
    onSurfaceVariant = Color(0xFFC8CAD0),
)

private val GalleryLightColors = lightColorScheme(
    primary = Color(0xFF245FC7),
    onPrimary = Color.White,
    background = Color(0xFFFAFAFC),
    onBackground = Color(0xFF17181B),
    surface = Color(0xFFFAFAFC),
    onSurface = Color(0xFF17181B),
    surfaceVariant = Color(0xFFE8EAF0),
    onSurfaceVariant = Color(0xFF454852),
)

@Composable
fun GalleryTheme(dynamicColor: Boolean = true, darkTheme: Boolean? = null, content: @Composable () -> Unit) {
    val dark = darkTheme ?: isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        dynamicColor && dark -> dynamicDarkColorScheme(context)
        dynamicColor -> dynamicLightColorScheme(context)
        dark -> GalleryDarkColors
        else -> GalleryLightColors
    }
    MaterialTheme(colorScheme = colors) {
        Surface(color = colors.background, contentColor = colors.onBackground, content = content)
    }
}
