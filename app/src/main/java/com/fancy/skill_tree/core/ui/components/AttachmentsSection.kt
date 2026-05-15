package com.fancy.skill_tree.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fancy.skill_tree.R
import com.fancy.skill_tree.core.domain.entity.AttachmentEntity
import com.fancy.skill_tree.core.ui.accessibility.accessibilityButton
import com.fancy.skill_tree.core.ui.accessibility.accessibilityLabel
import com.fancy.skill_tree.ui.theme.LocalThemeColors
import com.fancy.skill_tree.ui.theme.ThemeColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 附件列表组件
 * 显示节点的所有附件
 */
@Composable
fun AttachmentsSection(
    attachments: List<AttachmentEntity>,
    onAddAttachment: () -> Unit,
    onDeleteAttachment: (AttachmentEntity) -> Unit,
    onOpenAttachment: (AttachmentEntity) -> Unit
) {
    val colors = LocalThemeColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.attachment_section_title, attachments.size),
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            IconButton(
                onClick = onAddAttachment,
                modifier = Modifier.accessibilityButton(stringResource(R.string.a11y_add_attachment))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_add_attachment),
                    tint = colors.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (attachments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.attachment_empty),
                        color = colors.textSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onAddAttachment) {
                        Text(stringResource(R.string.attachment_add_first), color = colors.primary)
                    }
                }
            }
        } else {
            attachments.forEach { attachment ->
                AttachmentCard(
                    attachment = attachment,
                    onClick = { onOpenAttachment(attachment) },
                    onDelete = { onDeleteAttachment(attachment) },
                    colors = colors
                )
            }
        }
    }
}

/**
 * 附件卡片组件
 * 显示单个附件的信息
 */
@Composable
private fun AttachmentCard(
    attachment: AttachmentEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    colors: ThemeColors
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .accessibilityButton(stringResource(R.string.a11y_attachment_view, attachment.fileName))
            .clickable(onClick = onClick),
        color = colors.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                getFileTypeEmoji(attachment.mimeType),
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.fileName,
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Row {
                    Text(
                        text = formatFileSize(attachment.fileSize, stringResource(R.string.attachment_unknown_size)),
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = " · ",
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = formatDate(attachment.createdAt),
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cd_delete_attachment),
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 获取文件类型对应的 emoji
 */
private fun getFileTypeEmoji(mimeType: String?): String = when {
    mimeType == null -> "📄"
    mimeType.startsWith("image/") -> "📷"
    mimeType.startsWith("audio/") -> "🎤"
    mimeType.startsWith("video/") -> "🎬"
    mimeType.contains("pdf") -> "📕"
    else -> "📄"
}

/**
 * 格式化文件大小
 */
private fun formatFileSize(bytes: Long?, unknownSizeText: String): String = when {
    bytes == null -> unknownSizeText
    bytes < 1024 -> "${bytes}B"
    bytes < 1024 * 1024 -> "${bytes / 1024}KB"
    else -> "%.1fMB".format(bytes.toDouble() / 1024 / 1024)
}

/**
 * 格式化日期
 */
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
