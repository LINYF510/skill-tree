package com.fancy.skill_tree.core.domain.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 技能节点实体
 * 表示技能树中的一个节点，可以是能力节点或物质节点
 */
@Entity(
    tableName = "skill_node",
    foreignKeys = [
        ForeignKey(
            entity = SkillNodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("parentId"),
        Index("nodeType"),
        Index("title"),
        Index("createdAt")
    ]
)
data class SkillNodeEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val parentId: String? = null,
    val title: String,
    val content: String? = null,
    val nodeType: String = "ABILITY",
    val icon: String? = null,
    val level: Int = 1,
    val sortOrder: Int = 0,
    val isExpanded: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val metadata: String? = null
)

/**
 * 节点链接实体
 * 表示两个节点之间的跨分支关联关系
 */
@Entity(
    tableName = "node_link",
    foreignKeys = [
        ForeignKey(
            entity = SkillNodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SkillNodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["targetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sourceId"), Index("targetId")]
)
data class NodeLinkEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sourceId: String,
    val targetId: String,
    val linkType: String = "MANUAL",
    val bidirectional: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val confirmed: Boolean = false
)

/**
 * 标签实体
 * 用于对节点进行分类和检索的关键词标签
 */
@Entity(tableName = "tag")
data class TagEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val color: String? = null,
    val source: String = "MANUAL"
)

/**
 * 节点-标签关联实体
 * 表示节点和标签之间的多对多关联关系
 */
@Entity(
    tableName = "node_tag",
    primaryKeys = ["nodeId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = SkillNodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["nodeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("nodeId"), Index("tagId")]
)
data class NodeTagCrossRef(
    val nodeId: String,
    val tagId: String
)

/**
 * 附件实体
 * 存储与节点关联的文件附件信息
 */
@Entity(
    tableName = "attachment",
    foreignKeys = [
        ForeignKey(
            entity = SkillNodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["nodeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("nodeId")]
)
data class AttachmentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val nodeId: String,
    val fileName: String,
    val filePath: String,
    val mimeType: String? = null,
    val fileSize: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
