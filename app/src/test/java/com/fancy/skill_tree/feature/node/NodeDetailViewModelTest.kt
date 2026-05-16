package com.fancy.skill_tree.feature.node

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.fancy.skill_tree.R
import com.fancy.skill_tree.core.data.repository.SkillNodeWithTags
import com.fancy.skill_tree.core.domain.common.DomainException
import com.fancy.skill_tree.core.domain.common.Outcome
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
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
import com.fancy.skill_tree.core.ui.error.UiError
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("NodeDetailViewModel")
@OptIn(ExperimentalCoroutinesApi::class)
class NodeDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getNodeDetailUseCase = mockk<GetNodeDetailUseCase>()
    private val updateNodeUseCase = mockk<UpdateNodeUseCase>()
    private val deleteNodeUseCase = mockk<DeleteNodeUseCase>()
    private val getAllTagsUseCase = mockk<GetAllTagsUseCase>()
    private val getTagsForNodeUseCase = mockk<GetTagsForNodeUseCase>()
    private val createTagUseCase = mockk<CreateTagUseCase>()
    private val assignTagToNodeUseCase = mockk<AssignTagToNodeUseCase>()
    private val removeTagFromNodeUseCase = mockk<RemoveTagFromNodeUseCase>()
    private val getLinksForNodeUseCase = mockk<GetLinksForNodeUseCase>()
    private val getAllNodesUseCase = mockk<GetAllNodesUseCase>()
    private val createLinkUseCase = mockk<CreateLinkUseCase>()
    private val deleteLinkUseCase = mockk<DeleteLinkUseCase>()
    private val confirmLinkUseCase = mockk<ConfirmLinkUseCase>()
    private val getAttachmentsForNodeUseCase = mockk<GetAttachmentsForNodeUseCase>()
    private val addAttachmentUseCase = mockk<AddAttachmentUseCase>()
    private val deleteAttachmentUseCase = mockk<DeleteAttachmentUseCase>()
    private val errorStateManager = mockk<ErrorStateManager>(relaxed = true)

    private lateinit var viewModel: NodeDetailViewModel

    private val testNode = SkillNodeEntity(
        id = "test-node-id",
        parentId = null,
        title = "Test Node",
        nodeType = "ABILITY",
        level = 1,
        sortOrder = 0,
        isExpanded = true,
        createdAt = 1000L,
        updatedAt = 1000L
    )

    private val testTag = TagEntity(
        id = "tag1",
        name = "Kotlin",
        color = "#FF0000"
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        clearAllMocks()

        every { getNodeDetailUseCase(any()) } returns flowOf(Outcome.Success(SkillNodeWithTags(testNode, emptyList())))
        every { getAllTagsUseCase() } returns flowOf(emptyList())
        every { getAllNodesUseCase() } returns flowOf(emptyList())
        every { getLinksForNodeUseCase(any()) } returns flowOf(emptyList())
        every { getAttachmentsForNodeUseCase(any()) } returns flowOf(emptyList())
        every { errorStateManager.mapDomainException(any()) } returns UiError(
            titleResId = R.string.common_error,
            messageResId = R.string.common_unknown_error,
            severity = ErrorSeverity.WARNING
        )

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("加载节点详情")
    inner class LoadNodeDetail {

        @Test
        @DisplayName("成功加载节点详情后 uiState.node 非空")
        fun loadNodeDetailSuccess() = runTest {
            val node = SkillNodeEntity(id = "node1", title = "Kotlin", nodeType = "ABILITY")
            every { getNodeDetailUseCase("node1") } returns flowOf(Outcome.Success(SkillNodeWithTags(node, emptyList())))
            every { getAllTagsUseCase() } returns flowOf(emptyList())
            every { getAllNodesUseCase() } returns flowOf(emptyList())
            every { getLinksForNodeUseCase("node1") } returns flowOf(emptyList())
            every { getAttachmentsForNodeUseCase("node1") } returns flowOf(emptyList())

            val savedStateHandle = SavedStateHandle(mapOf("nodeId" to "node1"))
            val vm = NodeDetailViewModel(
                savedStateHandle = savedStateHandle,
                getNodeDetailUseCase = getNodeDetailUseCase,
                updateNodeUseCase = updateNodeUseCase,
                deleteNodeUseCase = deleteNodeUseCase,
                getAllTagsUseCase = getAllTagsUseCase,
                getTagsForNodeUseCase = getTagsForNodeUseCase,
                createTagUseCase = createTagUseCase,
                assignTagToNodeUseCase = assignTagToNodeUseCase,
                removeTagFromNodeUseCase = removeTagFromNodeUseCase,
                getLinksForNodeUseCase = getLinksForNodeUseCase,
                getAllNodesUseCase = getAllNodesUseCase,
                createLinkUseCase = createLinkUseCase,
                deleteLinkUseCase = deleteLinkUseCase,
                confirmLinkUseCase = confirmLinkUseCase,
                getAttachmentsForNodeUseCase = getAttachmentsForNodeUseCase,
                addAttachmentUseCase = addAttachmentUseCase,
                deleteAttachmentUseCase = deleteAttachmentUseCase,
                errorStateManager = errorStateManager
            )
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(vm.uiState.value.node).isNotNull()
            assertThat(vm.uiState.value.node?.id).isEqualTo("node1")
            assertThat(vm.uiState.value.isLoading).isFalse()
        }

        @Test
        @DisplayName("节点不存在时触发错误处理")
        fun loadNodeDetailNotFound() = runTest {
            every { getNodeDetailUseCase(any()) } returns flowOf(Outcome.Error(DomainException.NodeNotFound("missing-id")))

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            verify { errorStateManager.mapDomainException(any<DomainException.NodeNotFound>()) }
            assertThat(vm.uiState.value.isLoading).isFalse()
        }
    }

    @Nested
    @DisplayName("标签管理")
    inner class TagManagement {

        @Test
        @DisplayName("addTag 调用 assignTagToNodeUseCase")
        fun addTagCallsAssignTagToNodeUseCase() = runTest {
            coEvery { assignTagToNodeUseCase(any(), any()) } returns Outcome.Success(Unit)

            viewModel.addTag(testTag)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { assignTagToNodeUseCase("test-node-id", "tag1") }
        }

        @Test
        @DisplayName("addTag 失败时调用 ErrorStateManager")
        fun addTagFailureCallsErrorStateManager() = runTest {
            val exception = DomainException.StorageError(RuntimeException("DB error"))
            coEvery { assignTagToNodeUseCase(any(), any()) } returns Outcome.Error(exception)

            viewModel.addTag(testTag)
            testDispatcher.scheduler.advanceUntilIdle()

            verify { errorStateManager.mapDomainException(exception) }
        }

        @Test
        @DisplayName("removeTag 调用 removeTagFromNodeUseCase")
        fun removeTagCallsRemoveTagFromNodeUseCase() = runTest {
            coEvery { removeTagFromNodeUseCase(any(), any()) } returns Outcome.Success(Unit)

            viewModel.removeTag(testTag)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { removeTagFromNodeUseCase("test-node-id", "tag1") }
        }

        @Test
        @DisplayName("removeTag 失败时调用 ErrorStateManager")
        fun removeTagFailureCallsErrorStateManager() = runTest {
            val exception = DomainException.StorageError(RuntimeException("DB error"))
            coEvery { removeTagFromNodeUseCase(any(), any()) } returns Outcome.Error(exception)

            viewModel.removeTag(testTag)
            testDispatcher.scheduler.advanceUntilIdle()

            verify { errorStateManager.mapDomainException(exception) }
        }

        @Test
        @DisplayName("createAndAssignTag 先调用 createTagUseCase 再调用 assignTagToNodeUseCase")
        fun createAndAssignTagCallsBothUseCases() = runTest {
            val createdTag = TagEntity(id = "new-tag-id", name = "NewTag")
            coEvery { createTagUseCase("NewTag") } returns Outcome.Success(createdTag)
            coEvery { assignTagToNodeUseCase("test-node-id", "new-tag-id") } returns Outcome.Success(Unit)

            viewModel.createAndAssignTag("NewTag")
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(ordering = io.mockk.Ordering.ORDERED) {
                createTagUseCase("NewTag")
                assignTagToNodeUseCase("test-node-id", "new-tag-id")
            }
        }

        @Test
        @DisplayName("createAndAssignTag 创建标签失败时不调用 assignTagToNodeUseCase")
        fun createAndAssignTagFailureDoesNotCallAssign() = runTest {
            val exception = DomainException.ValidationError("name", "标签名称不能为空")
            coEvery { createTagUseCase("") } returns Outcome.Error(exception)

            viewModel.createAndAssignTag("")
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) { assignTagToNodeUseCase(any(), any()) }
            verify { errorStateManager.mapDomainException(exception) }
        }
    }

    @Nested
    @DisplayName("链接管理")
    inner class LinkManagement {

        @Test
        @DisplayName("createLink 调用 createLinkUseCase")
        fun createLinkCallsCreateLinkUseCase() = runTest {
            coEvery { createLinkUseCase(any(), any()) } returns Outcome.Success(
                com.fancy.skill_tree.core.domain.entity.NodeLinkEntity(
                    id = "link1",
                    sourceId = "test-node-id",
                    targetId = "target-id"
                )
            )

            viewModel.createLink("target-id")
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { createLinkUseCase("test-node-id", "target-id") }
        }

        @Test
        @DisplayName("createLink 失败时调用 ErrorStateManager")
        fun createLinkFailureCallsErrorStateManager() = runTest {
            val exception = DomainException.StorageError(RuntimeException("DB error"))
            coEvery { createLinkUseCase(any(), any()) } returns Outcome.Error(exception)

            viewModel.createLink("target-id")
            testDispatcher.scheduler.advanceUntilIdle()

            verify { errorStateManager.mapDomainException(exception) }
        }

        @Test
        @DisplayName("deleteLink 调用 deleteLinkUseCase")
        fun deleteLinkCallsDeleteLinkUseCase() = runTest {
            coEvery { deleteLinkUseCase(any()) } returns Outcome.Success(Unit)

            viewModel.deleteLink("link1")
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { deleteLinkUseCase("link1") }
        }

        @Test
        @DisplayName("deleteLink 失败时调用 ErrorStateManager")
        fun deleteLinkFailureCallsErrorStateManager() = runTest {
            val exception = DomainException.StorageError(RuntimeException("DB error"))
            coEvery { deleteLinkUseCase(any()) } returns Outcome.Error(exception)

            viewModel.deleteLink("link1")
            testDispatcher.scheduler.advanceUntilIdle()

            verify { errorStateManager.mapDomainException(exception) }
        }

        @Test
        @DisplayName("confirmLink 调用 confirmLinkUseCase")
        fun confirmLinkCallsConfirmLinkUseCase() = runTest {
            coEvery { confirmLinkUseCase(any()) } returns Outcome.Success(Unit)

            viewModel.confirmLink("link1")
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { confirmLinkUseCase("link1") }
        }

        @Test
        @DisplayName("confirmLink 失败时调用 ErrorStateManager")
        fun confirmLinkFailureCallsErrorStateManager() = runTest {
            val exception = DomainException.StorageError(RuntimeException("DB error"))
            coEvery { confirmLinkUseCase(any()) } returns Outcome.Error(exception)

            viewModel.confirmLink("link1")
            testDispatcher.scheduler.advanceUntilIdle()

            verify { errorStateManager.mapDomainException(exception) }
        }
    }

    @Nested
    @DisplayName("编辑模式")
    inner class EditingMode {

        @Test
        @DisplayName("toggleEditing 切换 isEditing 状态")
        fun toggleEditingSwitchesState() {
            assertThat(viewModel.uiState.value.isEditing).isFalse()

            viewModel.toggleEditing()

            assertThat(viewModel.uiState.value.isEditing).isTrue()

            viewModel.toggleEditing()

            assertThat(viewModel.uiState.value.isEditing).isFalse()
        }

        @Test
        @DisplayName("saveContent 调用 updateNodeUseCase")
        fun saveContentCallsUpdateNodeUseCase() = runTest {
            coEvery { updateNodeUseCase(any(), any(), any(), any()) } returns Outcome.Success(testNode)

            viewModel.saveContent("new content")
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { updateNodeUseCase("test-node-id", "Test Node", "new content", "ABILITY") }
        }

        @Test
        @DisplayName("saveContent 成功后退出编辑模式")
        fun saveContentSuccessExitsEditing() = runTest {
            coEvery { updateNodeUseCase(any(), any(), any(), any()) } returns Outcome.Success(testNode)

            viewModel.toggleEditing()
            assertThat(viewModel.uiState.value.isEditing).isTrue()

            viewModel.saveContent("new content")
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(viewModel.uiState.value.isEditing).isFalse()
        }

        @Test
        @DisplayName("saveContent 失败时调用 ErrorStateManager")
        fun saveContentFailureCallsErrorStateManager() = runTest {
            val exception = DomainException.NodeNotFound("test-node-id")
            coEvery { updateNodeUseCase(any(), any(), any(), any()) } returns Outcome.Error(exception)

            viewModel.saveContent("new content")
            testDispatcher.scheduler.advanceUntilIdle()

            verify { errorStateManager.mapDomainException(exception) }
        }
    }

    @Nested
    @DisplayName("错误处理")
    inner class ErrorHandling {

        @Test
        @DisplayName("UseCase 返回 WARNING 级别错误时调用 showSnackbar")
        fun warningErrorCallsShowSnackbar() = runTest {
            val exception = DomainException.NodeNotFound("node1")
            coEvery { deleteNodeUseCase(any()) } returns Outcome.Error(exception)

            viewModel.deleteNode {}
            testDispatcher.scheduler.advanceUntilIdle()

            verify { errorStateManager.mapDomainException(exception) }
            verify { errorStateManager.showSnackbar(any(), any(), isError = true) }
        }

        @Test
        @DisplayName("UseCase 返回 CRITICAL 级别错误时调用 showErrorDialog")
        fun criticalErrorCallsShowErrorDialog() = runTest {
            val exception = DomainException.StorageError(RuntimeException("critical"))
            every { errorStateManager.mapDomainException(exception) } returns UiError(
                titleResId = R.string.common_error,
                messageResId = R.string.common_unknown_error,
                severity = ErrorSeverity.CRITICAL
            )
            coEvery { deleteNodeUseCase(any()) } returns Outcome.Error(exception)

            viewModel.deleteNode {}
            testDispatcher.scheduler.advanceUntilIdle()

            verify { errorStateManager.mapDomainException(exception) }
            verify { errorStateManager.showErrorDialog(any(), any()) }
        }
    }

    private fun createViewModel(): NodeDetailViewModel {
        return NodeDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("nodeId" to "test-node-id")),
            getNodeDetailUseCase = getNodeDetailUseCase,
            updateNodeUseCase = updateNodeUseCase,
            deleteNodeUseCase = deleteNodeUseCase,
            getAllTagsUseCase = getAllTagsUseCase,
            getTagsForNodeUseCase = getTagsForNodeUseCase,
            createTagUseCase = createTagUseCase,
            assignTagToNodeUseCase = assignTagToNodeUseCase,
            removeTagFromNodeUseCase = removeTagFromNodeUseCase,
            getLinksForNodeUseCase = getLinksForNodeUseCase,
            getAllNodesUseCase = getAllNodesUseCase,
            createLinkUseCase = createLinkUseCase,
            deleteLinkUseCase = deleteLinkUseCase,
            confirmLinkUseCase = confirmLinkUseCase,
            getAttachmentsForNodeUseCase = getAttachmentsForNodeUseCase,
            addAttachmentUseCase = addAttachmentUseCase,
            deleteAttachmentUseCase = deleteAttachmentUseCase,
            errorStateManager = errorStateManager
        )
    }
}
