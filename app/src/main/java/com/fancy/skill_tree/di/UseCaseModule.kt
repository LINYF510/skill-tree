package com.fancy.skill_tree.di

import com.fancy.skill_tree.core.data.manager.AchievementManager
import com.fancy.skill_tree.core.data.manager.SearchHistoryManager
import com.fancy.skill_tree.core.domain.usecase.gamification.CheckAchievementsUseCase
import com.fancy.skill_tree.core.domain.usecase.gamification.SearchNodesUseCase
import com.fancy.skill_tree.core.domain.usecase.node.AddAttachmentUseCase
import com.fancy.skill_tree.core.domain.usecase.node.AssignTagToNodeUseCase
import com.fancy.skill_tree.core.domain.usecase.node.ClearAllDataUseCase
import com.fancy.skill_tree.core.domain.usecase.node.ConfirmLinkUseCase
import com.fancy.skill_tree.core.domain.usecase.node.CreateLinkUseCase
import com.fancy.skill_tree.core.domain.usecase.node.CreateNodeUseCase
import com.fancy.skill_tree.core.domain.usecase.node.CreateTagUseCase
import com.fancy.skill_tree.core.domain.usecase.node.DeleteAttachmentUseCase
import com.fancy.skill_tree.core.domain.usecase.node.DeleteLinkUseCase
import com.fancy.skill_tree.core.domain.usecase.node.DeleteNodeUseCase
import com.fancy.skill_tree.core.domain.usecase.node.ExportToMarkdownUseCase
import com.fancy.skill_tree.core.domain.usecase.node.GetAllNodesUseCase
import com.fancy.skill_tree.core.domain.usecase.node.GetAllTagsUseCase
import com.fancy.skill_tree.core.domain.usecase.node.GetAttachmentsForNodeUseCase
import com.fancy.skill_tree.core.domain.usecase.node.GetChildNodesUseCase
import com.fancy.skill_tree.core.domain.usecase.node.GetLinksForNodeUseCase
import com.fancy.skill_tree.core.domain.usecase.node.GetNodeDetailUseCase
import com.fancy.skill_tree.core.domain.usecase.node.GetRootNodesUseCase
import com.fancy.skill_tree.core.domain.usecase.node.GetStatisticsUseCase
import com.fancy.skill_tree.core.domain.usecase.node.GetTagsForNodeUseCase
import com.fancy.skill_tree.core.domain.usecase.node.LoadSampleDataUseCase
import com.fancy.skill_tree.core.domain.usecase.node.MoveNodeUseCase
import com.fancy.skill_tree.core.domain.usecase.node.RemoveTagFromNodeUseCase
import com.fancy.skill_tree.core.domain.usecase.node.ToggleNodeExpandUseCase
import com.fancy.skill_tree.core.domain.usecase.node.UpdateNodeUseCase
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

    @Provides
    @Singleton
    fun provideCreateNodeUseCase(
        repository: com.fancy.skill_tree.core.data.repository.SkillTreeRepository
    ): CreateNodeUseCase = CreateNodeUseCase(repository)

    @Provides
    @Singleton
    fun provideUpdateNodeUseCase(
        repository: com.fancy.skill_tree.core.data.repository.SkillTreeRepository
    ): UpdateNodeUseCase = UpdateNodeUseCase(repository)

    @Provides
    @Singleton
    fun provideDeleteNodeUseCase(
        repository: com.fancy.skill_tree.core.data.repository.SkillTreeRepository
    ): DeleteNodeUseCase = DeleteNodeUseCase(repository)

    @Provides
    @Singleton
    fun provideMoveNodeUseCase(
        repository: com.fancy.skill_tree.core.data.repository.SkillTreeRepository
    ): MoveNodeUseCase = MoveNodeUseCase(repository)

    @Provides
    @Singleton
    fun provideGetNodeDetailUseCase(
        repository: com.fancy.skill_tree.core.data.repository.SkillTreeRepository
    ): GetNodeDetailUseCase = GetNodeDetailUseCase(repository)

    @Provides
    @Singleton
    fun provideGetRootNodesUseCase(
        repository: com.fancy.skill_tree.core.data.repository.SkillTreeRepository
    ): GetRootNodesUseCase = GetRootNodesUseCase(repository)

    @Provides
    @Singleton
    fun provideGetChildNodesUseCase(
        repository: com.fancy.skill_tree.core.data.repository.SkillTreeRepository
    ): GetChildNodesUseCase = GetChildNodesUseCase(repository)

    @Provides
    @Singleton
    fun provideGetAllNodesUseCase(
        repository: com.fancy.skill_tree.core.data.repository.SkillTreeRepository
    ): GetAllNodesUseCase = GetAllNodesUseCase(repository)

    @Provides
    @Singleton
    fun provideToggleNodeExpandUseCase(
        repository: com.fancy.skill_tree.core.data.repository.SkillTreeRepository
    ): ToggleNodeExpandUseCase = ToggleNodeExpandUseCase(repository)

    @Provides
    @Singleton
    fun provideExportToMarkdownUseCase(
        repository: com.fancy.skill_tree.core.data.repository.SkillTreeRepository
    ): ExportToMarkdownUseCase = ExportToMarkdownUseCase(repository)

    @Provides
    @Singleton
    fun provideGetStatisticsUseCase(
        repository: com.fancy.skill_tree.core.data.repository.SkillTreeRepository
    ): GetStatisticsUseCase = GetStatisticsUseCase(repository)

    @Provides
    @Singleton
    fun provideClearAllDataUseCase(
        repository: com.fancy.skill_tree.core.data.repository.SkillTreeRepository
    ): ClearAllDataUseCase = ClearAllDataUseCase(repository)

    @Provides
    @Singleton
    fun provideCreateTagUseCase(
        repository: com.fancy.skill_tree.core.data.repository.SkillTreeRepository
    ): CreateTagUseCase = CreateTagUseCase(repository)

    @Provides
    @Singleton
    fun provideGetAllTagsUseCase(
        repository: com.fancy.skill_tree.core.data.repository.SkillTreeRepository
    ): GetAllTagsUseCase = GetAllTagsUseCase(repository)

    @Provides
    @Singleton
    fun provideGetTagsForNodeUseCase(
        repository: com.fancy.skill_tree.core.data.repository.SkillTreeRepository
    ): GetTagsForNodeUseCase = GetTagsForNodeUseCase(repository)

    @Provides
    @Singleton
    fun provideAssignTagToNodeUseCase(
        repository: com.fancy.skill_tree.core.data.repository.SkillTreeRepository
    ): AssignTagToNodeUseCase = AssignTagToNodeUseCase(repository)

    @Provides
    @Singleton
    fun provideRemoveTagFromNodeUseCase(
        repository: com.fancy.skill_tree.core.data.repository.SkillTreeRepository
    ): RemoveTagFromNodeUseCase = RemoveTagFromNodeUseCase(repository)

    @Provides
    @Singleton
    fun provideCreateLinkUseCase(
        repository: com.fancy.skill_tree.core.data.repository.SkillTreeRepository
    ): CreateLinkUseCase = CreateLinkUseCase(repository)

    @Provides
    @Singleton
    fun provideDeleteLinkUseCase(
        repository: com.fancy.skill_tree.core.data.repository.SkillTreeRepository
    ): DeleteLinkUseCase = DeleteLinkUseCase(repository)

    @Provides
    @Singleton
    fun provideGetLinksForNodeUseCase(
        repository: com.fancy.skill_tree.core.data.repository.SkillTreeRepository
    ): GetLinksForNodeUseCase = GetLinksForNodeUseCase(repository)

    @Provides
    @Singleton
    fun provideConfirmLinkUseCase(
        repository: com.fancy.skill_tree.core.data.repository.SkillTreeRepository
    ): ConfirmLinkUseCase = ConfirmLinkUseCase(repository)

    @Provides
    @Singleton
    fun provideGetAttachmentsForNodeUseCase(
        repository: com.fancy.skill_tree.core.data.repository.SkillTreeRepository
    ): GetAttachmentsForNodeUseCase = GetAttachmentsForNodeUseCase(repository)

    @Provides
    @Singleton
    fun provideAddAttachmentUseCase(
        repository: com.fancy.skill_tree.core.data.repository.SkillTreeRepository,
        fileManager: com.fancy.skill_tree.core.data.file.AttachmentFileManager
    ): AddAttachmentUseCase = AddAttachmentUseCase(repository, fileManager)

    @Provides
    @Singleton
    fun provideDeleteAttachmentUseCase(
        repository: com.fancy.skill_tree.core.data.repository.SkillTreeRepository,
        fileManager: com.fancy.skill_tree.core.data.file.AttachmentFileManager
    ): DeleteAttachmentUseCase = DeleteAttachmentUseCase(repository, fileManager)

    @Provides
    @Singleton
    fun provideLoadSampleDataUseCase(
        repository: com.fancy.skill_tree.core.data.repository.SkillTreeRepository,
        createNodeUseCase: CreateNodeUseCase
    ): LoadSampleDataUseCase = LoadSampleDataUseCase(repository, createNodeUseCase)

    @Provides
    @Singleton
    fun provideCheckAchievementsUseCase(
        repository: com.fancy.skill_tree.core.data.repository.SkillTreeRepository,
        achievementManager: AchievementManager
    ): CheckAchievementsUseCase = CheckAchievementsUseCase(repository, achievementManager)

    @Provides
    @Singleton
    fun provideSearchNodesUseCase(
        repository: com.fancy.skill_tree.core.data.repository.SkillTreeRepository
    ): SearchNodesUseCase = SearchNodesUseCase(repository)
}
