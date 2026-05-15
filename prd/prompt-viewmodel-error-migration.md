# ViewModel 错误处理迁移提示词

## 项目背景

Skill-Tree 项目已建立完整的错误处理体系（DataException + safeDbCall + ErrorStateManager + Outcome），SkillTreeViewModel 已完成迁移。现在需要将其余 3 个 ViewModel 和 1 处 Toast 统一迁移到 ErrorStateManager。

**重要**: 请先阅读以下文件理解已有的错误处理模式：
- `core/data/exception/DataException.kt`
- `core/data/repository/SafeDbCall.kt`
- `core/ui/error/ErrorStateManager.kt`
- `core/ui/error/UiError.kt`
- `feature/tree/SkillTreeViewModel.kt`（**参考实现**）

---

## 参考实现：SkillTreeViewModel 的模式

SkillTreeViewModel 已使用以下两个方法统一处理错误：

```kotlin
private fun handleDomainException(e: DomainException) {
    val uiError = errorStateManager.mapDomainException(e)
    when (uiError.severity) {
        ErrorSeverity.CRITICAL -> errorStateManager.showErrorDialog(uiError.title, uiError.message)
        else -> errorStateManager.showSnackbar(uiError.message, isError = true)
    }
}

private fun handleFlowException(e: Throwable): String {
    return when (e) {
        is DataException -> {
            val uiError = errorStateManager.mapDataException(e)
            when (uiError.severity) {
                ErrorSeverity.CRITICAL -> errorStateManager.showErrorDialog(uiError.title, uiError.message)
                else -> errorStateManager.showSnackbar(uiError.message, isError = true)
            }
            uiError.message
        }
        else -> {
            errorStateManager.showSnackbar(e.message ?: "未知错误", isError = true)
            e.message ?: "未知错误"
        }
    }
}
```

---

## 任务 1：迁移 NodeDetailViewModel

**文件**: `feature/node/NodeDetailViewModel.kt`

### 改造要点

1. **注入 ErrorStateManager**

```kotlin
@HiltViewModel
class NodeDetailViewModel @Inject constructor(
    // ... 现有依赖 ...
    private val errorStateManager: ErrorStateManager
) : ViewModel() {
```

2. **添加 handleDomainException 和 handleFlowException 方法**（与 SkillTreeViewModel 相同模式）

3. **改造 Flow .catch**（第 88-94 行）

```kotlin
// 改造前
.catch { e ->
    _uiState.update {
        it.copy(
            isLoading = false,
            errorMessage = e.message ?: "加载节点详情失败"
        )
    }
}

// 改造后
.catch { e ->
    val message = handleFlowException(e)
    _uiState.update {
        it.copy(isLoading = false, errorMessage = message)
    }
}
```

4. **改造 Outcome.Error 处理**（第 112-118 行、第 156-159 行、第 188-192 行、第 214-218 行）

```kotlin
// 改造前
is Outcome.Error -> {
    _uiState.update {
        it.copy(errorMessage = result.exception.message)
    }
}

// 改造后
is Outcome.Error -> {
    handleDomainException(result.exception)
}
```

5. **为缺少错误处理的方法添加错误处理**

以下方法直接调用 UseCase 但没有处理 Outcome.Error：
- `addTag()` — 调用 `assignTagToNodeUseCase` 但忽略返回值
- `removeTag()` — 调用 `removeTagFromNodeUseCase` 但忽略返回值
- `createLink()` — 调用 `createLinkUseCase` 但忽略返回值
- `deleteLink()` — 调用 `deleteLinkUseCase` 但忽略返回值
- `confirmLink()` — 调用 `confirmLinkUseCase` 但忽略返回值
- `addAttachment()` — 调用 `addAttachmentUseCase` 但忽略返回值
- `deleteAttachment()` — 调用 `deleteAttachmentUseCase` 但忽略返回值

这些 UseCase 返回 `Outcome<*>`，需要添加错误处理。改造模式：

```kotlin
// 改造前
fun addTag(tag: TagEntity) {
    viewModelScope.launch {
        assignTagToNodeUseCase(nodeId, tag.id)
    }
}

// 改造后
fun addTag(tag: TagEntity) {
    viewModelScope.launch {
        when (val result = assignTagToNodeUseCase(nodeId, tag.id)) {
            is Outcome.Success -> { }
            is Outcome.Error -> handleDomainException(result.exception)
        }
    }
}
```

对以上 7 个方法都按此模式改造。

---

