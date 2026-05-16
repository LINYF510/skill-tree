package com.fancy.skill_tree.feature.node

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fancy.skill_tree.R
import com.fancy.skill_tree.core.data.exception.DataException
import com.fancy.skill_tree.core.data.repository.NodeLinkWithTarget
import com.fancy.skill_tree.core.domain.common.Outcome
import com.fancy.skill_tree.core.domain.entity.AttachmentEntity
import com.fancy.skill_tree.core.domain.entity.TagEntity
import com.fancy.skill_tree.core.domain.usecase.node.AddAttachmentUseCase
import com.fancy.skill_tree.core.domain.usecase.node.AssignTagToNodeUseCase
import com.fancy.skill_tree.core.domain.usecase.node.ConfirmLinkUseCase
import com.fancy.skill_tree.core.domain.usecase.node.CreateLinkUseCase
import com.fancy.skill_tree.core.domain.usecase.node.CreateTagUseCase
import com.fancy.skill_tree.core.domain.usecase.node.DeleteAttachmentUseCase
import com.fancy.skill_tree.core.domain.usecase.node.DeleteLinkUseCase
import com.fancy.skill_tree.core.domain.usecase.node.DeleteNodeUseCase
import com.fancy.skill_tree.core.domain.usecase.node.GetAllNodesUseCase
import com.fancy.skill_tree.core.domain.usecase.node.GetAllTagsUseCase
import com.fancy.skill_tree.core.domain.usecase.node.GetAttachmentsForNodeUseCase
import com.fancy.skill_tree.core.domain.usecase.node.GetLinksForNodeUseCase
import com.fancy.skill_tree.core.domain.usecase.node.GetNodeDetailUseCase
import com.fancy.skill_tree.core.domain.usecase.node.GetTagsForNodeUseCase
import com.fancy.skill_tree.core.domain.usecase.node.RemoveTagFromNodeUseCase
import com.fancy.skill_tree.core.domain.usecase.node.UpdateNodeUseCase
import com.fancy.skill_tree.core.ui.error.ErrorSeverity
import com.fancy.skill_tree.core.ui.error.ErrorStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 节点详情界面 ViewModel
 * 负责加载和管理单个节点的详情数据，使用 ErrorStateManager 统一处理错误
 */
