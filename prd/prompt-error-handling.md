# 错误处理系统实现提示词

## 项目背景

你正在为 Skill-Tree Android 项目（Kotlin/Jetpack Compose/Hilt/Room）实现统一的错误处理系统。项目遵循 Clean Architecture + MVVM，数据流为 UI → ViewModel → UseCase → Repository → DataSource。

**重要**: 请先阅读项目规则文件 `CLAUDE.md` 和错误处理方案 `prd/02-error-handling.md`，理解项目架构和目标。

---

## 当前代码现状分析

### 已有的基础设施
1. **`Outcome<T>`** sealed class 已定义（`core/domain/common/Outcome.kt`），包含 `Success<T>` 和 `Error(exception: DomainException)`
2. **`DomainException`** sealed class 已定义（`core/domain/common/DomainException.kt`），包含 10 种异常类型：NodeNotFound, ParentNodeNotFound, CircularReference, MaxChildrenExceeded, InvalidNodeType, TagAlreadyExists, LinkAlreadyExists, SelfLinkNotAllowed, StorageError, ValidationError
3. **所有 UseCase** 已使用 `Outcome<T>` 作为返回类型
4. **所有 ViewModel** 已使用 `when (val result = xxxUseCase(...))` 模式处理 Outcome

### 当前存在的问题

#### 问题 1：`!!` 非空断言（6 处，违反编码规范）

| 文件 | 行号 | 代码 |
|------|------|------|
| `feature/tree/SkillTreeScreen.kt` | 411 | `viewModel.moveNode(draggedNodeId!!, targetParentId)` |
| `feature/node/NodeDetailScreen.kt` | 217 | `imagePath = currentViewingAttachmentPath!!` |
| `feature/node/NodeDetailScreen.kt` | 243 | `uiState.node!!.nodeType == NODE_TYPE_ABILITY` |
| `feature/node/NodeDetailScreen.kt` | 248 | `uiState.node!!.title` |
| `feature/node/NodeDetailScreen.kt` | 318 | `NodeTypeBadge(nodeType = uiState.node!!.nodeType)` |
| `feature/node/NodeDetailScreen.kt` | 332 | `content = uiState.node!!.content` |

#### 问题 2：Repository 层无错误捕获

`SkillTreeRepositoryImpl` 中所有 DAO 调用都是裸调用，没有 try-catch 或 `safeDbCall` 包装。如果数据库操作抛出 SQLiteException 或 IOException，异常会直接穿透到 ViewModel 层。

#### 问题 3：ViewModel 错误处理重复且不统一

所有 ViewModel 中的错误处理都是重复的 `is Outcome.Error -> { _uiState.update { it.copy(errorMessage = result.exception.message) } }` 模式，没有统一的错误映射和展示策略。

#### 问题 4：UI 层错误展示方式混乱

- SkillTreeScreen: 使用 Toast（导出成功/失败）+ 内联错误文本
- StatisticsScreen: 使用内联错误文本
- NodeDetailScreen: 使用内联错误文本
- SettingsScreen: 使用内联错误文本
- 没有统一的 Snackbar 或 Dialog 错误展示

#### 问题 5：Flow 的 `.catch` 处理不一致

ViewModel 中 Flow 的 `.catch` 仅设置 `errorMessage = e.message ?: "xxx失败"`，没有将异常转换为 DomainException 或 DataException。

---

## 实现任务

### 任务 1：创建 DataException 数据层异常（新增文件）

**文件**: `app/src/main/java/com/fancy/skill_tree/core/data/exception/DataException.kt`

```kotlin
package com.fancy.skill_tree.core.data.exception

sealed class DataException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class DatabaseError(cause: Throwable) : DataException("数据库操作失败", cause)
    class FileNotFound(path: String) : DataException("文件未找到: $path")
    class StorageFull(requiredBytes: Long) : DataException("存储空间不足，需要 ${requiredBytes / 1024 / 1024}MB")
    class IoError(cause: Throwable) : DataException("文件读写失败", cause)
}
```

### 任务 2：创建 safeDbCall Repository 安全包装（新增文件）

**文件**: `app/src/main/java/com/fancy/skill_tree/core/data/repository/SafeDbCall.kt`

```kotlin
package com.fancy.skill_tree.core.data.repository

import com.fancy.skill_tree.core.data.exception.DataException

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
```

### 任务 3：改造 SkillTreeRepositoryImpl 使用 safeDbCall

**文件**: `app/src/main/java/com/fancy/skill_tree/core/data/repository/SkillTreeRepositoryImpl.kt`

