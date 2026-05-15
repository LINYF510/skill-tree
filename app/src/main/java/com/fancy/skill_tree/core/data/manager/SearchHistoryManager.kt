package com.fancy.skill_tree.core.data.manager

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 搜索历史管理器
 * 最多保留 10 条搜索历史
 */
@Singleton
class SearchHistoryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)
    private val maxHistorySize = 10

    /**
     * 获取搜索历史
     */
    fun getHistory(): List<String> {
        val history = prefs.getString(KEY_HISTORY, "") ?: ""
        return if (history.isEmpty()) emptyList() else history.split(HISTORY_SEPARATOR)
    }

    /**
     * 添加搜索历史
     */
    fun addToHistory(query: String) {
        if (query.isBlank()) return

        val history = getHistory().toMutableList()
        history.remove(query) // 移除旧的重复项
        history.add(0, query) // 添加到最前面
        if (history.size > maxHistorySize) {
            history.removeAt(history.size - 1)
        }
        prefs.edit().putString(KEY_HISTORY, history.joinToString(HISTORY_SEPARATOR)).apply()
    }

    /**
     * 清除搜索历史
     */
    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    companion object {
        private const val KEY_HISTORY = "search_history"
        private const val HISTORY_SEPARATOR = "\n"
    }
}
