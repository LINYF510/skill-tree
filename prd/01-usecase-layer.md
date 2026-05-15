# UseCase 层设计与实现方案

> **关联 PRD**: 第 3.2 节（架构模式：Clean Architecture + MVVM）
> **当前状态**: UseCase 层完全缺失，ViewModel 直接调用 Repository
> **目标**: 补齐 Domain Layer 的 UseCase 层，实现完整 Clean Architecture

---

## 1. 现状与问题

### 1.1 当前数据流（不规范）

```
UI (Composable) → ViewModel → Repository → DAO → Room DB
                     ↑
               UseCase 层缺失！
```

### 1.2 目标数据流（规范后）

```
UI (Composable) → ViewModel → UseCase → Repository → DAO → Room DB
```

---

## 2. UseCase 设计原则

| 原则 | 说明 |
|------|------|
| **单一职责** | 每个 UseCase 只做一件事 |
| **无状态** | UseCase 不持有任何状态，通过参数输入、通过 Result 输出 |
| **可注入** | 通过 Hilt `@Inject constructor` 注入 |
| **返回 Result** | 统一使用 `kotlin.Result<T>` 或自定义 `Outcome<T>` 封装成功/失败 |
| **命名规范** | 动词 + 名词，如 `CreateNodeUseCase`、`GetRootNodesUseCase` |
| **包结构** | `core/domain/usecase/node/`、`core/domain/usecase/tag/` 等 |

---

## 3. 自定义 Outcome 类型

```kotlin
package com.fancy.skill_tree.core.domain.common

/**
 * 统一的操作结果封装
 * 替代 kotlin.Result，提供更丰富的错误信息
 */
sealed class Outcome<out T> {
    data class Success<T>(val data: T) : Outcome<T>()
    data class Error(val exception: DomainException) : Outcome<Nothing>()
}

/**
 * 领域层异常基类
 */
sealed class DomainException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NodeNotFound(nodeId: String) : DomainException("节点未找到: $nodeId")
    class ParentNodeNotFound(parentId: String) : DomainException("父节点未找到: $parentId")
    class CircularReference(nodeId: String, parentId: String) : DomainException("检测到循环引用: $nodeId -> $parentId")
    class MaxChildrenExceeded(parentId: String, max: Int) : DomainException("父节点 $parentId 子节点数超过上限 $max")
    class InvalidNodeType(type: String) : DomainException("无效的节点类型: $type")
    class TagAlreadyExists(name: String) : DomainException("标签已存在: $name")
    class LinkAlreadyExists(sourceId: String, targetId: String) : DomainException("链接已存在: $sourceId <-> $targetId")
    class SelfLinkNotAllowed(nodeId: String) : DomainException("不能链接自身: $nodeId")
    class StorageError(cause: Throwable) : DomainException("存储操作失败", cause)
    class ValidationError(field: String, reason: String) : DomainException("$field 验证失败: $reason")
}
```

---

## 4. 完整 UseCase 清单

### 4.1 节点管理 (core/domain/usecase/node/)

| UseCase | 职责 | 输入 | 输出 |
|---------|------|------|------|
| `CreateNodeUseCase` | 创建节点（含验证） | `title`, `nodeType`, `parentId?`, `content?` | `Outcome<SkillNodeEntity>` |
| `UpdateNodeUseCase` | 更新节点内容/标题 | `nodeId`, `title?`, `content?`, `nodeType?` | `Outcome<SkillNodeEntity>` |
| `DeleteNodeUseCase` | 删除节点及级联子节点 | `nodeId` | `Outcome<Unit>` |
| `MoveNodeUseCase` | 移动节点到新父节点（含循环引用检测） | `nodeId`, `newParentId` | `Outcome<SkillNodeEntity>` |
| `GetNodeDetailUseCase` | 获取节点详情（含标签、链接、附件计数） | `nodeId` | `Flow<Outcome<NodeDetail>>` |
| `GetRootNodesUseCase` | 获取所有根节点 | 无 | `Flow<List<SkillNodeEntity>>` |
| `GetChildNodesUseCase` | 获取子节点列表 | `parentId` | `Flow<List<SkillNodeEntity>>` |
| `GetAllNodesUseCase` | 获取所有节点 | 无 | `Flow<List<SkillNodeEntity>>` |
| `SearchNodesUseCase` | 搜索节点（标题+内容） | `query` | `Flow<List<SkillNodeEntity>>` |
| `GetNodeCountUseCase` | 获取节点统计 | 无 | `Flow<NodeCountStats>` |
| `ToggleNodeExpandUseCase` | 切换节点展开/折叠 | `nodeId` | `Outcome<SkillNodeEntity>` |
| `ExportToMarkdownUseCase` | 导出整棵树为 Markdown | 无 | `Outcome<String>` |

