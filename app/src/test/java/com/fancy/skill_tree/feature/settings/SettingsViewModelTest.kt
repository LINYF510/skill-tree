package com.fancy.skill_tree.feature.settings

import com.fancy.skill_tree.R
import com.fancy.skill_tree.core.data.preferences.UserPreferences
import com.fancy.skill_tree.core.domain.common.DomainException
import com.fancy.skill_tree.core.domain.common.Outcome
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import com.fancy.skill_tree.core.domain.usecase.node.ClearAllDataUseCase
import com.fancy.skill_tree.core.domain.usecase.node.CreateNodeUseCase
import com.fancy.skill_tree.core.domain.usecase.node.ExportToMarkdownUseCase
import com.fancy.skill_tree.core.ui.error.ErrorSeverity
import com.fancy.skill_tree.core.ui.error.ErrorStateManager
import com.fancy.skill_tree.core.ui.error.UiError
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SettingsViewModel")
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val exportToMarkdownUseCase = mockk<ExportToMarkdownUseCase>(relaxed = true)
    private val createNodeUseCase = mockk<CreateNodeUseCase>(relaxed = true)
    private val clearAllDataUseCase = mockk<ClearAllDataUseCase>(relaxed = true)
    private val errorStateManager = mockk<ErrorStateManager>(relaxed = true)
    private val userPreferences = mockk<UserPreferences>(relaxed = true)

    private lateinit var viewModel: SettingsViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        clearAllMocks()

        every { errorStateManager.mapDomainException(any()) } returns UiError(
            titleResId = R.string.common_error,
            messageResId = R.string.common_unknown_error,
            severity = ErrorSeverity.WARNING
        )

        viewModel = SettingsViewModel(
            exportToMarkdownUseCase = exportToMarkdownUseCase,
            createNodeUseCase = createNodeUseCase,
            clearAllDataUseCase = clearAllDataUseCase,
            errorStateManager = errorStateManager,
            userPreferences = userPreferences
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("导出 Markdown")
    inner class ExportMarkdown {

        @Test
        @DisplayName("成功导出时调用 onSuccess 回调")
        fun exportMarkdownSuccessCallsOnSuccess() = runTest {
            val markdown = "# 技能树\n\n暂无节点数据\n"
            coEvery { exportToMarkdownUseCase() } returns Outcome.Success(markdown)

            var callbackValue: String? = null
            viewModel.exportMarkdown { callbackValue = it }
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(callbackValue).isEqualTo(markdown)
        }

        @Test
        @DisplayName("导出失败时调用 ErrorStateManager")
        fun exportMarkdownFailureCallsErrorStateManager() = runTest {
            val exception = DomainException.StorageError(RuntimeException("导出失败"))
            coEvery { exportToMarkdownUseCase() } returns Outcome.Error(exception)

            viewModel.exportMarkdown { }
            testDispatcher.scheduler.advanceUntilIdle()

            verify { errorStateManager.mapDomainException(exception) }
            verify { errorStateManager.showSnackbar(any(), any(), isError = true) }
        }
    }

    @Nested
    @DisplayName("加载示例数据")
    inner class LoadSampleData {

        @Test
        @DisplayName("成功加载时 createNodeUseCase 被调用 7 次")
        fun loadSampleDataCallsCreateNodeSevenTimes() = runTest {
            coEvery {
                createNodeUseCase(any(), any(), any(), any(), any())
            } returns Outcome.Success(
                SkillNodeEntity(
                    id = "sample",
                    parentId = null,
                    title = "test",
                    nodeType = "ABILITY",
                    level = 1,
                    sortOrder = 0,
                    isExpanded = true,
                    createdAt = 1000L,
                    updatedAt = 1000L
                )
            )

            viewModel.loadSampleData()
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 7) { createNodeUseCase(any(), any(), any(), any(), any()) }
        }
    }

    @Nested
    @DisplayName("清除数据")
    inner class ClearAllData {

        @Test
        @DisplayName("成功清除时调用 clearAllDataUseCase")
        fun clearAllDataSuccessCallsUseCase() = runTest {
            coEvery { clearAllDataUseCase() } returns Outcome.Success(Unit)

            viewModel.clearAllData()
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { clearAllDataUseCase() }
            assertThat(viewModel.uiState.value.isLoading).isFalse()
        }

        @Test
        @DisplayName("清除失败时调用 ErrorStateManager")
        fun clearAllDataFailureCallsErrorStateManager() = runTest {
            val exception = DomainException.StorageError(RuntimeException("清除失败"))
            coEvery { clearAllDataUseCase() } returns Outcome.Error(exception)

            viewModel.clearAllData()
            testDispatcher.scheduler.advanceUntilIdle()

            verify { errorStateManager.mapDomainException(exception) }
            verify { errorStateManager.showSnackbar(any(), any(), isError = true) }
        }
    }
}
