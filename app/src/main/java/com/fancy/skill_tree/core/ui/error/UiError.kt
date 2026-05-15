package com.fancy.skill_tree.core.ui.error

import androidx.annotation.StringRes

/**
 * UI 层可展示的错误消息
 * 使用 string resource ID 支持国际化
 *
 * @param titleResId 错误标题字符串资源 ID
 * @param messageResId 错误详情字符串资源 ID
 * @param messageArgs 消息格式化参数
 * @param action 可选的错误操作（如重试）
 * @param severity 错误严重程度
 */
data class UiError(
    @StringRes val titleResId: Int,
    @StringRes val messageResId: Int,
    val messageArgs: Array<Any> = emptyArray(),
    val action: ErrorAction? = null,
    val severity: ErrorSeverity = ErrorSeverity.ERROR
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UiError) return false
        return titleResId == other.titleResId &&
            messageResId == other.messageResId &&
            messageArgs.contentEquals(other.messageArgs) &&
            action == other.action &&
            severity == other.severity
    }

    override fun hashCode(): Int {
        var result = titleResId
        result = 31 * result + messageResId
        result = 31 * result + messageArgs.contentHashCode()
        result = 31 * result + (action?.hashCode() ?: 0)
        result = 31 * result + severity.hashCode()
        return result
    }
}

/**
 * 错误严重程度枚举
 */
enum class ErrorSeverity { INFO, WARNING, ERROR, CRITICAL }

/**
 * 错误操作数据类
 * @param labelResId 操作按钮文本资源 ID
 * @param action 点击操作后的回调
 */
data class ErrorAction(
    @StringRes val labelResId: Int,
    val action: () -> Unit
)

/**
 * Snackbar 事件数据类
 * @param messageResId 消息内容资源 ID
 * @param messageArgs 消息格式化参数
 * @param isError 是否为错误消息
 * @param actionLabelResId 可选的操作按钮文本资源 ID
 * @param action 可选的操作回调
 */
data class SnackbarEvent(
    @StringRes val messageResId: Int,
    val messageArgs: Array<Any> = emptyArray(),
    val isError: Boolean = false,
    @StringRes val actionLabelResId: Int? = null,
    val action: (() -> Unit)? = null
)

/**
 * Dialog 事件数据类
 * @param titleResId 对话框标题资源 ID
 * @param messageResId 对话框内容资源 ID
 * @param messageArgs 消息格式化参数
 * @param confirmLabelResId 确认按钮文本资源 ID
 * @param onConfirm 确认回调
 */
data class DialogEvent(
    @StringRes val titleResId: Int,
    @StringRes val messageResId: Int,
    val messageArgs: Array<Any> = emptyArray(),
    @StringRes val confirmLabelResId: Int = com.fancy.skill_tree.R.string.common_ok,
    val onConfirm: (() -> Unit)? = null
)
