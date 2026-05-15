# 测试策略方案

> **关联 PRD**: 第 8 节（非功能性需求）
> **当前状态**: 仅有自动生成的 `ExampleUnitTest`（`2+2=4`）
> **CLAUDE.md 要求**: JUnit5 + MockK + Compose UI Test，每个功能必须有对应单元测试
> **目标**: 建立完整的测试体系和 CI 集成

---

## 1. 测试金字塔

```
          ╱──────╲
         ╱  E2E   ╲        少量：关键用户流程
        ╱──────────╲
       ╱  UI Test   ╲      中等：Compose UI Test
      ╱──────────────╲
     ╱ Integration Test╲   中等：DAO + Repository
    ╱──────────────────╲
   ╱   Unit Test         ╲ 大量：UseCase + ViewModel + Entity
  ╱────────────────────────╲
```

---

## 2. 测试框架与依赖

### 2.1 依赖添加

```toml
# gradle/libs.versions.toml
[versions]
junit5 = "5.10.2"
mockk = "1.13.10"
kotlinx-coroutines-test = "1.8.0"
compose-ui-test = "1.6.0"
turbine = "1.0.0"
truth = "1.4.2"

[libraries]
junit5-api = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "junit5" }
junit5-engine = { module = "org.junit.jupiter:junit-jupiter-engine", version.ref = "junit5" }
junit5-params = { module = "org.junit.jupiter:junit-jupiter-params", version.ref = "junit5" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinx-coroutines-test" }
compose-ui-test = { module = "androidx.compose.ui:ui-test-junit4", version.ref = "compose-ui-test" }
compose-ui-test-manifest = { module = "androidx.compose.ui:ui-test-manifest", version.ref = "compose-ui-test" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
truth = { module = "com.google.truth:truth", version.ref = "truth" }
```

### 2.2 build.gradle 配置

```kotlin
// app/build.gradle
android {
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.junit5.params)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)

    androidTestImplementation(libs.compose.ui.test)
    androidTestImplementation(libs.compose.ui.test.manifest)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

---

## 3. 单元测试模板

### 3.1 UseCase 测试

```kotlin
package com.fancy.skill_tree.core.domain.usecase.node

import app.cash.turbine.test
import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.common.DomainException
import com.fancy.skill_tree.core.domain.common.Outcome
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
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
        fun `创建根节点成功`() = runTest {
            // Given
            val title = "Python"
            val nodeType = "ABILITY"

            // When
            val result = useCase(title, nodeType)

            // Then
            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            val node = (result as Outcome.Success).data
            assertThat(node.title).isEqualTo("Python")
            assertThat(node.nodeType).isEqualTo("ABILITY")
            assertThat(node.parentId).isNull()
            assertThat(node.id).isNotEmpty()

            coVerify(exactly = 1) { repository.insertNode(any()) }
        }

        @Test
        fun `标题为空应返回错误`() = runTest {
            val result = useCase("", "ABILITY")
            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.ValidationError::class.java)
        }

        @Test
        fun `标题仅空格应返回错误`() = runTest {
            val result = useCase("   ", "ABILITY")
            assertThat(result).isInstanceOf(Outcome.Error::class.java)
        }

        @Test
        fun `无效的节点类型应返回错误`() = runTest {
            val result = useCase("Python", "INVALID_TYPE")
            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.InvalidNodeType::class.java)
        }
    }

    @Nested
    @DisplayName("创建子节点")
    inner class CreateChildNode {
        @Test
        fun `父节点不存在应返回错误`() = runTest {
            // Given
            coEvery { repository.getNodeById("nonexistent") } returns null

            // When
            val result = useCase("Child", "ABILITY", parentId = "nonexistent")

            // Then
            assertThat(result).isInstanceOf(Outcome.Error::class.java)
            val error = (result as Outcome.Error).exception
            assertThat(error).isInstanceOf(DomainException.ParentNodeNotFound::class.java)
        }

        @Test
        fun `创建成功`() = runTest {
            // Given
            val parentNode = SkillNodeEntity(
                id = "parent1", parentId = null,
                title = "Parent", nodeType = "ABILITY",
                level = 1, sortOrder = 0, isExpanded = true,
                createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
            )
            coEvery { repository.getNodeById("parent1") } returns parentNode

            // When
            val result = useCase("Child", "ABILITY", parentId = "parent1")

            // Then
            assertThat(result).isInstanceOf(Outcome.Success::class.java)
            val node = (result as Outcome.Success).data
            assertThat(node.parentId).isEqualTo("parent1")
        }
    }
}
```

### 3.2 Repository 测试

```kotlin
@DisplayName("SkillTreeRepositoryImpl")
class SkillTreeRepositoryImplTest {

