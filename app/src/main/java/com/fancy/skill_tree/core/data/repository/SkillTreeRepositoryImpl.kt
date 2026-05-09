package com.fancy.skill_tree.core.data.repository

import com.fancy.skill_tree.core.data.database.dao.NodeTagDao
import com.fancy.skill_tree.core.data.database.dao.SkillNodeDao
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 技能树数据仓库实现类
 * 封装所有节点相关的数据库操作
 */
@Singleton
class SkillTreeRepositoryImpl @Inject constructor(
    private val skillNodeDao: SkillNodeDao,
    private val nodeTagDao: NodeTagDao
) : SkillTreeRepository {

    override fun getAllNodes(): Flow<List<SkillNodeEntity>> {
        return skillNodeDao.getAllNodes()
    }

    override fun getRootNodes(): Flow<List<SkillNodeEntity>> {
        return skillNodeDao.getRootNodes()
    }

    override fun getChildNodes(parentId: String): Flow<List<SkillNodeEntity>> {
        return skillNodeDao.getChildNodes(parentId)
    }

    override suspend fun insertNode(node: SkillNodeEntity) {
        skillNodeDao.insertNode(node)
    }

    override suspend fun updateNode(node: SkillNodeEntity) {
        skillNodeDao.updateNode(node)
    }

    override suspend fun deleteNode(nodeId: String) {
        skillNodeDao.deleteNodeById(nodeId)
    }

    override fun getNodeWithTags(nodeId: String): Flow<SkillNodeWithTags?> {
        return combine(
            skillNodeDao.getNodeByIdFlow(nodeId),
            nodeTagDao.getTagsForNode(nodeId)
        ) { node, tags ->
            node?.let {
                SkillNodeWithTags(node = it, tags = tags)
            }
        }
    }

    override suspend fun getNodeById(nodeId: String): SkillNodeEntity? {
        return skillNodeDao.getNodeById(nodeId)
    }

    override fun getNodeCount(): Flow<Int> {
        return skillNodeDao.getNodeCount()
    }

    override fun getNodeCountByType(nodeType: String): Flow<Int> {
        return skillNodeDao.getNodeCountByType(nodeType)
    }
}
