# 错误处理策略细化方案

> **关联 PRD**: 第 3.5 节（错误处理策略）
> **当前状态**: 仅 ViewModel 层有简单的 try-catch，无统一错误处理机制
> **目标**: 建立从数据层到 UI 层的完整错误处理体系

---

## 1. 错误分层模型

```
┌─────────────────────────────────────────────────────────┐
│  UI Layer          Snackbar / Dialog / ErrorScreen       │
│                    ↑  ErrorMessage (用户可读)             │
├─────────────────────────────────────────────────────────┤
│  ViewModel Layer   SnackbarManager / ErrorStateManager   │
│                    ↑  UiError (映射后)                   │
├─────────────────────────────────────────────────────────┤
│  Domain Layer      Outcome<T> / DomainException          │
│                    ↑  (业务规则错误)                      │
├─────────────────────────────────────────────────────────┤
│  Data Layer        Repository 异常捕获                    │
│                    ↑  DataException (数据库/IO错误)       │
├─────────────────────────────────────────────────────────┤
│  Platform          SQLiteException / IOException         │
└─────────────────────────────────────────────────────────┘
```

---

## 2. 异常类型体系

```kotlin
package com.fancy.skill_tree.core.domain.common

/**
 * 数据层异常基类
 */
sealed class DataException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class DatabaseError(cause: Throwable) : DataException("数据库操作失败", cause)
    class FileNotFound(path: String) : DataException("文件未找到: $path")
    class StorageFull(requiredBytes: Long) : DataException("存储空间不足，需要 ${requiredBytes / 1024 / 1024}MB")
    class IoError(cause: Throwable) : DataException("文件读写失败", cause)
}

/**
 * 领域层异常（在 UseCase 方案中已定义，此处补充）
 */
sealed class DomainException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    // ... 同 01-usecase-layer.md 中的定义
    // 新增:
    class MaxDepthExceeded(maxDepth: Int) : DomainException("技能树深度超过上限 $maxDepth 层")
    class DuplicateTitle(title: String) : DomainException("已存在同名节点: $title")
    class EmptyTree : DomainException("技能树为空，请先创建节点")
}

/**
 * UI 层可展示的错误消息
 */
data class UiError(
    val title: String,
    val message: String,
    val action: ErrorAction? = null,
    val severity: ErrorSeverity = ErrorSeverity.ERROR
)

enum class ErrorSeverity { INFO, WARNING, ERROR, CRITICAL }

data class ErrorAction(
    val label: String,
    val action: () -> Unit
)
```

---

## 3. Repository 层错误处理

```kotlin
package com.fancy.skill_tree.core.data.repository

/**
 * Repository 层的安全包装方法
 * 所有数据库操作都应通过此扩展进行错误捕获和转换
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

/**
 * Repository 实现中的使用示例
 */
class SkillTreeRepositoryImpl @Inject constructor(
    private val skillNodeDao: SkillNodeDao,
    private val nodeTagDao: NodeTagDao
) : SkillTreeRepository {

    override suspend fun insertNode(node: SkillNodeEntity) {
        safeDbCall { skillNodeDao.insertNode(node) }.getOrThrow()
    }

    override suspend fun getNodeById(nodeId: String): SkillNodeEntity? {
        return safeDbCall { skillNodeDao.getNodeById(nodeId) }.getOrNull()
    }

    // Flow 版本 - 使用 catch 操作符
    override fun getAllNodes(): Flow<List<SkillNodeEntity>> {
        return skillNodeDao.getAllNodes()
            .catch { e ->
                when (e) {
                    is android.database.sqlite.SQLiteException -> throw DataException.DatabaseError(e)
                    else -> throw DataException.DatabaseError(e)
                }
            }
    }
}
```

---

## 4. UI 层错误展示策略

### 4.1 错误展示方式选择矩阵

| 错误类型 | 展示方式 | 自动消失 | 示例 |
|---------|---------|---------|------|
| 操作成功 | Snackbar (绿色) | 2秒 | "节点创建成功" |
| 普通操作失败 | Snackbar (红色) + 重试按钮 | 5秒 | "保存失败，点击重试" |
| 数据加载失败 | 内联错误状态 | 手动 | "加载失败，下拉刷新" |
| 严重错误 | AlertDialog | 手动 | "数据库损坏，需要恢复" |
| 网络/Git同步失败 | Snackbar + 通知 | 手动 | "同步失败，点击查看详情" |

