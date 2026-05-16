package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.common.DomainException
import com.fancy.skill_tree.core.domain.common.Outcome
import com.fancy.skill_tree.core.domain.entity.TagEntity
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("CreateTagUseCase")
class CreateTagUseCaseTest {

    private val repository = mockk<SkillTreeRepository>(relaxed = true)
    private lateinit var useCase: CreateTagUseCase

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        useCase = CreateTagUseCase(repository)
    }

    @Nested
    @DisplayName("创建标签")
    inner class CreateTag {

        @Test
        @DisplayName("成功创建返回 Outcome.Success")
        fun successReturnsTag() = runTest {
            val tag = TagEntity(id = "tag1", name = "Kotlin", color = "#FF0000")
            coEvery { repository.createTag("Kotlin", "#FF0000") } returns tag

            val result = useCase("Kotlin", "#FF0000")

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            val data = (result as Outcome.Success).data
            assertThat(data.id).isEqualTo("tag1")
            assertThat(data.name).isEqualTo("Kotlin")
            assertThat(data.color).isEqualTo("#FF0000")
        }

        @Test
        @DisplayName("空名称返回 Outcome.Error(ValidationError)")
        fun blankNameReturnsValidationError() = runTest {
            val result = useCase("")

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.ValidationError::class.java)
        }

        @Test
        @DisplayName("纯空格名称返回 Outcome.Error(ValidationError)")
        fun whitespaceOnlyNameReturnsValidationError() = runTest {
            val result = useCase("   ")

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.ValidationError::class.java)
        }

        @Test
        @DisplayName("Repository 异常返回 Outcome.Error(StorageError)")
        fun repositoryThrowsReturnsStorageError() = runTest {
            coEvery { repository.createTag(any(), any()) } throws RuntimeException("DB error")

            val result = useCase("Kotlin")

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.StorageError::class.java)
        }
    }
}
