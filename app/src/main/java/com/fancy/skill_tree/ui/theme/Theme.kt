package com.fancy.skill_tree.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
 * 支持 Material You 动态颜色（API 31+）
 *
 * @param themeMode 主题模式（DARK/LIGHT/SYSTEM/DYNAMIC）
 * @param content 主题包裹的内容
 */
@Composable
fun SkilltreeTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // 处理低版本设备回退
    val effectiveMode = if (themeMode == ThemeMode.DYNAMIC && !supportsDynamic) {
        ThemeMode.SYSTEM
    } else {
        themeMode
    }

    val isDark = when (effectiveMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.DYNAMIC -> isSystemDark
    }

    val colorScheme = when {
        effectiveMode == ThemeMode.DYNAMIC && supportsDynamic -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    val themeColors = if (effectiveMode == ThemeMode.DYNAMIC && supportsDynamic) {
        ThemeColors.fromDynamicColorScheme(colorScheme, isDark)
    } else {
        getThemeColors(effectiveMode, isSystemDark)
    }

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
