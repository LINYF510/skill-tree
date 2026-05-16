package com.fancy.skill_tree.core.data.repository

import com.fancy.skill_tree.core.data.database.dao.AttachmentDao
import com.fancy.skill_tree.core.data.database.dao.NodeLinkDao
import com.fancy.skill_tree.core.data.database.dao.NodeTagDao
import com.fancy.skill_tree.core.data.database.dao.SkillNodeDao
import com.fancy.skill_tree.core.data.database.dao.TagDao
import com.fancy.skill_tree.core.data.exception.DataException
import com.fancy.skill_tree.core.domain.entity.AttachmentEntity
import com.fancy.skill_tree.core.domain.entity.NodeLinkEntity
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import com.fancy.skill_tree.core.domain.entity.TagEntity
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 技能树数据仓库实现类
 * 封装所有节点相关的数据库操作，使用 safeDbCall 进行错误捕获
 */
@Singleton
class SkillTreeRepositoryImpl @Inject constructor(
    private val skillNodeDao: SkillNodeDao,
    private val tagDao: TagDao,
    private val nodeTagDao: NodeTagDao,
    private val nodeLinkDao: NodeLinkDao,
    private val attachmentDao: AttachmentDao
) : SkillTreeRepository {

    override fun getAllNodes(): Flow<List<SkillNodeEntity>> {
        return skillNodeDao.getAllNodes()
            .catch { e -> throw if (e is DataException) e else DataException.DatabaseError(e) }
    }

    override fun getRootNodes(): Flow<List<SkillNodeEntity>> {
        return skillNodeDao.getRootNodes()
            .catch { e -> throw if (e is DataException) e else DataException.DatabaseError(e) }
    }

    override fun getChildNodes(parentId: String): Flow<List<SkillNodeEntity>> {
        return skillNodeDao.getChildNodes(parentId)
            .catch { e -> throw if (e is DataException) e else DataException.DatabaseError(e) }
    }

    override suspend fun insertNode(node: SkillNodeEntity) {
        safeDbCall { skillNodeDao.insertNode(node) }.getOrThrow()
    }

    override suspend fun updateNode(node: SkillNodeEntity) {
        safeDbCall { skillNodeDao.updateNode(node) }.getOrThrow()
    }

    override suspend fun deleteNode(nodeId: String) {
        safeDbCall { skillNodeDao.deleteNodeById(nodeId) }.getOrThrow()
    }

    override fun getNodeWithTags(nodeId: String): Flow<SkillNodeWithTags?> {
        return combine(
            skillNodeDao.getNodeByIdFlow(nodeId),
            nodeTagDao.getTagsForNode(nodeId)
        ) { node, tags ->
            node?.let {
                SkillNodeWithTags(node = it, tags = tags)
            }
        }.catch { e -> throw if (e is DataException) e else DataException.DatabaseError(e) }
    }

    override suspend fun getNodeById(nodeId: String): SkillNodeEntity? {
        return safeDbCall { skillNodeDao.getNodeById(nodeId) }.getOrNull()
    }

    override fun getNodeCount(): Flow<Int> {
        return skillNodeDao.getNodeCount()
            .catch { e -> throw if (e is DataException) e else DataException.DatabaseError(e) }
    }

    override fun getLinkCount(): Flow<Int> {
        return nodeLinkDao.getLinkCount()
            .catch { e -> throw if (e is DataException) e else DataException.DatabaseError(e) }
    }

    override fun getNodeCountByType(nodeType: String): Flow<Int> {
        return skillNodeDao.getNodeCountByType(nodeType)
            .catch { e -> throw if (e is DataException) e else DataException.DatabaseError(e) }
    }

    override fun getMaxDepth(): Flow<Int> {
        return skillNodeDao.getMaxDepth()
            .map { it ?: 0 }
            .catch { e -> throw if (e is DataException) e else DataException.DatabaseError(e) }
    }

    override suspend fun deleteAllData() {
        safeDbCall { skillNodeDao.deleteAllNodes() }.getOrThrow()
    }

    override fun getAllTags(): Flow<List<TagEntity>> {
        return tagDao.getAllTags()
            .catch { e -> throw if (e is DataException) e else DataException.DatabaseError(e) }
    }

    override suspend fun getTagById(tagId: String): TagEntity? {
        return safeDbCall { tagDao.getTagById(tagId) }.getOrNull()
    }

    override suspend fun createTag(name: String, color: String?): TagEntity {
        val tag = TagEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            color = color
        )
        safeDbCall { tagDao.insertTag(tag) }.getOrThrow()
        return tag
    }

    override suspend fun deleteTag(tagId: String) {
        safeDbCall {
            nodeTagDao.deleteAllNodesForTag(tagId)
            tagDao.deleteTagById(tagId)
        }.getOrThrow()
    }

    override suspend fun assignTagToNode(nodeId: String, tagId: String) {
        safeDbCall {
            nodeTagDao.insertNodeTag(
                com.fancy.skill_tree.core.domain.entity.NodeTagCrossRef(
                    nodeId = nodeId,
                    tagId = tagId
                )
            )
        }.getOrThrow()
    }

    override suspend fun removeTagFromNode(nodeId: String, tagId: String) {
        safeDbCall {
            nodeTagDao.deleteNodeTag(
                com.fancy.skill_tree.core.domain.entity.NodeTagCrossRef(
                    nodeId = nodeId,
                    tagId = tagId
                )
            )
        }.getOrThrow()
    }

    override fun getTagsForNode(nodeId: String): Flow<List<TagEntity>> {
        return nodeTagDao.getTagsForNode(nodeId)
            .catch { e -> throw if (e is DataException) e else DataException.DatabaseError(e) }
    }

    override fun getNodesWithTag(tagId: String): Flow<List<SkillNodeEntity>> {
        return nodeTagDao.getNodesWithTag(tagId)
            .catch { e -> throw if (e is DataException) e else DataException.DatabaseError(e) }
    }

    override fun getAllLinks(): Flow<List<NodeLinkEntity>> {
        return nodeLinkDao.getAllLinks()
            .catch { e -> throw if (e is DataException) e else DataException.DatabaseError(e) }
    }

    override fun getLinksForNode(nodeId: String): Flow<List<NodeLinkEntity>> {
        return nodeLinkDao.getLinksByNodeId(nodeId)
            .catch { e -> throw if (e is DataException) e else DataException.DatabaseError(e) }
    }

    override suspend fun createLink(sourceId: String, targetId: String, linkType: String): NodeLinkEntity {
        val link = NodeLinkEntity(
            id = UUID.randomUUID().toString(),
            sourceId = sourceId,
            targetId = targetId,
            linkType = linkType
        )
        safeDbCall { nodeLinkDao.insertLink(link) }.getOrThrow()
        return link
    }

    override suspend fun deleteLink(linkId: String) {
        safeDbCall { nodeLinkDao.deleteLinkById(linkId) }.getOrThrow()
    }

    override suspend fun confirmLink(linkId: String) {
        safeDbCall {
            val link = nodeLinkDao.getLinkById(linkId) ?: return@safeDbCall
            nodeLinkDao.insertLink(link.copy(confirmed = true))
        }.getOrThrow()
    }

    override fun getAttachmentsForNode(nodeId: String): Flow<List<AttachmentEntity>> {
        return attachmentDao.getAttachmentsForNode(nodeId)
            .catch { e -> throw if (e is DataException) e else DataException.DatabaseError(e) }
    }

    override suspend fun getAttachmentById(attachmentId: String): AttachmentEntity? {
        return safeDbCall { attachmentDao.getAttachmentById(attachmentId) }.getOrNull()
    }

    override suspend fun addAttachment(
        nodeId: String,
        fileName: String,
        filePath: String,
        mimeType: String?,
        fileSize: Long?
    ): AttachmentEntity {
        val attachment = AttachmentEntity(
            id = UUID.randomUUID().toString(),
            nodeId = nodeId,
            fileName = fileName,
            filePath = filePath,
            mimeType = mimeType,
            fileSize = fileSize
        )
        safeDbCall { attachmentDao.insertAttachment(attachment) }.getOrThrow()
        return attachment
    }

    override suspend fun deleteAttachment(attachmentId: String) {
        safeDbCall { attachmentDao.deleteAttachmentById(attachmentId) }.getOrThrow()
    }

    override fun getAttachmentCountForNode(nodeId: String): Flow<Int> {
        return attachmentDao.getAttachmentCountForNode(nodeId)
            .catch { e -> throw if (e is DataException) e else DataException.DatabaseError(e) }
    }

    override fun searchFullText(query: String): Flow<List<SkillNodeEntity>> {
        return skillNodeDao.searchFullText(query)
            .catch { e -> throw if (e is DataException) e else DataException.DatabaseError(e) }
    }

    override fun searchByTags(tagIds: List<String>): Flow<List<SkillNodeEntity>> {
        return skillNodeDao.searchByTags(tagIds)
            .catch { e -> throw if (e is DataException) e else DataException.DatabaseError(e) }
    }

    override suspend fun getConfirmedAiLinkCount(): Int {
        return safeDbCall {
            nodeLinkDao.getConfirmedAiLinkCount().first()
        }.getOrDefault(0)
    }

    override suspend fun getImageAttachmentCount(): Int {
        return safeDbCall {
            attachmentDao.getAttachmentsByMimeType("image/").first().size
        }.getOrDefault(0)
    }
}
