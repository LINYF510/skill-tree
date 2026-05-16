# 提示词：CameraX 拍照功能

## 背景

项目附件系统已完整实现（AttachmentEntity/AttachmentDao/AttachmentFileManager/AddAttachmentUseCase/DeleteAttachmentUseCase），AddAttachmentDialog UI 中已有"拍照"选项入口，但 **onTakePhoto 回调是空壳**——仅关闭对话框，没有任何实际相机调用。

### 现状

- **AndroidManifest.xml** 已声明 `CAMERA` 权限，但代码中未使用
- **AddAttachmentDialog** 有 `onTakePhoto` 回调和 UI 选项（📷 "Take Photo"）
- **NodeDetailScreen** 中 `onTakePhoto` 仅执行 `showAddAttachmentDialog = false`
- **没有 CameraX 依赖**、没有 FileProvider 配置、没有运行时权限请求

## 目标

实现完整的拍照功能：用户点击"拍照"→ 请求相机权限 → 打开相机预览 → 拍照 → 保存到附件。

## 实施步骤

### Part A: 添加 CameraX 依赖

**文件**: `app/build.gradle`

在 dependencies 块中添加：
```groovy
def camerax_version = "1.3.4"
implementation "androidx.camera:camera-core:$camerax_version"
implementation "androidx.camera:camera-camera2:$camerax_version"
implementation "androidx.camera:camera-lifecycle:$camerax_version"
implementation "androidx.camera:camera-view:$camerax_version"
```

### Part B: 配置 FileProvider

**文件**: `app/src/main/AndroidManifest.xml`

在 `<application>` 标签内添加 FileProvider 声明：
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

**新建文件**: `app/src/main/res/xml/file_paths.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <files-path name="attachments" path="attachments/" />
    <cache-path name="cache" path="camera/" />
</paths>
```

### Part C: 新增字符串资源

**文件**: `app/src/main/res/values/strings.xml`
```xml
<string name="camera_permission_title">Camera Access</string>
<string name="camera_permission_message">Camera access is required to take photos for attachments.</string>
<string name="camera_permission_denied">Camera permission denied. You can enable it in Settings.</string>
<string name="camera_capture_button">Capture</string>
<string name="camera_switch_button">Switch Camera</string>
<string name="camera_error">Camera initialization failed</string>
```

**文件**: `app/src/main/res/values-zh/strings.xml`
```xml
<string name="camera_permission_title">相机权限</string>
<string name="camera_permission_message">需要相机权限才能拍照添加附件。</string>
<string name="camera_permission_denied">相机权限被拒绝，可在设置中开启。</string>
<string name="camera_capture_button">拍照</string>
<string name="camera_switch_button">切换镜头</string>
<string name="camera_error">相机初始化失败</string>
```

### Part D: 相机权限请求组件

**新建文件**: `app/src/main/java/com/fancy/skill_tree/core/ui/components/CameraPermissionHandler.kt`

```kotlin
/**
 * 相机权限请求处理组件
 * 处理运行时权限请求和"不再询问"后的引导
 */
@Composable
fun CameraPermissionHandler(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onPermissionGranted() else onPermissionDenied()
    }

    val permissionStatus = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
    }

    LaunchedEffect(permissionStatus) {
        when {
            permissionStatus == PackageManager.PERMISSION_GRANTED -> onPermissionGranted()
            ActivityCompat.shouldShowRequestPermissionRationale(
                context as Activity, Manifest.permission.CAMERA
            ) -> {
                // 显示解释对话框后请求权限
                // 使用 AlertDialog 提示用户
            }
            else -> launcher.launch(Manifest.permission.CAMERA)
        }
    }

    content()
}
```

### Part E: 相机预览 Screen

**新建文件**: `app/src/main/java/com/fancy/skill_tree/feature/node/CameraScreen.kt`

