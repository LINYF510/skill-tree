package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.file.AttachmentFileManager
import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.common.Outcome
import javax.inject.Inject

/**
 * 删除附件 UseCase
 * 用于删除节点的附件
 */
class DeleteAttachmentUseCase @Inject constructor(
    private val repository: SkillTreeRepository,
    private val fileManager: AttachmentFileManager
) {
    /**
     * 删除附件
     * @param attachmentId 附件 ID
     * @return 操作结果
     */
    suspend operator fun invoke(attachmentId: String): Outcome<Unit> {
        return try {
            // 先获取附件信息以删除文件
            val attachment = repository.getAttachmentById(attachmentId)
            attachment?.filePath?.takeIf { it.isNotBlank() }?.let { filePath ->
                fileManager.deleteFile(filePath)
            }
            repository.deleteAttachment(attachmentId)
            Outcome.Success(Unit)
        } catch (e: Exception) {
            Outcome.Error(com.fancy.skill_tree.core.domain.common.DomainException.StorageError(e))
        }
    }
}
