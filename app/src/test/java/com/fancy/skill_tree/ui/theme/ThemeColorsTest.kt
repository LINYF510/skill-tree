package com.fancy.skill_tree.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * ThemeColors 单元测试
 */
@DisplayName("ThemeColors")
class ThemeColorsTest {

    @Nested
    @DisplayName("fromDynamicColorScheme")
    inner class FromDynamicColorScheme {

        @Test
        @DisplayName("从 ColorScheme 正确提取暗色主题颜色")
        fun extractsDarkThemeColorsCorrectly() {
            val colorScheme = ColorScheme(
                primary = Color(0xFF58A6FF),
                onPrimary = Color(0xFF0D1117),
                primaryContainer = Color(0xFF1A3A5C),
                onPrimaryContainer = Color(0xFFFFFFFF),
                secondary = Color(0xFF3FB950),
                onSecondary = Color(0xFF0D1117),
                secondaryContainer = Color(0xFF1A3A5C),
                onSecondaryContainer = Color(0xFFFFFFFF),
                tertiary = Color(0xFFD2A8FF),
                onTertiary = Color(0xFF0D1117),
                tertiaryContainer = Color(0xFF1A3A5C),
                onTertiaryContainer = Color(0xFFFFFFFF),
                background = Color(0xFF0D1117),
                onBackground = Color(0xFFE6EDF3),
                surface = Color(0xFF161B22),
                onSurface = Color(0xFFE6EDF3),
                surfaceVariant = Color(0xFF161B22),
                onSurfaceVariant = Color(0xFF8B949E),
                error = Color(0xFFF85149),
                onError = Color(0xFFFFFFFF),
                errorContainer = Color(0xFF1A3A5C),
                onErrorContainer = Color(0xFFFFFFFF),
                outline = Color(0xFF8B949E),
                outlineVariant = Color(0xFF8B949E),
                scrim = Color(0xFF000000),
                surfaceTint = Color(0xFF58A6FF),
                inverseSurface = Color(0xFFE6EDF3),
                inverseOnSurface = Color(0xFF0D1117),
                inversePrimary = Color(0xFF58A6FF),
                surfaceDim = Color(0xFF0D1117),
                surfaceBright = Color(0xFF161B22),
                surfaceContainer = Color(0xFF161B22),
                surfaceContainerHigh = Color(0xFF161B22),
                surfaceContainerHighest = Color(0xFF161B22),
                surfaceContainerLow = Color(0xFF161B22),
                surfaceContainerLowest = Color(0xFF0D1117)
            )

            val themeColors = ThemeColors.fromDynamicColorScheme(colorScheme, isDark = true)

            assertThat(themeColors.background).isEqualTo(colorScheme.background)
            assertThat(themeColors.surface).isEqualTo(colorScheme.surface)
            assertThat(themeColors.primary).isEqualTo(colorScheme.primary)
            assertThat(themeColors.ability).isEqualTo(colorScheme.secondary)
            assertThat(themeColors.resource).isEqualTo(colorScheme.tertiary)
            assertThat(themeColors.ai).isEqualTo(colorScheme.primaryContainer)
            assertThat(themeColors.textPrimary).isEqualTo(colorScheme.onBackground)
            assertThat(themeColors.textSecondary).isEqualTo(colorScheme.onSurfaceVariant)
            assertThat(themeColors.error).isEqualTo(colorScheme.error)
            // 暗色模式下 warning 应为 0xFFD29922
            assertThat(themeColors.warning).isEqualTo(Color(0xFFD29922))
        }

        @Test
        @DisplayName("从 ColorScheme 正确提取亮色主题颜色")
        fun extractsLightThemeColorsCorrectly() {
            val colorScheme = ColorScheme(
                primary = Color(0xFF0969DA),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFDDF4FF),
                onPrimaryContainer = Color(0xFF0D1117),
                secondary = Color(0xFF1A7F37),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFDDF4FF),
                onSecondaryContainer = Color(0xFF0D1117),
                tertiary = Color(0xFF8250DF),
                onTertiary = Color(0xFFFFFFFF),
                tertiaryContainer = Color(0xFFDDF4FF),
                onTertiaryContainer = Color(0xFF0D1117),
                background = Color(0xFFFFFFFF),
                onBackground = Color(0xFF1F2328),
                surface = Color(0xFFF6F8FA),
                onSurface = Color(0xFF1F2328),
                surfaceVariant = Color(0xFFF6F8FA),
                onSurfaceVariant = Color(0xFF656D76),
                error = Color(0xFFCF222E),
                onError = Color(0xFFFFFFFF),
                errorContainer = Color(0xFFDDF4FF),
                onErrorContainer = Color(0xFF0D1117),
                outline = Color(0xFF656D76),
                outlineVariant = Color(0xFF656D76),
                scrim = Color(0xFF000000),
                surfaceTint = Color(0xFF0969DA),
                inverseSurface = Color(0xFF1F2328),
                inverseOnSurface = Color(0xFFFFFFFF),
                inversePrimary = Color(0xFF0969DA),
                surfaceDim = Color(0xFFFFFFFF),
                surfaceBright = Color(0xFFF6F8FA),
                surfaceContainer = Color(0xFFF6F8FA),
                surfaceContainerHigh = Color(0xFFF6F8FA),
                surfaceContainerHighest = Color(0xFFF6F8FA),
                surfaceContainerLow = Color(0xFFF6F8FA),
                surfaceContainerLowest = Color(0xFFFFFFFF)
            )

