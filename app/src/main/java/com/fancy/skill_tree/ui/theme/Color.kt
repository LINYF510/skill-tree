package com.fancy.skill_tree.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * 暗色主题配色
 */
object DarkColors {
    val Background = Color(0xFF0D1117)
    val Surface = Color(0xFF161B22)
    val Primary = Color(0xFF58A6FF)
    val Ability = Color(0xFF3FB950)
    val Resource = Color(0xFFD2A8FF)
    val Link = Color(0xFFF0883E)
    val Ai = Color(0xFF79C0FF)
    val TextPrimary = Color(0xFFE6EDF3)
    val TextSecondary = Color(0xFF8B949E)
    val Error = Color(0xFFF85149)
    val Warning = Color(0xFFD29922)
}

/**
 * 亮色主题配色
 */
object LightColors {
    val Background = Color(0xFFFFFFFF)
    val Surface = Color(0xFFF6F8FA)
    val Primary = Color(0xFF0969DA)
    val Ability = Color(0xFF1A7F37)
    val Resource = Color(0xFF8250DF)
    val Link = Color(0xFFD97706)
    val Ai = Color(0xFF0550AE)
    val TextPrimary = Color(0xFF1F2328)
    val TextSecondary = Color(0xFF656D76)
    val Error = Color(0xFFCF222E)
    val Warning = Color(0xFF9A6700)
}

/**
 * 主题模式枚举
 * DARK - 始终暗色, LIGHT - 始终亮色, SYSTEM - 跟随系统, DYNAMIC - Material You 动态颜色 (API 31+)
 */
enum class ThemeMode { DARK, LIGHT, SYSTEM, DYNAMIC }

/**
 * 统一配色数据类
 * 组件中通过 LocalThemeColors.current 获取当前配色
 */
data class ThemeColors(
    val background: Color,
    val surface: Color,
    val primary: Color,
    val ability: Color,
    val resource: Color,
    val link: Color,
    val ai: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val error: Color,
    val warning: Color
) {
    companion object {
        /**
         * 从暗色配色创建
         */
        fun fromDark(): ThemeColors = ThemeColors(
            DarkColors.Background, DarkColors.Surface, DarkColors.Primary,
            DarkColors.Ability, DarkColors.Resource, DarkColors.Link,
            DarkColors.Ai, DarkColors.TextPrimary, DarkColors.TextSecondary,
            DarkColors.Error, DarkColors.Warning
        )

        /**
         * 从亮色配色创建
         */
        fun fromLight(): ThemeColors = ThemeColors(
            LightColors.Background, LightColors.Surface, LightColors.Primary,
            LightColors.Ability, LightColors.Resource, LightColors.Link,
            LightColors.Ai, LightColors.TextPrimary, LightColors.TextSecondary,
            LightColors.Error, LightColors.Warning
        )

        /**
         * 从 Material3 ColorScheme 创建 ThemeColors（用于 Material You 动态颜色）
         *
         * @param colorScheme Material3 颜色方案
         * @param isDark 是否为暗色模式
         * @return 从 ColorScheme 提取的 ThemeColors
         */
        fun fromDynamicColorScheme(colorScheme: ColorScheme, isDark: Boolean): ThemeColors {
            return ThemeColors(
                background = colorScheme.background,
                surface = colorScheme.surface,
                primary = colorScheme.primary,
                ability = colorScheme.secondary,
                resource = colorScheme.tertiary,
                link = colorScheme.primary.copy(alpha = 0.7f),
                ai = colorScheme.primaryContainer,
                textPrimary = colorScheme.onBackground,
                textSecondary = colorScheme.onSurfaceVariant,
                error = colorScheme.error,
                warning = if (isDark) Color(0xFFD29922) else Color(0xFF9A6700)
            )
        }
    }
}

/**
 * 根据主题模式获取当前配色
 *
 * @param mode 主题模式
 * @param isSystemDark 系统是否为暗色模式
 * @return 当前主题配色
 */
fun getThemeColors(mode: ThemeMode, isSystemDark: Boolean): ThemeColors {
    return when (mode) {
        ThemeMode.DARK -> ThemeColors.fromDark()
        ThemeMode.LIGHT -> ThemeColors.fromLight()
        ThemeMode.SYSTEM -> if (isSystemDark) ThemeColors.fromDark() else ThemeColors.fromLight()
        ThemeMode.DYNAMIC -> if (isSystemDark) ThemeColors.fromDark() else ThemeColors.fromLight()
    }
}

@Deprecated("使用 LocalThemeColors.current.background", ReplaceWith("LocalThemeColors.current.background"))
val BackgroundDark = DarkColors.Background

@Deprecated("使用 LocalThemeColors.current.surface", ReplaceWith("LocalThemeColors.current.surface"))
val SurfaceDark = DarkColors.Surface

@Deprecated("使用 LocalThemeColors.current.primary", ReplaceWith("LocalThemeColors.current.primary"))
val PrimaryBlue = DarkColors.Primary

@Deprecated("使用 LocalThemeColors.current.ability", ReplaceWith("LocalThemeColors.current.ability"))
val AbilityGreen = DarkColors.Ability

@Deprecated("使用 LocalThemeColors.current.resource", ReplaceWith("LocalThemeColors.current.resource"))
val ResourcePurple = DarkColors.Resource

@Deprecated("使用 LocalThemeColors.current.link", ReplaceWith("LocalThemeColors.current.link"))
val LinkOrange = DarkColors.Link

@Deprecated("使用 LocalThemeColors.current.ai", ReplaceWith("LocalThemeColors.current.ai"))
val AiBlue = DarkColors.Ai

@Deprecated("使用 LocalThemeColors.current.textPrimary", ReplaceWith("LocalThemeColors.current.textPrimary"))
val TextPrimary = DarkColors.TextPrimary

@Deprecated("使用 LocalThemeColors.current.textSecondary", ReplaceWith("LocalThemeColors.current.textSecondary"))
val TextSecondary = DarkColors.TextSecondary
