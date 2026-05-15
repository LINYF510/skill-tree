# 主题系统（亮色/暗色）设计方案

> **关联 PRD**: 第 4.2.1 节（色彩体系）、第 4.2 节（视觉风格）
> **当前状态**: 仅定义了暗色主题配色常量，无亮色主题
> **目标**: 实现暗色/亮色双主题切换，完善 Material3 主题系统

---

## 1. 亮色主题配色方案

| 用途 | 暗色值 | 亮色值 | 说明 |
|------|--------|--------|------|
| **主背景** | `#0D1117` | `#FFFFFF` | 页面背景 |
| **卡片/面板** | `#161B22` | `#F6F8FA` | 卡片和面板背景 |
| **主强调色** | `#58A6FF` | `#0969DA` | 主要交互元素 |
| **能力节点** | `#3FB950` | `#1A7F37` | 能力类节点（暗色调暗） |
| **物质节点** | `#D2A8FF` | `#8250DF` | 物质条件类节点（暗色调暗） |
| **链接线** | `#F0883E` | `#D97706` | 跨分支链接线 |
| **AI 推荐** | `#79C0FF` | `#0550AE` | AI 推荐链接 |
| **文字主色** | `#E6EDF3` | `#1F2328` | 主要文字 |
| **文字辅色** | `#8B949E` | `#656D76` | 次要文字和说明 |
| **错误色** | `#F85149` | `#CF222E` | 错误/删除操作 |
| **警告色** | `#D29922` | `#9A6700` | 警告提示 |

---

## 2. Color.kt 改造

```kotlin
package com.fancy.skill_tree.ui.theme

import androidx.compose.ui.graphics.Color

// ==================== 暗色主题 ====================
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

// ==================== 亮色主题 ====================
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

// ==================== 主题状态管理 ====================
enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM  // 跟随系统设置
}

/**
 * 根据主题模式获取当前配色
 */
fun getThemeColors(mode: ThemeMode, isSystemDark: Boolean): ThemeColors {
    val isDark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemDark
    }
    return if (isDark) ThemeColors.from(DarkColors) else ThemeColors.from(LightColors)
}

/**
 * 统一配色访问接口
 * 组件中使用此接口而非直接引用 DarkColors/LightColors
 */
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
        fun from(colors: Any): ThemeColors {
            return when (colors) {
                is DarkColors -> ThemeColors(
                    background = colors.Background, surface = colors.Surface,
                    primary = colors.Primary, ability = colors.Ability,
                    resource = colors.Resource, link = colors.Link,
                    ai = colors.Ai, textPrimary = colors.TextPrimary,
                    textSecondary = colors.TextSecondary,
                    error = colors.Error, warning = colors.Warning
                )
                is LightColors -> ThemeColors(
                    background = colors.Background, surface = colors.Surface,
                    primary = colors.Primary, ability = colors.Ability,
                    resource = colors.Resource, link = colors.Link,
                    ai = colors.Ai, textPrimary = colors.TextPrimary,
                    textSecondary = colors.TextSecondary,
                    error = colors.Error, warning = colors.Warning
                )
                else -> throw IllegalArgumentException("Unknown colors type")
            }
        }
    }
}
```

---

## 3. Theme.kt 改造

```kotlin
package com.fancy.skill_tree.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF58A6FF),
    onPrimary = Color(0xFF0D1117),
    primaryContainer = Color(0xFF1A3A5C),
    secondary = Color(0xFF3FB950),
    onSecondary = Color(0xFF0D1117),
    background = Color(0xFF0D1117),
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF161B22),
    onSurface = Color(0xFFE6EDF3),
    error = Color(0xFFF85149),
    onError = Color(0xFFFFFFFF)
)

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

/**
 * 主题 Composable
 * 自动根据 ThemeMode 选择暗色/亮色主题
 */
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

    // 设置系统状态栏颜色
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
    }

    // 通过 CompositionLocal 提供自定义配色
    CompositionLocalProvider(LocalThemeColors provides themeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SkillTreeTypography,
            content = content
        )
    }
}

/**
 * CompositionLocal 用于在组件树中传递自定义配色
 */
val LocalThemeColors = staticCompositionLocalOf {
    ThemeColors.from(DarkColors) // 默认暗色
}
```

