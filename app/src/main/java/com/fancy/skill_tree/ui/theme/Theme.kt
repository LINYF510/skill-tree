package com.fancy.skill_tree.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * CompositionLocal 用于在组件树中传递自定义配色
 */
val LocalThemeColors = staticCompositionLocalOf { ThemeColors.fromDark() }

private val DarkColorScheme = darkColorScheme(
    primary = DarkColors.Primary,
    onPrimary = Color(0xFF0D1117),
    primaryContainer = Color(0xFF1A3A5C),
    secondary = DarkColors.Ability,
    onSecondary = Color(0xFF0D1117),
    tertiary = DarkColors.Resource,
    background = DarkColors.Background,
    onBackground = DarkColors.TextPrimary,
    surface = DarkColors.Surface,
    onSurface = DarkColors.TextPrimary,
    surfaceVariant = DarkColors.Surface,
    onSurfaceVariant = DarkColors.TextSecondary,
    error = DarkColors.Error,
    onError = Color(0xFFFFFFFF)
)

private val LightColorScheme = lightColorScheme(
    primary = LightColors.Primary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDF4FF),
    secondary = LightColors.Ability,
    onSecondary = Color(0xFFFFFFFF),
    background = LightColors.Background,
    onBackground = LightColors.TextPrimary,
    surface = LightColors.Surface,
    onSurface = LightColors.TextPrimary,
    surfaceVariant = LightColors.Surface,
    onSurfaceVariant = LightColors.TextSecondary,
    error = LightColors.Error,
    onError = Color(0xFFFFFFFF)
)

/**
 * 主题 Composable
 * 自动根据 ThemeMode 选择暗色/亮色主题
 *
 * @param themeMode 主题模式（DARK/LIGHT/SYSTEM）
 * @param content 主题包裹的内容
 */
@Composable
fun SkilltreeTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemDark
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val themeColors = getThemeColors(themeMode, isSystemDark)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    CompositionLocalProvider(LocalThemeColors provides themeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
