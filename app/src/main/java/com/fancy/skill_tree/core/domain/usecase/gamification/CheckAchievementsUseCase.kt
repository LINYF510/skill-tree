package com.fancy.skill_tree.core.domain.usecase.gamification

import com.fancy.skill_tree.core.data.manager.AchievementManager
import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.model.Achievement
import com.fancy.skill_tree.core.domain.model.NodeLevelConfig
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 成就检查 UseCase
 * 负责检查所有成就的解锁条件并更新状态
 */
@Singleton
class CheckAchievementsUseCase @Inject constructor(
    private val repository: SkillTreeRepository,
    private val achievementManager: AchievementManager
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * 检查所有成就并返回新解锁的成就
     */
    suspend operator fun invoke(): List<Achievement> {
        val newAchievements = mutableListOf<Achievement>()

        // 更新活动日期
        val today = LocalDate.now().format(dateFormatter)
        achievementManager.updateLastActivityDate(today)

        // 获取统计数据
        val nodeCount = repository.getNodeCount().first()
        val linkCount = repository.getLinkCount().first()
        val maxDepth = repository.getMaxDepth().first()
        val allNodes = repository.getAllNodes().first()

        // 计算 Lv.5 节点数量
        val level5Count = allNodes.count { node ->
            val contentLength = node.content?.length ?: 0
            val tagCount = repository.getTagsForNode(node.id).first().size
            val attachmentCount = repository.getAttachmentsForNode(node.id).first().size
            val linkCountForNode = repository.getLinksForNode(node.id).first().size
            NodeLevelConfig.calculateLevel(contentLength, tagCount, attachmentCount, linkCountForNode) >= 5
        }

        // 检查成就条件
        checkAndUnlock(Achievement.FIRST_NODE, nodeCount >= 1, newAchievements)
        checkAndUnlock(Achievement.TEN_NODES, nodeCount >= 10, newAchievements)
        checkAndUnlock(Achievement.FIFTY_NODES, nodeCount >= 50, newAchievements)
        checkAndUnlock(Achievement.TWENTY_LINKS, linkCount >= 20, newAchievements)
        checkAndUnlock(Achievement.FIVE_LEVEL5, level5Count >= 5, newAchievements)
        checkAndUnlock(Achievement.TREE_DEPTH_5, maxDepth >= 5, newAchievements)

        // 以下成就需要额外实现：语音输入计数、AI 确认计数、图片计数、连续天数
        // 暂时跳过，后续补充

        return newAchievements
    }

    /**
     * 检查单个成就是否满足条件，满足则解锁
     */
    private fun checkAndUnlock(
        achievement: Achievement,
        condition: Boolean,
        newList: MutableList<Achievement>
    ) {
        if (condition && achievementManager.unlockAchievement(achievement)) {
            newList.add(achievement)
        }
    }
}
