package com.fancy.skill_tree.core.domain.usecase.gamification

import com.fancy.skill_tree.core.data.manager.AchievementManager
import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import com.fancy.skill_tree.core.domain.model.Achievement
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("CheckAchievementsUseCase")
class CheckAchievementsUseCaseTest {

    private val repository = mockk<SkillTreeRepository>(relaxed = true)
    private val achievementManager = mockk<AchievementManager>(relaxed = true)
    private lateinit var useCase: CheckAchievementsUseCase

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        every { repository.getNodeCount() } returns flowOf(1)
        every { repository.getLinkCount() } returns flowOf(0)
        every { repository.getMaxDepth() } returns flowOf(1)
        every { repository.getAllNodes() } returns flowOf(emptyList())
        every { repository.getTagsForNode(any()) } returns flowOf(emptyList())
        every { repository.getAttachmentsForNode(any()) } returns flowOf(emptyList())
        every { repository.getLinksForNode(any()) } returns flowOf(emptyList())
        coEvery { repository.getConfirmedAiLinkCount() } returns 0
        coEvery { repository.getImageAttachmentCount() } returns 0
        every { achievementManager.unlockAchievement(any()) } returns true
        every { achievementManager.getVoiceInputCount() } returns 0
        every { achievementManager.updateDailyStreak() } returns 1
        every { achievementManager.updateLastActivityDate(any()) } just runs
        useCase = CheckAchievementsUseCase(repository, achievementManager)
    }

    @Nested
    @DisplayName("成就检查")
    inner class AchievementCheck {

        @Test
        @DisplayName("首个节点 → 解锁 FIRST_NODE")
        fun firstNodeUnlocksFirstNode() = runTest {
            every { repository.getNodeCount() } returns flowOf(1)

            val result = useCase()

            assertThat(result).contains(Achievement.FIRST_NODE)
        }

        @Test
        @DisplayName("10 个节点 → 解锁 TEN_NODES")
        fun tenNodesUnlocksTenNodes() = runTest {
            every { repository.getNodeCount() } returns flowOf(10)

            val result = useCase()

            assertThat(result).contains(Achievement.TEN_NODES)
        }

        @Test
        @DisplayName("50 个节点 → 解锁 FIFTY_NODES")
        fun fiftyNodesUnlocksFiftyNodes() = runTest {
            every { repository.getNodeCount() } returns flowOf(50)

            val result = useCase()

            assertThat(result).contains(Achievement.FIFTY_NODES)
        }

        @Test
        @DisplayName("20 条链接 → 解锁 TWENTY_LINKS")
        fun twentyLinksUnlocksTwentyLinks() = runTest {
            every { repository.getLinkCount() } returns flowOf(20)

            val result = useCase()

            assertThat(result).contains(Achievement.TWENTY_LINKS)
        }

        @Test
        @DisplayName("5 个 Lv5 节点 → 解锁 FIVE_LEVEL5")
        fun fiveLevel5NodesUnlocksFiveLevel5() = runTest {
            val level5Nodes = (1..5).map { i ->
                SkillNodeEntity(
                    id = "node$i",
                    title = "Node$i",
                    content = "x".repeat(500),
                    nodeType = "ABILITY"
                )
            }
            every { repository.getAllNodes() } returns flowOf(level5Nodes)
            every { repository.getTagsForNode(any()) } returns flowOf(listOf(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true)))
            every { repository.getAttachmentsForNode(any()) } returns flowOf(listOf(mockk(relaxed = true)))
            every { repository.getLinksForNode(any()) } returns flowOf(listOf(mockk(relaxed = true)))

            val result = useCase()

            assertThat(result).contains(Achievement.FIVE_LEVEL5)
        }

        @Test
        @DisplayName("深度 5 → 解锁 TREE_DEPTH_5")
        fun depth5UnlocksTreeDepth5() = runTest {
            every { repository.getMaxDepth() } returns flowOf(5)

            val result = useCase()

            assertThat(result).contains(Achievement.TREE_DEPTH_5)
        }

        @Test
        @DisplayName("10 条 AI 确认链接 → 解锁 TEN_AI_CONFIRMS")
        fun tenAiConfirmsUnlocksTenAiConfirms() = runTest {
            coEvery { repository.getConfirmedAiLinkCount() } returns 10

            val result = useCase()

            assertThat(result).contains(Achievement.TEN_AI_CONFIRMS)
        }

        @Test
        @DisplayName("10 次语音输入 → 解锁 TEN_VOICE")
        fun tenVoiceUnlocksTenVoice() = runTest {
            every { achievementManager.getVoiceInputCount() } returns 10

            val result = useCase()

            assertThat(result).contains(Achievement.TEN_VOICE)
        }

        @Test
        @DisplayName("20 张图片 → 解锁 TWENTY_IMAGES")
        fun twentyImagesUnlocksTwentyImages() = runTest {
            coEvery { repository.getImageAttachmentCount() } returns 20

            val result = useCase()

            assertThat(result).contains(Achievement.TWENTY_IMAGES)
        }

        @Test
        @DisplayName("连续 7 天 → 解锁 DAILY_STREAK_7")
        fun dailyStreak7UnlocksDailyStreak7() = runTest {
            every { achievementManager.updateDailyStreak() } returns 7

            val result = useCase()

            assertThat(result).contains(Achievement.DAILY_STREAK_7)
        }

        @Test
        @DisplayName("已解锁的成就不重复解锁")
        fun alreadyUnlockedAchievementNotRepeated() = runTest {
            every { repository.getNodeCount() } returns flowOf(1)
            every { achievementManager.unlockAchievement(Achievement.FIRST_NODE) } returns false

            val result = useCase()

            assertThat(result).doesNotContain(Achievement.FIRST_NODE)
        }

        @Test
        @DisplayName("多个条件同时满足 → 返回多个新成就")
        fun multipleConditionsMetReturnsMultipleAchievements() = runTest {
            every { repository.getNodeCount() } returns flowOf(10)
            every { repository.getLinkCount() } returns flowOf(20)
            every { repository.getMaxDepth() } returns flowOf(5)
            coEvery { repository.getConfirmedAiLinkCount() } returns 10
            every { achievementManager.getVoiceInputCount() } returns 10
            coEvery { repository.getImageAttachmentCount() } returns 20
            every { achievementManager.updateDailyStreak() } returns 7

            val result = useCase()

            assertThat(result).contains(Achievement.FIRST_NODE)
            assertThat(result).contains(Achievement.TEN_NODES)
            assertThat(result).contains(Achievement.TWENTY_LINKS)
            assertThat(result).contains(Achievement.TREE_DEPTH_5)
            assertThat(result).contains(Achievement.TEN_AI_CONFIRMS)
            assertThat(result).contains(Achievement.TEN_VOICE)
            assertThat(result).contains(Achievement.TWENTY_IMAGES)
            assertThat(result).contains(Achievement.DAILY_STREAK_7)
        }
    }
}
