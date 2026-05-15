package com.fancy.skill_tree.core.data.repository

import com.fancy.skill_tree.core.domain.entity.AttachmentEntity
import com.fancy.skill_tree.core.domain.entity.NodeLinkEntity
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import com.fancy.skill_tree.core.domain.entity.TagEntity
import kotlinx.coroutines.flow.Flow

/**
 * 节点链接与目标节点的数据类
 * @param link 链接实体
 * @param targetNode 目标节点实体
 */
data class NodeLinkWithTarget(
    val link: NodeLinkEntity,
    val targetNode: SkillNodeEntity
)

/**
 * 技能树数据仓库接口
 * 封装所有节点相关的数据库操作
 */
interface SkillTreeRepository {

    /**
     * 获取所有节点
     */
    fun getAllNodes(): Flow<List<SkillNodeEntity>>

    /**
     * 获取所有根节点（parentId 为 null 的节点）
     */
    fun getRootNodes(): Flow<List<SkillNodeEntity>>

    /**
     * 获取指定父节点的直接子节点
     * @param parentId 父节点 ID
     */
    fun getChildNodes(parentId: String): Flow<List<SkillNodeEntity>>

    /**
     * 插入或更新节点
     * @param node 要插入的节点
     */
    suspend fun insertNode(node: SkillNodeEntity)

    /**
     * 更新节点
     * @param node 要更新的节点
     */
    suspend fun updateNode(node: SkillNodeEntity)

    /**
     * 根据节点 ID 删除节点及其所有子节点
     * @param nodeId 要删除的节点 ID
     */
    suspend fun deleteNode(nodeId: String)

    /**
     * 获取节点及其关联的标签
     * @param nodeId 节点 ID
     * @return 节点及其标签的 Flow
     */
    fun getNodeWithTags(nodeId: String): Flow<SkillNodeWithTags?>

    /**
     * 根据节点 ID 获取单个节点
     * @param nodeId 节点 ID
     */
    suspend fun getNodeById(nodeId: String): SkillNodeEntity?

    /**
     * 获取节点数量
     */
    fun getNodeCount(): Flow<Int>

    /**
     * 获取链接数量
     */
    fun getLinkCount(): Flow<Int>

    /**
     * 根据类型获取节点数量
     * @param nodeType 节点类型 (ABILITY / RESOURCE)
     */
    fun getNodeCountByType(nodeType: String): Flow<Int>

    /**
     * 获取技能树的最大深度
     */
    fun getMaxDepth(): Flow<Int>

    /**
     * 清除所有数据
     */
    suspend fun deleteAllData()

    /**
     * 获取所有标签
     * @return 标签列表 Flow
     */
    fun getAllTags(): Flow<List<TagEntity>>

    /**
     * 根据 ID 获取标签
     * @param tagId 标签 ID
     * @return 标签实体，不存在则返回 null
     */
    suspend fun getTagById(tagId: String): TagEntity?

    /**
     * 创建新标签
     * @param name 标签名称
     * @param color 标签颜色（#RRGGBB 格式，可选）
     * @return 创建的标签实体
     */
    suspend fun createTag(name: String, color: String? = null): TagEntity

    /**
     * 删除标签
     * @param tagId 标签 ID
     */
    suspend fun deleteTag(tagId: String)

    /**
     * 为节点分配标签
     * @param nodeId 节点 ID
     * @param tagId 标签 ID
     */
    suspend fun assignTagToNode(nodeId: String, tagId: String)

    /**
     * 移除节点上的标签
     * @param nodeId 节点 ID
     * @param tagId 标签 ID
     */
    suspend fun removeTagFromNode(nodeId: String, tagId: String)

    /**
     * 获取节点的所有标签
     * @param nodeId 节点 ID
     * @return 标签列表 Flow
     */
    fun getTagsForNode(nodeId: String): Flow<List<TagEntity>>

    /**
     * 获取带有指定标签的所有节点
     * @param tagId 标签 ID
     * @return 节点列表 Flow
     */
    fun getNodesWithTag(tagId: String): Flow<List<SkillNodeEntity>>

    /**
     * 获取所有链接
     * @return 链接列表 Flow
     */
    fun getAllLinks(): Flow<List<NodeLinkEntity>>

    /**
     * 获取指定节点的所有链接
     * @param nodeId 节点 ID
     * @return 链接列表 Flow
     */
    fun getLinksForNode(nodeId: String): Flow<List<NodeLinkEntity>>

    /**
     * 创建新链接
     * @param sourceId 源节点 ID
     * @param targetId 目标节点 ID
     * @param linkType 链接类型，默认为 "MANUAL"
     * @return 创建的链接实体
     */
    suspend fun createLink(sourceId: String, targetId: String, linkType: String = "MANUAL"): NodeLinkEntity

    /**
     * 删除链接
     * @param linkId 链接 ID
     */
    suspend fun deleteLink(linkId: String)

    /**
     * 确认链接（将 AI 推荐标记为已确认）
     * @param linkId 链接 ID
     */
    suspend fun confirmLink(linkId: String)

    /**
     * 获取指定节点的附件
     * @param nodeId 节点 ID
     * @return 附件列表 Flow
     */
    fun getAttachmentsForNode(nodeId: String): Flow<List<AttachmentEntity>>

    /**
     * 根据 ID 获取附件
     * @param attachmentId 附件 ID
     * @return 附件实体，不存在则返回 null
     */
    suspend fun getAttachmentById(attachmentId: String): AttachmentEntity?

    /**
     * 添加附件
     * @param nodeId 节点 ID
     * @param fileName 文件名
     * @param filePath 文件路径
     * @param mimeType MIME 类型
     * @param fileSize 文件大小（字节）
     * @return 创建的附件实体
     */
    suspend fun addAttachment(
        nodeId: String, 
        fileName: String, 
        filePath: String, 
        mimeType: String?, 
        fileSize: Long?
    ): AttachmentEntity

    /**
     * 删除附件
     * @param attachmentId 附件 ID
     */
    suspend fun deleteAttachment(attachmentId: String)

    /**
     * 获取指定节点的附件数量
     * @param nodeId 节点 ID
     * @return 附件数量 Flow
     */
    fun getAttachmentCountForNode(nodeId: String): Flow<Int>

    /**
     * 全文搜索节点（标题 + 内容）
     * @param query 搜索关键词
     * @return 匹配的节点列表 Flow
     */
    fun searchFullText(query: String): Flow<List<SkillNodeEntity>>

    /**
     * 按标签搜索节点
     * @param tagIds 标签 ID 列表
     * @return 匹配的节点列表 Flow
     */
    fun searchByTags(tagIds: List<String>): Flow<List<SkillNodeEntity>>
}
