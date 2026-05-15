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

@DisplayName("DeleteNodeUseCase")
class DeleteNodeUseCaseTest {

    private val repository = mockk<SkillTreeRepository>(relaxed = true)
    private lateinit var useCase: DeleteNodeUseCase

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        useCase = DeleteNodeUseCase(repository)
    }

    @Nested
    @DisplayName("成功删除")
    inner class SuccessfulDelete {

        @Test
        @DisplayName("删除存在的节点成功")
        fun deleteExistingNodeSuccess() = runTest {
            val node = SkillNodeEntity(
                id = "node1", parentId = null,
                title = "Test", nodeType = "ABILITY",
                level = 1, sortOrder = 0, isExpanded = true,
                createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
            )
            coEvery { repository.getNodeById("node1") } returns node

            val result = useCase("node1")

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            coVerify(exactly = 1) { repository.deleteNode("node1") }
        }
    }

    @Nested
    @DisplayName("错误场景")
    inner class ErrorScenarios {

        @Test
        @DisplayName("删除不存在的节点返回 NodeNotFound")
        fun deleteNonexistentNodeReturnsError() = runTest {
            coEvery { repository.getNodeById("nonexistent") } returns null

            val result = useCase("nonexistent")

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.NodeNotFound::class.java)
        }

        @Test
        @DisplayName("存储异常返回 StorageError")
        fun storageErrorReturnsStorageError() = runTest {
            val node = SkillNodeEntity(
                id = "node1", parentId = null,
                title = "Test", nodeType = "ABILITY",
                level = 1, sortOrder = 0, isExpanded = true,
                createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
            )
            coEvery { repository.getNodeById("node1") } returns node
            coEvery { repository.deleteNode("node1") } throws RuntimeException("DB error")

            val result = useCase("node1")

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.StorageError::class.java)
        }
    }
}
