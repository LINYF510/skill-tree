# 主题系统实现提示词

## 项目背景

Skill-Tree 项目当前仅有暗色主题，所有颜色通过 `Color.kt` 中的全局常量（如 `BackgroundDark`、`PrimaryBlue`）硬编码。需要实现暗色/亮色双主题切换，完善 Material3 主题系统。

**重要**: 请先阅读项目规则 `CLAUDE.md` 和主题方案 `prd/05-theme-system.md`。

---

## 当前代码现状

### Color.kt
- 9 个暗色主题常量：BackgroundDark, SurfaceDark, PrimaryBlue, AbilityGreen, ResourcePurple, LinkOrange, AiBlue, TextPrimary, TextSecondary
- 6 个未使用的默认模板颜色：Purple80, PurpleGrey80, Pink80, Purple40, PurpleGrey40, Pink40

### Theme.kt
- 仅有 `DarkColorScheme`，`SkilltreeTheme` 的 `darkTheme` 参数被忽略（始终使用暗色）
- 状态栏颜色硬编码为 `BackgroundDark`
- 无 `LightColorScheme`
- 无 `ThemeMode` 枚举
- 无 `LocalThemeColors` CompositionLocal

### 硬编码颜色引用统计
- **16 个文件**中共 **370 处**直接引用颜色常量
- 主要集中在：SkillTreeScreen(78处)、NodeDetailScreen(34处)、StatisticsScreen(26处)、OnboardingScreen(50处)、CreateNodeDialog(35处)、SettingsScreen(19处)

### strings.xml
- 仅 `app_name` 一项

---

## 实现任务

### 任务 1：改造 Color.kt

**文件**: `app/src/main/java/com/fancy/skill_tree/ui/theme/Color.kt`

1. 删除 6 个未使用的模板颜色（Purple80, PurpleGrey80, Pink80, Purple40, PurpleGrey40, Pink40）
2. 创建 `DarkColors` 和 `LightColors` 对象
3. 创建 `ThemeColors` 数据类
4. 创建 `ThemeMode` 枚举
5. 创建 `getThemeColors()` 函数

```kotlin
package com.fancy.skill_tree.ui.theme

import androidx.compose.ui.graphics.Color

object DarkColors {
    val Background = Color(0xFF0D1117)
    val Surface = Color(0xFF161B22)
    val Primary = Color(0xFF58A6FF)
    val Ability = Color(0xFF3FB950)
    val Resource = Color(0xFFD2A8FF)
    val Link = Color(0xFFF0883E)
    val Ai = Color(0xFF79C0FF)
    val TextPrimary = Color(0xFFE6EDF3)
    val TextSecondary = Color(0xFF8B949E)
    val Error = Color(0xFFF85149)
    val Warning = Color(0xFFD29922)
}

object LightColors {
    val Background = Color(0xFFFFFFFF)
    val Surface = Color(0xFFF6F8FA)
    val Primary = Color(0xFF0969DA)
    val Ability = Color(0xFF1A7F37)
    val Resource = Color(0xFF8250DF)
    val Link = Color(0xFFD97706)
    val Ai = Color(0xFF0550AE)
    val TextPrimary = Color(0xFF1F2328)
    val TextSecondary = Color(0xFF656D76)
    val Error = Color(0xFFCF222E)
    val Warning = Color(0xFF9A6700)
}

enum class ThemeMode { DARK, LIGHT, SYSTEM }

data class ThemeColors(
    val background: Color,
    val surface: Color,
    val primary: Color,
    val ability: Color,
    val resource: Color,
    val link: Color,
    val ai: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val error: Color,
    val warning: Color
) {
    companion object {
        fun fromDark(): ThemeColors = ThemeColors(
            DarkColors.Background, DarkColors.Surface, DarkColors.Primary,
            DarkColors.Ability, DarkColors.Resource, DarkColors.Link,
            DarkColors.Ai, DarkColors.TextPrimary, DarkColors.TextSecondary,
            DarkColors.Error, DarkColors.Warning
        )

        fun fromLight(): ThemeColors = ThemeColors(
            LightColors.Background, LightColors.Surface, LightColors.Primary,
            LightColors.Ability, LightColors.Resource, LightColors.Link,
            LightColors.Ai, LightColors.TextPrimary, LightColors.TextSecondary,
            LightColors.Error, LightColors.Warning
        )
    }
}

fun getThemeColors(mode: ThemeMode, isSystemDark: Boolean): ThemeColors {
    val isDark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemDark
    }
    return if (isDark) ThemeColors.fromDark() else ThemeColors.fromLight()
}
```

### 任务 2：改造 Theme.kt

**文件**: `app/src/main/java/com/fancy/skill_tree/ui/theme/Theme.kt`

1. 添加 `LightColorScheme`
2. 改造 `SkilltreeTheme` 接受 `ThemeMode` 参数
3. 创建 `LocalThemeColors` CompositionLocal
4. 修复状态栏颜色（根据主题动态设置）
5. 修复 `statusBarColor` deprecated 警告（使用 `WindowCompat` 而非直接设置）

