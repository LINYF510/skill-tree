# 残留问题修复提示词

## 项目背景

Skill-Tree 是一个 Kotlin / Jetpack Compose / Hilt / Room 技术栈的 Android 应用。P1 修复已完成 97.6%，仅剩 3 处小问题需要修复。

---

## 修复内容

### 修复 1: SkillTreeScreen.kt 第 273 行 — "未知错误" 硬编码中文

**当前代码**:
```kotlin
text = uiState.errorMessage ?: "未知错误",
```

**修改为**:
```kotlin
text = uiState.errorMessage ?: stringResource(R.string.common_unknown_error),
```

说明：此处已在 Composable 上下文中，`stringResource` 可直接使用。`R.string.common_unknown_error` 已在 `values/strings.xml` 中定义（英文: "Unknown error"，中文: "未知错误"）。

---

### 修复 2: SkillTreeScreen.kt 第 1004 行 — "未知错误" 硬编码中文

**当前代码**:
```kotlin
viewModel.showExportError(e.message ?: "未知错误")
```

**修改为**:
```kotlin
viewModel.showExportError(e.message ?: context.getString(R.string.common_unknown_error))
```

说明：此处在 `exportMarkdown` 函数中，不是 Composable 上下文，需使用 `context.getString()` 获取国际化字符串。需确认 `exportMarkdown` 函数签名中 `context` 参数是否可用，如果不可用需要添加 `context: Context` 参数。

---

### 修复 3: AnimationConfig.kt — 移除未使用的 ZOOM_BOUNCE_DURATION 常量

**当前代码**:
```kotlin
const val ZOOM_BOUNCE_DURATION = 200
```

**修改为**: 删除该行，或在上方添加注释说明：
```kotlin
// 缩放回弹使用 SpringSpec 而非时长动画，此常量仅作参考
// const val ZOOM_BOUNCE_DURATION = 200
```

说明：实际回弹动画使用 `ZOOM_BOUNCE_SPRING`（SpringSpec），`ZOOM_BOUNCE_DURATION` 在代码中未被任何地方引用，属于冗余定义。

---

## 验证

修改完成后运行：
1. `./gradlew assembleDebug` — 确认编译通过
2. `./gradlew test` — 确认 42 测试全部通过
3. 全项目搜索 `"未知错误"` — 确认无残留硬编码中文
