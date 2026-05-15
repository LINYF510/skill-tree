package com.fancy.skill_tree

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.fancy.skill_tree.core.data.preferences.LocaleManager
import com.fancy.skill_tree.core.ui.render.PerformanceMonitor
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Skill-Tree 应用的 Hilt Application 类
 * 作为依赖注入的入口点，管理全局初始化和语言配置
 */
@HiltAndroidApp
class SkillTreeApplication : Application() {

    @Inject
    lateinit var localeManager: LocaleManager

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        PerformanceMonitor.isDebugEnabled = BuildConfig.DEBUG
        localeManager.applyLocale(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        localeManager.applyLocale(this)
    }
}
