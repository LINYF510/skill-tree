# 附件管理系统实现方案

> **关联 PRD**: 第 5.1.2 节（attachment 表）、第 2.3 节（F2.1 图片插入）
> **当前状态**: 数据库层完整（Entity + DAO），UI 为零
> **目标**: 实现附件的添加、展示和管理功能

---

## 1. 现状分析

| 层级 | 状态 |
|------|------|
| Entity (`AttachmentEntity`) | ✅ 已定义 |
| DAO (`AttachmentDao`) | ✅ 已实现 |
| Repository 接口 | ❌ 未暴露附件方法 |
| UseCase | ❌ 不存在 |
| UI | ❌ 不存在 |

---

## 2. 数据层补充

### 2.1 Repository 接口扩展

```kotlin
interface SkillTreeRepository {
    fun getAttachmentsForNode(nodeId: String): Flow<List<AttachmentEntity>>
    suspend fun addAttachment(nodeId: String, fileName: String, filePath: String, mimeType: String?, fileSize: Long?): AttachmentEntity
    suspend fun deleteAttachment(attachmentId: String)
    fun getAttachmentCountForNode(nodeId: String): Flow<Int>
}
```

### 2.2 文件存储管理

```kotlin
package com.fancy.skill_tree.core.data.file

import android.content.Context
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 附件文件管理器
 * 负责文件的存储、复制和删除
 */
@Singleton
class AttachmentFileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val attachmentsDir: File
        get() = File(context.filesDir, "attachments").also { if (!it.exists()) it.mkdirs() }

    /**
     * 将外部文件复制到应用内部存储
     * @param sourceUri 源文件的 content:// URI
     * @param originalFileName 原始文件名
     * @return 复制后的文件路径
     */
    fun copyToInternalStorage(sourceUri: android.net.Uri, originalFileName: String): String {
        val extension = originalFileName.substringAfterLast('.', "")
        val uniqueName = "${UUID.randomUUID()}.${extension}"
        val destFile = File(attachmentsDir, uniqueName)

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw java.io.IOException("无法打开文件: $sourceUri")

        return destFile.absolutePath
    }

    /**
     * 删除附件文件
     */
    fun deleteFile(filePath: String): Boolean {
        return File(filePath).delete()
    }

    /**
     * 获取附件目录总大小（字节）
     */
    fun getTotalSize(): Long {
        return attachmentsDir.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }
}
```

---

## 3. UI 设计

### 3.1 节点详情页中的附件区域

```
┌─────────────────────────────────────┐
│  ← 返回    ⚔️ Python    ✏️  ⋮      │
├─────────────────────────────────────┤
│  ...                                │
│                                     │
│  📎 附件 (3)                   ➕   │
│  ┌──────────────────────────────┐   │
│  │ 📷 screenshot.png            │   │
│  │    256KB · 2026-05-10   🗑️  │   │
│  └──────────────────────────────┘   │
│  ┌──────────────────────────────┐   │
│  │ 📄 notes.md                  │   │
│  │    12KB · 2026-05-08    🗑️  │   │
│  └──────────────────────────────┘   │
│  ┌──────────────────────────────┐   │
│  │ 🎤 recording.m4a             │   │
│  │    1.2MB · 2026-05-05   🗑️  │   │
│  └──────────────────────────────┘   │
└─────────────────────────────────────┘
```

### 3.2 附件添加方式

```
点击 ➕ 按钮
    │
    ├── 📷 拍照
    ├── 🖼️ 从相册选择
    ├── 📄 选择文件
    └── 取消
```

---

## 4. Composable 实现

### 4.1 附件列表组件

