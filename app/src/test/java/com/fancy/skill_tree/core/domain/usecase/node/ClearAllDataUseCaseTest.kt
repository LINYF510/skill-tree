package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.common.DomainException
import com.fancy.skill_tree.core.domain.common.Outcome
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

@DisplayName("ClearAllDataUseCase")
class ClearAllDataUseCaseTest {

    private val repository = mockk<SkillTreeRepository>(relaxed = true)
    private lateinit var useCase: ClearAllDataUseCase

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        useCase = ClearAllDataUseCase(repository)
    }

    @Nested
    @DisplayName("清除所有数据")
    inner class ClearAllData {

        @Test
        @DisplayName("成功清除返回 Outcome.Success(Unit)")
        fun successReturnsUnit() = runTest {
            coEvery { repository.deleteAllData() } returns Unit

            val result = useCase()

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            assertThat((result as Outcome.Success).data).isEqualTo(Unit)
        }

        @Test
        @DisplayName("Repository 异常返回 Outcome.Error(StorageError)")
        fun repositoryThrowsReturnsStorageError() = runTest {
            coEvery { repository.deleteAllData() } throws RuntimeException("DB error")

            val result = useCase()

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.StorageError::class.java)
        }

        @Test
        @DisplayName("验证 repository.deleteAllData() 被调用")
        fun verifyDeleteAllDataCalled() = runTest {
            coEvery { repository.deleteAllData() } returns Unit

            useCase()

            coVerify(exactly = 1) { repository.deleteAllData() }
        }
    }
}
