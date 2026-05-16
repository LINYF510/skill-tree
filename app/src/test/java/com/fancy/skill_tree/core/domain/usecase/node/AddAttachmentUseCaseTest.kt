package com.fancy.skill_tree.core.domain.usecase.node

import android.net.Uri
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

@DisplayName("AddAttachmentUseCase")
class AddAttachmentUseCaseTest {

    private val repository = mockk<SkillTreeRepository>(relaxed = true)
    private val fileManager = mockk<AttachmentFileManager>()
    private lateinit var useCase: AddAttachmentUseCase

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        useCase = AddAttachmentUseCase(repository, fileManager)
    }

    @Nested
    @DisplayName("添加附件")
    inner class AddAttachment {

        @Test
        @DisplayName("成功添加附件")
        fun addAttachmentSuccess() = runTest {
            val sourceUri = mockk<Uri>()
            coEvery { fileManager.copyToInternalStorage(sourceUri, "test.pdf") } returns "/data/attachments/test.pdf"
            coEvery {
                repository.addAttachment("node1", "test.pdf", "/data/attachments/test.pdf", "application/pdf", 1024L)
            } returns AttachmentEntity(
                id = "att1", nodeId = "node1", fileName = "test.pdf",
                filePath = "/data/attachments/test.pdf", mimeType = "application/pdf",
                fileSize = 1024L
            )

            val result = useCase("node1", sourceUri, "test.pdf", "application/pdf", 1024L)

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            val attachment = (result as Outcome.Success).data
            assertThat(attachment.id).isEqualTo("att1")
            assertThat(attachment.fileName).isEqualTo("test.pdf")
            coVerify(exactly = 1) { fileManager.copyToInternalStorage(sourceUri, "test.pdf") }
            coVerify(exactly = 1) { repository.addAttachment("node1", "test.pdf", "/data/attachments/test.pdf", "application/pdf", 1024L) }
        }

        @Test
        @DisplayName("文件复制失败返回 StorageError")
        fun fileCopyFailureReturnsStorageError() = runTest {
            val sourceUri = mockk<Uri>()
            coEvery { fileManager.copyToInternalStorage(sourceUri, "test.pdf") } throws java.io.IOException("无法打开文件")

            val result = useCase("node1", sourceUri, "test.pdf", "application/pdf", 1024L)

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.StorageError::class.java)
            coVerify(exactly = 1) { fileManager.copyToInternalStorage(sourceUri, "test.pdf") }
            coVerify(exactly = 0) { repository.addAttachment(any(), any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("Repository 异常返回 StorageError")
        fun repositoryExceptionReturnsStorageError() = runTest {
            val sourceUri = mockk<Uri>()
            coEvery { fileManager.copyToInternalStorage(sourceUri, "test.pdf") } returns "/data/attachments/test.pdf"
            coEvery {
                repository.addAttachment(any(), any(), any(), any(), any())
            } throws RuntimeException("DB error")

            val result = useCase("node1", sourceUri, "test.pdf", "application/pdf", 1024L)

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.StorageError::class.java)
        }
    }
}
