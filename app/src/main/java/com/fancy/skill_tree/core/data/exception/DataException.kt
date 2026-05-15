package com.fancy.skill_tree.core.data.exception

/**
 * 数据层异常基类
 * 封装所有数据源（数据库、文件系统等）相关的异常
 */
sealed class DataException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /**
     * 数据库操作异常
     * @param cause 原始异常
     */
    class DatabaseError(cause: Throwable) : DataException("数据库操作失败", cause)

    /**
     * 文件未找到异常
     * @param path 文件路径
     */
    class FileNotFound(path: String) : DataException("文件未找到: $path")

    /**
     * 存储空间不足异常
     * @param requiredBytes 需要的字节数
     */
    class StorageFull(requiredBytes: Long) : DataException("存储空间不足，需要 ${requiredBytes / 1024 / 1024}MB")

    /**
     * 文件读写异常
     * @param cause 原始异常
     */
    class IoError(cause: Throwable) : DataException("文件读写失败", cause)
}
