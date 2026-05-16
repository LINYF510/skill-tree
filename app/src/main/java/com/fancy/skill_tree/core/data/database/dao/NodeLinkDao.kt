package com.fancy.skill_tree.core.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fancy.skill_tree.core.domain.entity.NodeLinkEntity
import kotlinx.coroutines.flow.Flow

/**
 * 节点链接数据访问对象
 */
@Dao
interface NodeLinkDao {

    @Query("SELECT * FROM node_link")
    fun getAllLinks(): Flow<List<NodeLinkEntity>>

    @Query("SELECT * FROM node_link WHERE id = :linkId")
    suspend fun getLinkById(linkId: String): NodeLinkEntity?

    @Query("SELECT * FROM node_link WHERE sourceId = :nodeId OR targetId = :nodeId")
    fun getLinksByNodeId(nodeId: String): Flow<List<NodeLinkEntity>>

    @Query("SELECT * FROM node_link WHERE sourceId = :sourceId AND targetId = :targetId")
    suspend fun getLinkBetweenNodes(sourceId: String, targetId: String): NodeLinkEntity?

    @Query("SELECT * FROM node_link WHERE linkType = :linkType")
    fun getLinksByType(linkType: String): Flow<List<NodeLinkEntity>>

    @Query("SELECT * FROM node_link WHERE confirmed = :confirmed")
    fun getLinksByConfirmation(confirmed: Boolean): Flow<List<NodeLinkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: NodeLinkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLinks(links: List<NodeLinkEntity>)

    @Delete
    suspend fun deleteLink(link: NodeLinkEntity)

    @Query("DELETE FROM node_link WHERE id = :linkId")
    suspend fun deleteLinkById(linkId: String)

    @Query("DELETE FROM node_link WHERE sourceId = :nodeId OR targetId = :nodeId")
    suspend fun deleteLinksByNodeId(nodeId: String)

    @Query("SELECT COUNT(*) FROM node_link")
    fun getLinkCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM node_link WHERE linkType = 'MANUAL'")
    fun getManualLinkCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM node_link WHERE linkType = 'AI_SUGGESTED'")
    fun getAiSuggestedLinkCount(): Flow<Int>

    /**
     * 获取已确认的 AI 推荐链接数量
     */
    @Query("SELECT COUNT(*) FROM node_link WHERE linkType = 'AI_SUGGESTED' AND confirmed = 1")
    fun getConfirmedAiLinkCount(): Flow<Int>
}