改造规则：
- 所有 `suspend` 方法用 `safeDbCall` 包装
- `safeDbCall { ... }.getOrThrow()` 用于必须成功的操作（insert/update/delete）
- `safeDbCall { ... }.getOrNull()` 用于可能返回 null 的查询
- Flow 返回的方法使用 `.catch` 操作符转换异常为 DataException

示例改造：
```kotlin
// 改造前
override suspend fun insertNode(node: SkillNodeEntity) {
    skillNodeDao.insertNode(node)
}

// 改造后
override suspend fun insertNode(node: SkillNodeEntity) {
    safeDbCall { skillNodeDao.insertNode(node) }.getOrThrow()
}

// Flow 改造
override fun getAllNodes(): Flow<List<SkillNodeEntity>> {
    return skillNodeDao.getAllNodes()
        .catch { e -> throw DataException.DatabaseError(e) }
}
```

### 任务 4：创建 UiError 和 ErrorStateManager（新增文件）

**文件**: `app/src/main/java/com/fancy/skill_tree/core/ui/error/UiError.kt`

```kotlin
package com.fancy.skill_tree.core.ui.error

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

**文件**: `app/src/main/java/com/fancy/skill_tree/core/ui/error/ErrorStateManager.kt`

```kotlin
package com.fancy.skill_tree.core.ui.error