### 4.2 ErrorStateManager

```kotlin
package com.fancy.skill_tree.core.ui.error

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全局错误状态管理器
 * 在 Composable 根层级收集并展示错误
 */
@Singleton
class ErrorStateManager @Inject constructor() {

    private val _snackbarEvents = MutableSharedFlow<SnackbarEvent>(extraBufferCapacity = 5)
    val snackbarEvents: SharedFlow<SnackbarEvent> = _snackbarEvents.asSharedFlow()

    private val _dialogEvents = MutableSharedFlow<DialogEvent>(extraBufferCapacity = 3)
    val dialogEvents: SharedFlow<DialogEvent> = _dialogEvents.asSharedFlow()

    /**
     * 显示 Snackbar 消息
     */
    fun showSnackbar(message: String, isError: Boolean = false, actionLabel: String? = null, action: (() -> Unit)? = null) {
        _snackbarEvents.tryEmit(SnackbarEvent(message, isError, actionLabel, action))
    }

    /**
     * 显示错误对话框
     */
    fun showErrorDialog(title: String, message: String, confirmLabel: String = "确定", onConfirm: (() -> Unit)? = null) {
        _dialogEvents.tryEmit(DialogEvent(title, message, confirmLabel, onConfirm))
    }

    /**
     * 从 DomainException 映射到用户可读消息
     */
    fun mapDomainException(e: DomainException): UiError {
        return when (e) {
            is DomainException.NodeNotFound -> UiError(
                title = "节点不存在",
                message = "该节点可能已被删除",
                severity = ErrorSeverity.WARNING
            )
            is DomainException.CircularReference -> UiError(
                title = "无法移动节点",
                message = "不能将节点移动到其子节点下，这会形成循环引用",
                severity = ErrorSeverity.WARNING
            )
            is DomainException.MaxChildrenExceeded -> UiError(
                title = "子节点已达上限",
                message = "当前父节点下最多容纳 20 个子节点，请先删除或移动部分子节点",
                severity = ErrorSeverity.WARNING
            )
            is DomainException.StorageError -> UiError(
                title = "存储错误",
                message = "数据保存失败，请检查设备存储空间",
                action = ErrorAction("重试") { /* 触发重试 */ },
                severity = ErrorSeverity.ERROR
            )
            is DomainException.ValidationError -> UiError(
                title = "输入验证失败",
                message = e.message ?: "请检查输入内容",
                severity = ErrorSeverity.WARNING
            )
            else -> UiError(
                title = "操作失败",
                message = e.message ?: "未知错误",
                severity = ErrorSeverity.ERROR
            )
        }
    }
}

data class SnackbarEvent(
    val message: String,
    val isError: Boolean = false,
    val actionLabel: String? = null,
    val action: (() -> Unit)? = null
)

data class DialogEvent(
    val title: String,
    val message: String,
    val confirmLabel: String = "确定",
    val onConfirm: (() -> Unit)? = null
)
```

### 4.3 根 Composable 中的错误收集

```kotlin
@Composable
fun SkilltreeApp() {
    val errorStateManager: ErrorStateManager = hiltViewModel() // 或从 DI 获取
    val snackbarHostState = remember { SnackbarHostState() }
    var dialogEvent by remember { mutableStateOf<DialogEvent?>(null) }

    // 收集 Snackbar 事件
    LaunchedEffect(Unit) {
        errorStateManager.snackbarEvents.collect { event ->
            val result = snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = event.actionLabel,
                duration = if (event.isError) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                event.action?.invoke()
            }
        }
    }

    // 收集 Dialog 事件
    LaunchedEffect(Unit) {
        errorStateManager.dialogEvents.collect { event ->
            dialogEvent = event
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) {
        // 页面内容...
    }

    // 错误对话框
    dialogEvent?.let { event ->
        AlertDialog(
            onDismissRequest = { dialogEvent = null },
            title = { Text(event.title) },
            text = { Text(event.message) },
            confirmButton = {
                TextButton(onClick = {
                    event.onConfirm?.invoke()
                    dialogEvent = null
                }) {
                    Text(event.confirmLabel)
                }
            }
        )
    }
}
```

