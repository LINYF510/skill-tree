package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.entity.TagEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取节点标签 UseCase
 * 用于获取指定节点的所有标签
 */
class GetTagsForNodeUseCase @Inject constructor(
    private val repository: SkillTreeRepository
) {
    /**
     * 获取节点的所有标签
     * @param nodeId 节点 ID
     * @return 标签列表 Flow
     */
    operator fun invoke(nodeId: String): Flow<List<TagEntity>> {
        return repository.getTagsForNode(nodeId)
    }
}