import com.fancy.skill_tree.core.data.exception.DataException
import com.fancy.skill_tree.core.domain.common.DomainException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorStateManager @Inject constructor() {

    private val _snackbarEvents = MutableSharedFlow<SnackbarEvent>(extraBufferCapacity = 5)
    val snackbarEvents: SharedFlow<SnackbarEvent> = _snackbarEvents.asSharedFlow()

    private val _dialogEvents = MutableSharedFlow<DialogEvent>(extraBufferCapacity = 3)
    val dialogEvents: SharedFlow<DialogEvent> = _dialogEvents.asSharedFlow()

    fun showSnackbar(message: String, isError: Boolean = false, actionLabel: String? = null, action: (() -> Unit)? = null) {
        _snackbarEvents.tryEmit(SnackbarEvent(message, isError, actionLabel, action))
    }

    fun showErrorDialog(title: String, message: String, confirmLabel: String = "确定", onConfirm: (() -> Unit)? = null) {
        _dialogEvents.tryEmit(DialogEvent(title, message, confirmLabel, onConfirm))
    }

    fun mapDomainException(e: DomainException): UiError {
        return when (e) {
            is DomainException.NodeNotFound -> UiError("节点不存在", "该节点可能已被删除", severity = ErrorSeverity.WARNING)
            is DomainException.ParentNodeNotFound -> UiError("父节点不存在", "指定的父节点可能已被删除", severity = ErrorSeverity.WARNING)
            is DomainException.CircularReference -> UiError("无法移动节点", "不能将节点移动到其子节点下，这会形成循环引用", severity = ErrorSeverity.WARNING)
            is DomainException.MaxChildrenExceeded -> UiError("子节点已达上限", "当前父节点下最多容纳 20 个子节点", severity = ErrorSeverity.WARNING)
            is DomainException.StorageError -> UiError("存储错误", "数据保存失败，请检查设备存储空间", severity = ErrorSeverity.ERROR)
            is DomainException.ValidationError -> UiError("输入验证失败", e.message ?: "请检查输入内容", severity = ErrorSeverity.WARNING)
            is DomainException.InvalidNodeType -> UiError("无效的节点类型", e.message, severity = ErrorSeverity.WARNING)
            is DomainException.TagAlreadyExists -> UiError("标签已存在", e.message, severity = ErrorSeverity.INFO)
            is DomainException.LinkAlreadyExists -> UiError("链接已存在", e.message, severity = ErrorSeverity.INFO)
            is DomainException.SelfLinkNotAllowed -> UiError("无法链接自身", e.message, severity = ErrorSeverity.WARNING)
        }
    }

    fun mapDataException(e: DataException): UiError {
        return when (e) {
            is DataException.DatabaseError -> UiError("数据库错误", "数据库操作失败，请重试", ErrorAction("重试") { /* 调用方需设置 */ }, ErrorSeverity.ERROR)
            is DataException.FileNotFound -> UiError("文件未找到", e.message, severity = ErrorSeverity.WARNING)
            is DataException.StorageFull -> UiError("存储空间不足", e.message, severity = ErrorSeverity.CRITICAL)
            is DataException.IoError -> UiError("文件错误", "文件读写失败，请重试", severity = ErrorSeverity.ERROR)
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

### 任务 5：创建 ViewModel 错误处理扩展（新增文件）

**文件**: `app/src/main/java/com/fancy/skill_tree/core/ui/error/ViewModelErrorExt.kt`

```kotlin
package com.fancy.skill_tree.core.ui.error

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fancy.skill_tree.core.domain.common.Outcome
import kotlinx.coroutines.launch

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
                when (uiError.severity) {
                    ErrorSeverity.CRITICAL -> errorStateManager.showErrorDialog(uiError.title, uiError.message)
                    else -> errorStateManager.showSnackbar(uiError.message, isError = true)
                }
            }
        }
    }
}
```

### 任务 6：消除所有 `!!` 非空断言

#### 6.1 SkillTreeScreen.kt 第 411 行

```kotlin
// 改造前
viewModel.moveNode(draggedNodeId!!, targetParentId)

// 改造后
val draggedId = draggedNodeId
if (draggedId != null) {
    viewModel.moveNode(draggedId, targetParentId)
}
```

#### 6.2 NodeDetailScreen.kt 第 217 行

```kotlin
// 改造前
imagePath = currentViewingAttachmentPath!!,

// 改造后
val path = currentViewingAttachmentPath
if (path != null) {
    imagePath = path,
}
```

#### 6.3 NodeDetailScreen.kt 第 243-332 行（uiState.node!!）

NodeDetailScreen 中有多处 `uiState.node!!`。由于 NodeDetailScreen 只有在节点存在时才显示，最安全的做法是：

```kotlin
// 在 NodeDetailScreen 的内容区域顶部添加 early return
val node = uiState.node
if (node == null) {
    // 显示加载中或错误状态
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
    return
}

// 之后所有 uiState.node!! 替换为 node
// 如: uiState.node!!.title → node.title
// 如: uiState.node!!.nodeType → node.nodeType
// 如: uiState.node!!.content → node.content
```

### 任务 7：在 MainActivity 中集成 ErrorStateManager 的 Snackbar 和 Dialog 收集

**文件**: `app/src/main/java/com/fancy/skill_tree/MainActivity.kt`

在 `SkilltreeApp` Composable 中：
1. 注入 `ErrorStateManager`
2. 添加 `SnackbarHostState`
3. 用 `LaunchedEffect` 收集 `snackbarEvents` 和 `dialogEvents`
4. 在 Scaffold 中添加 `SnackbarHost`
5. 添加错误 `AlertDialog`

### 任务 8：在 Hilt DI 中注册 ErrorStateManager

**文件**: `app/src/main/java/com/fancy/skill_tree/di/UseCaseModule.kt`（或新建 ErrorModule）

ErrorStateManager 已标注 `@Singleton`，Hilt 可自动注入，但需确认不需要额外的 `@Provides`。如果使用 `@Inject constructor()`，则无需额外 Module 注册。

### 任务 9：改造 ViewModel 使用 ErrorStateManager

**重点**: 不要一次性改造所有 ViewModel，先改造 `SkillTreeViewModel` 作为示范。

改造要点：
1. 注入 `ErrorStateManager`
2. 将 `is Outcome.Error -> { _uiState.update { it.copy(errorMessage = result.exception.message) } }` 替换为使用 `errorStateManager.showSnackbar(...)` 或 `errorStateManager.showErrorDialog(...))`
3. 保留 `errorMessage` 字段在 UiState 中用于内联错误展示（如加载失败），但操作失败改用 Snackbar
4. Flow 的 `.catch` 中使用 `errorStateManager.showSnackbar(...)`

---

## 实现优先级

1. **P0（必须）**: 任务 1-4（DataException + safeDbCall + UiError + ErrorStateManager）
2. **P0（必须）**: 任务 6（消除 `!!` 非空断言）
3. **P1（重要）**: 任务 7-9（集成 ErrorStateManager 到 UI 和 ViewModel）
4. **P2（可选）**: 任务 5（ViewModel 扩展函数，可后续统一改造时使用）

---

## 编码规范提醒

- 所有 public 函数必须有 KDoc 注释
- 禁止使用 `!!` 非空断言
- 禁止在 Composable 中直接调用 Repository
- 数据流: UI → ViewModel → UseCase → Repository → DataSource
- 使用 Kotlin 协程 + Flow，不要使用 RxJava
- 不要引入未在 PRD 中列出的第三方依赖

---

## 实现完成后

1. 运行 `./gradlew assembleDebug` 确保编译通过
2. 运行 `./gradlew lint` 检查代码规范
3. 将本次实现内容总结写入 `doc/rep/2026-05-15-错误处理系统.md`，包含：
   - 实现概述
   - 新增/修改文件列表
   - 关键代码片段
   - 已知问题
