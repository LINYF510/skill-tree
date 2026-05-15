package com.fancy.skill_tree.feature.tree

/**
 * 技能树导航路由定义
 */
sealed class SkillTreeRoute(val route: String) {
    data object Tree : SkillTreeRoute("tree")
    data object NodeDetail : SkillTreeRoute("node/{nodeId}") {
        /**
         * 创建带节点 ID 的导航路由
         * @param nodeId 节点 ID
         */
        fun createRoute(nodeId: String) = "node/$nodeId"
    }
}
