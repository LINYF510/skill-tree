package com.fancy.skill_tree.core.data.database

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据库恢复管理器
 * 在数据库损坏时尝试恢复，最坏情况重建数据库
 */
@Singleton
class DatabaseRecoveryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * 尝试恢复数据库
     * 1. 尝试备份数据
     * 2. 删除损坏的数据库文件
     * 3. 重新创建数据库
     * @return 恢复是否成功
     */
    fun recoverDatabase(): Boolean {
        return try {
            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            if (dbFile.exists()) {
                val backupFile = File(context.cacheDir, "${AppDatabase.DATABASE_NAME}.bak")
                dbFile.renameTo(backupFile)
            }
            context.getDatabasePath("${AppDatabase.DATABASE_NAME}-wal").delete()
            context.getDatabasePath("${AppDatabase.DATABASE_NAME}-shm").delete()
            dbFile.delete()
            true
        } catch (e: Exception) {
            false
        }
    }
}
