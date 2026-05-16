package com.fancy.skill_tree.feature.statistics

import com.fancy.skill_tree.R
import com.fancy.skill_tree.core.data.exception.DataException
import com.fancy.skill_tree.core.data.manager.AchievementManager
import com.fancy.skill_tree.core.domain.model.Achievement
import com.fancy.skill_tree.core.domain.usecase.node.GetStatisticsUseCase
import com.fancy.skill_tree.core.ui.error.ErrorSeverity
import com.fancy.skill_tree.core.ui.error.ErrorStateManager
import com.fancy.skill_tree.core.ui.error.UiError
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
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

@DisplayName("StatisticsViewModel")
@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getStatisticsUseCase = mockk<GetStatisticsUseCase>(relaxed = true)
    private val achievementManager = mockk<AchievementManager>(relaxed = true)
    private val errorStateManager = mockk<ErrorStateManager>(relaxed = true)

    private lateinit var viewModel: StatisticsViewModel

    private val testStatistics = GetStatisticsUseCase.Statistics(
        totalNodes = 10,
        abilityNodes = 7,
        resourceNodes = 3,
        maxDepth = 3
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        clearAllMocks()

        every { getStatisticsUseCase() } returns flowOf(testStatistics)
        every { achievementManager.getAllAchievements() } returns Achievement.entries.toList()
        every { achievementManager.getAllUnlocked() } returns listOf(Achievement.FIRST_NODE)
        every { errorStateManager.mapDataException(any()) } returns UiError(
            titleResId = R.string.common_error,
            messageResId = R.string.common_unknown_error,
            severity = ErrorSeverity.ERROR
        )

        viewModel = StatisticsViewModel(
            getStatisticsUseCase = getStatisticsUseCase,
            achievementManager = achievementManager,
            errorStateManager = errorStateManager
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("加载统计数据")
    inner class LoadStatistics {

        @Test
        @DisplayName("成功加载统计数据后 uiState.statistics 非空")
        fun loadStatisticsSuccess() = runTest {
            val vm = StatisticsViewModel(
                getStatisticsUseCase = getStatisticsUseCase,
                achievementManager = achievementManager,
                errorStateManager = errorStateManager
            )
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(vm.uiState.value.statistics).isNotNull()
            assertThat(vm.uiState.value.statistics?.totalNodes).isEqualTo(10)
            assertThat(vm.uiState.value.statistics?.abilityNodes).isEqualTo(7)
            assertThat(vm.uiState.value.statistics?.resourceNodes).isEqualTo(3)
            assertThat(vm.uiState.value.statistics?.maxDepth).isEqualTo(3)
            assertThat(vm.uiState.value.isLoading).isFalse()
            assertThat(vm.uiState.value.errorMessage).isNull()
        }

        @Test
        @DisplayName("Flow 异常时调用 ErrorStateManager")
        fun loadStatisticsFlowError() = runTest {
            val exception = DataException.DatabaseError(RuntimeException("db error"))
            every { getStatisticsUseCase() } returns flow { throw exception }

            val vm = StatisticsViewModel(
                getStatisticsUseCase = getStatisticsUseCase,
                achievementManager = achievementManager,
                errorStateManager = errorStateManager
            )
            testDispatcher.scheduler.advanceUntilIdle()

            verify { errorStateManager.mapDataException(exception) }
            assertThat(vm.uiState.value.isLoading).isFalse()
        }
    }

    @Nested
    @DisplayName("成就数据")
    inner class Achievements {

        @Test
        @DisplayName("初始化时加载成就列表，uiState.achievements 非空")
        fun loadAchievementsOnInit() = runTest {
            assertThat(viewModel.uiState.value.achievements).isNotEmpty()
            assertThat(viewModel.uiState.value.achievements).isEqualTo(Achievement.entries.toList())
        }

        @Test
        @DisplayName("初始化时加载已解锁成就")
        fun loadUnlockedAchievementsOnInit() = runTest {
            assertThat(viewModel.uiState.value.unlockedAchievements).isNotEmpty()
            assertThat(viewModel.uiState.value.unlockedAchievements).isEqualTo(listOf(Achievement.FIRST_NODE))
        }

        @Test
        @DisplayName("refreshAchievements 更新成就列表")
        fun refreshAchievementsUpdatesList() = runTest {
            val updatedUnlocked = listOf(Achievement.FIRST_NODE, Achievement.TEN_NODES)
            every { achievementManager.getAllUnlocked() } returns updatedUnlocked

            viewModel.refreshAchievements()

            assertThat(viewModel.uiState.value.unlockedAchievements).isEqualTo(updatedUnlocked)
        }
    }
}
