package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取指定父节点的子节点
 */
class GetChildNodesUseCase @Inject constructor(
    private val repository: SkillTreeRepository
) {
    /**
     * @param parentId 父节点 ID
     * @return 子节点的 Flow
     */
    operator fun invoke(parentId: String): Flow<List<SkillNodeEntity>> {
        return repository.getChildNodes(parentId)
    }
}
