package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.common.DomainException
import com.fancy.skill_tree.core.domain.common.Outcome
import com.fancy.skill_tree.core.domain.entity.TagEntity
import javax.inject.Inject

/**
 * 创建标签 UseCase
 * 用于创建新的标签实体
 */
class CreateTagUseCase @Inject constructor(
    private val repository: SkillTreeRepository
) {
    /**
     * 创建新标签
     * @param name 标签名称（非空）
     * @param color 标签颜色（#RRGGBB 格式，可选）
     * @return 操作结果，包含创建的标签或错误信息
     */
    suspend operator fun invoke(
        name: String,
        color: String? = null
    ): Outcome<TagEntity> {
        if (name.isBlank()) {
            return Outcome.Error(DomainException.ValidationError("name", "Tag name cannot be empty"))
        }
        return try {
            val tag = repository.createTag(name, color)
            Outcome.Success(tag)
        } catch (e: Exception) {
            Outcome.Error(DomainException.StorageError(e))
        }
    }
}