    private val nodeDao = mockk<SkillNodeDao>(relaxed = true)
    private val nodeLinkDao = mockk<NodeLinkDao>(relaxed = true)
    private val nodeTagDao = mockk<NodeTagDao>(relaxed = true)
    private val tagDao = mockk<TagDao>(relaxed = true)
    private val attachmentDao = mockk<AttachmentDao>(relaxed = true)
    private lateinit var repository: SkillTreeRepositoryImpl

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        repository = SkillTreeRepositoryImpl(nodeDao, nodeLinkDao, nodeTagDao, tagDao, attachmentDao)
    }

    @Test
    fun `获取所有节点`() = runTest {
        val nodes = listOf(
            SkillNodeEntity(id = "1", title = "Node1", ...),
            SkillNodeEntity(id = "2", title = "Node2", ...)
        )
        coEvery { nodeDao.getAllNodes() } returns flowOf(nodes)

        repository.getAllNodes().test {
            val result = awaitItem()
            assertThat(result).hasSize(2)
            assertThat(result[0].title).isEqualTo("Node1")
            awaitComplete()
        }
    }

    @Test
    fun `插入节点异常应抛出`() = runTest {
        coEvery { nodeDao.insertNode(any()) } throws SQLiteException()

        assertThrows<DataException.DatabaseError> {
            runBlocking { repository.insertNode(SkillNodeEntity(...)) }
        }
    }
}
```

### 3.3 ViewModel 测试

```kotlin
@DisplayName("SkillTreeViewModel")
class SkillTreeViewModelTest {

    private val createNodeUseCase = mockk<CreateNodeUseCase>(relaxed = true)
    private val getAllNodesUseCase = mockk<GetAllNodesUseCase>(relaxed = true)
    private val deleteNodeUseCase = mockk<DeleteNodeUseCase>(relaxed = true)

    @Test
    fun `创建节点成功应更新 UI 状态`() = runTest {
        val viewModel = SkillTreeViewModel(createNodeUseCase, getAllNodesUseCase, ...)

        coEvery { createNodeUseCase(any(), any(), any()) } returns Outcome.Success(SkillNodeEntity(...))
        viewModel.createNode("Python", "ABILITY", null)

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }

    @Test
    fun `创建节点失败应设置错误消息`() = runTest {
        val viewModel = SkillTreeViewModel(createNodeUseCase, ...)

        coEvery { createNodeUseCase(any(), any(), any()) } returns Outcome.Error(DomainException.ValidationError("title", "不能为空"))
        viewModel.createNode("", "ABILITY", null)

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.errorMessage).isNotNull()
    }
}
```

---

## 4. Compose UI 测试

```kotlin
@RunWith(AndroidJUnit4::class)
class SkillTreeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `空状态应显示引导界面`() {
        composeTestRule.setContent {
            SkilltreeTheme {
                // 模拟空状态
                SkillTreeScreen(...)
            }
        }

        composeTestRule.onNodeWithText("欢迎来到技能树").assertIsDisplayed()
        composeTestRule.onNodeWithText("加载示例数据").assertIsDisplayed()
        composeTestRule.onNodeWithText("创建第一个节点").assertIsDisplayed()
    }

    @Test
    fun `点击创建按钮应弹出创建对话框`() {
        composeTestRule.setContent {
            SkilltreeTheme {
                SkillTreeScreen(...)
            }
        }

        composeTestRule.onNodeWithContentDescription("创建新节点").performClick()
        composeTestRule.onNodeWithText("新建节点").assertIsDisplayed()
    }

    @Test
    fun `搜索功能测试`() {
        composeTestRule.setContent {
            SkilltreeTheme {
                SkillTreeScreen(...)
            }
        }

        composeTestRule.onNodeWithContentDescription("搜索").performClick()
        composeTestRule.onNodeWithTag("search_input").performTextInput("Python")

        // 验证搜索结果
        composeTestRule.onNodeWithText("Python 编程").assertIsDisplayed()
    }
}
```

---

## 5. DAO 集成测试

```kotlin
@RunWith(AndroidJUnit4::class)
class SkillNodeDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: SkillNodeDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = database.skillNodeDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `插入并查询节点`() = runTest {
        val node = SkillNodeEntity(
            id = "test1", title = "Test Node", nodeType = "ABILITY",
            level = 1, sortOrder = 0, isExpanded = true,
            createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
        )
        dao.insertNode(node)

