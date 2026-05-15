package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.common.DomainException
import com.fancy.skill_tree.core.domain.common.Outcome
import javax.inject.Inject

/**
 * 移动节点到新的父节点
 * 包含循环引用检测，防止将父节点移动到其子节点下
 */
class MoveNodeUseCase @Inject constructor(
    private val repository: SkillTreeRepository
) {
    /**
     * @param nodeId 要移动的节点 ID
     * @param newParentId 新的父节点 ID（null 表示变为根节点）
     * @return 操作结果
     */
    suspend operator fun invoke(nodeId: String, newParentId: String?): Outcome<Unit> {
        val node = repository.getNodeById(nodeId)
            ?: return Outcome.Error(DomainException.NodeNotFound(nodeId))

        if (node.parentId == newParentId) {
            return Outcome.Success(Unit)
        }

        if (newParentId == nodeId) {
            return Outcome.Error(DomainException.CircularReference(nodeId, newParentId))
        }

        if (newParentId != null) {
            val parentExists = repository.getNodeById(newParentId) != null
            if (!parentExists) {
                return Outcome.Error(DomainException.ParentNodeNotFound(newParentId))
            }

            if (isDescendantOf(newParentId, nodeId)) {
                return Outcome.Error(DomainException.CircularReference(nodeId, newParentId))
            }
        }

        return try {
            val updatedNode = node.copy(
                parentId = newParentId,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateNode(updatedNode)
            Outcome.Success(Unit)
        } catch (e: Exception) {
            Outcome.Error(DomainException.StorageError(e))
        }
    }

    /**
     * 检查 targetNodeId 是否为 ancestorNodeId 的后代
     * 使用递归向上查找，防止循环引用
     */
    private suspend fun isDescendantOf(targetNodeId: String, ancestorNodeId: String): Boolean {
        var currentNodeId: String? = targetNodeId
        val visited = mutableSetOf<String>()

        while (currentNodeId != null && currentNodeId !in visited) {
            visited.add(currentNodeId)
            if (currentNodeId == ancestorNodeId) return true
            val node = repository.getNodeById(currentNodeId) ?: break
            currentNodeId = node.parentId
        }

        return false
    }
}
