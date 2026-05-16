# 🌳 Skill-Tree

一个功能丰富的 Android 技能树管理应用，使用 Jetpack Compose 构建现代化 UI，支持可视化构建和管理个人技能树。

## ✨ 功能亮点

### 🌳 核心功能
- **可视化技能树** — Canvas 绘制的交互式树形结构，支持缩放、平移、拖拽
- **节点管理** — 创建/编辑/删除/移动技能节点，支持 5 种等级（新手→大师）
- **跨分支连线** — 节点间支持确认/待确认两种连线状态
- **标签系统** — 为节点添加自定义标签，支持按标签筛选
- **附件系统** — 为节点添加图片、文件附件，支持拍照（CameraX）、相册选择、文件选择
- **Markdown 导出** — 将技能树导出为 Markdown 格式

### 🎨 主题与外观
- **Material You 动态颜色** — Android 12+ 支持壁纸取色
- **暗色/亮色/跟随系统** — 三种主题模式自由切换
- **粒子特效** — 节点创建和升级时的粒子动画
- **节点解锁光效** — 等级提升时的扩散光效动画
- **7 种动画效果** — 创建弹簧、树生长、选中脉冲、搜索呼吸、连线绘制、成就解锁、页面转场

### 🌐 国际化
- **中英双语** — 完整的 i18n 支持，源码零硬编码字符串
- **语言切换** — 支持中文/英文/跟随系统

### ♿ 无障碍
- **TalkBack 支持** — Canvas 节点语义代理层，最多 50 个无障碍节点
- **键盘导航** — 方向键移动焦点，Enter/Spacebar 激活
- **语义化描述** — 展开/折叠/开关状态的无障碍文本

### 🏆 游戏化
- **10 个成就** — 首个节点、10 节点、50 节点、7 天连续、全解锁等
- **每日连续天数** — 自动追踪使用连续性
- **成就解锁动画** — 底部滑入 + 缩放弹出

### 📊 其他
- **统计面板** — 节点数、连线数、附件数、标签数
- **新手引导** — 4 页功能介绍
- **搜索增强** — 支持标签筛选 Chips
- **本地存储** — Room 数据库，隐私安全，无需网络

## 🛠 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin 2.0+ |
| UI | Jetpack Compose + Material 3 |
| 架构 | Clean Architecture + MVVM |
| DI | Hilt |
| 数据库 | Room (WAL 模式) |
| 异步 | Kotlin 协程 + Flow |
| 图片 | Coil |
| 相机 | CameraX 1.3.4 |
| 测试 | JUnit5 + MockK + Turbine + Compose UI Test |
| 最低 SDK | API 26 (Android 8.0) |
| 目标 SDK | API 35 |

## 📐 架构

```
┌─────────────────────────────────────────┐
│                 UI Layer                 │
│  Composable → ViewModel → UiState       │
├─────────────────────────────────────────┤
│              Domain Layer               │
│  UseCase → Entity → DomainException     │
├─────────────────────────────────────────┤
│               Data Layer                │
│  Repository → DAO → Room Database       │
│  SafeDbCall → DataException             │
└─────────────────────────────────────────┘
```

**数据流**: UI → ViewModel → UseCase → Repository → DataSource

**错误处理**: DataException → DomainException → ErrorStateManager → UiError → Snackbar/Dialog

## 📁 项目结构

```
app/src/main/java/com/fancy/skill_tree/
├── core/
│   ├── data/           # 数据层（Database, DAO, Repository, File Manager）
│   ├── domain/         # 领域层（Entity, UseCase, DomainException）
│   └── ui/             # UI 公共组件（Theme, Animation, Accessibility, Error, Render）
├── feature/
│   ├── tree/           # 技能树主界面（Canvas 绘制 + 手势 + 动画）
│   ├── node/           # 节点详情（编辑、附件、相机）
│   ├── settings/       # 设置（主题、语言、示例数据、清除数据）
│   ├── statistics/     # 统计面板
│   └── onboarding/     # 新手引导
├── di/                 # Hilt 依赖注入模块
└── MainActivity.kt     # 入口 Activity + NavHost
```

## 🧪 测试

```bash
# 运行全部单元测试
./gradlew test

# 运行指定测试
./gradlew test --tests "com.fancy.skill_tree.core.domain.usecase.*"

# 构建调试 APK
./gradlew assembleDebug

# 构建发布 APK
./gradlew assembleRelease
```

测试覆盖 **131 个测试用例**，涵盖 20 个测试文件：
- 16 个 UseCase 测试（Create/Update/Delete/Move/Export/Clear/Tag/Link/Attachment/Achievement/Search）
- 4 个 ViewModel 测试（SkillTree/NodeDetail/Statistics/Settings）

## 📥 安装

1. 从 [Releases](https://github.com/LINYF510/skill-tree/releases) 下载最新 APK
2. 在 Android 设备上启用"允许安装未知来源应用"
3. 安装 APK
4. 最低系统要求：Android 8.0 (API 26)

## 📄 许可证

MIT License
