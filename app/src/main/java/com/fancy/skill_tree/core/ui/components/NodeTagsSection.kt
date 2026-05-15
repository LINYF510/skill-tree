package com.fancy.skill_tree.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fancy.skill_tree.R
import com.fancy.skill_tree.core.domain.entity.TagEntity
import com.fancy.skill_tree.ui.theme.LocalThemeColors

/**
 * 节点标签区域
 * 显示节点的标签并提供添加标签的功能
 *
 * @param tags 节点的标签列表
 * @param onAddTag 添加标签按钮点击回调
 * @param onRemoveTag 移除标签回调
 * @param onTagClick 标签点击回调
 * @param modifier 修饰符
 */
@Composable
fun NodeTagsSection(
    tags: List<TagEntity>,
    onAddTag: () -> Unit,
    onRemoveTag: (TagEntity) -> Unit,
    onTagClick: (TagEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalThemeColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.tag_section_title),
                color = colors.textSecondary,
                fontSize = 14.sp
            )
            IconButton(onClick = onAddTag) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_add_tag),
                    tint = colors.primary
                )
            }
        }

        if (tags.isEmpty()) {
            Text(
                text = stringResource(R.string.tag_empty_hint),
                color = colors.textSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                items(tags, key = { it.id }) { tag ->
                    TagChip(
                        tag = tag,
                        onRemove = { onRemoveTag(tag) },
                        onClick = { onTagClick(tag) }
                    )
                }
            }
        }
    }
}
