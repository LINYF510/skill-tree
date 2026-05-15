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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.fancy.skill_tree.core.data.repository.NodeLinkWithTarget
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import com.fancy.skill_tree.core.ui.accessibility.accessibilityButton
import com.fancy.skill_tree.core.ui.accessibility.accessibilityLabel
import com.fancy.skill_tree.ui.theme.LocalThemeColors
import com.fancy.skill_tree.ui.theme.ThemeColors

/**
 * 节点链接区域
 * 显示已确认的链接和 AI 推荐的链接
 */
@Composable
fun LinkedNodesSection(
    confirmedLinks: List<NodeLinkWithTarget>,
    suggestedLinks: List<NodeLinkWithTarget>,
    onLinkClick: (String) -> Unit,
    onRemoveLink: (String) -> Unit,
    onConfirmLink: (String) -> Unit,
    onIgnoreLink: (String) -> Unit,
    onAddLink: () -> Unit
) {
    val colors = LocalThemeColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.link_section_title, confirmedLinks.size),
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            IconButton(
                onClick = onAddLink,
                modifier = Modifier.accessibilityButton(stringResource(R.string.a11y_add_link))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_add_link),
                    tint = colors.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (confirmedLinks.isEmpty()) {
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
                        text = stringResource(R.string.link_empty),
                        color = colors.textSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onAddLink) {
                        Text(stringResource(R.string.link_add_first), color = colors.primary)
                    }
                }
            }
        } else {
            confirmedLinks.forEach { linkWithTarget ->
                LinkedNodeCard(
                    node = linkWithTarget.targetNode,
                    linkType = linkWithTarget.link.linkType,
                    onClick = { onLinkClick(linkWithTarget.targetNode.id) },
                    onRemove = { onRemoveLink(linkWithTarget.link.id) },
                    colors = colors
                )
            }
        }

        if (suggestedLinks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.link_ai_suggestions, suggestedLinks.size),
                color = colors.ai,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            suggestedLinks.forEach { linkWithTarget ->
                SuggestedLinkCard(
                    node = linkWithTarget.targetNode,
                    onConfirm = { onConfirmLink(linkWithTarget.link.id) },
                    onIgnore = { onIgnoreLink(linkWithTarget.link.id) },
                    onClick = { onLinkClick(linkWithTarget.targetNode.id) },
                    colors = colors
                )
            }
        }
    }
}

/**
 * 已确认的链接卡片
 */
@Composable
private fun LinkedNodeCard(
    node: SkillNodeEntity,
    linkType: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    colors: ThemeColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(colors.surface, shape = RoundedCornerShape(12.dp))
            .accessibilityButton(stringResource(R.string.a11y_linked_node_view, node.title))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (node.nodeType == "ABILITY") "⚔️" else "💎",
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = node.title,
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (linkType == "MANUAL") stringResource(R.string.link_type_manual) else stringResource(R.string.link_type_ai),
                color = colors.textSecondary,
                fontSize = 12.sp
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.cd_delete_link),
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * AI 推荐链接卡片
 */
@Composable
private fun SuggestedLinkCard(
    node: SkillNodeEntity,
    onConfirm: () -> Unit,
    onIgnore: () -> Unit,
    onClick: () -> Unit,
    colors: ThemeColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(colors.surface, shape = RoundedCornerShape(12.dp))
            .accessibilityLabel(stringResource(R.string.a11y_suggested_link, node.title))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (node.nodeType == "ABILITY") "⚔️" else "💎",
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = node.title,
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.link_type_ai),
                color = colors.ai,
                fontSize = 12.sp
            )
        }
        TextButton(onClick = onConfirm) {
            Text(stringResource(R.string.common_confirm), color = colors.ability, fontSize = 13.sp)
        }
        TextButton(onClick = onIgnore) {
            Text(stringResource(R.string.link_ignore), color = colors.textSecondary, fontSize = 13.sp)
        }
    }
}
