# P3 可选项提示词

## 项目背景

Skill-Tree 是一个 Kotlin / Jetpack Compose / Hilt / Room 技术栈的 Android 应用，采用 Clean Architecture + MVVM。项目完成度约 98%，所有 P0/P1/P2 问题已解决。本次 P3 包含 4 个可选增强任务。

---

## Task 1: UseCase 单元测试补充（高价值）

### 问题描述

当前仅有 3 个 UseCase 有测试（CreateNodeUseCase、MoveNodeUseCase、DeleteNodeUseCase），还有 12 个 UseCase 缺少测试。测试覆盖率不足可能导致回归问题。

### 现有测试风格

参考 `SkillTreeViewModelTest.kt` 和现有 UseCase 测试的模式：

| 项目 | 模式 |
|------|------|
| 测试框架 | JUnit5（`@Test`, `@Nested`, `@DisplayName`, `@BeforeEach`） |
| Mock 框架 | MockK（`mockk`, `coEvery`, `coVerify`, `every`） |
| 协程测试 | `runTest` + `StandardTestDispatcher` |
| Flow 测试 | `flowOf()` 模拟返回值 |
| 断言库 | Google Truth（`assertThat`） |
| 组织方式 | `@Nested` 内部类按功能分组 |
| 命名规范 | 中文 `@DisplayName` |

### 需要编写的测试文件

#### 1.1 UpdateNodeUseCaseTest

**被测 UseCase 核心逻辑**:
- 通过 `getNodeById` 查找节点，不存在返回 `NodeNotFound`
- 校验 `nodeType` 必须为 "ABILITY" 或 "RESOURCE"，否则返回 `InvalidNodeType`
- 合并更新字段，调用 `repository.updateNode`
- 成功返回 `Outcome.Success`，异常返回 `Outcome.Error(StorageError)`

**测试用例设计**:
```
@Nested @DisplayName("更新节点")
- 成功更新标题 → Outcome.Success
- 成功更新内容 → Outcome.Success
- 成功更新节点类型 → Outcome.Success
- 节点不存在 → Outcome.Error(NodeNotFound)
- 无效节点类型 → Outcome.Error(InvalidNodeType)
- 标题自动 trim → 验证 trim 后的值
- Repository 异常 → Outcome.Error(StorageError)
```

#### 1.2 ExportToMarkdownUseCaseTest

**被测 UseCase 核心逻辑**:
- 获取所有节点，按 parentId 递归构建树形 Markdown
- 空节点返回占位文本
- ABILITY 用 ⚔️ 图标，RESOURCE 用 💎 图标
- 内容按行缩进输出

**测试用例设计**:
```
@Nested @DisplayName("导出 Markdown")
- 空节点列表 → 返回占位文本
- 单个根节点 → 正确输出标题
- 带子节点的树 → 递归缩进正确
- ABILITY 节点 → ⚔️ 图标
- RESOURCE 节点 → 💎 图标
- 带内容的节点 → 内容按行缩进
- Repository 异常 → Outcome.Error(StorageError)
```

#### 1.3 ClearAllDataUseCaseTest

**测试用例设计**:
```
@Nested @DisplayName("清除所有数据")
- 成功清除 → Outcome.Success(Unit)
- Repository 异常 → Outcome.Error(StorageError)
- 验证 repository.deleteAllData() 被调用
```

#### 1.4 CreateTagUseCaseTest

**测试用例设计**:
```
@Nested @DisplayName("创建标签")
- 成功创建 → Outcome.Success
- 空名称 → Outcome.Error(ValidationError)
- 纯空格名称 → Outcome.Error(ValidationError)
- Repository 异常 → Outcome.Error(StorageError)
```

#### 1.5 CreateLinkUseCaseTest

**测试用例设计**:
```
@Nested @DisplayName("创建链接")
- 成功创建手动链接 → Outcome.Success，linkType = "MANUAL"
- 成功创建 AI 推荐链接 → Outcome.Success，linkType = "AI_SUGGESTED"
- 默认 linkType 为 MANUAL
- Repository 异常 → Outcome.Error(StorageError)
```

#### 1.6 DeleteLinkUseCaseTest / ConfirmLinkUseCaseTest

**测试用例设计**:
```
DeleteLink:
- 成功删除 → Outcome.Success(Unit)
- Repository 异常 → Outcome.Error(StorageError)

ConfirmLink:
- 成功确认 → Outcome.Success(Unit)
- Repository 异常 → Outcome.Error(StorageError)
```

#### 1.7 AddAttachmentUseCaseTest

**被测 UseCase 特殊依赖**: `SkillTreeRepository` + `AttachmentFileManager`

