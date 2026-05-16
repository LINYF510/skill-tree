# 提示词：Material You 动态颜色 (API 31+)

## 背景

项目当前采用双层配色体系：
1. **自定义 `ThemeColors`** 数据类（11 色：background/surface/primary/ability/resource/link/ai/textPrimary/textSecondary/error/warning），通过 `LocalThemeColors` CompositionLocal 传递
2. **Material3 `ColorScheme`**（通过 `darkColorScheme`/`lightColorScheme` 硬编码），用于 Material 组件

当前 `SkilltreeTheme` 中 `colorScheme` 选择逻辑为简单二选一：
```kotlin
val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
```

**没有任何 dynamicColor 相关代码**。SettingsScreen 的主题选择对话框仅支持 DARK/LIGHT/SYSTEM 三种模式。

## 目标

在 Android 12+ (API 31+) 设备上支持 Material You 动态取色，使用户可以选择"动态颜色"主题模式，应用壁纸颜色作为主题色。

## 实施步骤

### Part A: ThemeMode 枚举扩展

**文件**: `app/src/main/java/com/fancy/skill_tree/ui/theme/Color.kt`

1. 在 `ThemeMode` 枚举中新增 `DYNAMIC` 值：
```kotlin
enum class ThemeMode { DARK, LIGHT, SYSTEM, DYNAMIC }
```

2. 在 `strings.xml` 中新增：
```xml
<string name="settings_theme_dynamic">Dynamic Color</string>
```
在 `values-zh/strings.xml` 中新增：
```xml
<string name="settings_theme_dynamic">动态颜色</string>
```

3. 修改 `getThemeColors()` 函数，新增 DYNAMIC 分支：
```kotlin
fun getThemeColors(mode: ThemeMode, isSystemDark: Boolean): ThemeColors {
    return when (mode) {
        ThemeMode.DARK -> ThemeColors.fromDark()
        ThemeMode.LIGHT -> ThemeColors.fromLight()
        ThemeMode.SYSTEM -> if (isSystemDark) ThemeColors.fromDark() else ThemeColors.fromLight()
        ThemeMode.DYNAMIC -> if (isSystemDark) ThemeColors.fromDark() else ThemeColors.fromLight()
    }
}
```
注意：DYNAMIC 模式的 ThemeColors 将在 Part C 中用动态颜色覆盖。

### Part B: SkilltreeTheme 集成 dynamicColorScheme

**文件**: `app/src/main/java/com/fancy/skill_tree/ui/theme/Theme.kt`

1. 添加导入：
```kotlin
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.platform.LocalContext
```

2. 修改 `SkilltreeTheme` Composable：
```kotlin
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
        ThemeMode.DYNAMIC -> isSystemDark
    }

    val context = LocalContext.current
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        themeMode == ThemeMode.DYNAMIC && supportsDynamic -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    val themeColors = if (themeMode == ThemeMode.DYNAMIC && supportsDynamic) {
        ThemeColors.fromDynamicColorScheme(colorScheme, isDark)
    } else {
        getThemeColors(themeMode, isSystemDark)
    }

    // ... SideEffect 和 CompositionLocalProvider 保持不变
}
```

### Part C: ThemeColors 从 ColorScheme 提取

**文件**: `app/src/main/java/com/fancy/skill_tree/ui/theme/Color.kt`

1. 在 `ThemeColors` companion object 中新增工厂方法：
```kotlin
/**
 * 从 Material3 ColorScheme 创建 ThemeColors（用于 Material You 动态颜色）
 */
fun fromDynamicColorScheme(colorScheme: ColorScheme, isDark: Boolean): ThemeColors {
    return ThemeColors(
        background = colorScheme.background,
        surface = colorScheme.surface,
        primary = colorScheme.primary,
        ability = colorScheme.secondary,
        resource = colorScheme.tertiary,
        link = colorScheme.primary.copy(alpha = 0.7f),
        ai = colorScheme.primaryContainer,
        textPrimary = colorScheme.onBackground,
        textSecondary = colorScheme.onSurfaceVariant,
        error = colorScheme.error,
        warning = if (isDark) Color(0xFFD29922) else Color(0xFF9A6700)
    )
}
```

