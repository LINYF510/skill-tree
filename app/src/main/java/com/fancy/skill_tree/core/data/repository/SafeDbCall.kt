package com.fancy.skill_tree.core.data.repository

import com.fancy.skill_tree.core.data.exception.DataException

/**
 * Repository 层的安全包装方法
 * 所有数据库操作都应通过此方法进行错误捕获和转换
 * @param block 需要安全执行的数据库操作
 * @return Result<T> 包装的操作结果，成功或包含 DataException 的失败
 */
suspend fun <T> safeDbCall(block: suspend () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: android.database.sqlite.SQLiteException) {
        Result.failure(DataException.DatabaseError(e))
    } catch (e: java.io.IOException) {
        Result.failure(DataException.IoError(e))
    } catch (e: Exception) {
        Result.failure(DataException.DatabaseError(e))
    }
}
