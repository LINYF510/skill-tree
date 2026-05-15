package com.fancy.skill_tree.core.domain.model

/**
 * 节点等级配置
 * 定义不同等级节点的视觉和属性
 */
data class NodeLevelConfig(
    val level: Int,
    val emoji: String,
    val label: String,
    val glowColor: Long,
    val glowRadius: Float,
    val sizeMultiplier: Float,
    val borderWidth: Float
) {
    companion object {
        /**
         * 获取指定等级的配置
         */
        fun forLevel(level: Int): NodeLevelConfig = when (level) {
            5 -> NodeLevelConfig(5, "👑", "大师", 0x80FFD700, 20f, 1.3f, 3f)
            4 -> NodeLevelConfig(4, "⭐", "专家", 0x8058A6FF, 15f, 1.15f, 2.5f)
            3 -> NodeLevelConfig(3, "🌳", "熟练", 0x403FB950, 10f, 1.0f, 2f)
            2 -> NodeLevelConfig(2, "🌿", "入门", 0x208B949E, 5f, 0.9f, 1.5f)
            else -> NodeLevelConfig(1, "🌱", "新手", 0x00000000, 0f, 0.8f, 1f)
        }

        /**
         * 计算节点等级
         */
        fun calculateLevel(
            contentLength: Int,
            tagCount: Int,
            attachmentCount: Int,
            linkCount: Int
        ): Int {
            return when {
                contentLength >= 500 && tagCount >= 3 && attachmentCount >= 1 && linkCount >= 1 -> 5
                contentLength >= 300 && tagCount >= 2 && attachmentCount >= 1 -> 4
                contentLength >= 100 && tagCount >= 1 -> 3
                contentLength >= 20 -> 2
                else -> 1
            }
        }
    }
}

/**
 * 成就枚举
 * 定义所有可解锁的成就
 */
enum class Achievement(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val reward: String
) {
    FIRST_NODE("first_node", "🌱", "初出茅庐", "创建第一个节点", "解锁自定义图标"),
    TEN_NODES("ten_nodes", "🌿", "枝繁叶茂", "拥有 10 个节点", "解锁自定义主题色"),
    FIFTY_NODES("fifty_nodes", "🌳", "技能大师", "拥有 50 个节点", "解锁特殊树干样式"),
    TWENTY_LINKS("twenty_links", "🔗", "链接达人", "创建 20 条链接", "解锁特殊连线样式"),
    TEN_AI_CONFIRMS("ten_ai_confirms", "🤖", "AI 先驱", "确认 10 条 AI 推荐链接", "解锁 AI 专属光效"),
    FIVE_LEVEL5("five_level5", "✍️", "内容王者", "拥有 5 个 Lv.5 节点", "解锁金色主题"),
    TEN_VOICE("ten_voice", "🎤", "语音记录者", "使用语音输入 10 次", "解锁语音波形动画"),
    TWENTY_IMAGES("twenty_images", "📸", "图文并茂", "插入 20 张图片", "解锁图片墙展示模式"),
    DAILY_STREAK_7("daily_7", "🔥", "连续记录", "连续 7 天创建/编辑节点", "解锁连续天数徽章"),
    TREE_DEPTH_5("depth_5", "🏔️", "深不可测", "技能树深度达到 5 层", "解锁深层探索者称号")
}
