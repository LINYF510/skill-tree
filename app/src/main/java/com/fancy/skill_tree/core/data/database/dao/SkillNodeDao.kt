package com.fancy.skill_tree.core.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import kotlinx.coroutines.flow.Flow

/**
 * 技能节点数据访问对象
 */
@Dao
interface SkillNodeDao {

    @Query("SELECT * FROM skill_node ORDER BY sortOrder ASC")
    fun getAllNodes(): Flow<List<SkillNodeEntity>>

    @Query("SELECT * FROM skill_node WHERE id = :nodeId")
    suspend fun getNodeById(nodeId: String): SkillNodeEntity?

    @Query("SELECT * FROM skill_node WHERE id = :nodeId")
    fun getNodeByIdFlow(nodeId: String): Flow<SkillNodeEntity?>

    @Query("SELECT * FROM skill_node WHERE parentId IS NULL")
    fun getRootNodes(): Flow<List<SkillNodeEntity>>

    @Query("SELECT * FROM skill_node WHERE parentId = :parentId ORDER BY sortOrder ASC")
    fun getChildNodes(parentId: String): Flow<List<SkillNodeEntity>>

    @Query("SELECT * FROM skill_node WHERE nodeType = :nodeType")
    fun getNodesByType(nodeType: String): Flow<List<SkillNodeEntity>>

    @Query("SELECT * FROM skill_node WHERE title LIKE '%' || :keyword || '%' OR content LIKE '%' || :keyword || '%'")
    fun searchNodes(keyword: String): Flow<List<SkillNodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: SkillNodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<SkillNodeEntity>)

    @Update
    suspend fun updateNode(node: SkillNodeEntity)

    @Delete
    suspend fun deleteNode(node: SkillNodeEntity)

    @Query("DELETE FROM skill_node WHERE id = :nodeId")
    suspend fun deleteNodeById(nodeId: String)

    @Query("SELECT COUNT(*) FROM skill_node")
    fun getNodeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM skill_node WHERE nodeType = :nodeType")
    fun getNodeCountByType(nodeType: String): Flow<Int>

    @Query("SELECT MAX(level) FROM skill_node")
    fun getMaxDepth(): Flow<Int?>
}
