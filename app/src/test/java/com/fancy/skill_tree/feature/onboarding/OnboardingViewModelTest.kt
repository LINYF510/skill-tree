package com.fancy.skill_tree.feature.onboarding

import android.app.Application
import android.content.Context
import com.fancy.skill_tree.core.domain.common.Outcome
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import com.fancy.skill_tree.core.domain.usecase.node.CreateNodeUseCase
import com.fancy.skill_tree.core.domain.usecase.node.LoadSampleDataUseCase
import com.fancy.skill_tree.feature.onboarding.OnboardingManager
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
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

@DisplayName("OnboardingViewModel")
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val onboardingManager = mockk<OnboardingManager>(relaxed = true)
    private val loadSampleDataUseCase = mockk<LoadSampleDataUseCase>(relaxed = true)
    private val createNodeUseCase = mockk<CreateNodeUseCase>(relaxed = true)
    private val application = mockk<Application>(relaxed = true)

    private lateinit var viewModel: OnboardingViewModel

    private val testNode = SkillNodeEntity(
        id = "node1",
        parentId = null,
        title = "Test",
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

        coEvery { createNodeUseCase(any(), any(), any(), any(), any()) } returns Outcome.Success(testNode)

        viewModel = OnboardingViewModel(
            application = application,
            onboardingManager = onboardingManager,
            loadSampleDataUseCase = loadSampleDataUseCase,
            createNodeUseCase = createNodeUseCase
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("步骤导航")
    inner class StepNavigation {

        @Test
        @DisplayName("nextStep → currentStep + 1")
        fun nextStepIncrementsCurrentStep() = runTest {
            assertThat(viewModel.currentStep.value).isEqualTo(0)

            viewModel.nextStep()
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(viewModel.currentStep.value).isEqualTo(1)
        }

        @Test
        @DisplayName("prevStep → currentStep - 1")
        fun prevStepDecrementsCurrentStep() = runTest {
            viewModel.nextStep()
            testDispatcher.scheduler.advanceUntilIdle()

            viewModel.prevStep()
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(viewModel.currentStep.value).isEqualTo(0)
        }

        @Test
        @DisplayName("prevStep 在第一步 → 不变")
        fun prevStepAtFirstStepDoesNotGoBelowZero() = runTest {
            assertThat(viewModel.currentStep.value).isEqualTo(0)

            viewModel.prevStep()
            testDispatcher.scheduler.advanceUntilIdle()

            assertThat(viewModel.currentStep.value).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("完成引导")
    inner class FinishOnboarding {

        @Test
        @DisplayName("finish(true) → 保留示例数据，onboardingManager.completeOnboarding() 被调用")
        fun finishWithKeepSampleLoadsDataAndCompletes() = runTest {
            viewModel.finish(true)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { loadSampleDataUseCase(any<Context>()) }
            verify { onboardingManager.completeOnboarding() }
        }

        @Test
        @DisplayName("finish(false) → 不保留示例数据，onboardingManager.completeOnboarding() 被调用")
        fun finishWithoutKeepSampleSkipsDataAndCompletes() = runTest {
            viewModel.finish(false)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) { loadSampleDataUseCase(any<Context>()) }
            verify { onboardingManager.completeOnboarding() }
        }
    }

    @Nested
    @DisplayName("创建首个节点")
    inner class CreateFirstNode {

        @Test
        @DisplayName("createFirstNode → createNodeUseCase 被调用")
        fun createFirstNodeCallsCreateNodeUseCase() = runTest {
            viewModel.createFirstNode("Test", "ABILITY")
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { createNodeUseCase("Test", "ABILITY", null, any(), any()) }
        }
    }
}