```kotlin
/**
 * 相机预览界面
 * 使用 CameraX 实现拍照功能
 */
@Composable
fun CameraScreen(
    onPhotoCaptured: (Uri) -> Unit,
    onError: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 1. 获取 ProcessCameraProvider
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // 2. 预览 UseCase
    val preview = remember { Preview.Builder().build() }
    val previewView = remember { PreviewView(context) }

    // 3. ImageCapture UseCase
    val imageCapture = remember { ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
        .build() }

    // 4. 相机选择器（默认后置）
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }

    // 5. 绑定生命周期
    DisposableEffect(lifecycleOwner) {
        val cameraProvider = cameraProviderFuture.get()
        preview.setSurfaceProvider(previewView.surfaceProvider)
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
        } catch (e: Exception) {
            onError(e.message ?: "Camera initialization failed")
        }
        onDispose { cameraProvider.unbindAll() }
    }

    // 6. UI 布局
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // 底部控制栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 关闭按钮
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, ...) }
            // 拍照按钮
            FAB(onClick = { capturePhoto(imageCapture, context, onPhotoCaptured, onError) }) { ... }
            // 切换前后摄像头
            IconButton(onClick = {
                cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                    CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            }) { Icon(Icons.Default.Cameraswitch, ...) }
        }
    }
}

/**
 * 执行拍照并保存到临时文件
 */
private fun capturePhoto(
    imageCapture: ImageCapture,
    context: Context,
    onPhotoCaptured: (Uri) -> Unit,
    onError: (String) -> Unit
) {
    val photoFile = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                onPhotoCaptured(uri)
            }
            override fun onError(exception: ImageCaptureException) {
                onError(exception.message ?: "Capture failed")
            }
        }
    )
}
```

### Part F: NodeDetailScreen 集成

**文件**: `app/src/main/java/com/fancy/skill_tree/feature/node/NodeDetailScreen.kt`

1. 新增状态：
```kotlin
var showCameraScreen by remember { mutableStateOf(false) }
```

2. 修改 `onTakePhoto` 回调：
```kotlin
onTakePhoto = {
    showAddAttachmentDialog = false
    showCameraScreen = true
}
```

3. 添加 CameraScreen 显示逻辑：
```kotlin
if (showCameraScreen) {
    CameraScreen(
        onPhotoCaptured = { uri ->
            showCameraScreen = false
            val fileInfo = getFileInfo(context, uri)
            if (fileInfo != null) {
                viewModel.addAttachment(uri, fileInfo.fileName, fileInfo.mimeType, fileInfo.fileSize)
            }
        },
        onError = { message ->
            showCameraScreen = false
            viewModel.showError(message)
        },
        onDismiss = {
            showCameraScreen = false
        }
    )
}
```

4. CameraScreen 使用全屏 Dialog 或独立的 Activity 方式展示。推荐使用 `Dialog` 全屏模式：
```kotlin
Dialog(
    onDismissRequest = { showCameraScreen = false },
    properties = DialogProperties(usePlatformDefaultWidth = false)
) {
    CameraScreen(...)
}
```

### Part G: Hilt 注入（可选优化）

如果后续需要将 CameraX 相关逻辑抽取为可注入的 Manager，可新建：
- `app/src/main/java/com/fancy/skill_tree/core/data/camera/CameraManager.kt`

但当前阶段直接在 Composable 中使用 CameraX API 即可，无需过度抽象。

## 约束

1. **仅引入 CameraX 依赖**——`camera-core`、`camera-camera2`、`camera-lifecycle`、`camera-view`，不引入其他第三方库
2. **CameraX 版本使用 1.3.4**——与项目 compileSdk 35 兼容
3. **运行时权限必须处理**——CAMERA 是危险权限，必须请求运行时权限
4. **"不再询问"后引导用户到设置**——使用 `shouldShowRequestPermissionRationale` 判断
5. **FileProvider 必须配置**——拍照后通过 FileProvider 共享 URI 给 AttachmentFileManager
6. **拍照后清理临时文件**——在 `onPhotoCaptured` 回调中，文件复制到内部存储后删除 cache 中的临时文件
7. **所有 public 函数必须有 KDoc 注释**
8. **禁止使用 !! 非空断言**
9. **新增字符串资源必须同时添加英文和中文版本**
10. **相机预览生命周期管理**——进入后台时解绑，回到前台时重新绑定

## 测试要求

1. `CameraPermissionHandlerTest`：测试权限授予/拒绝/不再询问三种场景
2. `CameraScreen` 的 Compose UI Test：验证拍照按钮、切换按钮存在
3. 集成测试：拍照 → 保存 → 附件列表中出现新附件

注意：相机相关测试可能需要 Instrumentation Test（androidTest），在真机或模拟器上运行。

## 验证

1. `./gradlew assembleDebug` 编译通过
2. `./gradlew test` 全部单元测试通过
3. 真机/模拟器上：NodeDetail → Add Attachment → Take Photo → 相机预览 → 拍照 → 附件列表中出现照片
4. 拒绝权限后：显示提示，不崩溃
5. 前后摄像头切换正常
6. 拍照后退出相机，临时文件已清理
