package com.fancy.skill_tree.core.data.repository

import app.cash.turbine.test
import com.fancy.skill_tree.core.data.database.dao.AttachmentDao
import com.fancy.skill_tree.core.data.database.dao.NodeLinkDao
import com.fancy.skill_tree.core.data.database.dao.NodeTagDao
import com.fancy.skill_tree.core.data.database.dao.SkillNodeDao
import com.fancy.skill_tree.core.data.database.dao.TagDao
import com.fancy.skill_tree.core.data.exception.DataException
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SkillTreeRepositoryImpl")
class SkillTreeRepositoryImplTest {

    private val skillNodeDao = mockk<SkillNodeDao>(relaxed = true)
    private val nodeLinkDao = mockk<NodeLinkDao>(relaxed = true)
    private val nodeTagDao = mockk<NodeTagDao>(relaxed = true)
    private val tagDao = mockk<TagDao>(relaxed = true)
    private val attachmentDao = mockk<AttachmentDao>(relaxed = true)
    private lateinit var repository: SkillTreeRepositoryImpl

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        repository = SkillTreeRepositoryImpl(
            skillNodeDao, tagDao, nodeTagDao, nodeLinkDao, attachmentDao
        )
    }

    @Nested
    @DisplayName("getAllNodes")
    inner class GetAllNodes {

        @Test
        @DisplayName("正常返回节点列表")
        fun returnsNodesSuccessfully() = runTest {
            val nodes = listOf(
                SkillNodeEntity(
                    id = "1", parentId = null, title = "Node1", nodeType = "ABILITY",
                    level = 1, sortOrder = 0, isExpanded = true,
                    createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
                ),
                SkillNodeEntity(
                    id = "2", parentId = null, title = "Node2", nodeType = "RESOURCE",
                    level = 1, sortOrder = 1, isExpanded = true,
                    createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
                )
            )
            every { skillNodeDao.getAllNodes() } returns flowOf(nodes)

            repository.getAllNodes().test {
                val result = awaitItem()
                assertThat(result).hasSize(2)
                assertThat(result[0].title).isEqualTo("Node1")
                assertThat(result[1].title).isEqualTo("Node2")
                awaitComplete()
            }
        }

        @Test
        @DisplayName("Flow 异常转换为 DataException")
        fun flowExceptionConvertedToDataException() = runTest {
            every { skillNodeDao.getAllNodes() } returns flow {
                throw RuntimeException("DB error")
            }

            repository.getAllNodes().test {
                awaitError().also { error ->
                    assertThat(error).isInstanceOf(DataException.DatabaseError::class.java)
                }
            }
        }
    }

    @Nested
    @DisplayName("insertNode")
    inner class InsertNode {

        @Test
        @DisplayName("插入节点成功")
        fun insertNodeSuccess() = runTest {
            val node = SkillNodeEntity(
                id = "1", parentId = null, title = "Test", nodeType = "ABILITY",
                level = 1, sortOrder = 0, isExpanded = true,
                createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
            )

            repository.insertNode(node)

            coEvery { skillNodeDao.insertNode(any()) } returns Unit
        }

        @Test
        @DisplayName("DAO 抛异常时 safeDbCall 捕获并抛 DataException")
        fun daoThrowsCapturedAsDataException() = runTest {
            coEvery { skillNodeDao.insertNode(any()) } throws android.database.sqlite.SQLiteException("DB error")

            var caughtException: Exception? = null
            try {
                repository.insertNode(
                    SkillNodeEntity(
                        id = "1", parentId = null, title = "Test", nodeType = "ABILITY",
                        level = 1, sortOrder = 0, isExpanded = true,
                        createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                caughtException = e
            }

            assertThat(caughtException).isInstanceOf(DataException.DatabaseError::class.java)
        }
    }

    @Nested
    @DisplayName("getNodeById")
    inner class GetNodeById {

        @Test
        @DisplayName("返回存在的节点")
        fun returnsExistingNode() = runTest {
            val node = SkillNodeEntity(
                id = "1", parentId = null, title = "Test", nodeType = "ABILITY",
                level = 1, sortOrder = 0, isExpanded = true,
                createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
            )
            coEvery { skillNodeDao.getNodeById("1") } returns node

            val result = repository.getNodeById("1")

            assertThat(result).isNotNull()
            assertThat(result?.title).isEqualTo("Test")
        }

        @Test
        @DisplayName("返回 null 当节点不存在")
        fun returnsNullWhenNodeNotFound() = runTest {
            coEvery { skillNodeDao.getNodeById("nonexistent") } returns null

            val result = repository.getNodeById("nonexistent")

            assertThat(result).isNull()
        }
    }

    @Nested
    @DisplayName("deleteNode")
    inner class DeleteNode {

        @Test
        @DisplayName("删除节点成功")
        fun deleteNodeSuccess() = runTest {
            coEvery { skillNodeDao.deleteNodeById("1") } returns Unit

            repository.deleteNode("1")

            coEvery { skillNodeDao.deleteNodeById("1") } returns Unit
        }
    }
}
