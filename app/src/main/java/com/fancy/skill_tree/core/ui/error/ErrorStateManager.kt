package com.fancy.skill_tree.core.ui.error

import com.fancy.skill_tree.R
import com.fancy.skill_tree.core.data.exception.DataException
import com.fancy.skill_tree.core.domain.common.DomainException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全局错误状态管理器
 * 在 Composable 根层级收集并展示错误，提供 Snackbar 和 Dialog 两种展示方式
 * 所有错误消息使用 string resource ID 支持国际化
 */
@Singleton
class ErrorStateManager @Inject constructor() {

    private val _snackbarEvents = MutableSharedFlow<SnackbarEvent>(extraBufferCapacity = 5)
    val snackbarEvents: SharedFlow<SnackbarEvent> = _snackbarEvents.asSharedFlow()

    private val _dialogEvents = MutableSharedFlow<DialogEvent>(extraBufferCapacity = 3)
    val dialogEvents: SharedFlow<DialogEvent> = _dialogEvents.asSharedFlow()

    /**
     * 显示 Snackbar 消息
     * @param messageResId 消息字符串资源 ID
     * @param messageArgs 消息格式化参数
     * @param isError 是否为错误消息
     * @param actionLabelResId 可选的操作按钮文本资源 ID
     * @param action 可选的操作回调
     */
    fun showSnackbar(
        messageResId: Int,
        messageArgs: Array<Any> = emptyArray(),
        isError: Boolean = false,
        actionLabelResId: Int? = null,
        action: (() -> Unit)? = null
    ) {
        _snackbarEvents.tryEmit(SnackbarEvent(messageResId, messageArgs, isError, actionLabelResId, action))
    }

    /**
     * 显示错误对话框
     * @param titleResId 对话框标题资源 ID
     * @param messageResId 对话框内容资源 ID
     * @param messageArgs 消息格式化参数
     * @param confirmLabelResId 确认按钮文本资源 ID
     * @param onConfirm 确认回调
     */
    fun showErrorDialog(
        titleResId: Int,
        messageResId: Int,
        messageArgs: Array<Any> = emptyArray(),
        confirmLabelResId: Int = R.string.common_ok,
        onConfirm: (() -> Unit)? = null
    ) {
        _dialogEvents.tryEmit(DialogEvent(titleResId, messageResId, messageArgs, confirmLabelResId, onConfirm))
    }

    /**
     * 从 DomainException 映射到用户可读的 UiError
     * @param e 领域层异常
     * @return 映射后的 UI 错误信息
     */
    fun mapDomainException(e: DomainException): UiError {
        return when (e) {
            is DomainException.NodeNotFound -> UiError(
                R.string.error_node_not_found,
                R.string.error_node_not_found,
                severity = ErrorSeverity.WARNING
            )
            is DomainException.ParentNodeNotFound -> UiError(
                R.string.error_parent_not_found,
                R.string.error_parent_not_found,
                severity = ErrorSeverity.WARNING
            )
            is DomainException.CircularReference -> UiError(
                R.string.error_circular_reference,
                R.string.error_circular_reference,
                severity = ErrorSeverity.WARNING
            )
            is DomainException.MaxChildrenExceeded -> UiError(
                R.string.error_max_children,
                R.string.error_max_children,
                severity = ErrorSeverity.WARNING
            )
            is DomainException.StorageError -> UiError(
                R.string.error_storage,
                R.string.error_storage,
                severity = ErrorSeverity.ERROR
            )
            is DomainException.ValidationError -> UiError(
                R.string.error_validation,
                R.string.error_validation,
                severity = ErrorSeverity.WARNING
            )
            is DomainException.InvalidNodeType -> UiError(
                R.string.error_invalid_node_type,
                R.string.error_invalid_node_type,
                severity = ErrorSeverity.WARNING
            )
            is DomainException.TagAlreadyExists -> UiError(
                R.string.error_tag_exists,
                R.string.error_tag_exists,
                severity = ErrorSeverity.INFO
            )
            is DomainException.LinkAlreadyExists -> UiError(
                R.string.error_link_exists,
                R.string.error_link_exists,
                severity = ErrorSeverity.INFO
            )
            is DomainException.SelfLinkNotAllowed -> UiError(
                R.string.error_self_link,
                R.string.error_self_link,
                severity = ErrorSeverity.WARNING
            )
        }
    }

    /**
     * 从 DataException 映射到用户可读的 UiError
     * @param e 数据层异常
     * @return 映射后的 UI 错误信息
     */
    fun mapDataException(e: DataException): UiError {
        return when (e) {
            is DataException.DatabaseError -> UiError(
                R.string.error_database,
                R.string.error_database,
                action = ErrorAction(R.string.common_retry) { },
                severity = ErrorSeverity.ERROR
            )
            is DataException.FileNotFound -> UiError(
                R.string.error_file_not_found,
                R.string.error_file_not_found,
                severity = ErrorSeverity.WARNING
            )
            is DataException.StorageFull -> UiError(
                R.string.error_storage_full,
                R.string.error_storage_full,
                severity = ErrorSeverity.CRITICAL
            )
            is DataException.IoError -> UiError(
                R.string.error_io,
                R.string.error_io,
                severity = ErrorSeverity.ERROR
            )
        }
    }
}