        val result = dao.getNodeById("test1")
        assertThat(result).isNotNull()
        assertThat(result?.title).isEqualTo("Test Node")
    }

    @Test
    fun `全文搜索`() = runTest {
        dao.insertNode(SkillNodeEntity(id = "1", title = "Python", content = "Python is great", ...))
        dao.insertNode(SkillNodeEntity(id = "2", title = "Java", content = "Java is popular", ...))

        val results = dao.searchFullText("Python").first()
        assertThat(results).hasSize(1)
        assertThat(results[0].title).isEqualTo("Python")
    }
}
```

---

## 6. 测试覆盖率目标

| 层级 | 目标覆盖率 | 测试类型 |
|------|----------|---------|
| UseCase | ≥ 95% | 单元测试 |
| Entity / 数据类 | ≥ 90% | 单元测试 |
| Repository | ≥ 85% | 单元测试 + Mock DAO |
| ViewModel | ≥ 85% | 单元测试 + Mock UseCase |
| DAO | ≥ 80% | 集成测试 (In-Memory Room) |
| UI (Composable) | ≥ 70% | Compose UI Test |
| 关键用户流程 | 100% | E2E (手动 + 未来自动化) |

---

## 7. CI 集成（GitHub Actions 示例）

```yaml
name: Android CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Run Unit Tests
        run: ./gradlew test

      - name: Run Lint
        run: ./gradlew lint

      - name: Upload Test Results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: |
            app/build/reports/tests/
            app/build/reports/lint-results.html
```

---

## 8. 测试文件组织结构

```
app/src/
├── test/java/com/fancy/skill_tree/
│   ├── core/domain/usecase/node/
│   │   ├── CreateNodeUseCaseTest.kt
│   │   ├── UpdateNodeUseCaseTest.kt
│   │   ├── DeleteNodeUseCaseTest.kt
│   │   └── MoveNodeUseCaseTest.kt
│   ├── core/domain/usecase/tag/
│   │   ├── CreateTagUseCaseTest.kt
│   │   └── AssignTagToNodeUseCaseTest.kt
│   ├── core/domain/usecase/link/
│   │   └── CreateLinkUseCaseTest.kt
│   ├── core/domain/usecase/gamification/
│   │   ├── CalculateNodeLevelUseCaseTest.kt
│   │   └── CheckAchievementsUseCaseTest.kt
│   ├── core/data/repository/
│   │   └── SkillTreeRepositoryImplTest.kt
│   └── feature/tree/
│       ├── SkillTreeViewModelTest.kt
│       └── TreeLayoutTest.kt
│
├── androidTest/java/com/fancy/skill_tree/
│   ├── core/data/database/dao/
│   │   ├── SkillNodeDaoTest.kt
│   │   ├── NodeLinkDaoTest.kt
│   │   └── TagDaoTest.kt
│   └── feature/tree/
│       └── SkillTreeScreenTest.kt
```

---

## 9. 实施步骤

| 步骤 | 内容 |
|------|------|
| 1 | 添加 JUnit5、MockK、Turbine、Truth 测试依赖 |
| 2 | 配置 `build.gradle` 使用 JUnit5 Platform |
| 3 | 编写 `CreateNodeUseCaseTest`（高优先级） |
| 4 | 编写 `MoveNodeUseCaseTest`（含循环引用测试） |
| 5 | 编写 `DeleteNodeUseCaseTest`（含级联删除测试） |
| 6 | 编写 `CalculateNodeLevelUseCaseTest` |
| 7 | 编写 `SkillTreeRepositoryImplTest` |
| 8 | 编写 `SkillTreeViewModelTest` |
| 9 | 编写 `SkillNodeDaoTest`（In-Memory Room） |
| 10 | 编写 `SkillTreeScreenTest`（Compose UI Test） |
| 11 | 配置 GitHub Actions CI |
| 12 | 删除旧的 `ExampleUnitTest` |