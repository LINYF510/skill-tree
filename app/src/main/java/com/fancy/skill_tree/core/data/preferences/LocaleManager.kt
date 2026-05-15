package com.fancy.skill_tree.core.data.preferences

import android.content.Context
import android.content.res.Configuration
import com.fancy.skill_tree.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 语言管理器
 * 管理应用内语言切换，持久化用户语言偏好
 */
@Singleton
class LocaleManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 当前语言标签
     * null 表示跟随系统
     */
    var languageTag: String?
        get() = prefs.getString(KEY_LANGUAGE, null)
        set(value) {
            prefs.edit().putString(KEY_LANGUAGE, value).apply()
        }

    /**
     * 获取当前 Locale
     * 如果用户未设置则跟随系统默认
     */
    fun getCurrentLocale(): Locale {
        val tag = languageTag
        return if (tag != null) Locale.forLanguageTag(tag) else Locale.getDefault()
    }

    /**
     * 将语言设置应用到 Context
     * 在 Application.attachBaseContext 和 onConfigurationChanged 中调用
     *
     * @param baseContext 原始 Context
     * @return 应用语言设置后的 Context
     */
    fun applyLocale(baseContext: Context): Context {
        val locale = getCurrentLocale()
        Locale.setDefault(locale)
        val config = Configuration(baseContext.resources.configuration)
        config.setLocale(locale)
        return baseContext.createConfigurationContext(config)
    }

    companion object {
        private const val PREFS_NAME = "locale"
        private const val KEY_LANGUAGE = "app_language"
        const val LANG_ZH = "zh"
        const val LANG_EN = "en"
        val LANG_SYSTEM: String? = null
    }
}
