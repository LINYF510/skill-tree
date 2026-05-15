package com.fancy.skill_tree.feature.onboarding

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 引导状态管理器
 * 使用 SharedPreferences 持久化引导完成状态
 */
@Singleton
class OnboardingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)

    val isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_COMPLETED, false)

    val shouldShowOnboarding: Boolean
        get() = !isOnboardingCompleted

    fun completeOnboarding() {
        prefs.edit().putBoolean(KEY_COMPLETED, true).apply()
    }

    fun resetOnboarding() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_COMPLETED = "onboarding_completed"
    }
}
