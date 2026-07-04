package com.example.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color

data class TreeTheme(
    val styleId: Int = 1,
    val fontSize: TextUnit = 12.sp,
    val primaryColor: Color = Color(0xFFD4AF37), // Gold
    val secondaryColor: Color = Color(0xFF2E7D32) // Forest Green
) {
    fun withAccentColor(color: Color): TreeTheme {
        return this.copy(primaryColor = color)
    }
}

val LocalTreeTheme = staticCompositionLocalOf { TreeTheme() }