```kotlin
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0969DA),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDF4FF),
    secondary = Color(0xFF1A7F37),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1F2328),
    surface = Color(0xFFF6F8FA),
    onSurface = Color(0xFF1F2328),
    error = Color(0xFFCF222E),
    onError = Color(0xFFFFFFFF)
)

val LocalThemeColors = staticCompositionLocalOf { ThemeColors.fromDark() }

@Composable
fun SkilltreeTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemDark
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val themeColors = getThemeColors(themeMode, isSystemDark)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    CompositionLocalProvider(LocalThemeColors provides themeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
```

### 任务 3：创建 UserPreferences 偏好设置管理器

**新增文件**: `app/src/main/java/com/fancy/skill_tree/core/data/preferences/UserPreferences.kt`

```kotlin
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() {
            val name = prefs.getString(KEY_THEME, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
            return try { ThemeMode.valueOf(name) } catch (_: IllegalArgumentException) { ThemeMode.DARK }
        }
        set(value) { prefs.edit().putString(KEY_THEME, value.name).apply() }

    companion object {
        private const val KEY_THEME = "theme_mode"
    }
}
```

### 任务 4：在 MainActivity 中集成主题模式

**文件**: `app/src/main/java/com/fancy/skill_tree/MainActivity.kt`

1. 注入 `UserPreferences`
2. 读取持久化的 `themeMode`
3. 传递给 `SkilltreeTheme(themeMode = ...)`
4. 主题切换后刷新 Activity

### 任务 5：在 SettingsScreen 中添加主题切换 UI

**文件**: `app/src/main/java/com/fancy/skill_tree/feature/settings/SettingsScreen.kt`

在外观设置区域添加主题选择器：
- 三个选项：暗色模式 / 亮色模式 / 跟随系统
- 使用 `DropdownMenu` 或 `RadioButton` 组
- 选择后立即保存到 `UserPreferences` 并刷新主题

### 任务 6：批量迁移颜色引用（核心任务）

将所有 Composable 中的硬编码颜色常量替换为 `LocalThemeColors.current` 引用。

**迁移映射表**:

| 旧引用 | 新引用 |
|--------|--------|
| `BackgroundDark` | `colors.background` |
| `SurfaceDark` | `colors.surface` |
| `PrimaryBlue` | `colors.primary` |
| `AbilityGreen` | `colors.ability` |
| `ResourcePurple` | `colors.resource` |
| `LinkOrange` | `colors.link` |
| `AiBlue` | `colors.ai` |
| `TextPrimary` | `colors.textPrimary` |
| `TextSecondary` | `colors.textSecondary` |

**迁移模式**:

```kotlin
// 改造前
Box(modifier = Modifier.background(BackgroundDark))
Text("标题", color = TextPrimary)

// 改造后
val colors = LocalThemeColors.current
Box(modifier = Modifier.background(colors.background))
Text("标题", color = colors.textPrimary)
```

**需要迁移的文件**（按引用数量排序）：
1. SkillTreeScreen.kt (78处)
2. OnboardingScreen.kt (50处)
3. CreateNodeDialog.kt (35处)
4. NodeDetailScreen.kt (34处)
5. StatisticsScreen.kt (26处)
6. AddLinkDialog.kt (19处)
7. SettingsScreen.kt (19处)
8. LinkedNodesSection.kt (21处)
9. AddTagDialog.kt (21处)
10. AttachmentsSection.kt (15处)
11. TagChip.kt (9处)
12. AddAttachmentDialog.kt (10处)
13. NodeTagsSection.kt (5处)
14. MainActivity.kt (6处)
15. ImageViewerDialog.kt

**注意**: Canvas 绘制函数（`drawNodes`、`drawConnections`）中的颜色也需要迁移。由于 Canvas 的 `DrawScope` 不是 Composable，需要在 Composable 层获取 `colors` 对象后传入绘制函数。

### 任务 7：保留旧常量作为兼容过渡（可选）

为了降低迁移风险，可以在 `Color.kt` 中保留旧常量但标记为 `@Deprecated`：

```kotlin
@Deprecated("使用 LocalThemeColors.current.background", ReplaceWith("LocalThemeColors.current.background"))
val BackgroundDark = DarkColors.Background
```

---

## 实现优先级

1. **P0**: 任务 1-4（Color.kt + Theme.kt + UserPreferences + MainActivity 集成）
2. **P0**: 任务 6（批量迁移颜色引用）
3. **P1**: 任务 5（SettingsScreen 主题切换 UI）
4. **P2**: 任务 7（兼容过渡标记）

---

## 编码规范提醒

- 所有 public 函数必须有 KDoc 注释
- 禁止使用 `!!` 非空断言
- Canvas 绘制函数需要额外参数接收 ThemeColors
- 不要引入未在 PRD 中列出的第三方依赖

---

## 实现完成后

1. 运行 `./gradlew assembleDebug` 确保编译通过
2. 运行 `./gradlew test` 确保测试通过
3. 将本次实现内容总结写入 `doc/rep/2026-05-15-主题系统.md`