            val themeColors = ThemeColors.fromDynamicColorScheme(colorScheme, isDark = false)

            assertThat(themeColors.background).isEqualTo(colorScheme.background)
            assertThat(themeColors.surface).isEqualTo(colorScheme.surface)
            assertThat(themeColors.primary).isEqualTo(colorScheme.primary)
            assertThat(themeColors.ability).isEqualTo(colorScheme.secondary)
            assertThat(themeColors.resource).isEqualTo(colorScheme.tertiary)
            assertThat(themeColors.ai).isEqualTo(colorScheme.primaryContainer)
            assertThat(themeColors.textPrimary).isEqualTo(colorScheme.onBackground)
            assertThat(themeColors.textSecondary).isEqualTo(colorScheme.onSurfaceVariant)
            assertThat(themeColors.error).isEqualTo(colorScheme.error)
            // 亮色模式下 warning 应为 0xFF9A6700
            assertThat(themeColors.warning).isEqualTo(Color(0xFF9A6700))
        }

        @Test
        @DisplayName("link 颜色为 primary 的 70% 透明度")
        fun linkColorIsPrimaryWith70PercentAlpha() {
            val colorScheme = ColorScheme(
                primary = Color(0xFF58A6FF),
                onPrimary = Color(0xFF0D1117),
                primaryContainer = Color(0xFF1A3A5C),
                onPrimaryContainer = Color(0xFFFFFFFF),
                secondary = Color(0xFF3FB950),
                onSecondary = Color(0xFF0D1117),
                secondaryContainer = Color(0xFF1A3A5C),
                onSecondaryContainer = Color(0xFFFFFFFF),
                tertiary = Color(0xFFD2A8FF),
                onTertiary = Color(0xFF0D1117),
                tertiaryContainer = Color(0xFF1A3A5C),
                onTertiaryContainer = Color(0xFFFFFFFF),
                background = Color(0xFF0D1117),
                onBackground = Color(0xFFE6EDF3),
                surface = Color(0xFF161B22),
                onSurface = Color(0xFFE6EDF3),
                surfaceVariant = Color(0xFF161B22),
                onSurfaceVariant = Color(0xFF8B949E),
                error = Color(0xFFF85149),
                onError = Color(0xFFFFFFFF),
                errorContainer = Color(0xFF1A3A5C),
                onErrorContainer = Color(0xFFFFFFFF),
                outline = Color(0xFF8B949E),
                outlineVariant = Color(0xFF8B949E),
                scrim = Color(0xFF000000),
                surfaceTint = Color(0xFF58A6FF),
                inverseSurface = Color(0xFFE6EDF3),
                inverseOnSurface = Color(0xFF0D1117),
                inversePrimary = Color(0xFF58A6FF),
                surfaceDim = Color(0xFF0D1117),
                surfaceBright = Color(0xFF161B22),
                surfaceContainer = Color(0xFF161B22),
                surfaceContainerHigh = Color(0xFF161B22),
                surfaceContainerHighest = Color(0xFF161B22),
                surfaceContainerLow = Color(0xFF161B22),
                surfaceContainerLowest = Color(0xFF0D1117)
            )

            val themeColors = ThemeColors.fromDynamicColorScheme(colorScheme, isDark = true)

            assertThat(themeColors.link).isEqualTo(colorScheme.primary.copy(alpha = 0.7f))
        }
    }
}
