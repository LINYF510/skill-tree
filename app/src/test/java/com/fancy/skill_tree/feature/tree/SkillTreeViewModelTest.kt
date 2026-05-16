package com.fancy.skill_tree.feature.tree

import app.cash.turbine.test
import android.app.Application
import android.content.Context
import com.fancy.skill_tree.R
import com.fancy.skill_tree.core.data.manager.SearchHistoryManager
import com.fancy.skill_tree.core.domain.common.DomainException
import com.fancy.skill_tree.core.domain.common.Outcome
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import com.fancy.skill_tree.core.domain.entity.TagEntity
import com.fancy.skill_tree.core.domain.usecase.gamification.CheckAchievementsUseCase
import com.fancy.skill_tree.core.domain.usecase.gamification.SearchNodesUseCase
import com.fancy.skill_tree.core.domain.usecase.node.CreateNodeUseCase
import com.fancy.skill_tree.core.domain.usecase.node.DeleteNodeUseCase
import com.fancy.skill_tree.core.domain.usecase.node.ExportToMarkdownUseCase
import com.fancy.skill_tree.core.domain.usecase.node.GetAllNodesUseCase
import com.fancy.skill_tree.core.domain.usecase.node.GetAllTagsUseCase
import com.fancy.skill_tree.core.domain.usecase.node.LoadSampleDataUseCase
import com.fancy.skill_tree.core.domain.usecase.node.MoveNodeUseCase
import com.fancy.skill_tree.core.domain.usecase.node.ToggleNodeExpandUseCase
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