**测试用例设计**:
```
@Nested @DisplayName("添加附件")
- 成功添加 → fileManager.copyToInternalStorage + repository.addAttachment 被调用
- 文件复制失败 → Outcome.Error(StorageError)
- Repository 异常 → Outcome.Error(StorageError)
```

#### 1.8 DeleteAttachmentUseCaseTest

**被测 UseCase 特殊依赖**: `SkillTreeRepository` + `AttachmentFileManager`

**测试用例设计**:
```
@Nested @DisplayName("删除附件")
- 成功删除（有文件路径）→ fileManager.deleteFile + repository.deleteAttachment 被调用
- 附件无文件路径 → 仅 repository.deleteAttachment 被调用
- 附件不存在 → repository.deleteAttachment 仍被调用
- Repository 异常 → Outcome.Error(StorageError)
```

#### 1.9 CheckAchievementsUseCaseTest

**被测 UseCase 特殊依赖**: `SkillTreeRepository` + `AchievementManager`

**测试用例设计**:
```
@Nested @DisplayName("成就检查")
- 首个节点 → 解锁 FIRST_NODE
- 10 个节点 → 解锁 TEN_NODES
- 50 个节点 → 解锁 FIFTY_NODES
- 20 条链接 → 解锁 TWENTY_LINKS
- 5 个 Lv5 节点 → 解锁 FIVE_LEVEL5
- 深度 5 → 解锁 TREE_DEPTH_5
- 10 条 AI 确认链接 → 解锁 TEN_AI_CONFIRMS
- 10 次语音输入 → 解锁 TEN_VOICE
- 20 张图片 → 解锁 TWENTY_IMAGES
- 连续 7 天 → 解锁 DAILY_STREAK_7
- 已解锁的成就不重复解锁
- 多个条件同时满足 → 返回多个新成就
```

#### 1.10 SearchNodesUseCaseTest

**被测 UseCase 特殊**: 返回 `Flow` 而非 `Outcome`，非 suspend 函数

**测试用例设计**:
```
@Nested @DisplayName("搜索节点")
- 空查询 + 空标签 → 返回所有节点
- 有关键词 + 空标签 → 全文搜索
- 空查询 + 有标签 → 标签搜索
- 有关键词 + 有标签 → 取交集
- 查询自动 trim
```

### 实现要求

1. 每个测试文件放在 `app/src/test/java/com/fancy/skill_tree/core/domain/usecase/node/` 或 `gamification/` 目录下
2. 使用 JUnit5 + MockK + Truth + Turbine 测试栈
3. `@Nested` 内部类按功能分组，`@DisplayName` 使用中文
4. Mock 所有依赖（repository、fileManager、achievementManager）
5. 每个测试方法有明确的断言（`assertThat`）
6. 修改完成后运行 `./gradlew test` 确认所有测试通过

---

## Task 2: ViewModel 单元测试补充

### 需要编写的测试文件

#### 2.1 NodeDetailViewModelTest

**被测 ViewModel 依赖**: 16 个 UseCase + ErrorStateManager（依赖较多，建议重点测试核心流程）

**测试用例设计**:
```
@Nested @DisplayName("加载节点详情")
- 成功加载 → uiState.node 非空
- 节点不存在 → 错误处理

@Nested @DisplayName("标签管理")
- addTag → assignTagToNodeUseCase 被调用
- removeTag → removeTagFromNodeUseCase 被调用
- createAndAssignTag → createTagUseCase + assignTagToNodeUseCase 被调用

@Nested @DisplayName("链接管理")
- createLink → createLinkUseCase 被调用
- deleteLink → deleteLinkUseCase 被调用
- confirmLink → confirmLinkUseCase 被调用

@Nested @DisplayName("编辑模式")
- toggleEditing → isEditing 状态切换
- saveContent → updateNodeUseCase 被调用

@Nested @DisplayName("错误处理")
- UseCase 返回 Error → ErrorStateManager 被调用
```

#### 2.2 SettingsViewModelTest

**测试用例设计**:
```
@Nested @DisplayName("导出 Markdown")
- 成功导出 → onSuccess 回调被调用
- 导出失败 → ErrorStateManager 被调用

@Nested @DisplayName("加载示例数据")
- 成功加载 → createNodeUseCase 被调用 7 次

@Nested @DisplayName("清除数据")
- 成功清除 → clearAllDataUseCase 被调用
- 清除失败 → ErrorStateManager 被调用
```

#### 2.3 StatisticsViewModelTest

**测试用例设计**:
```
@Nested @DisplayName("加载统计数据")
- 成功加载 → uiState.statistics 非空
- Flow 异常 → ErrorStateManager 被调用

@Nested @DisplayName("成就数据")
- loadAchievements → uiState.achievements 非空
- refreshAchievements → 成就列表更新
```

