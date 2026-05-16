package com.fancy.skill_tree.core.domain.usecase.gamification

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SearchNodesUseCase")
class SearchNodesUseCaseTest {

    private val repository = mockk<SkillTreeRepository>(relaxed = true)
    private lateinit var useCase: SearchNodesUseCase

    private val testNodes = listOf(
        SkillNodeEntity(id = "1", title = "Kotlin", nodeType = "ABILITY"),
        SkillNodeEntity(id = "2", title = "Android", nodeType = "ABILITY")
    )

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        every { repository.getAllNodes() } returns flowOf(testNodes)
        every { repository.searchFullText(any()) } returns flowOf(testNodes)
        every { repository.searchByTags(any()) } returns flowOf(testNodes)
        useCase = SearchNodesUseCase(repository)
    }

    @Nested
    @DisplayName("搜索节点")
    inner class SearchNodes {

        @Test
        @DisplayName("空查询 + 空标签 → 返回所有节点")
        fun emptyQueryAndEmptyTagsReturnsAllNodes() = runTest {
            val result = useCase().first()

            assertThat(result).isEqualTo(testNodes)
            every { repository.getAllNodes() }
        }

        @Test
        @DisplayName("有关键词 + 空标签 → 全文搜索")
        fun queryWithNoTagsCallsFullTextSearch() = runTest {
            val searchResults = listOf(testNodes[0])
            every { repository.searchFullText("Kotlin") } returns flowOf(searchResults)

            val result = useCase("Kotlin").first()

            assertThat(result).isEqualTo(searchResults)
        }

        @Test
        @DisplayName("空查询 + 有标签 → 标签搜索")
        fun noQueryWithTagsCallsTagSearch() = runTest {
            val tagResults = listOf(testNodes[1])
            every { repository.searchByTags(listOf("tag1")) } returns flowOf(tagResults)

            val result = useCase("", listOf("tag1")).first()

            assertThat(result).isEqualTo(tagResults)
        }

        @Test
        @DisplayName("有关键词 + 有标签 → 取交集")
        fun queryWithTagsReturnsIntersection() = runTest {
            val textResults = listOf(testNodes[0], testNodes[1])
            val tagResults = listOf(testNodes[1])
            every { repository.searchFullText("Kotlin") } returns flowOf(textResults)
            every { repository.searchByTags(listOf("tag1")) } returns flowOf(tagResults)

            val result = useCase("Kotlin", listOf("tag1")).first()

            assertThat(result).containsExactly(testNodes[1])
        }

        @Test
        @DisplayName("查询自动 trim")
        fun queryAutoTrims() = runTest {
            val searchResults = listOf(testNodes[0])
            every { repository.searchFullText("Kotlin") } returns flowOf(searchResults)

            val result = useCase("  Kotlin  ").first()

            assertThat(result).isEqualTo(searchResults)
        }
    }
}
