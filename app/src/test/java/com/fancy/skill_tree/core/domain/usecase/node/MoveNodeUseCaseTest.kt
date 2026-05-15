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

@DisplayName("MoveNodeUseCase")
class MoveNodeUseCaseTest {

    private val repository = mockk<SkillTreeRepository>(relaxed = true)
    private lateinit var useCase: MoveNodeUseCase

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        useCase = MoveNodeUseCase(repository)
    }

    @Nested
    @DisplayName("成功移动")
    inner class SuccessfulMove {

        @Test
        @DisplayName("移动到根节点成功")
        fun moveToRootSuccess() = runTest {
            val node = SkillNodeEntity(
                id = "node1", parentId = "parent1",
                title = "Child", nodeType = "ABILITY",
                level = 1, sortOrder = 0, isExpanded = true,
                createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
            )
            coEvery { repository.getNodeById("node1") } returns node

            val result = useCase("node1", null)

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            coVerify(exactly = 1) { repository.updateNode(any()) }
        }

        @Test
        @DisplayName("移动到新父节点成功")
        fun moveToNewParentSuccess() = runTest {
            val node = SkillNodeEntity(
                id = "node1", parentId = "parent1",
                title = "Child", nodeType = "ABILITY",
                level = 1, sortOrder = 0, isExpanded = true,
                createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
            )
            val newParent = SkillNodeEntity(
                id = "parent2", parentId = null,
                title = "NewParent", nodeType = "ABILITY",
                level = 1, sortOrder = 0, isExpanded = true,
                createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
            )
            coEvery { repository.getNodeById("node1") } returns node
            coEvery { repository.getNodeById("parent2") } returns newParent

            val result = useCase("node1", "parent2")

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
        }

        @Test
        @DisplayName("移动到相同父节点返回成功（无操作）")
        fun moveToSameParentReturnsSuccess() = runTest {
            val node = SkillNodeEntity(
                id = "node1", parentId = "parent1",
                title = "Child", nodeType = "ABILITY",
                level = 1, sortOrder = 0, isExpanded = true,
                createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
            )
            coEvery { repository.getNodeById("node1") } returns node

            val result = useCase("node1", "parent1")

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            coVerify(exactly = 0) { repository.updateNode(any()) }
        }
    }

    @Nested
    @DisplayName("错误场景")
    inner class ErrorScenarios {

        @Test
        @DisplayName("节点不存在返回 NodeNotFound")
        fun nodeNotFoundReturnsError() = runTest {
            coEvery { repository.getNodeById("nonexistent") } returns null

            val result = useCase("nonexistent", null)

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.NodeNotFound::class.java)
        }

        @Test
        @DisplayName("目标父节点不存在返回 ParentNodeNotFound")
        fun parentNotFoundReturnsError() = runTest {
            val node = SkillNodeEntity(
                id = "node1", parentId = null,
                title = "Node", nodeType = "ABILITY",
                level = 1, sortOrder = 0, isExpanded = true,
                createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
            )
            coEvery { repository.getNodeById("node1") } returns node
            coEvery { repository.getNodeById("nonexistent-parent") } returns null

            val result = useCase("node1", "nonexistent-parent")

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.ParentNodeNotFound::class.java)
        }

        @Test
        @DisplayName("移动到自身返回 CircularReference")
        fun moveToSelfReturnsCircularReference() = runTest {
            val node = SkillNodeEntity(
                id = "node1", parentId = null,
                title = "Node", nodeType = "ABILITY",
                level = 1, sortOrder = 0, isExpanded = true,
                createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
            )
            coEvery { repository.getNodeById("node1") } returns node

            val result = useCase("node1", "node1")

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.CircularReference::class.java)
        }

        @Test
        @DisplayName("将父节点移动到子节点下返回 CircularReference")
        fun moveParentToChildReturnsCircularReference() = runTest {
            val parent = SkillNodeEntity(
                id = "parent1", parentId = null,
                title = "Parent", nodeType = "ABILITY",
                level = 1, sortOrder = 0, isExpanded = true,
                createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
            )
            val child = SkillNodeEntity(
                id = "child1", parentId = "parent1",
                title = "Child", nodeType = "ABILITY",
                level = 2, sortOrder = 0, isExpanded = true,
                createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
            )
            coEvery { repository.getNodeById("parent1") } returns parent
            coEvery { repository.getNodeById("child1") } returns child

            val result = useCase("parent1", "child1")

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.CircularReference::class.java)
        }

        @Test
        @DisplayName("存储异常返回 StorageError")
        fun storageErrorReturnsStorageError() = runTest {
            val node = SkillNodeEntity(
                id = "node1", parentId = "parent1",
                title = "Node", nodeType = "ABILITY",
                level = 1, sortOrder = 0, isExpanded = true,
                createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
            )
            coEvery { repository.getNodeById("node1") } returns node
            coEvery { repository.updateNode(any()) } throws RuntimeException("DB error")

            val result = useCase("node1", null)

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.StorageError::class.java)
        }
    }
}