2. 需要添加导入：
```kotlin
import androidx.compose.material3.ColorScheme
```

### Part D: SettingsScreen 主题选择对话框更新

**文件**: `app/src/main/java/com/fancy/skill_tree/feature/settings/SettingsScreen.kt`

1. 在 `ThemeSelectionDialog` 中新增 Dynamic Color 选项，仅在 API 31+ 设备上显示：
```kotlin
import android.os.Build

// 在 Column 中添加（在 SYSTEM 选项之后）：
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    ThemeOption(
        label = stringResource(R.string.settings_theme_dynamic),
        selected = currentMode == ThemeMode.DYNAMIC,
        onClick = { onModeSelected(ThemeMode.DYNAMIC) },
        colors = colors
    )
}
```

### Part E: UserPreferences 兼容处理

**文件**: `app/src/main/java/com/fancy/skill_tree/core/data/preferences/UserPreferences.kt`

1. `themeMode` getter 已经使用 `ThemeMode.valueOf(name)`，新增的 `DYNAMIC` 枚举值会自动被支持。
2. 但需要处理旧版本用户升级的情况——如果用户在 API 31+ 设备上选择了 DYNAMIC，然后降级到 API 30 以下设备，需要回退到 SYSTEM：
```kotlin
var themeMode: ThemeMode
    get() {
        val name = prefs.getString(KEY_THEME, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        val mode = try { ThemeMode.valueOf(name) } catch (_: IllegalArgumentException) { ThemeMode.DARK }
        return if (mode == ThemeMode.DYNAMIC && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            ThemeMode.SYSTEM
        } else {
            mode
        }
    }
    set(value) { prefs.edit().putString(KEY_THEME, value.name).apply() }
```

### Part F: 低版本设备回退处理

**文件**: `app/src/main/java/com/fancy/skill_tree/ui/theme/Theme.kt`

在 `SkilltreeTheme` 中，当 `themeMode == DYNAMIC` 但设备不支持时，回退到 SYSTEM：
```kotlin
val effectiveMode = if (themeMode == ThemeMode.DYNAMIC && !supportsDynamic) {
    ThemeMode.SYSTEM
} else {
    themeMode
}
```
后续所有逻辑使用 `effectiveMode` 代替 `themeMode`。

## 约束

1. **不引入新的第三方依赖**——`dynamicDarkColorScheme()`/`dynamicLightColorScheme()` 已包含在现有 `material3` 依赖中
2. **API 31+ 运行时检查**——所有动态颜色代码必须有 `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S` 保护
3. **向后兼容**——API 30 及以下设备不受影响，DYNAMIC 选项不显示，选择 DYNAMIC 的用户自动回退到 SYSTEM
4. **ThemeColors 双层体系不变**——`LocalThemeColors` 继续作为业务组件的颜色来源，只是 DYNAMIC 模式下从 ColorScheme 提取
5. **所有 public 函数必须有 KDoc 注释**
6. **禁止使用 !! 非空断言**
7. **新增字符串资源必须同时添加英文和中文版本**

## 测试要求

1. 为 `ThemeColors.fromDynamicColorScheme()` 编写单元测试，验证从 ColorScheme 提取的颜色映射正确
2. 为 `getThemeColors()` 新增 DYNAMIC 分支编写单元测试
3. 为 `UserPreferences.themeMode` 的 DYNAMIC 回退逻辑编写测试（使用 Robolectric 或 Mock Build.VERSION）

## 验证

1. `./gradlew assembleDebug` 编译通过
2. `./gradlew test` 全部测试通过
3. API 31+ 模拟器上：Settings → Theme → 可见 Dynamic Color 选项，选择后应用颜色跟随壁纸
4. API 30 模拟器上：Dynamic Color 选项不可见，功能正常
