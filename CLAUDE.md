# Skill-Tree 项目规则

## 技术栈
- Kotlin 2.0+ / Jetpack Compose / Hilt / Room
- 最低 SDK: API 26, 目标 SDK: API 35
- Compose BOM: 2025.12.00

## 架构
- Clean Architecture + MVVM
- 模块划分: feature/*, core/*, ai/*, sync/*
- 包结构:
  - com.fancy.skill_tree.core.data (database/dao/repository)
  - com.fancy.skill_tree.core.domain (entity/usecase)
  - com.fancy.skill_tree.core.ui (theme/components)
  - com.fancy.skill_tree.feature.tree, node, editor
  - com.fancy.skill_tree.di (Hilt modules)

## 编码规范
- 所有 public 函数必须有 KDoc 注释
- 使用 StateFlow + Compose State 管理状态
- 禁止使用 !! 非空断言
- 禁止在 Composable 中直接调用 Repository
- 数据流: UI → ViewModel → UseCase → Repository → DataSource

## 数据库
- Room ORM 用于本地数据持久化
- 核心 Entity: SkillNodeEntity, NodeLinkEntity, TagEntity, NodeTagCrossRef, AttachmentEntity

## 测试
- 测试框架: JUnit5 + MockK + Compose UI Test
- 每个功能必须有对应的单元测试

## 禁止事项
- 不要引入未在 PRD 中列出的第三方依赖
- 不要修改 core/domain 中的 Entity 定义（除非有明确需求）
- 不要使用 RxJava（统一用 Kotlin 协程 + Flow）

## 常用命令
- 构建: ./gradlew assembleDebug
- 测试: ./gradlew test
- Lint: ./gradlew lint
