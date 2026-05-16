package com.fancy.skill_tree.core.domain.model

import androidx.annotation.StringRes
import com.fancy.skill_tree.R

/**
 * 节点等级配置
 * 定义不同等级节点的视觉和属性
 */
data class NodeLevelConfig(
    val level: Int,
    val emoji: String,
    @StringRes val labelResId: Int,
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
            5 -> NodeLevelConfig(5, "👑", R.string.level_master, 0x80FFD700, 20f, 1.3f, 3f)
            4 -> NodeLevelConfig(4, "⭐", R.string.level_expert, 0x8058A6FF, 15f, 1.15f, 2.5f)
            3 -> NodeLevelConfig(3, "🌳", R.string.level_proficient, 0x403FB950, 10f, 1.0f, 2f)
            2 -> NodeLevelConfig(2, "🌿", R.string.level_beginner, 0x208B949E, 5f, 0.9f, 1.5f)
            else -> NodeLevelConfig(1, "🌱", R.string.level_novice, 0x00000000, 0f, 0.8f, 1f)
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
    @StringRes val titleResId: Int,
    @StringRes val descriptionResId: Int,
    @StringRes val rewardResId: Int
) {
    FIRST_NODE("first_node", "🌱", R.string.achievement_first_node_title, R.string.achievement_first_node_desc, R.string.achievement_first_node_reward),
    TEN_NODES("ten_nodes", "🌿", R.string.achievement_ten_nodes_title, R.string.achievement_ten_nodes_desc, R.string.achievement_ten_nodes_reward),
    FIFTY_NODES("fifty_nodes", "🌳", R.string.achievement_fifty_nodes_title, R.string.achievement_fifty_nodes_desc, R.string.achievement_fifty_nodes_reward),
    TWENTY_LINKS("twenty_links", "🔗", R.string.achievement_twenty_links_title, R.string.achievement_twenty_links_desc, R.string.achievement_twenty_links_reward),
    TEN_AI_CONFIRMS("ten_ai_confirms", "🤖", R.string.achievement_ten_ai_confirms_title, R.string.achievement_ten_ai_confirms_desc, R.string.achievement_ten_ai_confirms_reward),
    FIVE_LEVEL5("five_level5", "✍️", R.string.achievement_five_level5_title, R.string.achievement_five_level5_desc, R.string.achievement_five_level5_reward),
    TEN_VOICE("ten_voice", "🎤", R.string.achievement_ten_voice_title, R.string.achievement_ten_voice_desc, R.string.achievement_ten_voice_reward),
    TWENTY_IMAGES("twenty_images", "📸", R.string.achievement_twenty_images_title, R.string.achievement_twenty_images_desc, R.string.achievement_twenty_images_reward),
    DAILY_STREAK_7("daily_7", "🔥", R.string.achievement_daily_streak_title, R.string.achievement_daily_streak_desc, R.string.achievement_daily_streak_reward),
    TREE_DEPTH_5("depth_5", "🏔️", R.string.achievement_tree_depth5_title, R.string.achievement_tree_depth5_desc, R.string.achievement_tree_depth5_reward)
}
