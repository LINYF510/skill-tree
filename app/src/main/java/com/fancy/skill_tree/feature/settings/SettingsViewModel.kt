package com.fancy.skill_tree.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fancy.skill_tree.R
import com.fancy.skill_tree.core.data.preferences.UserPreferences
import com.fancy.skill_tree.core.domain.common.Outcome
import com.fancy.skill_tree.core.domain.usecase.node.ClearAllDataUseCase
import com.fancy.skill_tree.core.domain.usecase.node.CreateNodeUseCase
import com.fancy.skill_tree.core.domain.usecase.node.ExportToMarkdownUseCase
import com.fancy.skill_tree.core.ui.error.ErrorSeverity
import com.fancy.skill_tree.core.ui.error.ErrorStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 设置页 ViewModel
 * 使用 ErrorStateManager 统一处理错误
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exportToMarkdownUseCase: ExportToMarkdownUseCase,
    private val createNodeUseCase: CreateNodeUseCase,
    private val clearAllDataUseCase: ClearAllDataUseCase,
    private val errorStateManager: ErrorStateManager,
    val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /**
     * 导出为 Markdown
     * @param onSuccess 成功回调，参数是 Markdown 字符串
     */
    fun exportMarkdown(onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = exportToMarkdownUseCase()) {
                is Outcome.Success -> {
                    onSuccess(result.data)
                }

                is Outcome.Error -> {
                    handleDomainException(result.exception)
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    /**
     * 加载示例数据
     */
    fun loadSampleData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            createNodeUseCase(
                title = "技能树",
                nodeType = "ABILITY",
                parentId = null,
                content = null,
                customId = "sample-root"
            )

            createNodeUseCase(
                title = "编程",
                nodeType = "ABILITY",
                parentId = "sample-root",
                content = null,
                customId = "sample-programming"
            )

            createNodeUseCase(
                title = "Python",
                nodeType = "ABILITY",
                parentId = "sample-programming",
                content = "## Python 学习路线\n\n- 基础语法\n- 数据结构\n- 面向对象编程\n- 异步编程",
                customId = "sample-python"
            )

            createNodeUseCase(
                title = "Kotlin",
                nodeType = "ABILITY",
                parentId = "sample-programming",
                content = "## Kotlin 学习路线\n\n- 协程\n- Compose\n- KMP",
                customId = "sample-kotlin"
            )

            createNodeUseCase(
                title = "Web开发",
                nodeType = "ABILITY",
                parentId = "sample-programming",
                content = null,
                customId = "sample-web"
            )

            createNodeUseCase(
                title = "设计",
                nodeType = "RESOURCE",
                parentId = "sample-root",
                content = "## 设计资源\n\n- Figma\n- 色彩理论\n- 排版原则",
                customId = "sample-design"
            )

            createNodeUseCase(
                title = "数据分析",
                nodeType = "RESOURCE",
                parentId = "sample-python",
                content = null,
                customId = "sample-data-analysis"
            )

            _uiState.update { it.copy(isLoading = false) }
            errorStateManager.showSnackbar(R.string.settings_load_sample, isError = false)
        }
    }

    /**
     * 清除所有数据
     */
    fun clearAllData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = clearAllDataUseCase()) {
                is Outcome.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                }

                is Outcome.Error -> {
                    handleDomainException(result.exception)
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * 处理 DomainException，根据严重程度选择 Snackbar 或 Dialog 展示
     * @param e 领域层异常
     */
    private fun handleDomainException(e: com.fancy.skill_tree.core.domain.common.DomainException) {
        val uiError = errorStateManager.mapDomainException(e)
        when (uiError.severity) {
            ErrorSeverity.CRITICAL -> errorStateManager.showErrorDialog(uiError.titleResId, uiError.messageResId, uiError.messageArgs)
            else -> errorStateManager.showSnackbar(uiError.messageResId, uiError.messageArgs, isError = true)
        }
    }
}

/**
 * 设置页 UI 状态
 */
data class SettingsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
