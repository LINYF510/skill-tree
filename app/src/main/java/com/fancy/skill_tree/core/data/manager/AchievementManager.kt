package com.fancy.skill_tree.core.data.manager

import android.content.Context
import android.content.SharedPreferences
import com.fancy.skill_tree.core.domain.model.Achievement
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 成就管理器
 * 负责成就状态的持久化和查询
 */
@Singleton
class AchievementManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        private const val PREFS_NAME = "achievements"
        private const val KEY_LAST_ACTIVITY_DATE = "last_activity_date"
    }

    private val prefs: SharedPreferences
        get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 检查成就是否已解锁
     */
    fun isUnlocked(achievement: Achievement): Boolean =
        prefs.getBoolean(achievement.id, false)

    /**
     * 获取所有已解锁的成就
     */
    fun getAllUnlocked(): List<Achievement> =
        Achievement.entries.filter { isUnlocked(it) }

    /**
     * 获取所有成就（包括未解锁的）
     */
    fun getAllAchievements(): List<Achievement> =
        Achievement.entries.toList()

    /**
     * 解锁成就（不检查条件）
     */
    fun unlockAchievement(achievement: Achievement): Boolean {
        if (isUnlocked(achievement)) return false
        prefs.edit().putBoolean(achievement.id, true).apply()
        return true
    }

    /**
     * 更新最后活动日期（用于连续天数计算）
     */
    fun updateLastActivityDate(dateString: String) {
        prefs.edit().putString(KEY_LAST_ACTIVITY_DATE, dateString).apply()
    }

    /**
     * 获取最后活动日期
     */
    fun getLastActivityDate(): String? =
        prefs.getString(KEY_LAST_ACTIVITY_DATE, null)

    /**
     * 重置所有成就（仅用于测试）
     */
    fun resetAllAchievements() {
        val editor = prefs.edit()
        Achievement.entries.forEach { achievement ->
            editor.putBoolean(achievement.id, false)
        }
        editor.putString(KEY_LAST_ACTIVITY_DATE, null)
        editor.apply()
    }
}
