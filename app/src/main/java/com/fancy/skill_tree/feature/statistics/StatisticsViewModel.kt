package com.fancy.skill_tree.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fancy.skill_tree.R
import com.fancy.skill_tree.core.data.exception.DataException
import com.fancy.skill_tree.core.data.manager.AchievementManager
import com.fancy.skill_tree.core.domain.model.Achievement
import com.fancy.skill_tree.core.domain.usecase.node.GetStatisticsUseCase
import com.fancy.skill_tree.core.ui.error.ErrorSeverity
import com.fancy.skill_tree.core.ui.error.ErrorStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 统计面板 ViewModel
 * 使用 ErrorStateManager 统一处理错误
 */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val getStatisticsUseCase: GetStatisticsUseCase,
    private val achievementManager: AchievementManager,
    private val errorStateManager: ErrorStateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadStatistics()
        loadAchievements()
    }

    /**
     * 加载统计数据
     */
    private fun loadStatistics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            getStatisticsUseCase()
                .catch { e ->
                    val message = handleFlowException(e)
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = message)
                    }
                }
                .collect { stats ->
                    _uiState.update {
                        it.copy(
                            statistics = stats,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    /**
     * 加载成就数据
     */
    private fun loadAchievements() {
        _uiState.update {
            it.copy(
                achievements = achievementManager.getAllAchievements(),
                unlockedAchievements = achievementManager.getAllUnlocked()
            )
        }
    }

    /**
     * 刷新成就数据
     */
    fun refreshAchievements() {
        loadAchievements()
    }

    /**
     * 检查成就是否已解锁
     */
    fun isAchievementUnlocked(achievement: Achievement): Boolean =
        achievementManager.isUnlocked(achievement)

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * 处理 Flow 中的异常，将 DataException 映射为用户可读消息
     * @param e Flow 中捕获的异常
     * @return 用户可读的错误消息
     */
    private fun handleFlowException(e: Throwable): String {
        return when (e) {
            is DataException -> {
                val uiError = errorStateManager.mapDataException(e)
                when (uiError.severity) {
                    ErrorSeverity.CRITICAL -> errorStateManager.showErrorDialog(uiError.titleResId, uiError.messageResId, uiError.messageArgs)
                    else -> errorStateManager.showSnackbar(uiError.messageResId, uiError.messageArgs, isError = true)
                }
                ""
            }
            else -> {
                errorStateManager.showSnackbar(R.string.common_unknown_error, isError = true)
                ""
            }
        }
    }
}
