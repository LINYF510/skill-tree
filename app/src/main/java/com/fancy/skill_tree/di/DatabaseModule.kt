package com.fancy.skill_tree.di

import android.content.Context
import androidx.room.Room
import com.fancy.skill_tree.core.data.database.AppDatabase
import com.fancy.skill_tree.core.data.database.dao.AttachmentDao
import com.fancy.skill_tree.core.data.database.dao.NodeLinkDao
import com.fancy.skill_tree.core.data.database.dao.NodeTagDao
import com.fancy.skill_tree.core.data.database.dao.SkillNodeDao
import com.fancy.skill_tree.core.data.database.dao.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt 应用级别依赖注入模块
 * 提供数据库和 DAO 的单例实例
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideSkillNodeDao(database: AppDatabase): SkillNodeDao {
        return database.skillNodeDao()
    }

    @Provides
    @Singleton
    fun provideNodeLinkDao(database: AppDatabase): NodeLinkDao {
        return database.nodeLinkDao()
    }

    @Provides
    @Singleton
    fun provideTagDao(database: AppDatabase): TagDao {
        return database.tagDao()
    }

    @Provides
    @Singleton
    fun provideNodeTagDao(database: AppDatabase): NodeTagDao {
        return database.nodeTagDao()
    }

    @Provides
    @Singleton
    fun provideAttachmentDao(database: AppDatabase): AttachmentDao {
        return database.attachmentDao()
    }
}
