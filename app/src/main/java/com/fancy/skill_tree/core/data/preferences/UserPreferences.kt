package com.fancy.skill_tree.core.data.preferences

import android.content.Context
import android.os.Build
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
     * 处理低版本设备回退：如果设备不支持动态颜色（API < 31），则回退到 SYSTEM 模式
     */
    var themeMode: ThemeMode
        get() {
            val name = prefs.getString(KEY_THEME, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
            val mode = try {
                ThemeMode.valueOf(name)
            } catch (_: IllegalArgumentException) {
                ThemeMode.DARK
            }
            return if (mode == ThemeMode.DYNAMIC && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                ThemeMode.SYSTEM
            } else {
                mode
            }
        }
        set(value) {
            prefs.edit().putString(KEY_THEME, value.name).apply()
        }

    companion object {
        private const val KEY_THEME = "theme_mode"
    }
}