@HiltViewModel
class NodeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getNodeDetailUseCase: GetNodeDetailUseCase,
    private val updateNodeUseCase: UpdateNodeUseCase,
    private val deleteNodeUseCase: DeleteNodeUseCase,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val getTagsForNodeUseCase: GetTagsForNodeUseCase,
    private val createTagUseCase: CreateTagUseCase,
    private val assignTagToNodeUseCase: AssignTagToNodeUseCase,
    private val removeTagFromNodeUseCase: RemoveTagFromNodeUseCase,
    private val getLinksForNodeUseCase: GetLinksForNodeUseCase,
    private val getAllNodesUseCase: GetAllNodesUseCase,
    private val createLinkUseCase: CreateLinkUseCase,
    private val deleteLinkUseCase: DeleteLinkUseCase,
    private val confirmLinkUseCase: ConfirmLinkUseCase,
    private val getAttachmentsForNodeUseCase: GetAttachmentsForNodeUseCase,
    private val addAttachmentUseCase: AddAttachmentUseCase,
    private val deleteAttachmentUseCase: DeleteAttachmentUseCase,
    private val errorStateManager: ErrorStateManager
) : ViewModel() {

    private val nodeId: String = savedStateHandle.get<String>("nodeId")
        ?: throw IllegalArgumentException("nodeId must not be null")

    private val _uiState = MutableStateFlow(NodeDetailUiState())
    val uiState: StateFlow<NodeDetailUiState> = _uiState.asStateFlow()

    init {
        loadNodeDetail()
    }

    /**
     * 加载节点详情和标签
     */
    private fun loadNodeDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            combine(
                getNodeDetailUseCase(nodeId),
                getAllTagsUseCase(),
                getAllNodesUseCase(),
                getLinksForNodeUseCase(nodeId),
                getAttachmentsForNodeUseCase(nodeId)
            ) { nodeResult, allTags, allNodes, linksWithTarget, attachments ->
                FiveTuple(nodeResult, allTags, allNodes, linksWithTarget, attachments)
            }
            .catch { e ->
                val message = handleFlowException(e)
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = message)
                }
            }
            .collect { (nodeResult, allTags, allNodes, linksWithTarget, attachments) ->
                when (nodeResult) {
                    is Outcome.Success -> {
                        _uiState.update {
                            it.copy(
                                node = nodeResult.data.node,
                                tags = nodeResult.data.tags,
                                allTags = allTags,
                                allNodes = allNodes,
                                linksWithTarget = linksWithTarget,
                                attachments = attachments,
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                    }
                    is Outcome.Error -> {
                        handleDomainException(nodeResult.exception)
                        _uiState.update {
                            it.copy(isLoading = false)
                        }
                    }
                }
            }
        }
    }

    /**
     * 为节点添加标签
     * @param tag 要添加的标签
     */
    fun addTag(tag: TagEntity) {
        viewModelScope.launch {
            when (val result = assignTagToNodeUseCase(nodeId, tag.id)) {
                is Outcome.Success -> { }
                is Outcome.Error -> handleDomainException(result.exception)
            }
        }
    }

    /**
     * 从节点移除标签
     * @param tag 要移除的标签
     */
    fun removeTag(tag: TagEntity) {
        viewModelScope.launch {
            when (val result = removeTagFromNodeUseCase(nodeId, tag.id)) {
                is Outcome.Success -> { }
                is Outcome.Error -> handleDomainException(result.exception)
            }
        }
    }

    /**
     * 创建新标签并分配给节点
     * @param tagName 标签名称
     */
    fun createAndAssignTag(tagName: String) {
        viewModelScope.launch {
            when (val result = createTagUseCase(tagName)) {
                is Outcome.Success -> {
                    assignTagToNodeUseCase(nodeId, result.data.id)
                }
                is Outcome.Error -> {
                    handleDomainException(result.exception)
                }
            }
        }
    }

    /**
     * 切换编辑/查看模式
     */
    fun toggleEditing() {
        _uiState.update { it.copy(isEditing = !it.isEditing) }
    }

    /**
     * 保存编辑后的内容
     * @param content 编辑后的 Markdown 内容
     */
    fun saveContent(content: String) {
        val currentNode = _uiState.value.node ?: return
        viewModelScope.launch {
            when (val result = updateNodeUseCase(
                currentNode.id,
                currentNode.title,
                content,
                currentNode.nodeType
            )) {
                is Outcome.Success -> {
                    _uiState.update { it.copy(isEditing = false) }
                }
                is Outcome.Error -> {
                    handleDomainException(result.exception)
                }
            }
        }
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * 删除当前节点
     * @param onDeleted 删除成功后回调
     */
    fun deleteNode(onDeleted: () -> Unit) {
        viewModelScope.launch {
            when (val result = deleteNodeUseCase(nodeId)) {
                is Outcome.Success -> {
                    onDeleted()
                }
                is Outcome.Error -> {
                    handleDomainException(result.exception)
                }
            }
        }
    }

    /**
     * 创建链接
     * @param targetId 目标节点 ID
     */
    fun createLink(targetId: String) {
        viewModelScope.launch {
            when (val result = createLinkUseCase(nodeId, targetId)) {
                is Outcome.Success -> { }
                is Outcome.Error -> handleDomainException(result.exception)
            }
        }
    }

    /**
     * 删除链接
     * @param linkId 链接 ID
     */
    fun deleteLink(linkId: String) {
        viewModelScope.launch {
            when (val result = deleteLinkUseCase(linkId)) {
                is Outcome.Success -> { }
                is Outcome.Error -> handleDomainException(result.exception)
            }
        }
    }

    /**
     * 确认链接
     * @param linkId 链接 ID
     */
    fun confirmLink(linkId: String) {
        viewModelScope.launch {
            when (val result = confirmLinkUseCase(linkId)) {
                is Outcome.Success -> { }
                is Outcome.Error -> handleDomainException(result.exception)
            }
        }
    }

    /**
     * 获取已确认的链接
     */
    fun getConfirmedLinks(): List<NodeLinkWithTarget> {
        return getLinksForNodeUseCase.getConfirmedLinks(_uiState.value.linksWithTarget)
    }

    /**
     * 获取 AI 推荐的链接
     */
    fun getSuggestedLinks(): List<NodeLinkWithTarget> {
        return getLinksForNodeUseCase.getSuggestedLinks(_uiState.value.linksWithTarget)
    }

    /**
     * 添加附件
     * @param uri 文件 URI
     * @param fileName 文件名
     * @param mimeType MIME 类型
     * @param fileSize 文件大小
     */
    fun addAttachment(uri: Uri, fileName: String, mimeType: String?, fileSize: Long?) {
        viewModelScope.launch {
            when (val result = addAttachmentUseCase(nodeId, uri, fileName, mimeType, fileSize)) {
                is Outcome.Success -> { }
                is Outcome.Error -> handleDomainException(result.exception)
            }
        }
    }

    /**
     * 删除附件
     * @param attachmentId 附件 ID
     */
    fun deleteAttachment(attachmentId: String) {
        viewModelScope.launch {
            when (val result = deleteAttachmentUseCase(attachmentId)) {
                is Outcome.Success -> { }
                is Outcome.Error -> handleDomainException(result.exception)
            }
        }
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

/**
 * 五元素元组辅助类
 */
private data class FiveTuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
