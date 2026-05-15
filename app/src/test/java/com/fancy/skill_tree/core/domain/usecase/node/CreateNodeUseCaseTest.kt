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

@DisplayName("CreateNodeUseCase")
class CreateNodeUseCaseTest {

    private val repository = mockk<SkillTreeRepository>(relaxed = true)
    private lateinit var useCase: CreateNodeUseCase

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        useCase = CreateNodeUseCase(repository)
    }

    @Nested
    @DisplayName("创建根节点")
    inner class CreateRootNode {

        @Test
        @DisplayName("创建根节点成功")
        fun createRootNodeSuccess() = runTest {
            val title = "Python"
            val nodeType = "ABILITY"

            val result = useCase(title, nodeType)

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            val node = (result as Outcome.Success).data
            assertThat(node.title).isEqualTo("Python")
            assertThat(node.nodeType).isEqualTo("ABILITY")
            assertThat(node.parentId).isNull()
            assertThat(node.id).isNotEmpty()
            coVerify(exactly = 1) { repository.insertNode(any()) }
        }

        @Test
        @DisplayName("标题为空应返回验证错误")
        fun blankTitleReturnsError() = runTest {
            val result = useCase("", "ABILITY")

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.ValidationError::class.java)
        }

        @Test
        @DisplayName("标题仅空格应返回验证错误")
        fun whitespaceOnlyTitleReturnsError() = runTest {
            val result = useCase("   ", "ABILITY")

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
        }

        @Test
        @DisplayName("无效的节点类型应返回错误")
        fun invalidNodeTypeReturnsError() = runTest {
            val result = useCase("Python", "INVALID_TYPE")

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.InvalidNodeType::class.java)
        }

        @Test
        @DisplayName("RESOURCE 类型节点创建成功")
        fun resourceTypeNodeSuccess() = runTest {
            val result = useCase("设计资源", "RESOURCE")

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            val node = (result as Outcome.Success).data
            assertThat(node.nodeType).isEqualTo("RESOURCE")
        }

        @Test
        @DisplayName("自定义 ID 应正确设置")
        fun customIdSetCorrectly() = runTest {
            val result = useCase("Test", "ABILITY", customId = "my-custom-id")

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            val node = (result as Outcome.Success).data
            assertThat(node.id).isEqualTo("my-custom-id")
        }

        @Test
        @DisplayName("标题应去除前后空格")
        fun titleTrimmed() = runTest {
            val result = useCase("  Python  ", "ABILITY")

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            val node = (result as Outcome.Success).data
            assertThat(node.title).isEqualTo("Python")
        }
    }

    @Nested
    @DisplayName("创建子节点")
    inner class CreateChildNode {

        @Test
        @DisplayName("父节点不存在应返回错误")
        fun parentNotFoundReturnsError() = runTest {
            coEvery { repository.getNodeById("nonexistent") } returns null

            val result = useCase("Child", "ABILITY", parentId = "nonexistent")

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.ParentNodeNotFound::class.java)
        }

        @Test
        @DisplayName("父节点存在时创建成功")
        fun parentExistsCreateSuccess() = runTest {
            val parentNode = SkillNodeEntity(
                id = "parent1", parentId = null,
                title = "Parent", nodeType = "ABILITY",
                level = 1, sortOrder = 0, isExpanded = true,
                createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
            )
            coEvery { repository.getNodeById("parent1") } returns parentNode

            val result = useCase("Child", "ABILITY", parentId = "parent1")

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            val node = (result as Outcome.Success).data
            assertThat(node.parentId).isEqualTo("parent1")
        }
    }

    @Nested
    @DisplayName("存储异常")
    inner class StorageError {

        @Test
        @DisplayName("插入异常应返回存储错误")
        fun insertThrowsReturnsStorageError() = runTest {
            coEvery { repository.insertNode(any()) } throws RuntimeException("DB error")

            val result = useCase("Test", "ABILITY")

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.StorageError::class.java)
        }
    }
}
