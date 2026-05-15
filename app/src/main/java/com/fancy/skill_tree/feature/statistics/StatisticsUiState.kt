package com.fancy.skill_tree.feature.statistics

import com.fancy.skill_tree.core.domain.model.Achievement
import com.fancy.skill_tree.core.domain.usecase.node.GetStatisticsUseCase

/**
 * 统计面板 UI 状态
 */
data class StatisticsUiState(
    val isLoading: Boolean = false,
    val statistics: GetStatisticsUseCase.Statistics? = null,
    val achievements: List<Achievement> = emptyList(),
    val unlockedAchievements: List<Achievement> = emptyList(),
    val errorMessage: String? = null
)
