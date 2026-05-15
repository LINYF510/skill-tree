package com.fancy.skill_tree.core.domain.usecase.node

import android.net.Uri
import com.fancy.skill_tree.core.data.file.AttachmentFileManager
import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.common.Outcome
import com.fancy.skill_tree.core.domain.entity.AttachmentEntity
import javax.inject.Inject

/**
 * 添加附件 UseCase
 * 用于为节点添加附件
 */
class AddAttachmentUseCase @Inject constructor(
    private val repository: SkillTreeRepository,
    private val fileManager: AttachmentFileManager
) {
    /**
     * 添加附件
     * @param nodeId 节点 ID
     * @param sourceUri 源文件 URI
     * @param fileName 文件名
     * @param mimeType MIME 类型
     * @param fileSize 文件大小
     * @return 操作结果，包含创建的附件或错误信息
     */
    suspend operator fun invoke(
        nodeId: String,
        sourceUri: Uri,
        fileName: String,
        mimeType: String?,
        fileSize: Long?
    ): Outcome<AttachmentEntity> {
        return try {
            val filePath = fileManager.copyToInternalStorage(sourceUri, fileName)
            val attachment = repository.addAttachment(
                nodeId = nodeId,
                fileName = fileName,
                filePath = filePath,
                mimeType = mimeType,
                fileSize = fileSize
            )
            Outcome.Success(attachment)
        } catch (e: Exception) {
            Outcome.Error(com.fancy.skill_tree.core.domain.common.DomainException.StorageError(e))
        }
    }
}
