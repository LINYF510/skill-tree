package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * 获取统计数据 UseCase
 * 聚合各类统计信息用于展示
 */
class GetStatisticsUseCase @Inject constructor(
    private val repository: SkillTreeRepository
) {
    /**
     * 统计数据
     */
    data class Statistics(
        val totalNodes: Int,
        val abilityNodes: Int,
        val resourceNodes: Int,
        val maxDepth: Int
    )

    /**
     * 获取统计数据
     * @return 统计数据 Flow
     */
    operator fun invoke(): Flow<Statistics> {
        return combine(
            repository.getNodeCount(),
            repository.getNodeCountByType("ABILITY"),
            repository.getNodeCountByType("RESOURCE"),
            repository.getMaxDepth()
        ) { total, ability, resource, depth ->
            Statistics(
                totalNodes = total,
                abilityNodes = ability,
                resourceNodes = resource,
                maxDepth = depth ?: 0
            )
        }
    }
}
