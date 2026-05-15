package com.fancy.skill_tree.core.domain.usecase.node

import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import javax.inject.Inject

/**
 * 加载示例数据 UseCase
 */
class LoadSampleDataUseCase @Inject constructor(
    private val repository: SkillTreeRepository,
    private val createNodeUseCase: CreateNodeUseCase
) {
    suspend operator fun invoke() {
        // 按顺序创建节点（从根节点开始）
        createNodeUseCase("技能树", "ABILITY", null, customId = "sample-root")
        createNodeUseCase("编程", "ABILITY", "sample-root", customId = "sample-programming")
        createNodeUseCase(
            "Python",
            "ABILITY",
            "sample-programming",
            content = "## Python 学习路线\n\n- 基础语法\n- 数据结构\n- 面向对象编程\n- 异步编程",
            customId = "sample-python"
        )
        createNodeUseCase(
            "Kotlin",
            "ABILITY",
            "sample-programming",
            content = "## Kotlin 学习路线\n\n- 协程\n- Compose\n- KMP",
            customId = "sample-kotlin"
        )
        createNodeUseCase("Web开发", "ABILITY", "sample-programming", customId = "sample-web")
        createNodeUseCase(
            "设计",
            "RESOURCE",
            "sample-root",
            content = "## 设计资源\n\n- Figma\n- 色彩理论\n- 排版原则",
            customId = "sample-design"
        )
        createNodeUseCase("数据分析", "RESOURCE", "sample-python", customId = "sample-data-analysis")
    }
}
