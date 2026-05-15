package com.fancy.skill_tree.core.domain.usecase.gamification

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 搜索节点 UseCase
 * 支持全文搜索（标题 + 内容）和标签筛选
 */
@Singleton
class SearchNodesUseCase @Inject constructor(
    private val repository: SkillTreeRepository
) {
    /**
     * 搜索节点
     * @param query 搜索关键词（可选）
     * @param tagIds 筛选标签 ID 列表（可选）
     * @return 匹配的节点列表 Flow
     */
    operator fun invoke(
        query: String = "",
        tagIds: List<String> = emptyList()
    ): Flow<List<SkillNodeEntity>> {
        val searchQuery = query.trim()

        // 没有搜索条件时返回所有节点
        if (searchQuery.isEmpty() && tagIds.isEmpty()) {
            return repository.getAllNodes()
        }

        // 仅按搜索关键词
        if (searchQuery.isNotEmpty() && tagIds.isEmpty()) {
            return repository.searchFullText(searchQuery)
        }

        // 仅按标签
        if (searchQuery.isEmpty() && tagIds.isNotEmpty()) {
            return repository.searchByTags(tagIds)
        }

        // 同时按关键词和标签筛选
        return combine(
            repository.searchFullText(searchQuery),
            repository.searchByTags(tagIds)
        ) { textResults, tagResults ->
            // 取两个结果的交集
            val textResultIds = textResults.map { it.id }.toSet()
            tagResults.filter { it.id in textResultIds }
        }
    }
}
