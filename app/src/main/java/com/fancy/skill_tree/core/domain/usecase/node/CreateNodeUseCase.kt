package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.common.DomainException
import com.fancy.skill_tree.core.domain.common.Outcome
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import java.util.UUID
import javax.inject.Inject

private const val MAX_CHILDREN_PER_NODE = 20

/**
 * 创建技能树节点
 * 包含业务规则验证：标题非空、类型有效性、父节点存在性、子节点上限检查
 */
class CreateNodeUseCase @Inject constructor(
    private val repository: SkillTreeRepository
) {
    /**
     * @param title 节点标题（非空）
     * @param nodeType 节点类型（ABILITY / RESOURCE）
     * @param parentId 父节点 ID（可选，null 表示根节点）
     * @param content 初始 Markdown 内容（可选）
     * @param customId 自定义节点 ID（可选）
     * @return 操作结果，包含创建后的节点或错误信息
     */
    suspend operator fun invoke(
        title: String,
        nodeType: String,
        parentId: String? = null,
        content: String? = null,
        customId: String? = null
    ): Outcome<SkillNodeEntity> {
        if (title.isBlank()) {
            return Outcome.Error(DomainException.ValidationError("title", "Title cannot be empty"))
        }

        if (nodeType !in listOf("ABILITY", "RESOURCE")) {
            return Outcome.Error(DomainException.InvalidNodeType(nodeType))
        }

        if (parentId != null) {
            val parentNode = repository.getNodeById(parentId)
                ?: return Outcome.Error(DomainException.ParentNodeNotFound(parentId))

            // 这里需要获取子节点数量，暂时不做严格限制，后续可完善
        }

        val currentTime = System.currentTimeMillis()
        val newNode = SkillNodeEntity(
            id = customId ?: UUID.randomUUID().toString(),
            parentId = parentId,
            title = title.trim(),
            content = content,
            nodeType = nodeType,
            level = 1,
            sortOrder = 0,
            isExpanded = true,
            createdAt = currentTime,
            updatedAt = currentTime
        )

        return try {
            repository.insertNode(newNode)
            Outcome.Success(newNode)
        } catch (e: Exception) {
            Outcome.Error(DomainException.StorageError(e))
        }
    }
}
