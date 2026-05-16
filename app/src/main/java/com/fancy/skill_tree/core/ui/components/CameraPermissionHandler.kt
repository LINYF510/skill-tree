package com.fancy.skill_tree.core.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fancy.skill_tree.R
import com.fancy.skill_tree.ui.theme.LocalThemeColors

/**
 * 相机权限请求状态
 */
enum class CameraPermissionState {
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED
}

/**
 * 相机权限请求处理组件
 * 处理运行时权限请求和"不再询问"后的引导
 * @param onPermissionResult 权限结果回调，参数为权限状态
 * @param content 权限请求期间显示的内容
 */
@Composable
fun CameraPermissionHandler(
    onPermissionResult: (CameraPermissionState) -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalThemeColors.current
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onPermissionResult(CameraPermissionState.GRANTED)
        } else {
            val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                context as android.app.Activity,
                Manifest.permission.CAMERA
            )
            if (!shouldShowRationale) {
                onPermissionResult(CameraPermissionState.PERMANENTLY_DENIED)
                showSettingsDialog = true
            } else {
                onPermissionResult(CameraPermissionState.DENIED)
            }
        }
    }

    LaunchedEffect(Unit) {
        val permissionStatus = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        )
        when {
            permissionStatus == PackageManager.PERMISSION_GRANTED -> {
                onPermissionResult(CameraPermissionState.GRANTED)
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                context as android.app.Activity,
                Manifest.permission.CAMERA
            ) -> {
                showRationaleDialog = true
            }
            else -> {
                launcher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = {
                showRationaleDialog = false
                onPermissionResult(CameraPermissionState.DENIED)
            },
            title = {
                Text(
                    text = stringResource(R.string.camera_rationale_title),
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.camera_rationale_message),
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRationaleDialog = false
                    launcher.launch(Manifest.permission.CAMERA)
                }) {
                    Text(stringResource(R.string.common_ok), color = colors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRationaleDialog = false
                    onPermissionResult(CameraPermissionState.DENIED)
                }) {
                    Text(stringResource(R.string.common_cancel), color = colors.textSecondary)
                }
            },
            containerColor = colors.surface
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = {
                showSettingsDialog = false
            },
            title = {
                Text(
                    text = stringResource(R.string.camera_permission_title),
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.camera_permission_denied),
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSettingsDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text(
                        text = stringResource(R.string.camera_go_to_settings),
                        color = colors.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSettingsDialog = false
                }) {
                    Text(stringResource(R.string.common_cancel), color = colors.textSecondary)
                }
            },
            containerColor = colors.surface
        )
    }

    content()
}
