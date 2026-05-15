# 用户引导（Onboarding）详细设计方案

> **关联 PRD**: 第 2.2 节（F1.6 用户引导）
> **当前状态**: 只有一个空状态引导页（emoji + 两个按钮），远不满足 PRD 要求
> **目标**: 实现 4 步引导流程 + 完整的空状态处理

---

## 1. 引导流程设计

### 1.1 完整引导流程

```
应用首次启动
    │
    ▼
┌─────────────┐
│ Step 1: 欢迎页 │  ← 产品介绍 + 品牌展示
└──────┬──────┘
       │ 点击「开始」
       ▼
┌─────────────┐
│ Step 2: 创建  │  ← 引导用户创建第一个节点
│  第一个节点   │
└──────┬──────┘
       │ 创建成功后
       ▼
┌─────────────┐
│ Step 3: 示例  │  ← 展示一棵完整的示例技能树
│  技能树展示   │
└──────┬──────┘
       │ 点击「进入主页」
       ▼
┌─────────────┐
│ Step 4: 主页  │  ← 进入技能树主页，显示功能提示
│  功能导览     │
└─────────────┘
```

### 1.2 跳过与重看机制

- 任何步骤都可以「跳过」，直接进入主页
- 主页设置菜单中提供「重新引导」选项
- 使用 SharedPreferences 存储引导完成状态
- 引导过程中按返回键不退出应用，而是回到上一步

---

## 2. 各步骤详细设计

### 2.1 Step 1 — 欢迎页

```
┌─────────────────────────────────────┐
│                                     │
│                                     │
│            🌳                       │  ← Logo 动画（生长效果）
│                                     │
│         技能树 Skill-Tree            │  ← 品牌名（大字）
│                                     │
│   把你的能力和资产                   │
│   变成一棵可生长的技能树             │  ← 一句话 Slogan
│                                     │
│                                     │
│  ┌─────────────────────────────┐    │
│  │  📝 随手记录  3秒完成       │    │
│  │  🌳 树状可视化  一目了然    │    │
│  │  🔗 智能关联  发现新可能    │    │  ← 3 个核心卖点
│  │  🔒 本地存储  数据安全     │    │
│  └─────────────────────────────┘    │
│                                     │
│       ┌───────────────────┐         │
│       │    开 始 探 索     │         │  ← 主按钮（PrimaryBlue）
│       └───────────────────┘         │
│                                     │
│              跳过引导                │  ← 小字链接
└─────────────────────────────────────┘
```

**动画设计**：
- Logo 图标带轻微缩放呼吸动画
- 3 个卖点卡片依次从下往上淡入（staggered animation）
- 按钮带微光扫过效果

**实现要点**：

```kotlin
@Composable
fun WelcomeStep(onNext: () -> Unit, onSkip: () -> Unit) {
    var logoScale by remember { mutableFloatStateOf(0.5f) }
    val featureItems = listOf(
        FeatureItem("📝", "随手记录", "3秒完成"),
        FeatureItem("🌳", "树状可视化", "一目了然"),
        FeatureItem("🔗", "智能关联", "发现新可能"),
        FeatureItem("🔒", "本地存储", "数据安全")
    )

    // Logo 入场动画
    LaunchedEffect(Unit) {
        animate(0.5f, 1f, animationSpec = spring(dampingRatio = 0.6f)) { value, _ ->
            logoScale = value
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(BackgroundDark),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🌳", fontSize = 72.sp, modifier = Modifier.graphicsLayer { scaleX = logoScale; scaleY = logoScale })

        Spacer(modifier = Modifier.height(24.dp))

        Text("技能树 Skill-Tree", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(12.dp))

        Text("把你的能力和资产变成一棵可生长的技能树", color = TextSecondary, fontSize = 16.sp)

        Spacer(modifier = Modifier.height(40.dp))

        // 特性列表 - 逐个淡入
        featureItems.forEachIndexed { index, item ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(400, delayMillis = index * 200)) +
                        slideInVertically(initialOffsetY = { it / 2 })
            ) {
                FeatureRow(item)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(onClick = onNext, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) {
            Text("开 始 探 索", fontSize = 18.sp, modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onSkip) {
            Text("跳过引导", color = TextSecondary)
        }
    }
}

data class FeatureItem(val icon: String, val title: String, val description: String)
```

### 2.2 Step 2 — 创建第一个节点

