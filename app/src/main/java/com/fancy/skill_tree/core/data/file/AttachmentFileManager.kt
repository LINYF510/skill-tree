package com.fancy.skill_tree.core.data.file

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 附件文件管理器
 * 负责文件的存储、复制和删除
 */
@Singleton
class AttachmentFileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val attachmentsDir: File
        get() = File(context.filesDir, "attachments").also { if (!it.exists()) it.mkdirs() }

    /**
     * 将外部文件复制到应用内部存储
     * @param sourceUri 源文件的 content:// URI
     * @param originalFileName 原始文件名
     * @return 复制后的文件路径
     */
    fun copyToInternalStorage(sourceUri: Uri, originalFileName: String): String {
        val extension = originalFileName.substringAfterLast('.', "")
        val uniqueName = "${UUID.randomUUID()}.${extension}"
        val destFile = File(attachmentsDir, uniqueName)

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IOException("无法打开文件: $sourceUri")

        return destFile.absolutePath
    }

    /**
     * 删除附件文件
     */
    fun deleteFile(filePath: String): Boolean {
        return File(filePath).delete()
    }

    /**
     * 获取附件目录总大小（字节）
     */
    fun getTotalSize(): Long {
        return attachmentsDir.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }
}
