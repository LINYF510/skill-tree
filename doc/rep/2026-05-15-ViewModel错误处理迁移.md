# 2026-05-15-ViewModel错误处理迁移

## 实现概述

将剩余 3 个 ViewModel（NodeDetailViewModel、StatisticsViewModel、SettingsViewModel）统一迁移到 ErrorStateManager 错误处理体系，并消除 SkillTreeScreen 中的 Toast 使用。至此，项目全部 4 个 ViewModel 均已使用 ErrorStateManager 统一处理错误。

### 迁移模式

所有 ViewModel 统一采用与 SkillTreeViewModel 相同的两个 private 方法：

- `handleDomainException(e: DomainException)` — 处理 Outcome.Error，根据 ErrorSeverity 选择 Snackbar 或 Dialog
- `handleFlowException(e: Throwable): String` — 处理 Flow .catch 中的异常，映射 DataException 为 UiError

### 改造统计

| ViewModel | 注入 ErrorStateManager | .catch 改造 | Outcome.Error 改造 | 新增错误处理方法 |
|-----------|----------------------|-------------|-------------------|----------------|
| NodeDetailViewModel | ✅ | ✅ | ✅ (4处) | ✅ addTag/removeTag/createLink/deleteLink/confirmLink/addAttachment/deleteAttachment (7处) |
| StatisticsViewModel | ✅ | ✅ | N/A | N/A |
| SettingsViewModel | ✅ | N/A | ✅ (2处) | ✅ loadSampleData 成功提示 |

## 修改文件列表

| 文件 | 操作 | 说明 |
|------|------|------|
| `feature/node/NodeDetailViewModel.kt` | 修改 | 注入 ErrorStateManager，改造 .catch 和 Outcome.Error，7 个方法新增错误处理 |
| `feature/statistics/StatisticsViewModel.kt` | 修改 | 注入 ErrorStateManager，改造 .catch |
| `feature/settings/SettingsViewModel.kt` | 修改 | 注入 ErrorStateManager，改造 Outcome.Error，loadSampleData 添加成功提示 |
| `feature/tree/SkillTreeViewModel.kt` | 修改 | 新增 showExportSuccess/showExportError 方法 |
| `feature/tree/SkillTreeScreen.kt` | 修改 | 消除 Toast，改用 ViewModel 方法，移除 Context/Toast/LocalContext import |

## 关键代码片段

### NodeDetailViewModel — 7 个方法新增错误处理

```kotlin
// 改造前：忽略 Outcome 返回值
fun addTag(tag: TagEntity) {
    viewModelScope.launch {
        assignTagToNodeUseCase(nodeId, tag.id)
    }
}

// 改造后：处理 Outcome.Error
fun addTag(tag: TagEntity) {
    viewModelScope.launch {
        when (val result = assignTagToNodeUseCase(nodeId, tag.id)) {
            is Outcome.Success -> { }
            is Outcome.Error -> handleDomainException(result.exception)
        }
    }
}
```

### NodeDetailViewModel — Flow .catch 改造

```kotlin
// 改造前
.catch { e ->
    _uiState.update {
        it.copy(isLoading = false, errorMessage = e.message ?: "加载节点详情失败")
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

### NodeDetailViewModel — Outcome.Error 改造

```kotlin
// 改造前
is Outcome.Error -> {
    _uiState.update {
        it.copy(errorMessage = result.exception.message)
    }
}

// 改造后
is Outcome.Error -> {
    handleDomainException(nodeResult.exception)
    _uiState.update {
        it.copy(isLoading = false)
    }
}
```

### SettingsViewModel — loadSampleData 成功提示

```kotlin
fun loadSampleData() {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        // ... 现有 createNodeUseCase 调用保持不变 ...
        _uiState.update { it.copy(isLoading = false) }
        errorStateManager.showSnackbar("示例数据加载完成", isError = false)
    }
}
```

### SkillTreeScreen — Toast 消除

```kotlin
// 改造前
private fun exportMarkdown(context: Context, viewModel: SkillTreeViewModel) {
    viewModel.exportToMarkdown { markdown ->
        try {
            // ...
            Toast.makeText(context, "已导出到 ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

// 改造后
private fun exportMarkdown(viewModel: SkillTreeViewModel) {
    viewModel.exportToMarkdown { markdown ->
        try {
            // ...
            viewModel.showExportSuccess(file.absolutePath)
        } catch (e: Exception) {
            viewModel.showExportError(e.message ?: "未知错误")
        }
    }
}
```

### SkillTreeViewModel — 新增导出方法

```kotlin
fun showExportSuccess(path: String) {
    errorStateManager.showSnackbar("已导出到 $path", isError = false)
}

fun showExportError(message: String) {
    errorStateManager.showSnackbar("导出失败: $message", isError = true)
}
```

## 已知问题

1. Lint 报告的 CAMERA 权限缺少 `<uses-feature>` 标签是已有问题，非本次引入
2. `Icons.Filled.ArrowBack` 已弃用警告是已有代码，非本次引入
3. `NodeDetailViewModel.createAndAssignTag` 中 `assignTagToNodeUseCase` 的返回值未处理（仅在 `createTagUseCase` 成功后调用），后续可补充
4. `SettingsViewModel.loadSampleData` 中 `createNodeUseCase` 的返回值未逐一处理（批量操作场景），后续可考虑添加错误收集和汇总提示
