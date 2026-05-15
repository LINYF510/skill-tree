package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.entity.TagEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取所有标签 UseCase
 * 用于获取系统中所有的标签
 */
class GetAllTagsUseCase @Inject constructor(
    private val repository: SkillTreeRepository
) {
    /**
     * 获取所有标签
     * @return 标签列表 Flow
     */
    operator fun invoke(): Flow<List<TagEntity>> {
        return repository.getAllTags()
    }
}
