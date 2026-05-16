package com.fancy.skill_tree.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.fancy.skill_tree.R
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fancy.skill_tree.ui.theme.LocalThemeColors
import com.fancy.skill_tree.ui.theme.ThemeColors

/**
 * 附件添加对话框
 * 用于选择添加附件的方式
 */
@Composable
fun AddAttachmentDialog(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
    onPickFile: () -> Unit
) {
    val colors = LocalThemeColors.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = colors.surface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.attachment_add_title),
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                AttachmentOption("📷", stringResource(R.string.attachment_take_photo), stringResource(R.string.attachment_take_photo_desc), onClick = onTakePhoto, colors = colors)
                AttachmentOption("🖼️", stringResource(R.string.attachment_from_gallery), stringResource(R.string.attachment_from_gallery_desc), onClick = onPickFromGallery, colors = colors)
                AttachmentOption("📄", stringResource(R.string.attachment_from_file), stringResource(R.string.attachment_from_file_desc), onClick = onPickFile, colors = colors)

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.common_cancel), color = colors.textSecondary)
                }
            }
        }
    }
}

/**
 * 附件选项组件
 */
@Composable
private fun AttachmentOption(
    emoji: String,
    title: String,
    description: String,
    onClick: () -> Unit,
    colors: ThemeColors
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        color = colors.background,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    color = colors.textSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}
