package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取所有根节点
 */
class GetRootNodesUseCase @Inject constructor(
    private val repository: SkillTreeRepository
) {
    /**
     * @return 所有根节点的 Flow
     */
    operator fun invoke(): Flow<List<SkillNodeEntity>> {
        return repository.getRootNodes()
    }
}
