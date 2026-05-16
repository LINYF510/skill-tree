package com.fancy.skill_tree.core.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.fancy.skill_tree.ui.theme.ThemeMode
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * UserPreferences 单元测试
 */
@DisplayName("UserPreferences")
class UserPreferencesTest {

    private val context = mockk<Context>(relaxed = true)
    private val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)

    private lateinit var userPreferences: UserPreferences

    @BeforeEach
    fun setUp() {
        every { context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
    }

    @Nested
    @DisplayName("themeMode getter")
    inner class ThemeModeGetter {

        @Test
        @DisplayName("默认返回 DARK 模式")
        fun defaultReturnsDark() {
            every { sharedPreferences.getString("theme_mode", ThemeMode.DARK.name) } returns null

            userPreferences = UserPreferences(context)

            assertThat(userPreferences.themeMode).isEqualTo(ThemeMode.DARK)
        }

        @Test
        @DisplayName("返回保存的 DARK 模式")
        fun returnsSavedDark() {
            every { sharedPreferences.getString("theme_mode", ThemeMode.DARK.name) } returns ThemeMode.DARK.name

            userPreferences = UserPreferences(context)

            assertThat(userPreferences.themeMode).isEqualTo(ThemeMode.DARK)
        }

        @Test
        @DisplayName("返回保存的 LIGHT 模式")
        fun returnsSavedLight() {
            every { sharedPreferences.getString("theme_mode", ThemeMode.DARK.name) } returns ThemeMode.LIGHT.name

            userPreferences = UserPreferences(context)

            assertThat(userPreferences.themeMode).isEqualTo(ThemeMode.LIGHT)
        }

        @Test
        @DisplayName("返回保存的 SYSTEM 模式")
        fun returnsSavedSystem() {
            every { sharedPreferences.getString("theme_mode", ThemeMode.DARK.name) } returns ThemeMode.SYSTEM.name

            userPreferences = UserPreferences(context)

            assertThat(userPreferences.themeMode).isEqualTo(ThemeMode.SYSTEM)
        }

        @Test
        @DisplayName("返回保存的 DYNAMIC 模式")
        fun returnsSavedDynamic() {
            every { sharedPreferences.getString("theme_mode", ThemeMode.DARK.name) } returns ThemeMode.DYNAMIC.name

            userPreferences = UserPreferences(context)

            // 注意：在低版本设备上 DYNAMIC 会回退到 SYSTEM
            // 这个测试在 API 31+ 设备上返回 DYNAMIC，在 API 30 及以下返回 SYSTEM
            val mode = userPreferences.themeMode
            assertThat(mode == ThemeMode.DYNAMIC || mode == ThemeMode.SYSTEM).isTrue()
        }

        @Test
        @DisplayName("无效值时返回 DARK 模式")
        fun invalidValueReturnsDark() {
            every { sharedPreferences.getString("theme_mode", ThemeMode.DARK.name) } returns "INVALID_MODE"

            userPreferences = UserPreferences(context)

            assertThat(userPreferences.themeMode).isEqualTo(ThemeMode.DARK)
        }
    }

    @Nested
    @DisplayName("themeMode setter")
    inner class ThemeModeSetter {

        @Test
        @DisplayName("保存 DARK 模式到 SharedPreferences")
        fun savesDarkMode() {
            every { sharedPreferences.getString("theme_mode", any()) } returns ThemeMode.DARK.name

            userPreferences = UserPreferences(context)
            userPreferences.themeMode = ThemeMode.DARK

            verify { editor.putString("theme_mode", ThemeMode.DARK.name) }
            verify { editor.apply() }
        }

        @Test
        @DisplayName("保存 LIGHT 模式到 SharedPreferences")
        fun savesLightMode() {
            every { sharedPreferences.getString("theme_mode", any()) } returns ThemeMode.DARK.name

            userPreferences = UserPreferences(context)
            userPreferences.themeMode = ThemeMode.LIGHT

            verify { editor.putString("theme_mode", ThemeMode.LIGHT.name) }
            verify { editor.apply() }
        }

        @Test
        @DisplayName("保存 SYSTEM 模式到 SharedPreferences")
        fun savesSystemMode() {
            every { sharedPreferences.getString("theme_mode", any()) } returns ThemeMode.DARK.name

            userPreferences = UserPreferences(context)
            userPreferences.themeMode = ThemeMode.SYSTEM

            verify { editor.putString("theme_mode", ThemeMode.SYSTEM.name) }
            verify { editor.apply() }
        }

        @Test
        @DisplayName("保存 DYNAMIC 模式到 SharedPreferences")
        fun savesDynamicMode() {
            every { sharedPreferences.getString("theme_mode", any()) } returns ThemeMode.DARK.name

            userPreferences = UserPreferences(context)
            userPreferences.themeMode = ThemeMode.DYNAMIC

            verify { editor.putString("theme_mode", ThemeMode.DYNAMIC.name) }
            verify { editor.apply() }
        }
    }
}