---

## 4. 组件中使用主题配色

### 4.1 改造前（硬编码颜色）

```kotlin
// ❌ 当前代码
Box(modifier = Modifier.background(BackgroundDark))
Text(text = "标题", color = TextPrimary)
```

### 4.2 改造后（通过 LocalThemeColors）

```kotlin
// ✅ 改造后代码
@Composable
fun MyComponent() {
    val colors = LocalThemeColors.current

    Box(modifier = Modifier.background(colors.background))
    Text(text = "标题", color = colors.textPrimary)
}
```

### 4.3 批量迁移辅助脚本

```kotlin
/**
 * 迁移映射表
 * 用于将旧的全局常量引用替换为 LocalThemeColors 引用
 *
 * BackgroundDark    → colors.background
 * SurfaceDark       → colors.surface
 * PrimaryBlue       → colors.primary
 * AbilityGreen      → colors.ability
 * ResourcePurple    → colors.resource
 * LinkOrange        → colors.link
 * AiBlue            → colors.ai
 * TextPrimary       → colors.textPrimary
 * TextSecondary     → colors.textSecondary
 */
```

---

## 5. 主题持久化

```kotlin
package com.fancy.skill_tree.core.data.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户偏好设置管理器
 */
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() {
            val name = prefs.getString(KEY_THEME, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
            return ThemeMode.valueOf(name)
        }
        set(value) {
            prefs.edit().putString(KEY_THEME, value.name).apply()
        }

    companion object {
        private const val KEY_THEME = "theme_mode"
    }
}
```

---

## 6. 设置页面中的主题切换

```kotlin
@Composable
fun SettingsScreen(...) {
    val preferences: UserPreferences = hiltViewModel()
    var themeMode by remember { mutableStateOf(preferences.themeMode) }

    // 在设置页中添加主题选择
    SettingsSection("🎨 外观") {
        // 主题选择器
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("🌓 主题", color = TextPrimary, fontSize = 15.sp)
                Text(getThemeModeLabel(themeMode), color = TextSecondary, fontSize = 13.sp)
            }
            // 点击弹出主题选择菜单
            Box {
                var expanded by remember { mutableStateOf(false) }
                TextButton(onClick = { expanded = true }) {
                    Text(getThemeModeLabel(themeMode), color = PrimaryBlue)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    ThemeMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(getThemeModeLabel(mode)) },
                            onClick = {
                                themeMode = mode
                                preferences.themeMode = mode
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun getThemeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.DARK -> "暗色模式"
    ThemeMode.LIGHT -> "亮色模式"
    ThemeMode.SYSTEM -> "跟随系统"
}
```

---

## 7. 节点卡片在亮色模式下的调整

```
暗色模式:                    亮色模式:
┌──────────────────┐        ┌──────────────────┐
│  ⚔️ 节点图标      │        │  ⚔️ 节点图标      │
│  节点标题         │        │  节点标题         │
│  简短描述...      │        │  简短描述...      │
│  🏷️ 标签1 🏷️ 标签2│        │  🏷️ 标签1 🏷️ 标签2│
└──────────────────┘        └──────────────────┘
背景: #161B22               背景: #F6F8FA
文字: #E6EDF3               文字: #1F2328
边框: #3FB950               边框: #1A7F37
```

---

## 8. 实施步骤

| 步骤 | 内容 |
|------|------|
| 1 | 创建 `LightColors` 亮色主题配色对象 |
| 2 | 创建 `ThemeColors` 统一配色数据类 |
| 3 | 创建 `ThemeMode` 枚举和 `LocalThemeColors` CompositionLocal |
| 4 | 创建 `LightColorScheme` Material3 亮色配色方案 |
| 5 | 改造 `SkilltreeTheme` 支持 ThemeMode 参数 |
| 6 | 创建 `UserPreferences` 偏好设置管理器 |
| 7 | 批量迁移所有 Composable 中的颜色引用（`BackgroundDark` → `colors.background`） |
| 8 | 在设置页面中添加主题切换 UI |
| 9 | 在 `SkillTreeApplication` 中读取持久化的主题设置 |
| 10 | 测试暗色/亮色/系统三种模式下的 UI 显示 |