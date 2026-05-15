package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillNodeWithTags
import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.common.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 获取节点详情（包括标签）
 */
class GetNodeDetailUseCase @Inject constructor(
    private val repository: SkillTreeRepository
) {
    /**
     * @param nodeId 节点 ID
     * @return 节点详情的 Flow
     */
    operator fun invoke(nodeId: String): Flow<Outcome<SkillNodeWithTags>> {
        return repository.getNodeWithTags(nodeId).map { nodeWithTags ->
            if (nodeWithTags != null) {
                Outcome.Success(nodeWithTags)
            } else {
                Outcome.Error(com.fancy.skill_tree.core.domain.common.DomainException.NodeNotFound(nodeId))
            }
        }
    }
}
