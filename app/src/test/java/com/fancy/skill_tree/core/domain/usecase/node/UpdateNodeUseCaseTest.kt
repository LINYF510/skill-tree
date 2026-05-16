package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.common.DomainException
import com.fancy.skill_tree.core.domain.common.Outcome
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("UpdateNodeUseCase")
class UpdateNodeUseCaseTest {

    private val repository = mockk<SkillTreeRepository>(relaxed = true)
    private lateinit var useCase: UpdateNodeUseCase

    private val testNode = SkillNodeEntity(
        id = "node1", parentId = null,
        title = "Test", nodeType = "ABILITY",
        level = 1, sortOrder = 0, isExpanded = true,
        createdAt = 1000L, updatedAt = 1000L
    )

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        useCase = UpdateNodeUseCase(repository)
    }

    @Nested
    @DisplayName("更新节点")
    inner class UpdateNode {

        @Test
        @DisplayName("成功更新标题")
        fun updateTitleSuccess() = runTest {
            coEvery { repository.getNodeById("node1") } returns testNode

            val result = useCase(nodeId = "node1", title = "New Title")

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            val updated = (result as Outcome.Success).data
            assertThat(updated.title).isEqualTo("New Title")
            assertThat(updated.content).isEqualTo(testNode.content)
            assertThat(updated.nodeType).isEqualTo(testNode.nodeType)
            coVerify(exactly = 1) { repository.updateNode(any()) }
        }

        @Test
        @DisplayName("成功更新内容")
        fun updateContentSuccess() = runTest {
            coEvery { repository.getNodeById("node1") } returns testNode

            val result = useCase(nodeId = "node1", content = "New Content")

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            val updated = (result as Outcome.Success).data
            assertThat(updated.content).isEqualTo("New Content")
            assertThat(updated.title).isEqualTo(testNode.title)
            assertThat(updated.nodeType).isEqualTo(testNode.nodeType)
        }

        @Test
        @DisplayName("成功更新节点类型")
        fun updateNodeTypeSuccess() = runTest {
            coEvery { repository.getNodeById("node1") } returns testNode

            val result = useCase(nodeId = "node1", nodeType = "RESOURCE")

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            val updated = (result as Outcome.Success).data
            assertThat(updated.nodeType).isEqualTo("RESOURCE")
            assertThat(updated.title).isEqualTo(testNode.title)
        }

        @Test
        @DisplayName("节点不存在")
        fun nodeNotFoundReturnsError() = runTest {
            coEvery { repository.getNodeById("nonexistent") } returns null

            val result = useCase(nodeId = "nonexistent", title = "New Title")

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.NodeNotFound::class.java)
        }

        @Test
        @DisplayName("无效节点类型")
        fun invalidNodeTypeReturnsError() = runTest {
            coEvery { repository.getNodeById("node1") } returns testNode

            val result = useCase(nodeId = "node1", nodeType = "INVALID")

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.InvalidNodeType::class.java)
        }

        @Test
        @DisplayName("标题自动 trim")
        fun titleAutoTrim() = runTest {
            coEvery { repository.getNodeById("node1") } returns testNode

            val result = useCase(nodeId = "node1", title = "  Trimmed Title  ")

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            val updated = (result as Outcome.Success).data
            assertThat(updated.title).isEqualTo("Trimmed Title")
        }

        @Test
        @DisplayName("Repository 异常返回存储错误")
        fun repositoryThrowsReturnsStorageError() = runTest {
            coEvery { repository.getNodeById("node1") } returns testNode
            coEvery { repository.updateNode(any()) } throws RuntimeException("DB error")

            val result = useCase(nodeId = "node1", title = "New Title")

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.StorageError::class.java)
        }
    }
}
