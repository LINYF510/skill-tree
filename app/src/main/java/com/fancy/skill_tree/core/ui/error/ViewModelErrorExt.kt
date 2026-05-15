package com.fancy.skill_tree.core.ui.error

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fancy.skill_tree.core.domain.common.Outcome
import kotlinx.coroutines.launch

/**
 * ViewModel 扩展：安全执行 UseCase 并自动处理错误
 * 根据 UiError 的严重程度自动选择 Snackbar 或 Dialog 展示方式
 * @param errorStateManager 全局错误状态管理器
 * @param block 返回 Outcome 的挂起操作
 * @param onSuccess 操作成功后的回调
 */
fun ViewModel.executeWithErrorHandling(
    errorStateManager: ErrorStateManager,
    block: suspend () -> Outcome<*>,
    onSuccess: (() -> Unit)? = null
) {
    viewModelScope.launch {
        when (val result = block()) {
            is Outcome.Success -> onSuccess?.invoke()
            is Outcome.Error -> {
                val uiError = errorStateManager.mapDomainException(result.exception)
                when (uiError.severity) {
                    ErrorSeverity.CRITICAL -> errorStateManager.showErrorDialog(
                        uiError.titleResId, uiError.messageResId, uiError.messageArgs
                    )
                    else -> errorStateManager.showSnackbar(
                        uiError.messageResId, uiError.messageArgs, isError = true
                    )
                }
            }
        }
    }
}
