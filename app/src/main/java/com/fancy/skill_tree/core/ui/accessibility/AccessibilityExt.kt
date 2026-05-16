package com.fancy.skill_tree.core.ui.accessibility

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

/**
 * 为 Compose 组件添加无障碍标签
 * 合并后代语义节点，设置 contentDescription
 *
 * @param label 无障碍描述文本
 * @return 添加了无障碍语义的 Modifier
 */
fun Modifier.accessibilityLabel(label: String): Modifier {
    return this.semantics(mergeDescendants = true) {
        contentDescription = label
    }
}

/**
 * 为按钮添加无障碍描述
 * 合并后代语义节点，设置 contentDescription 和 Role.Button
 *
 * @param label 按钮的无障碍描述文本
 * @param actionLabel 可选的状态描述文本
 * @return 添加了按钮无障碍语义的 Modifier
 */
fun Modifier.accessibilityButton(label: String, actionLabel: String? = null): Modifier {
    return this.semantics(mergeDescendants = true) {
        contentDescription = label
        role = Role.Button
        if (actionLabel != null) {
            stateDescription = actionLabel
        }
    }
}

/**
 * 为可展开/折叠元素添加状态描述
 * 合并后代语义节点，根据展开状态设置 stateDescription
 *
 * @param isExpanded 当前是否展开
 * @param label 元素的无障碍描述文本
 * @param expandedLabel 展开状态的描述，默认"已展开"
 * @param collapsedLabel 折叠状态的描述，默认"已折叠"
 * @return 添加了展开/折叠无障碍语义的 Modifier
 */
fun Modifier.accessibilityExpandable(
    isExpanded: Boolean,
    label: String,
    expandedLabel: String? = null,
    collapsedLabel: String? = null
): Modifier {
    return this.semantics(mergeDescendants = true) {
        contentDescription = label
        stateDescription = if (isExpanded) expandedLabel ?: label else collapsedLabel ?: label
    }
}

/**
 * 为切换元素添加状态描述
 * 合并后代语义节点，设置 Role.Switch 和开关状态描述
 *
 * @param isChecked 当前是否开启
 * @param label 切换元素的无障碍描述文本
 * @param checkedLabel 开启状态的描述，默认"已开启"
 * @param uncheckedLabel 关闭状态的描述，默认"已关闭"
 * @return 添加了切换无障碍语义的 Modifier
 */
fun Modifier.accessibilityToggle(
    isChecked: Boolean,
    label: String,
    checkedLabel: String? = null,
    uncheckedLabel: String? = null
): Modifier {
    return this.semantics(mergeDescendants = true) {
        contentDescription = label
        role = Role.Switch
        stateDescription = if (isChecked) checkedLabel ?: label else uncheckedLabel ?: label
    }
}

/**
 * 为列表项添加无障碍描述
 * 合并后代语义节点，设置 Role.Button 和点击提示
 *
 * @param label 列表项的无障碍描述文本
 * @param actionHint 操作提示文本
 * @return 添加了列表项无障碍语义的 Modifier
 */
fun Modifier.accessibilityListItem(label: String, actionHint: String? = null): Modifier {
    return this.semantics(mergeDescendants = true) {
        contentDescription = label
        role = Role.Button
        if (actionHint != null) {
            stateDescription = actionHint
        }
    }
}
