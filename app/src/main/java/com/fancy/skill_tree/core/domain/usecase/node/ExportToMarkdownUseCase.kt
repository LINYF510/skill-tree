package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.common.DomainException
import com.fancy.skill_tree.core.domain.common.Outcome
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 导出技能树为 Markdown 格式
 */
class ExportToMarkdownUseCase @Inject constructor(
    private val repository: SkillTreeRepository
) {
    /**
     * @return Markdown 格式的字符串
     */
    suspend operator fun invoke(): Outcome<String> {
        return try {
            val nodes = repository.getAllNodes().first()
            val markdown = exportToMarkdown(nodes)
            Outcome.Success(markdown)
        } catch (e: Exception) {
            Outcome.Error(DomainException.StorageError(e))
        }
    }

    private fun exportToMarkdown(nodes: List<SkillNodeEntity>): String {
        if (nodes.isEmpty()) return "# 技能树\n\n暂无节点数据\n"

        val rootNodes = nodes.filter { it.parentId == null }
        val sb = StringBuilder()
        sb.appendLine("# 技能树")
        sb.appendLine()

        fun appendNode(node: SkillNodeEntity, depth: Int) {
            val indent = "  ".repeat(depth)
            val typeIcon = if (node.nodeType == "ABILITY") "⚔️" else "💎"
            sb.appendLine("${indent}- $typeIcon **${node.title}**")
            if (!node.content.isNullOrBlank()) {
                node.content.lines().forEach { line ->
                    sb.appendLine("${indent}  $line")
                }
            }
            val children = nodes.filter { it.parentId == node.id }
            children.forEach { child -> appendNode(child, depth + 1) }
        }

        rootNodes.forEach { root -> appendNode(root, 0) }
        return sb.toString()
    }
}
