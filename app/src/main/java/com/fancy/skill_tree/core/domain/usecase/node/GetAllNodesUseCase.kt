package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取所有节点
 */
class GetAllNodesUseCase @Inject constructor(
    private val repository: SkillTreeRepository
) {
    /**
     * @return 所有节点的 Flow
     */
    operator fun invoke(): Flow<List<SkillNodeEntity>> {
        return repository.getAllNodes()
    }
}
