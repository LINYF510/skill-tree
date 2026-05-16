package com.fancy.skill_tree.ui.theme

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * getThemeColors 函数单元测试
 */
@DisplayName("getThemeColors")
class GetThemeColorsTest {

    @Nested
    @DisplayName("DARK 模式")
    inner class DarkMode {

        @Test
        @DisplayName("始终返回暗色主题配色")
        fun alwaysReturnsDarkThemeColors() {
            val colorsSystemDark = getThemeColors(ThemeMode.DARK, isSystemDark = true)
            val colorsSystemLight = getThemeColors(ThemeMode.DARK, isSystemDark = false)

            assertThat(colorsSystemDark).isEqualTo(ThemeColors.fromDark())
            assertThat(colorsSystemLight).isEqualTo(ThemeColors.fromDark())
        }
    }

    @Nested
    @DisplayName("LIGHT 模式")
    inner class LightMode {

        @Test
        @DisplayName("始终返回亮色主题配色")
        fun alwaysReturnsLightThemeColors() {
            val colorsSystemDark = getThemeColors(ThemeMode.LIGHT, isSystemDark = true)
            val colorsSystemLight = getThemeColors(ThemeMode.LIGHT, isSystemDark = false)

            assertThat(colorsSystemDark).isEqualTo(ThemeColors.fromLight())
            assertThat(colorsSystemLight).isEqualTo(ThemeColors.fromLight())
        }
    }

    @Nested
    @DisplayName("SYSTEM 模式")
    inner class SystemMode {

        @Test
        @DisplayName("系统暗色时返回暗色主题")
        fun returnsDarkWhenSystemIsDark() {
            val colors = getThemeColors(ThemeMode.SYSTEM, isSystemDark = true)
            assertThat(colors).isEqualTo(ThemeColors.fromDark())
        }

        @Test
        @DisplayName("系统亮色时返回亮色主题")
        fun returnsLightWhenSystemIsLight() {
            val colors = getThemeColors(ThemeMode.SYSTEM, isSystemDark = false)
            assertThat(colors).isEqualTo(ThemeColors.fromLight())
        }
    }

    @Nested
    @DisplayName("DYNAMIC 模式")
    inner class DynamicMode {

        @Test
        @DisplayName("系统暗色时返回暗色主题（作为回退）")
        fun returnsDarkWhenSystemIsDark() {
            val colors = getThemeColors(ThemeMode.DYNAMIC, isSystemDark = true)
            assertThat(colors).isEqualTo(ThemeColors.fromDark())
        }

        @Test
        @DisplayName("系统亮色时返回亮色主题（作为回退）")
        fun returnsLightWhenSystemIsLight() {
            val colors = getThemeColors(ThemeMode.DYNAMIC, isSystemDark = false)
            assertThat(colors).isEqualTo(ThemeColors.fromLight())
        }
    }
}