@DisplayName("SkillTreeViewModel")
@OptIn(ExperimentalCoroutinesApi::class)
class SkillTreeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getAllNodesUseCase = mockk<GetAllNodesUseCase>()
    private val createNodeUseCase = mockk<CreateNodeUseCase>()
    private val updateNodeUseCase = mockk<UpdateNodeUseCase>()
    private val deleteNodeUseCase = mockk<DeleteNodeUseCase>()
    private val moveNodeUseCase = mockk<MoveNodeUseCase>()
    private val toggleNodeExpandUseCase = mockk<ToggleNodeExpandUseCase>()
    private val loadSampleDataUseCase = mockk<LoadSampleDataUseCase>(relaxed = true)
    private val exportToMarkdownUseCase = mockk<ExportToMarkdownUseCase>()
    private val checkAchievementsUseCase = mockk<CheckAchievementsUseCase>()
    private val searchNodesUseCase = mockk<SearchNodesUseCase>()
    private val getAllTagsUseCase = mockk<GetAllTagsUseCase>()
    private val searchHistoryManager = mockk<SearchHistoryManager>()
    private val errorStateManager = mockk<ErrorStateManager>(relaxed = true)

    private lateinit var viewModel: SkillTreeViewModel

    private val testNode = SkillNodeEntity(
        id = "node1",
        parentId = null,
        title = "Test Node",
        nodeType = "ABILITY",
        level = 1,
        sortOrder = 0,
        isExpanded = true,
        createdAt = 1000L,
        updatedAt = 1000L
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        clearAllMocks()

        every { getAllNodesUseCase() } returns flowOf(emptyList())
        every { getAllTagsUseCase() } returns flowOf(emptyList())
        every { searchHistoryManager.getHistory() } returns emptyList()
        coEvery { checkAchievementsUseCase() } returns emptyList()
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
    @DisplayName("初始化")
    inner class Initialization {

        @Test
        @DisplayName("初始化时加载节点列表")
        fun initLoadsNodes() = runTest {
            val nodes = listOf(testNode)
            every { getAllNodesUseCase() } returns flowOf(nodes)
            every { getAllTagsUseCase() } returns flowOf(emptyList())
            every { searchHistoryManager.getHistory() } returns emptyList()

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(vm.uiState.value.nodes).isEqualTo(nodes)
            assertThat(vm.uiState.value.isLoading).isFalse()
        }

        @Test
        @DisplayName("初始化时加载标签列表")
        fun initLoadsTags() = runTest {
            val tags = listOf(TagEntity(id = "tag1", name = "Kotlin", color = "#FF0000"))
            every { getAllNodesUseCase() } returns flowOf(emptyList())
            every { getAllTagsUseCase() } returns flowOf(tags)
            every { searchHistoryManager.getHistory() } returns emptyList()

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(vm.allTags.value).isEqualTo(tags)
        }

        @Test
        @DisplayName("初始化时加载搜索历史")
        fun initLoadsSearchHistory() = runTest {
            val history = listOf("Kotlin", "Python")
            every { searchHistoryManager.getHistory() } returns history

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(vm.searchHistory.value).isEqualTo(history)
        }

        @Test
        @DisplayName("初始化时通过 Turbine 验证加载状态流转")
        fun initLoadingStateTransition() = runTest {
            val nodes = listOf(testNode)
            every { getAllNodesUseCase() } returns flowOf(nodes)
            every { getAllTagsUseCase() } returns flowOf(emptyList())
            every { searchHistoryManager.getHistory() } returns emptyList()

            val vm = createViewModel()

            assertThat(vm.uiState.value.isLoading).isTrue()

            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(vm.uiState.value.isLoading).isFalse()
            assertThat(vm.uiState.value.nodes).isEqualTo(nodes)
        }
    }

    @Nested
    @DisplayName("创建节点")
    inner class CreateNode {

        @Test
        @DisplayName("创建节点成功后检查成就")
        fun createNodeSuccessChecksAchievements() = runTest {
            coEvery {
                createNodeUseCase(any(), any(), any(), any(), any())
            } returns Outcome.Success(testNode)

            viewModel.createNode("Test", "ABILITY", null)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { createNodeUseCase("Test", "ABILITY", null, any(), any()) }
            coVerify { checkAchievementsUseCase() }
        }

        @Test
        @DisplayName("创建节点失败时调用 ErrorStateManager")
        fun createNodeFailureCallsErrorStateManager() = runTest {
            val exception = DomainException.ValidationError("title", "标题不能为空")
            coEvery {
                createNodeUseCase(any(), any(), any(), any(), any())
            } returns Outcome.Error(exception)

            viewModel.createNode("", "ABILITY", null)
            testDispatcher.scheduler.advanceUntilIdle()

            verify { errorStateManager.mapDomainException(exception) }
            verify { errorStateManager.showSnackbar(any(), isError = true) }
        }
    }

    @Nested
    @DisplayName("删除节点")
    inner class DeleteNode {

        @Test
        @DisplayName("删除节点成功后清除选中节点并检查成就")
        fun deleteNodeSuccessClearsSelectionAndChecksAchievements() = runTest {
            viewModel.onSelectNode("node1")
            coEvery { deleteNodeUseCase("node1") } returns Outcome.Success(Unit)

            viewModel.deleteNode("node1")
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(viewModel.selectedNodeId.value).isNull()
            coVerify { checkAchievementsUseCase() }
        }

        @Test
        @DisplayName("删除节点失败时调用 ErrorStateManager")
        fun deleteNodeFailureCallsErrorStateManager() = runTest {
            val exception = DomainException.NodeNotFound("node1")
            coEvery { deleteNodeUseCase("node1") } returns Outcome.Error(exception)

            viewModel.deleteNode("node1")
            testDispatcher.scheduler.advanceUntilIdle()

            verify { errorStateManager.mapDomainException(exception) }
        }
    }

    @Nested
    @DisplayName("移动节点")
    inner class MoveNode {

        @Test
        @DisplayName("移动节点成功后检查成就")
        fun moveNodeSuccessChecksAchievements() = runTest {
            coEvery { moveNodeUseCase("node1", "parent1") } returns Outcome.Success(Unit)

            viewModel.moveNode("node1", "parent1")
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { moveNodeUseCase("node1", "parent1") }
            coVerify { checkAchievementsUseCase() }
        }

        @Test
        @DisplayName("移动节点失败时调用 ErrorStateManager")
        fun moveNodeFailureCallsErrorStateManager() = runTest {
            val exception = DomainException.CircularReference("node1", "child1")
            coEvery { moveNodeUseCase("node1", "child1") } returns Outcome.Error(exception)

            viewModel.moveNode("node1", "child1")
            testDispatcher.scheduler.advanceUntilIdle()

            verify { errorStateManager.mapDomainException(exception) }
        }
    }

    @Nested
    @DisplayName("搜索功能")
    inner class Search {

        @Test
        @DisplayName("搜索时更新搜索历史和查询状态")
        fun searchUpdatesHistoryAndQuery() = runTest {
            every { searchHistoryManager.addToHistory("Kotlin") } just runs
            every { searchHistoryManager.getHistory() } returns listOf("Kotlin")
            every { searchNodesUseCase("Kotlin", emptyList()) } returns flowOf(listOf(testNode))

            viewModel.search("Kotlin")
            testDispatcher.scheduler.advanceUntilIdle()

            verify { searchHistoryManager.addToHistory("Kotlin") }
            assertThat(viewModel.uiState.value.searchQuery).isEqualTo("Kotlin")
        }

        @Test
        @DisplayName("搜索返回过滤后的节点列表")
        fun searchReturnsFilteredNodes() = runTest {
            every { searchHistoryManager.addToHistory(any()) } just runs
            every { searchHistoryManager.getHistory() } returns listOf("Test")
            every { searchNodesUseCase("Test", emptyList()) } returns flowOf(listOf(testNode))

            viewModel.search("Test")
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(viewModel.uiState.value.filteredNodes).isEqualTo(listOf(testNode))
        }

        @Test
        @DisplayName("清除搜索历史")
        fun clearSearchHistory() = runTest {
            every { searchHistoryManager.clearHistory() } just runs

            viewModel.clearSearchHistory()

            verify { searchHistoryManager.clearHistory() }
            assertThat(viewModel.searchHistory.value).isEmpty()
        }

        @Test
        @DisplayName("搜索时通过 Turbine 验证状态更新")
        fun searchStateUpdateWithTurbine() = runTest {
            every { searchHistoryManager.addToHistory("Kotlin") } just runs
            every { searchHistoryManager.getHistory() } returns listOf("Kotlin")
            every { searchNodesUseCase("Kotlin", emptyList()) } returns flowOf(listOf(testNode))

            viewModel.search("Kotlin")
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.uiState.test {
                val state = awaitItem()
                assertThat(state.searchQuery).isEqualTo("Kotlin")
                assertThat(state.filteredNodes).isEqualTo(listOf(testNode))
            }
        }
    }

    private val application = mockk<Application>(relaxed = true)

    private fun createViewModel(): SkillTreeViewModel {
        return SkillTreeViewModel(
            application = application,
            getAllNodesUseCase = getAllNodesUseCase,
            createNodeUseCase = createNodeUseCase,
            updateNodeUseCase = updateNodeUseCase,
            deleteNodeUseCase = deleteNodeUseCase,
            moveNodeUseCase = moveNodeUseCase,
            toggleNodeExpandUseCase = toggleNodeExpandUseCase,
            exportToMarkdownUseCase = exportToMarkdownUseCase,
            loadSampleDataUseCase = loadSampleDataUseCase,
            checkAchievementsUseCase = checkAchievementsUseCase,
            searchNodesUseCase = searchNodesUseCase,
            getAllTagsUseCase = getAllTagsUseCase,
            searchHistoryManager = searchHistoryManager,
            errorStateManager = errorStateManager
        )
    }
}
