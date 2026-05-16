package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.file.AttachmentFileManager
import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.common.DomainException
import com.fancy.skill_tree.core.domain.common.Outcome
import com.fancy.skill_tree.core.domain.entity.AttachmentEntity
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

@DisplayName("DeleteAttachmentUseCase")
class DeleteAttachmentUseCaseTest {

    private val repository = mockk<SkillTreeRepository>(relaxed = true)
    private val fileManager = mockk<AttachmentFileManager>(relaxed = true)
    private lateinit var useCase: DeleteAttachmentUseCase

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        useCase = DeleteAttachmentUseCase(repository, fileManager)
    }

    @Nested
    @DisplayName("删除附件")
    inner class DeleteAttachment {

        @Test
        @DisplayName("成功删除（有文件路径）")
        fun deleteAttachmentWithFilePath() = runTest {
            coEvery { repository.getAttachmentById("att1") } returns AttachmentEntity(
                id = "att1", nodeId = "node1", fileName = "test.pdf",
                filePath = "/data/attachments/test.pdf"
            )

            val result = useCase("att1")

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            coVerify(exactly = 1) { fileManager.deleteFile("/data/attachments/test.pdf") }
            coVerify(exactly = 1) { repository.deleteAttachment("att1") }
        }

        @Test
        @DisplayName("附件无文件路径仅删除数据库记录")
        fun deleteAttachmentWithoutFilePath() = runTest {
            coEvery { repository.getAttachmentById("att_no_path") } returns AttachmentEntity(
                id = "att_no_path", nodeId = "node1", fileName = "note.txt",
                filePath = ""
            )

            val result = useCase("att_no_path")

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            coVerify(exactly = 0) { fileManager.deleteFile(any()) }
            coVerify(exactly = 1) { repository.deleteAttachment("att_no_path") }
        }

        @Test
        @DisplayName("附件不存在仍调用删除")
        fun deleteNonexistentAttachment() = runTest {
            coEvery { repository.getAttachmentById("att_null") } returns null

            val result = useCase("att_null")

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            coVerify(exactly = 0) { fileManager.deleteFile(any()) }
            coVerify(exactly = 1) { repository.deleteAttachment("att_null") }
        }

        @Test
        @DisplayName("Repository 异常返回 StorageError")
        fun repositoryExceptionReturnsStorageError() = runTest {
            coEvery { repository.getAttachmentById("att1") } returns AttachmentEntity(
                id = "att1", nodeId = "node1", fileName = "test.pdf",
                filePath = "/data/attachments/test.pdf"
            )
            coEvery { repository.deleteAttachment("att1") } throws RuntimeException("DB error")

            val result = useCase("att1")

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.StorageError::class.java)
        }
    }
}
