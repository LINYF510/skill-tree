# 测试策略实现提示词

## 项目背景

Skill-Tree 项目（Kotlin/Jetpack Compose/Hilt/Room）已完成核心功能开发和错误处理体系，现在需要建立完整的测试体系。项目规则要求使用 JUnit5 + MockK + Compose UI Test，每个功能必须有对应单元测试。

**重要**: 请先阅读项目规则 `CLAUDE.md` 和测试策略方案 `prd/15-testing-strategy.md`。

---

## 当前测试现状

- 仅有自动生成的 `ExampleUnitTest`（`2+2=4`）和 `ExampleInstrumentedTest`
- 无任何业务逻辑测试
- 测试依赖仅有 JUnit4，缺少 JUnit5/MockK/Turbine/Truth
- `build.gradle` 未配置 JUnit5 Platform

---

## 实现任务

### 任务 1：添加测试依赖

**文件**: `gradle/libs.versions.toml`

在 `[versions]` 中添加：
```toml
junit5 = "5.10.2"
mockk = "1.13.10"
kotlinx-coroutines-test = "1.8.0"
turbine = "1.0.0"
truth = "1.4.2"
```

在 `[libraries]` 中添加：
```toml
junit5-api = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "junit5" }
junit5-engine = { module = "org.junit.jupiter:junit-jupiter-engine", version.ref = "junit5" }
junit5-params = { module = "org.junit.jupiter:junit-jupiter-params", version.ref = "junit5" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinx-coroutines-test" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
truth = { module = "com.google.truth:truth", version.ref = "truth" }
```

**文件**: `app/build.gradle`

1. 在 `android {}` 块中添加：
```groovy
testOptions {
    unitTests {
        includeAndroidResources = true
        returnDefaultValues = true
    }
}
```

2. 替换现有测试依赖：
```groovy
// 删除这行
testImplementation 'junit:junit:4.13.2'

// 替换为
testImplementation libs.junit5.api
testRuntimeOnly libs.junit5.engine
testImplementation libs.junit5.params
testImplementation libs.mockk
testImplementation libs.kotlinx.coroutines.test
testImplementation libs.turbine
testImplementation libs.truth
```

3. 在文件末尾（`dependencies {}` 之后）添加：
```groovy
tasks.withType(Test) {
    useJUnitPlatform()
}
```

### 任务 2：删除旧测试文件

删除以下文件：
- `app/src/test/java/com/fancy/skill_tree/ExampleUnitTest.kt`
- `app/src/androidTest/java/com/fancy/skill_tree/ExampleInstrumentedTest.kt`

### 任务 3：编写 CreateNodeUseCaseTest

**文件**: `app/src/test/java/com/fancy/skill_tree/core/domain/usecase/node/CreateNodeUseCaseTest.kt`

```kotlin
package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.common.DomainException
import com.fancy.skill_tree.core.domain.common.Outcome
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import com.google.common.truth.Truth.assertThat
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("CreateNodeUseCase")
class CreateNodeUseCaseTest {

    private val repository = mockk<SkillTreeRepository>(relaxed = true)
    private lateinit var useCase: CreateNodeUseCase

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        useCase = CreateNodeUseCase(repository)
    }

    @Nested
    @DisplayName("创建根节点")
    inner class CreateRootNode {

        @Test
        @DisplayName("创建根节点成功")
        fun createRootNodeSuccess() = runTest {
            val title = "Python"
            val nodeType = "ABILITY"

            val result = useCase(title, nodeType)

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            val node = (result as Outcome.Success).data
            assertThat(node.title).isEqualTo("Python")
            assertThat(node.nodeType).isEqualTo("ABILITY")
            assertThat(node.parentId).isNull()
            assertThat(node.id).isNotEmpty()
            coVerify(exactly = 1) { repository.insertNode(any()) }
        }

        @Test
        @DisplayName("标题为空应返回验证错误")
        fun blankTitleReturnsError() = runTest {
            val result = useCase("", "ABILITY")

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.ValidationError::class.java)
        }

        @Test
        @DisplayName("标题仅空格应返回验证错误")
        fun whitespaceOnlyTitleReturnsError() = runTest {
            val result = useCase("   ", "ABILITY")

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
        }

        @Test
        @DisplayName("无效的节点类型应返回错误")
        fun invalidNodeTypeReturnsError() = runTest {
            val result = useCase("Python", "INVALID_TYPE")

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.InvalidNodeType::class.java)
        }

        @Test
        @DisplayName("RESOURCE 类型节点创建成功")
        fun resourceTypeNodeSuccess() = runTest {
            val result = useCase("设计资源", "RESOURCE")

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            val node = (result as Outcome.Success).data
            assertThat(node.nodeType).isEqualTo("RESOURCE")
        }

        @Test
        @DisplayName("自定义 ID 应正确设置")
        fun customIdSetCorrectly() = runTest {
            val result = useCase("Test", "ABILITY", customId = "my-custom-id")

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            val node = (result as Outcome.Success).data
            assertThat(node.id).isEqualTo("my-custom-id")
        }

        @Test
        @DisplayName("标题应去除前后空格")
        fun titleTrimmed() = runTest {
            val result = useCase("  Python  ", "ABILITY")

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            val node = (result as Outcome.Success).data
            assertThat(node.title).isEqualTo("Python")
        }
    }

    @Nested
    @DisplayName("创建子节点")
    inner class CreateChildNode {

        @Test
        @DisplayName("父节点不存在应返回错误")
        fun parentNotFoundReturnsError() = runTest {
            coEvery { repository.getNodeById("nonexistent") } returns null

            val result = useCase("Child", "ABILITY", parentId = "nonexistent")

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.ParentNodeNotFound::class.java)
        }

        @Test
        @DisplayName("父节点存在时创建成功")
        fun parentExistsCreateSuccess() = runTest {
            val parentNode = SkillNodeEntity(
                id = "parent1", parentId = null,
                title = "Parent", nodeType = "ABILITY",
                level = 1, sortOrder = 0, isExpanded = true,
                createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
            )
            coEvery { repository.getNodeById("parent1") } returns parentNode

            val result = useCase("Child", "ABILITY", parentId = "parent1")

            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            val node = (result as Outcome.Success).data
            assertThat(node.parentId).isEqualTo("parent1")
        }
    }

    @Nested
    @DisplayName("存储异常")
    inner class StorageError {

        @Test
        @DisplayName("插入异常应返回存储错误")
        fun insertThrowsReturnsStorageError() = runTest {
            coEvery { repository.insertNode(any()) } throws RuntimeException("DB error")

            val result = useCase("Test", "ABILITY")

            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.StorageError::class.java)
        }
    }
}
```