### 4.2 标签管理 (core/domain/usecase/tag/)

| UseCase | 职责 | 输入 | 输出 |
|---------|------|------|------|
| `CreateTagUseCase` | 创建标签 | `name`, `color?` | `Outcome<TagEntity>` |
| `DeleteTagUseCase` | 删除标签 | `tagId` | `Outcome<Unit>` |
| `AssignTagToNodeUseCase` | 给节点打标签 | `nodeId`, `tagId` | `Outcome<Unit>` |
| `RemoveTagFromNodeUseCase` | 移除节点标签 | `nodeId`, `tagId` | `Outcome<Unit>` |
| `GetTagsForNodeUseCase` | 获取节点的标签 | `nodeId` | `Flow<List<TagEntity>>` |
| `GetAllTagsUseCase` | 获取所有标签 | 无 | `Flow<List<TagEntity>>` |

### 4.3 链接管理 (core/domain/usecase/link/)

| UseCase | 职责 | 输入 | 输出 |
|---------|------|------|------|
| `CreateLinkUseCase` | 创建跨分支链接 | `sourceId`, `targetId`, `linkType` | `Outcome<NodeLinkEntity>` |
| `DeleteLinkUseCase` | 删除链接 | `linkId` | `Outcome<Unit>` |
| `ConfirmAiLinkUseCase` | 确认 AI 推荐链接 | `linkId` | `Outcome<NodeLinkEntity>` |
| `GetLinksForNodeUseCase` | 获取节点的所有链接 | `nodeId` | `Flow<List<NodeLinkWithTarget>>` |
| `GetSuggestedLinksUseCase` | 获取 AI 推荐链接 | `nodeId` | `Flow<List<NodeLinkEntity>>` |

### 4.4 附件管理 (core/domain/usecase/attachment/)

| UseCase | 职责 | 输入 | 输出 |
|---------|------|------|------|
| `AddAttachmentUseCase` | 添加附件 | `nodeId`, `fileUri` | `Outcome<AttachmentEntity>` |
| `DeleteAttachmentUseCase` | 删除附件 | `attachmentId` | `Outcome<Unit>` |
| `GetAttachmentsForNodeUseCase` | 获取节点附件 | `nodeId` | `Flow<List<AttachmentEntity>>` |

### 4.5 游戏化 (core/domain/usecase/gamification/)

| UseCase | 职责 | 输入 | 输出 |
|---------|------|------|------|
| `CalculateNodeLevelUseCase` | 计算节点等级 | `nodeId` | `Outcome<Int>` |
| `GetNodeLevelStatsUseCase` | 获取等级分布统计 | 无 | `Flow<LevelDistribution>` |
| `CheckAchievementsUseCase` | 检查成就触发 | 无 | `Outcome<List<Achievement>>` |
| `GetStatisticsUseCase` | 获取统计面板数据 | 无 | `Flow<StatisticsData>` |

### 4.6 示例数据 (core/domain/usecase/sample/)

| UseCase | 职责 | 输入 | 输出 |
|---------|------|------|------|
| `LoadSampleDataUseCase` | 加载示例技能树 | 无 | `Outcome<Unit>` |
| `ClearSampleDataUseCase` | 清除示例数据 | 无 | `Outcome<Unit>` |

---

## 5. 关键 UseCase 实现示例

### 5.1 CreateNodeUseCase