## 任务 2：迁移 StatisticsViewModel

**文件**: `feature/statistics/StatisticsViewModel.kt`

### 改造要点

1. **注入 ErrorStateManager**

```kotlin
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val getStatisticsUseCase: GetStatisticsUseCase,
    private val achievementManager: AchievementManager,
    private val errorStateManager: ErrorStateManager
) : ViewModel() {
```

2. **添加 handleFlowException 方法**

3. **改造 Flow .catch**（第 42-48 行）

```kotlin
// 改造前
.catch { e ->
    _uiState.update {
        it.copy(
            isLoading = false,
            errorMessage = e.message ?: "加载统计数据失败"
        )
    }
}

// 改造后
.catch { e ->
    val message = handleFlowException(e)
    _uiState.update {
        it.copy(isLoading = false, errorMessage = message)
    }
}
```

---

## 任务 3：迁移 SettingsViewModel

**文件**: `feature/settings/SettingsViewModel.kt`

### 改造要点

1. **注入 ErrorStateManager**

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exportToMarkdownUseCase: ExportToMarkdownUseCase,
    private val createNodeUseCase: CreateNodeUseCase,
    private val clearAllDataUseCase: ClearAllDataUseCase,
    private val errorStateManager: ErrorStateManager
) : ViewModel() {
```

2. **添加 handleDomainException 方法**

3. **改造 exportMarkdown**（第 38-51 行）

```kotlin
// 改造前
is Outcome.Error -> {
    _uiState.update {
        it.copy(
            isLoading = false,
            errorMessage = result.exception.message ?: "导出失败"
        )
    }
}

// 改造后
is Outcome.Error -> {
    handleDomainException(result.exception)
    _uiState.update { it.copy(isLoading = false) }
}
```

4. **改造 clearAllData**（第 130-143 行）

```kotlin
// 改造前
is Outcome.Error -> {
    _uiState.update {
        it.copy(
            isLoading = false,
            errorMessage = result.exception.message ?: "清除失败"
        )
    }
}

// 改造后
is Outcome.Error -> {
    handleDomainException(result.exception)
    _uiState.update { it.copy(isLoading = false) }
}
```

5. **loadSampleData 方法** — 该方法连续调用 createNodeUseCase 但不处理返回值。由于是批量操作，建议在最后添加成功提示：

```kotlin
fun loadSampleData() {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }

        // ... 现有的 createNodeUseCase 调用保持不变 ...

        _uiState.update { it.copy(isLoading = false) }
        errorStateManager.showSnackbar("示例数据加载完成", isError = false)
    }
}
```

---

## 任务 4：消除 SkillTreeScreen 中的 Toast

**文件**: `feature/tree/SkillTreeScreen.kt`

第 829-831 行使用了 `Toast.makeText`，应改用 ErrorStateManager。

### 改造方式

SkillTreeScreen 中已有 `SkillTreeViewModel`，而 ViewModel 已注入 ErrorStateManager。需要在 ViewModel 中添加一个导出成功的 Snackbar 方法：

```kotlin
// 在 SkillTreeViewModel 中添加
fun showExportSuccess(path: String) {
    errorStateManager.showSnackbar("已导出到 $path", isError = false)
}

fun showExportError(message: String) {
    errorStateManager.showSnackbar("导出失败: $message", isError = true)
}
```

然后在 SkillTreeScreen 中替换 Toast：

```kotlin
// 改造前
Toast.makeText(context, "已导出到 ${file.absolutePath}", Toast.LENGTH_LONG).show()
Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()

// 改造后
viewModel.showExportSuccess(file.absolutePath)
viewModel.showExportError(e.message ?: "未知错误")
```

同时移除 `import android.widget.Toast` 和 `import android.content.Context`（如果 Context 不再被其他地方使用）。

---

## 编码规范提醒

- 所有 public 函数必须有 KDoc 注释
- 禁止使用 `!!` 非空断言
- handleDomainException 和 handleFlowException 应为 private 方法
- 保持与 SkillTreeViewModel 的代码风格一致
- 添加必要的 import（ErrorStateManager, ErrorSeverity, DataException, DomainException）

---

## 实现完成后

1. 运行 `./gradlew assembleDebug` 确保编译通过
2. 运行 `./gradlew lint` 检查代码规范
3. 将本次实现内容总结写入 `doc/rep/2026-05-15-ViewModel错误处理迁移.md`，包含：
   - 实现概述
   - 修改文件列表
   - 关键代码片段
   - 已知问题