### 任务 4：编写 MoveNodeUseCaseTest

**文件**: `app/src/test/java/com/fancy/skill_tree/core/domain/usecase/node/MoveNodeUseCaseTest.kt`

测试要点：
- 移动到根节点成功
- 移动到新父节点成功
- 节点不存在返回 NodeNotFound
- 目标父节点不存在返回 ParentNodeNotFound
- 循环引用检测（将父节点移动到子节点下）返回 CircularReference
- 移动到自身返回 CircularReference
- 移动到相同父节点返回 Success（无操作）
- 存储异常返回 StorageError

使用与 CreateNodeUseCaseTest 相同的测试风格（@Nested + @DisplayName + runTest + Truth + MockK）。

### 任务 5：编写 DeleteNodeUseCaseTest

**文件**: `app/src/test/java/com/fancy/skill_tree/core/domain/usecase/node/DeleteNodeUseCaseTest.kt`

测试要点：
- 删除存在的节点成功
- 删除不存在的节点返回 NodeNotFound
- 存储异常返回 StorageError

### 任务 6：编写 SkillTreeRepositoryImplTest

**文件**: `app/src/test/java/com/fancy/skill_tree/core/data/repository/SkillTreeRepositoryImplTest.kt`

测试要点：
- `getAllNodes()` Flow 正常返回
- `insertNode()` 成功
- `insertNode()` DAO 抛异常时 safeDbCall 捕获并抛 DataException.DatabaseError
- `getNodeById()` 返回节点
- `getNodeById()` 返回 null
- `deleteNode()` 成功
- Flow 方法 `.catch` 转换异常为 DataException

需要 mock 5 个 DAO：SkillNodeDao, NodeLinkDao, NodeTagDao, TagDao, AttachmentDao。

### 任务 7：编写 SkillTreeViewModelTest

**文件**: `app/src/test/java/com/fancy/skill_tree/feature/tree/SkillTreeViewModelTest.kt`

测试要点：
- 初始化时加载节点
- 创建节点成功
- 创建节点失败时调用 ErrorStateManager
- 删除节点成功
- 移动节点成功
- 搜索功能

需要 mock 所有 UseCase + SearchHistoryManager + ErrorStateManager。
使用 `Turbine` 测试 StateFlow。
使用 `runTest` + `advanceUntilIdle()`。

---

## 编码规范提醒

- 所有 public 函数必须有 KDoc 注释
- 测试类使用 `@DisplayName` 注解
- 测试方法使用 `@DisplayName` 注解，方法名用英文描述性命名
- 使用 `@Nested` 按场景分组
- 使用 `Truth.assertThat()` 而非 JUnit Assert
- 使用 `runTest` 而非 `runBlocking`
- MockK 使用 `relaxed = true` 仅在必要时
- `@BeforeEach` 中 `clearAllMocks()` 清理 mock 状态

---

## 实现优先级

1. **P0（必须）**: 任务 1-2（依赖配置 + 删除旧测试）
2. **P0（必须）**: 任务 3-5（3 个核心 UseCase 测试）
3. **P1（重要）**: 任务 6-7（Repository 和 ViewModel 测试）

---

## 实现完成后

1. 运行 `./gradlew test` 确保所有测试通过
2. 运行 `./gradlew assembleDebug` 确保编译通过
3. 将本次实现内容总结写入 `doc/rep/2026-05-15-测试策略.md`，包含：
   - 实现概述
   - 新增/修改文件列表
   - 测试用例统计
   - 已知问题