```kotlin
package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.common.DomainException
import com.fancy.skill_tree.core.domain.common.Outcome
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import java.util.UUID
import javax.inject.Inject

private const val MAX_CHILDREN_PER_NODE = 20

/**
 * 创建技能树节点
 * 包含业务规则验证：标题非空、类型有效性、父节点存在性、子节点上限检查
 */
class CreateNodeUseCase @Inject constructor(
    private val repository: SkillTreeRepository
) {
    /**
     * @param title 节点标题（非空）
     * @param nodeType 节点类型（ABILITY / RESOURCE）
     * @param parentId 父节点 ID（可选，null 表示根节点）
     * @param content 初始 Markdown 内容（可选）
     * @return 操作结果，包含创建后的节点或错误信息
     */
    suspend operator fun invoke(
        title: String,
        nodeType: String,
        parentId: String? = null,
        content: String? = null
    ): Outcome<SkillNodeEntity> {
        if (title.isBlank()) {
            return Outcome.Error(DomainException.ValidationError("title", "标题不能为空"))
        }

        if (nodeType !in listOf("ABILITY", "RESOURCE")) {
            return Outcome.Error(DomainException.InvalidNodeType(nodeType))
        }

        if (parentId != null) {
            val parentNode = repository.getNodeById(parentId)
                ?: return Outcome.Error(DomainException.ParentNodeNotFound(parentId))

            val childCount = repository.getChildNodes(parentId)
            // 注意：需要在 Repository 中新增同步方法或使用 count
            if (parentNode.childrenCount >= MAX_CHILDREN_PER_NODE) {
                return Outcome.Error(DomainException.MaxChildrenExceeded(parentId, MAX_CHILDREN_PER_NODE))
            }
        }

        val currentTime = System.currentTimeMillis()
        val newNode = SkillNodeEntity(
            id = UUID.randomUUID().toString(),
            parentId = parentId,
            title = title.trim(),
            content = content,
            nodeType = nodeType,
            level = 1,
            sortOrder = 0,
            isExpanded = true,
            createdAt = currentTime,
            updatedAt = currentTime
        )

        return try {
            repository.insertNode(newNode)
            Outcome.Success(newNode)
        } catch (e: Exception) {
            Outcome.Error(DomainException.StorageError(e))
        }
    }
}
```

### 5.2 MoveNodeUseCase（含循环引用检测）

```kotlin
package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.common.DomainException
import com.fancy.skill_tree.core.domain.common.Outcome
import javax.inject.Inject

/**
 * 移动节点到新的父节点
 * 包含循环引用检测，防止将父节点移动到其子节点下
 */
class MoveNodeUseCase @Inject constructor(
    private val repository: SkillTreeRepository
) {
    /**
     * @param nodeId 要移动的节点 ID
     * @param newParentId 新的父节点 ID（null 表示变为根节点）
     * @return 操作结果
     */
    suspend operator fun invoke(nodeId: String, newParentId: String?): Outcome<Unit> {
        val node = repository.getNodeById(nodeId)
            ?: return Outcome.Error(DomainException.NodeNotFound(nodeId))

        if (node.parentId == newParentId) {
            return Outcome.Success(Unit)
        }

        if (newParentId == nodeId) {
            return Outcome.Error(DomainException.CircularReference(nodeId, newParentId))
        }

        if (newParentId != null) {
            val parentExists = repository.getNodeById(newParentId) != null
            if (!parentExists) {
                return Outcome.Error(DomainException.ParentNodeNotFound(newParentId))
            }

            if (isDescendantOf(newParentId, nodeId)) {
                return Outcome.Error(DomainException.CircularReference(nodeId, newParentId))
            }
        }

        return try {
            val updatedNode = node.copy(
                parentId = newParentId,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateNode(updatedNode)
            Outcome.Success(Unit)
        } catch (e: Exception) {
            Outcome.Error(DomainException.StorageError(e))
        }
    }

    /**
     * 检查 targetNodeId 是否为 ancestorNodeId 的后代
     * 使用递归向上查找，防止循环引用
     */
    private suspend fun isDescendantOf(targetNodeId: String, ancestorNodeId: String): Boolean {
        var currentNodeId: String? = targetNodeId
        val visited = mutableSetOf<String>()

        while (currentNodeId != null && currentNodeId !in visited) {
            visited.add(currentNodeId)
            if (currentNodeId == ancestorNodeId) return true
            val node = repository.getNodeById(currentNodeId) ?: break
            currentNodeId = node.parentId
        }

        return false
    }
}
```

---

## 6. Hilt 依赖注入

