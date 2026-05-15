package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.common.Outcome
import javax.inject.Inject

/**
 * 清除所有数据 UseCase
 */
class ClearAllDataUseCase @Inject constructor(
    private val repository: SkillTreeRepository
) {
    /**
     * 清除所有数据
     * @return 操作结果
     */
    suspend operator fun invoke(): Outcome<Unit> {
        return try {
            repository.deleteAllData()
            Outcome.Success(Unit)
        } catch (e: Exception) {
            Outcome.Error(com.fancy.skill_tree.core.domain.common.DomainException.StorageError(e))
        }
    }
}
