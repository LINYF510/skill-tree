package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.common.DomainException
import com.fancy.skill_tree.core.domain.common.Outcome
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ExportToMarkdownUseCase")
class ExportToMarkdownUseCaseTest {

    private val repository = mockk<SkillTreeRepository>(relaxed = true)

    private lateinit var useCase: ExportToMarkdownUseCase

    private val abilityNode = SkillNodeEntity(
        id = "node1",
        parentId = null,
        title = "Test",
        nodeType = "ABILITY",
        level = 1,
        sortOrder = 0,
        isExpanded = true,
        createdAt = 1000L,
        updatedAt = 1000L
    )

    private val resourceNode = SkillNodeEntity(
        id = "node2",
        parentId = null,
        title = "Resource",
        nodeType = "RESOURCE",
        level = 1,
        sortOrder = 0,
        isExpanded = true,
        createdAt = 1000L,
        updatedAt = 1000L
    )

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        useCase = ExportToMarkdownUseCase(repository)
    }

    @Nested
    @DisplayName("导出 Markdown")
    inner class ExportMarkdown {

        @Test
        @DisplayName("空节点列表 → 返回占位文本")
        fun emptyNodeList_returnsPlaceholder() = runTest {
            coEvery { repository.getAllNodes() } returns flowOf(emptyList())

            val result = useCase()

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            val markdown = (result as Outcome.Success).data
            assertThat(markdown).isEqualTo("# 技能树\n\n暂无节点数据\n")
        }

        @Test
        @DisplayName("单个根节点 → 正确输出标题")
        fun singleRootNode_outputsTitle() = runTest {
            coEvery { repository.getAllNodes() } returns flowOf(listOf(abilityNode))

            val result = useCase()

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            val markdown = (result as Outcome.Success).data
            assertThat(markdown).contains("- ⚔️ **Test**")
        }

        @Test
        @DisplayName("带子节点的树 → 递归缩进正确")
        fun treeWithChildren_recursiveIndentation() = runTest {
            val childNode = SkillNodeEntity(
                id = "child1",
                parentId = "node1",
                title = "Child",
                nodeType = "ABILITY",
                level = 2,
                sortOrder = 0,
                isExpanded = true,
                createdAt = 1000L,
                updatedAt = 1000L
            )
            coEvery { repository.getAllNodes() } returns flowOf(listOf(abilityNode, childNode))

            val result = useCase()

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            val markdown = (result as Outcome.Success).data
            assertThat(markdown).contains("- ⚔️ **Test**")
            assertThat(markdown).contains("  - ⚔️ **Child**")
        }

        @Test
        @DisplayName("ABILITY 节点 → ⚔️ 图标")
        fun abilityNode_swordIcon() = runTest {
            coEvery { repository.getAllNodes() } returns flowOf(listOf(abilityNode))

            val result = useCase()

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            val markdown = (result as Outcome.Success).data
            assertThat(markdown).contains("⚔️")
            assertThat(markdown).doesNotContain("💎")
        }

        @Test
        @DisplayName("RESOURCE 节点 → 💎 图标")
        fun resourceNode_gemIcon() = runTest {
            coEvery { repository.getAllNodes() } returns flowOf(listOf(resourceNode))

            val result = useCase()

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            val markdown = (result as Outcome.Success).data
            assertThat(markdown).contains("💎")
            assertThat(markdown).doesNotContain("⚔️")
        }

        @Test
        @DisplayName("带内容的节点 → 内容按行缩进")
        fun nodeWithContent_indentedContent() = runTest {
            val nodeWithContent = SkillNodeEntity(
                id = "node1",
                parentId = null,
                title = "Test",
                content = "line1\nline2",
                nodeType = "ABILITY",
                level = 1,
                sortOrder = 0,
                isExpanded = true,
                createdAt = 1000L,
                updatedAt = 1000L
            )
            coEvery { repository.getAllNodes() } returns flowOf(listOf(nodeWithContent))

            val result = useCase()

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            val markdown = (result as Outcome.Success).data
            assertThat(markdown).contains("  line1")
            assertThat(markdown).contains("  line2")
        }

        @Test
        @DisplayName("Repository 异常 → Outcome.Error(StorageError)")
        fun repositoryException_returnsStorageError() = runTest {
            coEvery { repository.getAllNodes() } returns flow { throw RuntimeException("db error") }

            val result = useCase()

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.StorageError::class.java)
        }
    }
}
