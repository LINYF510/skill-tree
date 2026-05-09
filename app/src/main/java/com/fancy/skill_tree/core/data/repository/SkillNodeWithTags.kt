package com.fancy.skill_tree.core.data.repository

import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import com.fancy.skill_tree.core.domain.entity.TagEntity

/**
 * 节点及其关联标签的联合数据结构
 * 用于 getNodeWithTags 方法返回节点及其所有标签
 */
data class SkillNodeWithTags(
    val node: SkillNodeEntity,
    val tags: List<TagEntity>
)