```kotlin
package com.fancy.skill_tree.di

import com.fancy.skill_tree.core.domain.usecase.node.*
import com.fancy.skill_tree.core.domain.usecase.tag.*
import com.fancy.skill_tree.core.domain.usecase.link.*
import com.fancy.skill_tree.core.domain.usecase.attachment.*
import com.fancy.skill_tree.core.domain.usecase.gamification.*
import com.fancy.skill_tree.core.domain.usecase.sample.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt 依赖注入模块
 * 提供所有 UseCase 的单例实例
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides @Singleton
    fun provideCreateNodeUseCase(repo: SkillTreeRepository) = CreateNodeUseCase(repo)

    @Provides @Singleton
    fun provideUpdateNodeUseCase(repo: SkillTreeRepository) = UpdateNodeUseCase(repo)

    @Provides @Singleton
    fun provideDeleteNodeUseCase(repo: SkillTreeRepository) = DeleteNodeUseCase(repo)

    @Provides @Singleton
    fun provideMoveNodeUseCase(repo: SkillTreeRepository) = MoveNodeUseCase(repo)

    @Provides @Singleton
    fun provideGetNodeDetailUseCase(repo: SkillTreeRepository) = GetNodeDetailUseCase(repo)

    @Provides @Singleton
    fun provideGetRootNodesUseCase(repo: SkillTreeRepository) = GetRootNodesUseCase(repo)

    @Provides @Singleton
    fun provideGetChildNodesUseCase(repo: SkillTreeRepository) = GetChildNodesUseCase(repo)

    @Provides @Singleton
    fun provideGetAllNodesUseCase(repo: SkillTreeRepository) = GetAllNodesUseCase(repo)

    @Provides @Singleton
    fun provideSearchNodesUseCase(repo: SkillTreeRepository) = SearchNodesUseCase(repo)

    @Provides @Singleton
    fun provideExportToMarkdownUseCase(repo: SkillTreeRepository) = ExportToMarkdownUseCase(repo)

    @Provides @Singleton
    fun provideCreateTagUseCase(repo: SkillTreeRepository) = CreateTagUseCase(repo)

    @Provides @Singleton
    fun provideAssignTagToNodeUseCase(repo: SkillTreeRepository) = AssignTagToNodeUseCase(repo)

    @Provides @Singleton
    fun provideGetTagsForNodeUseCase(repo: SkillTreeRepository) = GetTagsForNodeUseCase(repo)

    @Provides @Singleton
    fun provideCreateLinkUseCase(repo: SkillTreeRepository) = CreateLinkUseCase(repo)

    @Provides @Singleton
    fun provideGetLinksForNodeUseCase(repo: SkillTreeRepository) = GetLinksForNodeUseCase(repo)

    @Provides @Singleton
    fun provideLoadSampleDataUseCase(repo: SkillTreeRepository) = LoadSampleDataUseCase(repo)
}
```

---

## 7. ViewModel 改造示例

改造前（直接调用 Repository）：

```kotlin
// ❌ 当前代码
fun createNode(title: String, nodeType: String, parentId: String?) {
    val newNode = SkillNodeEntity(...)
    viewModelScope.launch {
        try { repository.insertNode(newNode) }
        catch (e: Exception) { ... }
    }
}
```

改造后（通过 UseCase）：

```kotlin
// ✅ 改造后代码
@HiltViewModel
class SkillTreeViewModel @Inject constructor(
    private val createNodeUseCase: CreateNodeUseCase,
    private val getAllNodesUseCase: GetAllNodesUseCase,
    private val moveNodeUseCase: MoveNodeUseCase
) : ViewModel() {

    fun createNode(title: String, nodeType: String, parentId: String?) {
        viewModelScope.launch {
            when (val result = createNodeUseCase(title, nodeType, parentId)) {
                is Outcome.Success -> {
                    _uiState.update { it.copy(errorMessage = null) }
                }
                is Outcome.Error -> {
                    _uiState.update { it.copy(errorMessage = result.exception.message) }
                }
            }
        }
    }
}
```

---

## 8. 实施步骤

| 步骤 | 内容 | 预估工作量 |
|------|------|-----------|
| 1 | 创建 `Outcome` 和 `DomainException` 类 | 0.5h |
| 2 | 创建 `core/domain/usecase/` 包结构 | 0.1h |
| 3 | 实现 6 个节点相关 UseCase | 2h |
| 4 | 实现 6 个标签相关 UseCase | 1h |
| 5 | 实现 5 个链接相关 UseCase | 1h |
| 6 | 实现 3 个附件相关 UseCase | 0.5h |
| 7 | 实现 3 个游戏化相关 UseCase | 1h |
| 8 | 创建 `UseCaseModule` Hilt 模块 | 0.5h |
| 9 | 改造 `SkillTreeViewModel` 使用 UseCase | 1h |
| 10 | 改造 `NodeDetailViewModel` 使用 UseCase | 0.5h |
| 11 | 编写 UseCase 单元测试 | 2h |
| 12 | 编译验证 | 0.5h |

**总计预估**: 约 10.5 小时