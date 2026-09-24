package com.samge.qanvas.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Apple-inspired design tokens (same system as BiTrans):
 *  - single accent Action Blue #0066cc, Parchment canvas #f5f5f7, Ink text #1d1d1f
 *  - pill CTAs, 18dp card radius, hairline borders, no chrome shadows
 * Qanvas adds a violet "creation" accent for the brand mark.
 */
object AppleTokens {
    val ActionBlue = Color(0xFF0066CC)
    val ActionBlueFocus = Color(0xFF0071E3)
    val SkyLinkOnDark = Color(0xFF2997FF)
    val Violet = Color(0xFF6C5CE7)
    val VioletSoft = Color(0xFFEFEBFF)
    val Ink = Color(0xFF1D1D1F)
    val InkMuted80 = Color(0xFF333333)
    val InkMuted48 = Color(0xFF7A7A7A)
    val Parchment = Color(0xFFF5F5F7)
    val Pearl = Color(0xFFFAFAFC)
    val Canvas = Color(0xFFFFFFFF)
    val Hairline = Color(0xFFE0E0E0)
    val DividerSoft = Color(0xFFF0F0F0)
    val Green = Color(0xFF34C759)
    val Orange = Color(0xFFFF9500)
    val Red = Color(0xFFD70015)
    val DarkTile = Color(0xFF272729)
    val DarkTile2 = Color(0xFF2A2A2C)
    val DarkTile3 = Color(0xFF252527)
    val OnDark = Color(0xFFFFFFFF)
    val BodyMutedOnDark = Color(0xFFCCCCCC)
}

private val LightScheme = lightColorScheme(
    primary = AppleTokens.ActionBlue,
    onPrimary = AppleTokens.Canvas,
    primaryContainer = AppleTokens.VioletSoft,
    onPrimaryContainer = AppleTokens.Ink,
    secondary = AppleTokens.InkMuted80,
    background = AppleTokens.Pearl,
    onBackground = AppleTokens.Ink,
    surface = AppleTokens.Canvas,
    onSurface = AppleTokens.Ink,
    surfaceVariant = AppleTokens.Parchment,
    onSurfaceVariant = AppleTokens.InkMuted80,
    outline = AppleTokens.Hairline,
    error = AppleTokens.Red,
)

private val DarkScheme = darkColorScheme(
    primary = AppleTokens.SkyLinkOnDark,
    onPrimary = Color(0xFF00315C),
    primaryContainer = AppleTokens.DarkTile,
    onPrimaryContainer = AppleTokens.OnDark,
    secondary = AppleTokens.BodyMutedOnDark,
    background = Color(0xFF000000),
    onBackground = AppleTokens.OnDark,
    surface = AppleTokens.DarkTile,
    onSurface = AppleTokens.OnDark,
    surfaceVariant = AppleTokens.DarkTile2,
    onSurfaceVariant = AppleTokens.BodyMutedOnDark,
    outline = Color(0xFF3A3A3C),
    error = Color(0xFFFF453A),
)

private val AppleTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight(600), fontSize = 21.sp, letterSpacing = 0.2.sp),
    titleMedium = TextStyle(fontWeight = FontWeight(600), fontSize = 17.sp, letterSpacing = (-0.374).sp),
    bodyLarge = TextStyle(fontWeight = FontWeight(400), fontSize = 17.sp, lineHeight = 25.sp, letterSpacing = (-0.374).sp),
    bodyMedium = TextStyle(fontWeight = FontWeight(400), fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = (-0.224).sp),
    bodySmall = TextStyle(fontWeight = FontWeight(400), fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = (-0.12).sp),
    labelLarge = TextStyle(fontWeight = FontWeight(400), fontSize = 17.sp, letterSpacing = (-0.374).sp),
    labelMedium = TextStyle(fontWeight = FontWeight(500), fontSize = 12.sp, letterSpacing = 0.1.sp),
)

@Composable
fun QanvasTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme,
        typography = AppleTypography,
        content = content,
    )
}
