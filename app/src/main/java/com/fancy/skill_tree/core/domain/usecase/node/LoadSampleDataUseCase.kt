package com.fancy.skill_tree.core.domain.usecase.node

import android.content.Context
import com.fancy.skill_tree.R
import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import javax.inject.Inject

/**
 * 加载示例数据 UseCase
 */
class LoadSampleDataUseCase @Inject constructor(
    private val repository: SkillTreeRepository,
    private val createNodeUseCase: CreateNodeUseCase
) {
    /**
     * 加载预置的示例技能树数据
     * @param context Android Context，用于获取国际化字符串
     */
    suspend operator fun invoke(context: Context) {
        createNodeUseCase(context.getString(R.string.sample_root_title), "ABILITY", null, customId = "sample-root")
        createNodeUseCase(context.getString(R.string.sample_programming_title), "ABILITY", "sample-root", customId = "sample-programming")
        createNodeUseCase(
            context.getString(R.string.sample_python_title),
            "ABILITY",
            "sample-programming",
            content = context.getString(R.string.sample_python_content),
            customId = "sample-python"
        )
        createNodeUseCase(
            context.getString(R.string.sample_kotlin_title),
            "ABILITY",
            "sample-programming",
            content = context.getString(R.string.sample_kotlin_content),
            customId = "sample-kotlin"
        )
        createNodeUseCase(context.getString(R.string.sample_web_title), "ABILITY", "sample-programming", customId = "sample-web")
        createNodeUseCase(
            context.getString(R.string.sample_design_title),
            "RESOURCE",
            "sample-root",
            content = context.getString(R.string.sample_design_content),
            customId = "sample-design"
        )
        createNodeUseCase(context.getString(R.string.sample_data_analysis_title), "RESOURCE", "sample-python", customId = "sample-data-analysis")
    }
}
