package com.fancy.skill_tree.core.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移管理器
 * 集中管理所有版本的数据库迁移脚本
 */
object MigrationManager {

    /**
     * 所有已注册的迁移脚本
     * 每次数据库版本升级时，在此列表中添加对应的 Migration
     */
    val ALL_MIGRATIONS: List<Migration>
        get() = listOf(
            // 示例：从版本 1 迁移到版本 2
            // MIGRATION_1_2,
        )

    /**
     * 数据库迁移示例（版本 1 → 2）
     * 当需要升级数据库时，取消注释并修改 SQL 语句
     *
     * @see MIGRATION_1_2
     */
    // val MIGRATION_1_2 = object : Migration(1, 2) {
    //     override fun migrate(db: SupportSQLiteDatabase) {
    //         // 示例：添加新列
    //         // db.execSQL("ALTER TABLE skill_node ADD COLUMN newColumn TEXT DEFAULT ''")
    //     }
    // }
}