```kotlin
@Composable
fun AttachmentsSection(
    attachments: List<AttachmentEntity>,
    onAddAttachment: () -> Unit,
    onDeleteAttachment: (AttachmentEntity) -> Unit,
    onOpenAttachment: (AttachmentEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📎 附件 (${attachments.size})", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            IconButton(onClick = onAddAttachment) {
                Icon(Icons.Default.Add, contentDescription = "添加附件", tint = PrimaryBlue)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (attachments.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceDark).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("暂无附件", color = TextSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onAddAttachment) { Text("添加第一个附件", color = PrimaryBlue) }
                }
            }
        } else {
            attachments.forEach { attachment ->
                AttachmentCard(
                    attachment = attachment,
                    onClick = { onOpenAttachment(attachment) },
                    onDelete = { onDeleteAttachment(attachment) }
                )
            }
        }
    }
}

@Composable
private fun AttachmentCard(
    attachment: AttachmentEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        color = SurfaceDark,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 文件类型图标
            Text(getFileTypeEmoji(attachment.mimeType), fontSize = 24.sp)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(attachment.fileName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Row {
                    Text(formatFileSize(attachment.fileSize), color = TextSecondary, fontSize = 12.sp)
                    Text(" · ", color = TextSecondary, fontSize = 12.sp)
                    Text(formatDate(attachment.createdAt), color = TextSecondary, fontSize = 12.sp)
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除附件", tint = TextSecondary)
            }
        }
    }
}

private fun getFileTypeEmoji(mimeType: String?): String = when {
    mimeType == null -> "📄"
    mimeType.startsWith("image/") -> "📷"
    mimeType.startsWith("audio/") -> "🎤"
    mimeType.startsWith("video/") -> "🎬"
    mimeType.contains("pdf") -> "📕"
    else -> "📄"
}

private fun formatFileSize(bytes: Long?): String = when {
    bytes == null -> "未知大小"
    bytes < 1024 -> "${bytes}B"
    bytes < 1024 * 1024 -> "${bytes / 1024}KB"
    else -> "${"%.1f".format(bytes.toDouble() / 1024 / 1024)}MB"
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
```

### 4.2 附件添加弹窗

```kotlin
@Composable
fun AddAttachmentDialog(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
    onPickFile: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("添加附件", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                AttachmentOption("📷", "拍照", "使用相机拍摄照片", onClick = onTakePhoto)
                AttachmentOption("🖼️", "从相册选择", "从相册中选择图片", onClick = onPickFromGallery)
                AttachmentOption("📄", "选择文件", "从文件管理器选择任意文件", onClick = onPickFile)

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("取消", color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun AttachmentOption(emoji: String, title: String, description: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        color = BackgroundDark,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(description, color = TextSecondary, fontSize = 13.sp)
            }
        }
    }
}
```

### 4.3 图片查看器（简单版）

```kotlin
@Composable
fun ImageViewerDialog(imagePath: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)).clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = "附件图片",
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentScale = ContentScale.Fit
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    }
}
```

---

## 5. ViewModel 扩展

```kotlin
// NodeDetailViewModel 扩展
class NodeDetailViewModel @Inject constructor(
    // ... 现有依赖 ...
    private val addAttachmentUseCase: AddAttachmentUseCase,
    private val deleteAttachmentUseCase: DeleteAttachmentUseCase
) : ViewModel() {

    fun addAttachment(uri: android.net.Uri, fileName: String, mimeType: String?, fileSize: Long?) {
        viewModelScope.launch {
            addAttachmentUseCase(nodeId, uri, fileName, mimeType, fileSize)
        }
    }

    fun deleteAttachment(attachmentId: String) {
        viewModelScope.launch {
            deleteAttachmentUseCase(attachmentId)
        }
    }
}
```

---

## 6. 权限处理

```kotlin
// 在 AndroidManifest.xml 中添加权限声明
// <uses-permission android:name="android.permission.CAMERA" />
// <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
// <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />

// 在 Composable 中请求权限
@Composable
fun AttachmentSection(...) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    val onTakePhoto = {
        if (cameraPermissionState.status.isGranted) {
            // 打开相机
        } else {
            cameraPermissionState.launchPermissionRequest()
        }
    }
}
```

---

## 7. 实施步骤

| 步骤 | 内容 |
|------|------|
| 1 | 扩展 `SkillTreeRepository` 接口添加附件方法 |
| 2 | 创建 `AttachmentFileManager` 文件管理器 |
| 3 | 创建附件相关 UseCase |
| 4 | 创建 `AttachmentsSection` 附件列表组件 |
| 5 | 创建 `AttachmentCard` 附件卡片组件 |
| 6 | 创建 `AddAttachmentDialog` 附件添加弹窗 |
| 7 | 创建 `ImageViewerDialog` 图片查看器 |
| 8 | 在 `NodeDetailScreen` 中集成附件区域 |
| 9 | 添加 CAMERA 和存储权限声明 |
| 10 | 测试拍照、选图、文件选择流程 |