package com.fancy.skill_tree.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fancy.skill_tree.core.domain.entity.TagEntity
import com.fancy.skill_tree.core.ui.accessibility.accessibilityButton
import com.fancy.skill_tree.ui.theme.LocalThemeColors

/**
 * 标签 Chip 组件
 * 显示带有颜色点和可选删除按钮的标签
 *
 * @param tag 标签实体
 * @param isSelected 是否选中状态
 * @param onRemove 删除按钮点击回调
 * @param onClick 标签点击回调
 * @param modifier 修饰符
 */
@Composable
fun TagChip(
    tag: TagEntity,
    isSelected: Boolean = false,
    onRemove: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalThemeColors.current
    val chipColor = tag.color?.let {
        try {
            Color(android.graphics.Color.parseColor(it))
        } catch (e: Exception) {
            colors.primary
        }
    } ?: colors.primary

    val backgroundColor = if (isSelected) {
        chipColor.copy(alpha = 0.2f)
    } else {
        colors.surface
    }

    Row(
        modifier = modifier
            .background(backgroundColor, shape = RoundedCornerShape(16.dp))
            .accessibilityButton("标签：${tag.name}${if (onRemove != null) "，点击移除" else ""}")
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(chipColor, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = tag.name,
            color = colors.textPrimary,
            fontSize = 13.sp
        )
        if (onRemove != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "✕",
                color = colors.textSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onRemove)
            )
        }
    }
}
