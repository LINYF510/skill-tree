package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.common.DomainException
import com.fancy.skill_tree.core.domain.common.Outcome
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("DeleteLinkUseCase")
class DeleteLinkUseCaseTest {

    private val repository = mockk<SkillTreeRepository>(relaxed = true)
    private lateinit var useCase: DeleteLinkUseCase

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        useCase = DeleteLinkUseCase(repository)
    }

    @Nested
    @DisplayName("删除链接")
    inner class DeleteLink {

        @Test
        @DisplayName("成功删除返回 Outcome.Success(Unit)")
        fun successReturnsUnit() = runTest {
            coEvery { repository.deleteLink("link1") } returns Unit

            val result = useCase("link1")

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            assertThat((result as Outcome.Success).data).isEqualTo(Unit)
        }

        @Test
        @DisplayName("Repository 异常返回 Outcome.Error(StorageError)")
        fun repositoryThrowsReturnsStorageError() = runTest {
            coEvery { repository.deleteLink("link1") } throws RuntimeException("DB error")

            val result = useCase("link1")

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.StorageError::class.java)
        }
    }
}
