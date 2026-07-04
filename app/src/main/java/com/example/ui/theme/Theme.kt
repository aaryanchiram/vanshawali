package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Premium Indian Traditional / Heritage Color Palette (Saffron, Maroon, Gold & Warm Cream)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF9E2A2B),         // Heritage Deep Crimson / Maroon Red
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFBECEB), // Delicate Warm Rose Petal Cream
    onPrimaryContainer = Color(0xFF531112),
    secondary = Color(0xFFE07A5F),       // Rich Indian Saffron / Terracotta
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCECD9), // Soft Creamy Saffron
    onSecondaryContainer = Color(0xFF7A270D),
    tertiary = Color(0xFF3F5E3D),        // Holy Banyan Leaf Green (family growth & prosperity)
    onTertiary = Color.White,
    background = Color(0xFFFFFDFA),      // Majestic Alabaster / Warm Cream Base
    onBackground = Color(0xFF2B221E),    // Elegant Charcoal Cocoa (Highly legible)
    surface = Color(0xFFFAF5EC),         // Soft Ivory Silk Card Base
    onSurface = Color(0xFF2B221E),
    surfaceVariant = Color(0xFFEFE6D5),  // Beautiful Warm Sandstoning
    onSurfaceVariant = Color(0xFF4D413A),
    outline = Color(0xFF82746B)          // Muted Bronze Outline
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF4A261),         // Radiant Saffron Amber
    onPrimary = Color(0xFF4A1E05),
    primaryContainer = Color(0xFF331D08),
    onPrimaryContainer = Color(0xFFFFDDB8),
    secondary = Color(0xFFE76F51),       // Soft Terracotta sunset
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A1A0C),
    onSecondaryContainer = Color(0xFFFFDCD3),
    tertiary = Color(0xFF8AB183),        // Sage Green
    onTertiary = Color(0xFF132A15),
    background = Color(0xFF151916),      // Deep Holy Forest Nights
    onBackground = Color(0xFFE6E2DC),
    surface = Color(0xFF1C221D),         // Slate Green Jade
    onSurface = Color(0xFFE6E2DC),
    surfaceVariant = Color(0xFF2D352F),  // Warm Muted Green Ash
    onSurfaceVariant = Color(0xFFCFCECA),
    outline = Color(0xFF8D938E)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+ (setting false to prioritize our beautiful custom heritage themes)
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