```
┌─────────────────────────────────────┐
│  ← 返回          1/4         跳过   │  ← 步骤指示器
├─────────────────────────────────────┤
│                                     │
│           ✨                        │
│                                     │
│      创建你的第一个节点              │
│                                     │
│   技能树由「能力」和「物质」        │
│   两种节点组成                      │
│                                     │
│  ┌─────────────────────────────┐    │
│  │ 例如：Python 编程            │    │  ← 输入框（自动聚焦）
│  └─────────────────────────────┘    │
│                                     │
│  选择节点类型：                      │
│  ┌──────────┐  ┌──────────┐        │
│  │ ⚔️ 能力   │  │ 💎 物质   │        │  ← 类型选择（默认选中能力）
│  └──────────┘  └──────────┘        │
│                                     │
│  💡 提示：能力节点代表你的技能和知识 │
│  物质节点代表你拥有的物品和资源      │
│                                     │
│       ┌───────────────────┐         │
│       │    创  建  节  点  │         │
│       └───────────────────┘         │
└─────────────────────────────────────┘
```

**交互细节**：
- 输入框自动聚焦并弹出键盘
- 提供 placeholder 示例文本
- 类型选择高亮动画
- 创建成功后节点卡片放大动画 → 自动进入下一步
- 可以跳过（直接进入下一步但使用默认示例数据）

### 2.3 Step 3 — 示例技能树展示

```
┌─────────────────────────────────────┐
│  ← 返回          3/4         跳过   │
├─────────────────────────────────────┤
│                                     │
│      这是你的技能树！                │
│                                     │
│  ┌──────────────────────────────┐   │
│  │                              │   │
│  │       ┌─────────┐            │   │
│  │       │ 技能树   │            │   │
│  │       └────┬────┘            │   │
│  │     ┌──────┼──────┐         │   │
│  │     ▼      ▼      ▼         │   │
│  │  ┌────┐ ┌────┐ ┌────┐      │   │  ← 展示一棵 10-15 节点的示例树
│  │  │编程│ │设计│ │语言│      │   │
│  │  └──┬─┘ └────┘ └──┬─┘      │   │
│  │     ▼             ▼         │   │
│  │  ┌────┐       ┌────┐       │   │
│  │  │Python│     │英语│       │   │
│  │  └─────┘     └─────┘       │   │
│  │                              │   │
│  └──────────────────────────────┘   │
│                                     │
│  💡 提示：                           │
│  • 双指缩放查看全局                   │
│  • 单指拖动平移画布                   │
│  • 点击节点查看详情                   │
│                                     │
│  ┌──────────────────────────────┐   │
│  │  ✓ 保留示例数据               │   │  ← 复选框
│  └──────────────────────────────┘   │
│                                     │
│       ┌───────────────────┐         │
│       │    进 入 主 页     │         │
│       └───────────────────┘         │
└─────────────────────────────────────┘
```

**示例数据结构（10-15 个节点）**：

```
技能树 (根节点)
├── ⚔️ 编程
│   ├── ⚔️ Python
│   │   ├── ⚔️ 数据分析
│   │   └── ⚔️ Web开发
│   ├── ⚔️ Kotlin
│   │   └── ⚔️ Android开发
│   └── 💎 VS Code
├── ⚔️ 设计
│   ├── ⚔️ UI设计
│   └── 💎 Figma
├── ⚔️ 语言
│   ├── ⚔️ 英语
│   └── ⚔️ 日语
└── 💎 笔记本电脑
```

### 2.4 Step 4 — 主页功能导览

```
┌─────────────────────────────────────┐
│  ← 返回          4/4               │
├─────────────────────────────────────┤
│                                     │
│          🎉 准备就绪！               │
│                                     │
│  你已经掌握了基础操作，              │
│  以下是更多功能介绍：                │
│                                     │
│  ┌─────────────────────────────┐    │
│  │ 🔍 搜索       查找节点      │    │
│  │ ➕ 创建节点   添加新能力/物质│    │
│  │ ✏️ 编辑内容   Markdown笔记  │    │
│  │ 🔗 关联节点   建立跨分支链接 │    │
│  │ 📤 导出       导出Markdown  │    │
│  └─────────────────────────────┘    │
│                                     │
│       ┌───────────────────┐         │
│       │    开 始 使 用     │         │
│       └───────────────────┘         │
└─────────────────────────────────────┘
```

---

## 3. 状态管理

### 3.1 OnboardingState

```kotlin
package com.fancy.skill_tree.feature.onboarding

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 引导状态管理器
 * 使用 SharedPreferences 持久化引导完成状态
 */
@Singleton
class OnboardingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)

    val isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_COMPLETED, false)

    val shouldShowOnboarding: Boolean
        get() = !isOnboardingCompleted

    fun completeOnboarding() {
        prefs.edit().putBoolean(KEY_COMPLETED, true).apply()
    }

    fun resetOnboarding() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_COMPLETED = "onboarding_completed"
    }
}
```

