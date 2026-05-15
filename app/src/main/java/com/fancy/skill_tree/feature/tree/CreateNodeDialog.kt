package com.fancy.skill_tree.feature.tree

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.fancy.skill_tree.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import com.fancy.skill_tree.ui.theme.LocalThemeColors
import com.fancy.skill_tree.ui.theme.ThemeColors

private const val NODE_TYPE_ABILITY = "ABILITY"
private const val NODE_TYPE_RESOURCE = "RESOURCE"

/**
 * 创建节点对话框
 *
 * @param allNodes 所有节点列表
 * @param onDismiss 关闭回调
 * @param onCreateNode 创建节点回调
 */
@Composable
fun CreateNodeDialog(
    allNodes: List<SkillNodeEntity>,
    onDismiss: () -> Unit,
    onCreateNode: (title: String, nodeType: String, parentId: String?) -> Unit
) {
    val colors = LocalThemeColors.current
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(NODE_TYPE_ABILITY) }
    var selectedParentId by remember { mutableStateOf<String?>(null) }
    var parentDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.create_node_title),
                    color = colors.textPrimary,
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = androidx.compose.ui.unit.TextUnit(20f, androidx.compose.ui.unit.TextUnitType.Sp),
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.create_node_label), color = colors.textSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.textSecondary,
                        focusedLabelColor = colors.primary,
                        unfocusedLabelColor = colors.textSecondary,
                        cursorColor = colors.primary,
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.create_node_type),
                    color = colors.textSecondary,
                    style = androidx.compose.ui.text.TextStyle(fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp))
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NodeTypeChip(
                        text = stringResource(R.string.node_type_ability),
                        isSelected = selectedType == NODE_TYPE_ABILITY,
                        color = colors.ability,
                        onClick = { selectedType = NODE_TYPE_ABILITY },
                        colors = colors
                    )

                    NodeTypeChip(
                        text = stringResource(R.string.node_type_resource),
                        isSelected = selectedType == NODE_TYPE_RESOURCE,
                        color = colors.resource,
                        onClick = { selectedType = NODE_TYPE_RESOURCE },
                        colors = colors
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.create_node_parent),
                    color = colors.textSecondary,
                    style = androidx.compose.ui.text.TextStyle(fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp))
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box {
                    val selectedParent = allNodes.find { it.id == selectedParentId }
                    val displayText = selectedParent?.title ?: stringResource(R.string.create_node_no_parent)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.textSecondary, RoundedCornerShape(8.dp))
                            .clickable { parentDropdownExpanded = true }
                            .padding(12.dp)
                    ) {
                        Text(
                            text = displayText,
                            color = if (selectedParent != null) colors.textPrimary else colors.textSecondary
                        )
                    }

                    DropdownMenu(
                        expanded = parentDropdownExpanded,
                        onDismissRequest = { parentDropdownExpanded = false },
                        modifier = Modifier.background(colors.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.create_node_no_parent), color = colors.textPrimary) },
                            onClick = {
                                selectedParentId = null
                                parentDropdownExpanded = false
                            }
                        )

                        allNodes.forEach { node ->
                            val depth = getNodeDepth(node, allNodes)
                            val indent = "  ".repeat(depth)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "$indent${node.title}",
                                        color = colors.textPrimary
                                    )
                                },
                                onClick = {
                                    selectedParentId = node.id
                                    parentDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colors.textSecondary
                        )
                    ) {
                        Text(stringResource(R.string.common_cancel))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onCreateNode(title.trim(), selectedType, selectedParentId)
                            }
                        },
                        enabled = title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.textPrimary
                        )
                    ) {
                        Text(stringResource(R.string.create_node_button))
                    }
                }
            }
        }
    }
}

/**
 * 节点类型选择芯片
 */
@Composable
private fun NodeTypeChip(
    text: String,
    isSelected: Boolean,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    colors: ThemeColors
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) color.copy(alpha = 0.2f) else colors.surface)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) color else colors.textSecondary,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) color else colors.textSecondary,
            style = androidx.compose.ui.text.TextStyle(
                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
            )
        )
    }
}

private fun getNodeDepth(node: SkillNodeEntity, allNodes: List<SkillNodeEntity>): Int {
    var depth = 0
    var currentNode: SkillNodeEntity? = node

    while (currentNode?.parentId != null) {
        depth++
        currentNode = allNodes.find { it.id == currentNode?.parentId }
    }

    return depth
}
