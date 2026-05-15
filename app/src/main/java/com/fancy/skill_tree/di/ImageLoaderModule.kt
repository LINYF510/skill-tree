package com.fancy.skill_tree.di

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt 图片加载模块
 * 配置 Coil 内存和磁盘缓存限制，避免大量图片导致 OOM
 */
@Module
@InstallIn(SingletonComponent::class)
object ImageLoaderModule {

    /**
     * 提供自定义 ImageLoader 实例
     * 限制内存缓存为可用内存的 15%，磁盘缓存为 50MB
     *
     * @param context 应用上下文
     * @return 配置好缓存的 ImageLoader 实例
     */
    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.15)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