#### 2.4 OnboardingViewModelTest

**测试用例设计**:
```
@Nested @DisplayName("步骤导航")
- nextStep → currentStep + 1
- prevStep → currentStep - 1
- prevStep 在第一步 → 不变

@Nested @DisplayName("完成引导")
- finish(true) → 保留示例数据，onboardingManager.complete() 被调用
- finish(false) → 不保留示例数据，onboardingManager.complete() 被调用

@Nested @DisplayName("创建首个节点")
- createFirstNode → createNodeUseCase 被调用
```

### 实现要求

1. 每个测试文件放在 `app/src/test/java/com/fancy/skill_tree/feature/` 对应目录下
2. 使用 JUnit5 + MockK + Truth + Turbine
3. `Dispatchers.setMain(StandardTestDispatcher())` 在 `@BeforeEach` 中设置
4. `Dispatchers.resetMain()` 在 `@AfterEach` 中恢复
5. ViewModel 通过工厂方法创建，手动注入所有 mock
6. 修改完成后运行 `./gradlew test` 确认所有测试通过

---

## Task 3: DatabaseRecoveryManager 实现

### 问题描述

当 Room 数据库损坏（如 WAL 文件丢失、IO 错误等）时，应用会崩溃。需要实现 `DatabaseRecoveryManager` 在数据库损坏时自动恢复。

### PRD 设计（来自 prd/02-error-handling.md）

```kotlin
/**
 * 数据库恢复管理器
 * 在数据库损坏时尝试恢复，最坏情况重建数据库
 */
@Singleton
class DatabaseRecoveryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * 尝试恢复数据库
     * 1. 尝试备份数据
     * 2. 删除损坏的数据库文件
     * 3. 重新创建数据库
     */
    fun recoverDatabase(): Boolean {
        return try {
            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            if (dbFile.exists()) {
                // 尝试备份
                val backupFile = File(context.cacheDir, "${AppDatabase.DATABASE_NAME}.bak")
                dbFile.renameTo(backupFile)
            }
            // 删除 WAL 和 SHM 文件
            context.getDatabasePath("${AppDatabase.DATABASE_NAME}-wal").delete()
            context.getDatabasePath("${AppDatabase.DATABASE_NAME}-shm").delete()
            // 删除主数据库文件
            dbFile.delete()
            true
        } catch (e: Exception) {
            false
        }
    }
}
```

### 实现方案

1. 在 `core/data/database/` 目录下创建 `DatabaseRecoveryManager.kt`
2. 在 `DatabaseModule.kt` 中注册为 `@Singleton`
3. 在 `SkillTreeApplication.kt` 中添加全局异常处理：

```kotlin
override fun onCreate() {
    super.onCreate()
    val recoveryManager = DatabaseRecoveryManager(this)
    
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        if (throwable is SQLiteException || throwable?.cause is SQLiteException) {
            recoveryManager.recoverDatabase()
            // 重启应用
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            Process.killProcess(Process.myPid())
        } else {
            // 交给原有异常处理器
            Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(thread, throwable)
        }
    }
}
```

### 实现要求

1. 创建 `DatabaseRecoveryManager.kt`，包含 KDoc 注释
2. 恢复流程：备份损坏文件 → 删除 WAL/SHM/主文件 → 返回是否成功
3. 在 `SkillTreeApplication` 中注册全局异常处理
4. 保留原有 `UncaughtExceptionHandler` 链
5. 修改完成后运行 `./gradlew assembleDebug` 确认编译通过

---

## Task 4: build.gradle 依赖引用统一

### 问题描述

`app/build.gradle` 第 93 行使用硬编码字符串引入 splashscreen 依赖：
```groovy
implementation 'androidx.core:core-splashscreen:1.0.1'
```

而 `libs.versions.toml` 中已定义了该依赖：
```toml
[versions]
core-splashscreen = "1.0.1"

[libraries]
core-splashscreen = { group = "androidx.core", name = "core-splashscreen", version.ref = "core-splashscreen" }
```

### 修改方案

将 `app/build.gradle` 中的硬编码替换为版本目录引用：
```groovy
// 修改前
implementation 'androidx.core:core-splashscreen:1.0.1'

// 修改后
implementation libs.core.splashscreen
```

### 验证

修改完成后运行 `./gradlew assembleDebug` 确认编译通过。

---

## 验证清单

1. `./gradlew assembleDebug` 编译通过
2. `./gradlew test` 所有测试通过（包括新增测试）
3. 新增测试文件数量：12 个 UseCase 测试 + 4 个 ViewModel 测试 = 16 个
4. `DatabaseRecoveryManager` 存在且可编译
5. `build.gradle` 中无硬编码依赖字符串
