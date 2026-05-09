package com.fancy.skill_tree.core.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fancy.skill_tree.core.domain.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

/**
 * 附件数据访问对象
 */
@Dao
interface AttachmentDao {

    @Query("SELECT * FROM attachment ORDER BY createdAt DESC")
    fun getAllAttachments(): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachment WHERE id = :attachmentId")
    suspend fun getAttachmentById(attachmentId: String): AttachmentEntity?

    @Query("SELECT * FROM attachment WHERE nodeId = :nodeId ORDER BY createdAt DESC")
    fun getAttachmentsForNode(nodeId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachment WHERE mimeType LIKE :mimePrefix || '%'")
    fun getAttachmentsByMimeType(mimePrefix: String): Flow<List<AttachmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: AttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(attachments: List<AttachmentEntity>)

    @Delete
    suspend fun deleteAttachment(attachment: AttachmentEntity)

    @Query("DELETE FROM attachment WHERE id = :attachmentId")
    suspend fun deleteAttachmentById(attachmentId: String)

    @Query("DELETE FROM attachment WHERE nodeId = :nodeId")
    suspend fun deleteAttachmentsForNode(nodeId: String)

    @Query("SELECT COUNT(*) FROM attachment WHERE nodeId = :nodeId")
    fun getAttachmentCountForNode(nodeId: String): Flow<Int>

    @Query("SELECT SUM(fileSize) FROM attachment")
    fun getTotalAttachmentSize(): Flow<Long?>
}
