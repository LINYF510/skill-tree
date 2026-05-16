package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.common.DomainException
import com.fancy.skill_tree.core.domain.common.Outcome
import com.fancy.skill_tree.core.domain.entity.NodeLinkEntity
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("CreateLinkUseCase")
class CreateLinkUseCaseTest {

    private val repository = mockk<SkillTreeRepository>(relaxed = true)
    private lateinit var useCase: CreateLinkUseCase

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        useCase = CreateLinkUseCase(repository)
    }

    @Nested
    @DisplayName("创建链接")
    inner class CreateLink {

        @Test
        @DisplayName("成功创建手动链接返回 Outcome.Success，linkType = MANUAL")
        fun successManualLink() = runTest {
            val link = NodeLinkEntity(
                id = "link1", sourceId = "node1", targetId = "node2", linkType = "MANUAL"
            )
            coEvery { repository.createLink("node1", "node2", "MANUAL") } returns link

            val result = useCase("node1", "node2", "MANUAL")

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            val data = (result as Outcome.Success).data
            assertThat(data.linkType).isEqualTo("MANUAL")
        }

        @Test
        @DisplayName("成功创建 AI 推荐链接返回 Outcome.Success，linkType = AI_SUGGESTED")
        fun successAiSuggestedLink() = runTest {
            val link = NodeLinkEntity(
                id = "link2", sourceId = "node1", targetId = "node3", linkType = "AI_SUGGESTED"
            )
            coEvery { repository.createLink("node1", "node3", "AI_SUGGESTED") } returns link

            val result = useCase("node1", "node3", "AI_SUGGESTED")

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            val data = (result as Outcome.Success).data
            assertThat(data.linkType).isEqualTo("AI_SUGGESTED")
        }

        @Test
        @DisplayName("默认 linkType 为 MANUAL")
        fun defaultLinkTypeIsManual() = runTest {
            val link = NodeLinkEntity(
                id = "link1", sourceId = "node1", targetId = "node2", linkType = "MANUAL"
            )
            coEvery { repository.createLink("node1", "node2", "MANUAL") } returns link

            val result = useCase("node1", "node2")

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            val data = (result as Outcome.Success).data
            assertThat(data.linkType).isEqualTo("MANUAL")
        }

        @Test
        @DisplayName("Repository 异常返回 Outcome.Error(StorageError)")
        fun repositoryThrowsReturnsStorageError() = runTest {
            coEvery { repository.createLink(any(), any(), any()) } throws RuntimeException("DB error")

            val result = useCase("node1", "node2")

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.StorageError::class.java)
        }
    }
}
