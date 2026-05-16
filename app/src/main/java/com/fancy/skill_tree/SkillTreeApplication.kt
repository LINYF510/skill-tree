package com.fancy.skill_tree

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Process
import android.database.sqlite.SQLiteException
import com.fancy.skill_tree.core.data.database.DatabaseRecoveryManager
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

    @Inject
    lateinit var databaseRecoveryManager: DatabaseRecoveryManager

    private var defaultExceptionHandler: Thread.UncaughtExceptionHandler? = null

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        PerformanceMonitor.isDebugEnabled = BuildConfig.DEBUG
        localeManager.applyLocale(this)
        registerDatabaseRecoveryHandler()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        localeManager.applyLocale(this)
    }

    /**
     * 注册数据库损坏恢复的全局异常处理器
     * 当捕获到 SQLiteException 时尝试恢复数据库并重启应用
     */
    private fun registerDatabaseRecoveryHandler() {
        defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (throwable is SQLiteException || throwable?.cause is SQLiteException) {
                databaseRecoveryManager.recoverDatabase()
                val intent = packageManager.getLaunchIntentForPackage(packageName)
                intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                Process.killProcess(Process.myPid())
            } else {
                defaultExceptionHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
