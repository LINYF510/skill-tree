package com.fancy.skill_tree.core.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.fancy.skill_tree.core.domain.entity.NodeTagCrossRef
import com.fancy.skill_tree.core.domain.entity.TagEntity
import kotlinx.coroutines.flow.Flow

/**
 * 标签数据访问对象
 */
@Dao
interface TagDao {

    @Query("SELECT * FROM tag ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tag WHERE id = :tagId")
    suspend fun getTagById(tagId: String): TagEntity?

    @Query("SELECT * FROM tag WHERE name = :name")
    suspend fun getTagByName(name: String): TagEntity?

    @Query("SELECT * FROM tag WHERE source = :source")
    fun getTagsBySource(source: String): Flow<List<TagEntity>>

    @Query("SELECT * FROM tag WHERE name LIKE '%' || :keyword || '%'")
    fun searchTags(keyword: String): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<TagEntity>)

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Query("DELETE FROM tag WHERE id = :tagId")
    suspend fun deleteTagById(tagId: String)

    @Query("SELECT COUNT(*) FROM tag")
    fun getTagCount(): Flow<Int>
}

/**
 * 节点-标签关联数据访问对象
 */
@Dao
interface NodeTagDao {

    @Query("""
        SELECT t.* FROM tag t
        INNER JOIN node_tag nt ON t.id = nt.tagId
        WHERE nt.nodeId = :nodeId
    """)
    fun getTagsForNode(nodeId: String): Flow<List<TagEntity>>

    @Query("""
        SELECT n.* FROM skill_node n
        INNER JOIN node_tag nt ON n.id = nt.nodeId
        WHERE nt.tagId = :tagId
    """)
    fun getNodesWithTag(tagId: String): Flow<List<com.fancy.skill_tree.core.domain.entity.SkillNodeEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNodeTag(crossRef: NodeTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNodeTags(crossRefs: List<NodeTagCrossRef>)

    @Delete
    suspend fun deleteNodeTag(crossRef: NodeTagCrossRef)

    @Query("DELETE FROM node_tag WHERE nodeId = :nodeId")
    suspend fun deleteAllTagsForNode(nodeId: String)

    @Query("DELETE FROM node_tag WHERE tagId = :tagId")
    suspend fun deleteAllNodesForTag(tagId: String)

    @Query("SELECT COUNT(*) FROM node_tag WHERE nodeId = :nodeId")
    fun getTagCountForNode(nodeId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM node_tag WHERE tagId = :tagId")
    fun getNodeCountForTag(tagId: String): Flow<Int>
}