### 3.2 OnboardingViewModel

```kotlin
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingManager: OnboardingManager,
    private val loadSampleDataUseCase: LoadSampleDataUseCase,
    private val createNodeUseCase: CreateNodeUseCase
) : ViewModel() {

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _keepSampleData = MutableStateFlow(true)
    val keepSampleData: StateFlow<Boolean> = _keepSampleData.asStateFlow()

    fun nextStep() { _currentStep.update { it + 1 } }
    fun prevStep() { _currentStep.update { (it - 1).coerceAtLeast(0) } }

    fun skip() {
        viewModelScope.launch {
            loadSampleDataUseCase()
            onboardingManager.completeOnboarding()
        }
    }

    fun finish(keepSample: Boolean) {
        viewModelScope.launch {
            if (!keepSample) {
                clearSampleDataUseCase()
            }
            onboardingManager.completeOnboarding()
        }
    }
}
```

### 3.3 主入口集成

```kotlin
@Composable
fun SkilltreeApp() {
    val onboardingManager: OnboardingManager = hiltViewModel()
    var showOnboarding by remember { mutableStateOf(onboardingManager.shouldShowOnboarding) }

    if (showOnboarding) {
        OnboardingFlow(
            onComplete = {
                showOnboarding = false
            }
        )
    } else {
        MainAppContent()
    }
}
```

---

## 4. 空状态设计

### 4.1 主页面空状态（已有，需增强）

```
┌─────────────────────────────────────┐
│  🌳 Skill-Tree          🔍  ⚙️     │
├─────────────────────────────────────┤
│                                     │
│                                     │
│              🌳                     │
│                                     │
│        欢迎来到技能树                │
│    开始构建你的技能知识图谱          │
│                                     │
│   ┌───────────────────────────┐     │
│   │     📥 加载示例数据        │     │
│   └───────────────────────────┘     │
│                                     │
│   ┌───────────────────────────┐     │
│   │     ➕ 创建第一个节点       │     │
│   └───────────────────────────┘     │
│                                     │
│   没有数据？试试这些模板：           │
│   ┌──────┐ ┌──────┐ ┌──────┐      │
│   │编程  │ │设计  │ │语言  │      │
│   │入门  │ │入门  │ │学习  │      │  ← 新增：快捷模板
│   └──────┘ └──────┘ └──────┘      │
│                                     │
└─────────────────────────────────────┘
```

### 4.2 搜索空状态

```
┌─────────────────────────────────────┐
│  [搜索框: "xyz"]                ✕   │
├─────────────────────────────────────┤
│                                     │
│             🔍                      │
│                                     │
│      没有找到 "xyz" 相关的结果       │
│                                     │
│   试试：                             │
│   • 检查拼写是否正确                  │
│   • 尝试更简短的关键词                │
│   • 浏览技能树直接查找                │
│                                     │
└─────────────────────────────────────┘
```

### 4.3 标签/链接/附件空状态

```kotlin
@Composable
fun EmptySectionState(
    icon: String,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 40.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(description, color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (actionLabel != null) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onAction!!) {
                Text(actionLabel, color = PrimaryBlue)
            }
        }
    }
}

// 使用:
EmptySectionState("🏷️", "暂无标签", "给节点添加标签方便分类和搜索", "添加标签") { /* 打开标签管理 */ }
EmptySectionState("🔗", "暂无关联节点", "链接可以表示节点之间的关联关系", "添加链接") { /* 打开链接管理 */ }
EmptySectionState("📎", "暂无附件", "支持添加图片和文件作为附件", "添加附件") { /* 打开附件选择 */ }
```

---

## 5. 实施步骤

| 步骤 | 内容 |
|------|------|
| 1 | 创建 `OnboardingManager` 持久化引导状态 |
| 2 | 创建 `OnboardingViewModel` 管理引导流程状态 |
| 3 | 实现 `WelcomeStep`（Step 1）欢迎页 Composable |
| 4 | 实现 `CreateFirstNodeStep`（Step 2）创建节点 Composable |
| 5 | 实现 `SampleTreeStep`（Step 3）示例树展示 Composable |
| 6 | 实现 `FeatureOverviewStep`（Step 4）功能导览 Composable |
| 7 | 创建 `OnboardingFlow` 容器 Composable（步骤切换 + 指示器） |
| 8 | 在 `SkilltreeApp` 入口集成引导流程判断 |
| 9 | 增强主页空状态（添加快捷模板） |
| 10 | 创建通用 `EmptySectionState` 组件 |
| 11 | 在所有列表空场景使用 `EmptySectionState` |