package com.fancy.skill_tree.core.data.preferences

import android.content.Context
import com.fancy.skill_tree.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户偏好设置管理器
 * 持久化存储主题模式等用户偏好
 */
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    /**
     * 当前主题模式
     * 读取时从 SharedPreferences 获取，写入时立即持久化
     */
    var themeMode: ThemeMode
        get() {
            val name = prefs.getString(KEY_THEME, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
            return try {
                ThemeMode.valueOf(name)
            } catch (_: IllegalArgumentException) {
                ThemeMode.DARK
            }
        }
        set(value) {
            prefs.edit().putString(KEY_THEME, value.name).apply()
        }

    companion object {
        private const val KEY_THEME = "theme_mode"
    }
}
