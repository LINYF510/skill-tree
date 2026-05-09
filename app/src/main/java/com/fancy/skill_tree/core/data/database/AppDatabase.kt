package com.fancy.skill_tree.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fancy.skill_tree.core.data.database.dao.AttachmentDao
import com.fancy.skill_tree.core.data.database.dao.NodeLinkDao
import com.fancy.skill_tree.core.data.database.dao.NodeTagDao
import com.fancy.skill_tree.core.data.database.dao.SkillNodeDao
import com.fancy.skill_tree.core.data.database.dao.TagDao
import com.fancy.skill_tree.core.domain.entity.AttachmentEntity
import com.fancy.skill_tree.core.domain.entity.NodeLinkEntity
import com.fancy.skill_tree.core.domain.entity.NodeTagCrossRef
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import com.fancy.skill_tree.core.domain.entity.TagEntity

/**
 * Skill-Tree 应用数据库
 * 使用 Room ORM 管理所有本地数据存储
 */
@Database(
    entities = [
        SkillNodeEntity::class,
        NodeLinkEntity::class,
        TagEntity::class,
        NodeTagCrossRef::class,
        AttachmentEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun skillNodeDao(): SkillNodeDao
    abstract fun nodeLinkDao(): NodeLinkDao
    abstract fun tagDao(): TagDao
    abstract fun nodeTagDao(): NodeTagDao
    abstract fun attachmentDao(): AttachmentDao

    companion object {
        const val DATABASE_NAME = "skill_tree.db"
    }
}
