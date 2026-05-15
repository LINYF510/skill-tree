package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.NodeLinkWithTarget
import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.entity.NodeLinkEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * 获取节点链接 UseCase
 * 用于获取指定节点的所有链接及其目标节点信息
 */
class GetLinksForNodeUseCase @Inject constructor(
    private val repository: SkillTreeRepository
) {
    /**
     * 获取节点的所有链接及目标节点信息
     * @param nodeId 节点 ID
     * @return 链接与目标节点列表的 Flow
     */
    operator fun invoke(nodeId: String): Flow<List<NodeLinkWithTarget>> {
        return combine(
            repository.getLinksForNode(nodeId),
            repository.getAllNodes()
        ) { links, allNodes ->
            links.mapNotNull { link ->
                val targetId = if (link.sourceId == nodeId) link.targetId else link.sourceId
                val targetNode = allNodes.find { it.id == targetId }
                targetNode?.let { NodeLinkWithTarget(link = link, targetNode = it) }
            }
        }
    }

    /**
     * 获取已确认的链接
     * @param linksWithTarget 所有链接与目标节点
     * @return 已确认的链接与目标节点
     */
    fun getConfirmedLinks(linksWithTarget: List<NodeLinkWithTarget>): List<NodeLinkWithTarget> {
        return linksWithTarget.filter { it.link.confirmed }
    }

    /**
     * 获取 AI 推荐的链接（未确认）
     * @param linksWithTarget 所有链接与目标节点
     * @return AI 推荐的链接与目标节点
     */
    fun getSuggestedLinks(linksWithTarget: List<NodeLinkWithTarget>): List<NodeLinkWithTarget> {
        return linksWithTarget.filter { !it.link.confirmed && it.link.linkType == "AI_SUGGESTED" }
    }
}
