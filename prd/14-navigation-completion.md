# 导航栏补全方案

> **关联 PRD**: 第 4.3.1 节（技能树主页布局）
> **当前状态**: 3 个导航项中 2 个为空壳（Favorites、Profile 无对应页面）
> **目标**: 补全底部导航栏功能，使每个 tab 都有实际页面

---

## 1. 导航栏重新设计

| Tab | 当前 | 目标 | 说明 |
|-----|------|------|------|
| Home | ✅ 技能树主页 | ✅ 技能树主页 | 保持 |
| Favorites | 🔴 空壳 | 📊 统计面板 | 替换为 StatisticsScreen |
| Profile | 🔴 空壳 | ⚙️ 设置页 | 替换为 SettingsScreen |

---

## 2. 设置页面设计

```
┌─────────────────────────────────────┐
│  ← 返回          设置               │
├─────────────────────────────────────┤
│  📁 数据管理                         │
│  ┌─────────────────────────────┐    │
│  │ 📤 导出 Markdown          > │    │
│  │ 📋 加载示例数据           > │    │
│  │ 🗑️ 清除所有数据           > │    │
│  └─────────────────────────────┘    │
│                                     │
│  🏷️ 内容管理                         │
│  ┌─────────────────────────────┐    │
│  │ 🏷️ 标签管理              > │    │
│  │ 🔗 链接管理              > │    │
│  └─────────────────────────────┘    │
│                                     │
│  ℹ️ 关于                             │
│  ┌─────────────────────────────┐    │
│  │ 📖 重新引导               > │    │
│  │ ℹ️ 关于 Skill-Tree          │    │
│  └─────────────────────────────┘    │
└─────────────────────────────────────┘
```

---

## 3. 设置页面 Composable

```kotlin
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onExportMarkdown: () -> Unit,
    onLoadSampleData: () -> Unit,
    onClearAllData: () -> Unit,
    onManageTags: () -> Unit,
    onManageLinks: () -> Unit,
    onRestartOnboarding: () -> Unit
) {
    var showClearDataDialog by remember { mutableStateOf(false) }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("清除所有数据", color = TextPrimary) },
            text = { Text("此操作将删除所有节点、标签和链接，且无法恢复。确定要继续吗？", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { onClearAllData(); showClearDataDialog = false }) {
                    Text("确认清除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showClearDataDialog = false }) { Text("取消", color = TextSecondary) } },
            containerColor = SurfaceDark
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        TopAppBar(
            title = { Text("设置", color = TextPrimary) },
            navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = TextPrimary) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
        )

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            SettingsSection("📁 数据管理") {
                SettingsItem("📤 导出 Markdown", "将所有节点导出为 Markdown 文件", onClick = onExportMarkdown)
                SettingsItem("📋 加载示例数据", "加载一棵预置的示例技能树", onClick = onLoadSampleData)
                SettingsItem("🗑️ 清除所有数据", "删除所有节点、标签和链接", textColor = MaterialTheme.colorScheme.error, onClick = { showClearDataDialog = true })
            }

            SettingsSection("🏷️ 内容管理") {
                SettingsItem("🏷️ 标签管理", "查看和管理所有标签", onClick = onManageTags)
                SettingsItem("🔗 链接管理", "查看和管理跨分支链接", onClick = onManageLinks)
            }

            SettingsSection("ℹ️ 关于") {
                SettingsItem("📖 重新引导", "重新查看新手引导", onClick = onRestartOnboarding)
                SettingsItem("ℹ️ 关于 Skill-Tree", "版本 1.0.0", showArrow = false)
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(top = 24.dp)) {
        Text(title, color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        Surface(modifier = Modifier.fillMaxWidth(), color = SurfaceDark) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: String, title: String, subtitle: String? = null,
    showArrow: Boolean = true, textColor: Color = TextPrimary,
    onClick: (() -> Unit)? = null
) {
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(modifier = Modifier.fillMaxWidth().then(clickModifier).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = textColor, fontSize = 15.sp)
            if (subtitle != null) Text(subtitle, color = TextSecondary, fontSize = 13.sp)
        }
        if (showArrow && onClick != null) Text(">", color = TextSecondary, fontSize = 16.sp)
    }
}
```

---

## 4. MainActivity 导航改造

```kotlin
@Composable
fun SkilltreeApp() {
    var currentTab by rememberSaveable { mutableStateOf(0) }
    val navController = rememberNavController()

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            NavigationSuiteItem(
                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                label = { Text("技能树") },
                selected = currentTab == 0,
                onClick = {
                    currentTab = 0
                    navController.navigate(SkillTreeRoute.Tree.route) { popUpTo(SkillTreeRoute.Tree.route) { inclusive = true } }
                }
            )
            NavigationSuiteItem(
                icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                label = { Text("统计") },
                selected = currentTab == 1,
                onClick = { currentTab = 1 }
            )
            NavigationSuiteItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                label = { Text("设置") },
                selected = currentTab == 2,
                onClick = { currentTab = 2 }
            )
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentTab) {
                    0 -> {
                        NavHost(navController = navController, startDestination = SkillTreeRoute.Tree.route) {
                            composable(SkillTreeRoute.Tree.route) {
                                SkillTreeScreen(onNodeClick = { nodeId ->
                                    navController.navigate(SkillTreeRoute.NodeDetail.createRoute(nodeId))
                                })
                            }
                            composable(route = SkillTreeRoute.NodeDetail.route, arguments = listOf(navArgument("nodeId") { type = NavType.StringType })) {
                                NodeDetailScreen(onNavigateBack = { navController.popBackStack() })
                            }
                        }
                    }
                    1 -> StatisticsScreen()
                    2 -> SettingsScreen(
                        onNavigateBack = { currentTab = 0 },
                        onExportMarkdown = { /* TODO */ },
                        onLoadSampleData = { /* TODO */ },
                        onClearAllData = { /* TODO */ },
                        onManageTags = { /* TODO */ },
                        onManageLinks = { /* TODO */ },
                        onRestartOnboarding = { /* TODO */ }
                    )
                }
            }
        }
    }
}
```

---

## 5. 图标资源补充

| 用途 | Material Icon | 资源文件 |
|------|-------------|---------|
| 技能树 Tab | `Icons.Default.Home` | 已有 `ic_home.xml` |
| 统计 Tab | `Icons.Default.BarChart` | 需新建 `ic_statistics.xml` |
| 设置 Tab | `Icons.Default.Settings` | 需新建 `ic_settings.xml` |

---

## 6. 实施步骤

| 步骤 | 内容 |
|------|------|
| 1 | 创建 `ic_statistics.xml` 和 `ic_settings.xml` 图标资源 |
| 2 | 创建 `SettingsScreen` Composable |
| 3 | 创建 `SettingsSection` 和 `SettingsItem` 通用组件 |
| 4 | 创建 `SettingsViewModel` |
| 5 | 更新 `AppDestinations` 枚举 |
| 6 | 改造 `SkilltreeApp` NavigationSuiteScaffold 逻辑 |
| 7 | 集成 StatisticsScreen（来自 11-gamification.md） |
| 8 | 实现标签/链接管理子页面入口 |
| 9 | 编译验证并测试导航切换 |