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
        private const val KEY_VOICE_INPUT_COUNT = "voice_input_count"
        private const val KEY_DAILY_STREAK = "daily_streak"
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
     * 递增语音输入计数
     */
    fun incrementVoiceInputCount() {
        val count = prefs.getInt(KEY_VOICE_INPUT_COUNT, 0)
        prefs.edit().putInt(KEY_VOICE_INPUT_COUNT, count + 1).apply()
    }

    /**
     * 获取语音输入计数
     */
    fun getVoiceInputCount(): Int = prefs.getInt(KEY_VOICE_INPUT_COUNT, 0)

    /**
     * 更新每日连续天数并返回当前连续天数
     * 连续天数的计算逻辑：同一天不增加，隔天+1，中断则重置为1
     */
    fun updateDailyStreak(): Int {
        val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val lastDate = prefs.getString(KEY_LAST_ACTIVITY_DATE, null)

        val streak = if (lastDate == null) {
            1
        } else {
            val lastActivity = java.time.LocalDate.parse(lastDate, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(lastActivity, java.time.LocalDate.now())
            when {
                daysDiff == 0L -> prefs.getInt(KEY_DAILY_STREAK, 1)
                daysDiff == 1L -> (prefs.getInt(KEY_DAILY_STREAK, 0) + 1)
                else -> 1
            }
        }

        prefs.edit()
            .putString(KEY_LAST_ACTIVITY_DATE, today)
            .putInt(KEY_DAILY_STREAK, streak)
            .apply()

        return streak
    }

    /**
     * 获取当前连续天数
     */
    fun getDailyStreak(): Int = prefs.getInt(KEY_DAILY_STREAK, 0)

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