---

## 5. ViewModel 层统一错误处理扩展

```kotlin
package com.fancy.skill_tree.core.ui.error

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fancy.skill_tree.core.domain.common.Outcome
import kotlinx.coroutines.launch

/**
 * ViewModel 扩展：安全执行 UseCase 并自动处理错误
 */
fun ViewModel.executeWithErrorHandling(
    errorStateManager: ErrorStateManager,
    block: suspend () -> Outcome<*>,
    onSuccess: (() -> Unit)? = null
) {
    viewModelScope.launch {
        when (val result = block()) {
            is Outcome.Success -> onSuccess?.invoke()
            is Outcome.Error -> {
                val uiError = errorStateManager.mapDomainException(result.exception)
                errorStateManager.showSnackbar(uiError.message, isError = true)
            }
        }
    }
}

/**
 * ViewModel 中使用示例
 */
fun createNode(title: String, nodeType: String, parentId: String?) {
    executeWithErrorHandling(errorStateManager,
        block = { createNodeUseCase(title, nodeType, parentId) },
        onSuccess = {
            errorStateManager.showSnackbar("节点创建成功", isError = false)
        }
    )
}
```

---

## 6. 数据库损坏恢复方案

```kotlin
package com.fancy.skill_tree.core.data.database

import android.content.Context
import androidx.room.Room
import java.io.File

/**
 * 数据库健康检查与恢复管理器
 */
class DatabaseRecoveryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dbFile: File
        get() = context.getDatabasePath(AppDatabase.DATABASE_NAME)

    private val backupDir: File
        get() = File(context.filesDir, "db_backups")

    /**
     * 检查数据库完整性
     * @return true 表示数据库健康
     */
    fun checkIntegrity(): Boolean {
        if (!dbFile.exists()) return false

        return try {
            val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                dbFile.absolutePath, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY
            )
            db.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                cursor.moveToFirst() && cursor.getString(0) == "ok"
            }
            db.close()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 从最近备份恢复数据库
     */
    fun restoreFromBackup(): Boolean {
        val backups = backupDir.listFiles()
            ?.filter { it.extension == "db" }
            ?.sortedByDescending { it.lastModified() }
            ?: return false

        if (backups.isEmpty()) return false

        return try {
            val latestBackup = backups.first()
            dbFile.delete()
            latestBackup.copyTo(dbFile, overwrite = true)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 创建数据库备份
     */
    fun createBackup(): Boolean {
        if (!backupDir.exists()) backupDir.mkdirs()
        if (!dbFile.exists()) return false

        return try {
            val backupFile = File(backupDir, "backup_${System.currentTimeMillis()}.db")
            dbFile.copyTo(backupFile, overwrite = true)

            // 只保留最近 7 天的备份
            cleanupOldBackups(7)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun cleanupOldBackups(keepDays: Int) {
        val cutoffTime = System.currentTimeMillis() - keepDays * 24 * 60 * 60 * 1000L
        backupDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoffTime) file.delete()
        }
    }
}
```

---

## 7. 实施步骤

| 步骤 | 内容 |
|------|------|
| 1 | 创建 `DataException`、`UiError`、`ErrorAction` 类型定义 |
| 2 | 创建 `ErrorStateManager` 全局错误管理器 |
| 3 | 创建 `safeDbCall` Repository 安全包装扩展 |
| 4 | 改造所有 DAO 调用使用 `safeDbCall` |
| 5 | 创建 `executeWithErrorHandling` ViewModel 扩展 |
| 6 | 改造所有 ViewModel 使用统一错误处理 |
| 7 | 在 `SkilltreeApp` 根 Composable 集成错误收集 |
| 8 | 创建 `DatabaseRecoveryManager` 备份恢复机制 |
| 9 | 在 `DatabaseModule` 中注入恢复管理器 |
| 10 | 编写错误处理相关单元测试 |