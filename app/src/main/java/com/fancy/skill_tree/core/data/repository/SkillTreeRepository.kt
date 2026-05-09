package com.fancy.skill_tree.core.data.repository

import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import kotlinx.coroutines.flow.Flow

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
     * 根据类型获取节点数量
     * @param nodeType 节点类型 (ABILITY / RESOURCE)
     */
    fun getNodeCountByType(nodeType: String): Flow<Int>
}
