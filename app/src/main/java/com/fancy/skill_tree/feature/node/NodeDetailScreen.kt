package com.fancy.skill_tree.feature.node

import android.net.Uri
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.fancy.skill_tree.R
import com.fancy.skill_tree.core.ui.accessibility.accessibilityButton
import com.fancy.skill_tree.core.ui.accessibility.accessibilityLabel
import com.fancy.skill_tree.core.ui.components.AddAttachmentDialog
import com.fancy.skill_tree.core.ui.components.AddLinkDialog
import com.fancy.skill_tree.core.ui.components.AddTagDialog
import com.fancy.skill_tree.core.ui.components.AttachmentsSection
import com.fancy.skill_tree.core.ui.components.CameraPermissionHandler
import com.fancy.skill_tree.core.ui.components.CameraPermissionState
import com.fancy.skill_tree.core.ui.components.ImageViewerDialog
import com.fancy.skill_tree.core.ui.components.LinkedNodesSection
import com.fancy.skill_tree.core.ui.components.NodeTagsSection
import com.fancy.skill_tree.ui.theme.LocalThemeColors
import com.fancy.skill_tree.ui.theme.ThemeColors
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin

private const val NODE_TYPE_ABILITY = "ABILITY"
private const val NODE_TYPE_RESOURCE = "RESOURCE"

/**
 * 从 URI 获取文件信息
 */
private fun getFileInfo(context: android.content.Context, uri: Uri): Triple<String, String?, Long?> {
    var fileName = "file"
    var mimeType: String? = null
    var fileSize: Long? = null

    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                fileName = cursor.getString(nameIndex)
            }
            val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
            if (sizeIndex != -1) {
                fileSize = cursor.getLong(sizeIndex)
            }
        }
    }
    mimeType = context.contentResolver.getType(uri)
    return Triple(fileName, mimeType, fileSize)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNode: (String) -> Unit,
    viewModel: NodeDetailViewModel = hiltViewModel()
) {
    val colors = LocalThemeColors.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var showAddLinkDialog by remember { mutableStateOf(false) }
    var showAddAttachmentDialog by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf(false) }
    var currentViewingAttachmentPath by remember { mutableStateOf<String?>(null) }
    var showCameraScreen by remember { mutableStateOf(false) }
    var requestCameraPermission by remember { mutableStateOf(false) }

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val (fileName, mimeType, fileSize) = getFileInfo(context, it)
            viewModel.addAttachment(it, fileName, mimeType, fileSize)
        }
        showAddAttachmentDialog = false
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val (fileName, mimeType, fileSize) = getFileInfo(context, it)
            viewModel.addAttachment(it, fileName, mimeType, fileSize)
        }
        showAddAttachmentDialog = false
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.node_delete), color = colors.textPrimary) },
            text = { Text(stringResource(R.string.node_delete_confirm), color = colors.textSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteNode {
                            showDeleteDialog = false
                            onNavigateBack()
                        }
                    }
                ) {
                    Text(stringResource(R.string.node_delete), color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.common_cancel), color = colors.textSecondary)
                }
            },
            containerColor = colors.surface
        )
    }

    if (showAddTagDialog) {
        AddTagDialog(
            allTags = uiState.allTags,
            currentNodeTags = uiState.tags,
            onDismiss = { showAddTagDialog = false },
            onToggleTag = { tag ->
                viewModel.addTag(tag)
            },
            onCreateTag = { tagName ->
                viewModel.createAndAssignTag(tagName)
            }
        )
    }

    if (showAddLinkDialog) {
        AddLinkDialog(
            allNodes = uiState.allNodes,
            currentNodeId = uiState.node?.id ?: "",
            existingLinkedNodeIds = viewModel.getConfirmedLinks().map { it.targetNode.id }.toSet(),
            onDismiss = { showAddLinkDialog = false },
            onConfirm = { targetNodeIds ->
                targetNodeIds.forEach { targetId ->
                    viewModel.createLink(targetId)
                }
                showAddLinkDialog = false
            }
        )
    }

    if (showAddAttachmentDialog) {
        AddAttachmentDialog(
            onDismiss = { showAddAttachmentDialog = false },
            onTakePhoto = {
                showAddAttachmentDialog = false
                requestCameraPermission = true
            },
            onPickFromGallery = {
                pickImageLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            },
            onPickFile = {
                pickFileLauncher.launch("*/*")
            }
        )
    }

    if (showImageViewer) {
        val viewingPath = currentViewingAttachmentPath
        if (viewingPath != null) {
            ImageViewerDialog(
                imagePath = viewingPath,
                onDismiss = {
                    showImageViewer = false
                    currentViewingAttachmentPath = null
                }
            )
        }
    }

    if (requestCameraPermission) {
        CameraPermissionHandler(
            onPermissionResult = { state ->
                requestCameraPermission = false
                when (state) {
                    CameraPermissionState.GRANTED -> {
                        showCameraScreen = true
                    }
                    CameraPermissionState.DENIED,
                    CameraPermissionState.PERMANENTLY_DENIED -> { }
                }
            },
            content = { }
        )
    }

    if (showCameraScreen) {
        Dialog(
            onDismissRequest = { showCameraScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            CameraScreen(
                onPhotoCaptured = { uri ->
                    showCameraScreen = false
                    val (fileName, mimeType, fileSize) = getFileInfo(context, uri)
                    viewModel.addAttachment(uri, fileName, mimeType, fileSize)
                },
                onError = { _ ->
                    showCameraScreen = false
                },
                onDismiss = {
                    showCameraScreen = false
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.primary
                )
            }
            uiState.errorMessage != null -> {
                Text(
                    text = uiState.errorMessage ?: stringResource(R.string.common_unknown_error),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                val node = uiState.node
                if (node != null) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (node.nodeType == NODE_TYPE_ABILITY) "⚔️" else "💎",
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = node.title,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.node_back),
                                    tint = colors.textPrimary
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.toggleEditing() }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = if (uiState.isEditing) stringResource(R.string.node_view) else stringResource(R.string.node_edit),
                                    tint = if (uiState.isEditing) colors.primary else colors.textSecondary
                                )
                            }
                            Box {
                                IconButton(onClick = { showMoreMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = stringResource(R.string.node_more),
                                        tint = colors.textSecondary
                                    )
                                }
                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = stringResource(R.string.cd_delete_action),
                                                    tint = androidx.compose.material3.MaterialTheme.colorScheme.error
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    stringResource(R.string.node_delete),
                                                    color = androidx.compose.material3.MaterialTheme.colorScheme.error
                                                )
                                            }
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            showDeleteDialog = true
                                        }
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = colors.surface
                        )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        NodeTypeBadge(nodeType = node.nodeType, colors = colors)

                        Spacer(modifier = Modifier.height(16.dp))

                        NodeTagsSection(
                            tags = uiState.tags,
                            onAddTag = { showAddTagDialog = true },
                            onRemoveTag = { viewModel.removeTag(it) },
                            onTagClick = { }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        ContentSection(
                            content = node.content,
                            isEditing = uiState.isEditing,
                            onSave = { viewModel.saveContent(it) },
                            colors = colors
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        LinkedNodesSection(
                            confirmedLinks = viewModel.getConfirmedLinks(),
                            suggestedLinks = viewModel.getSuggestedLinks(),
                            onLinkClick = { nodeId ->
                                onNavigateToNode(nodeId)
                            },
                            onRemoveLink = { linkId ->
                                viewModel.deleteLink(linkId)
                            },
                            onConfirmLink = { linkId ->
                                viewModel.confirmLink(linkId)
                            },
                            onIgnoreLink = { linkId ->
                                viewModel.deleteLink(linkId)
                            },
                            onAddLink = {
                                showAddLinkDialog = true
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        AttachmentsSection(
                            attachments = uiState.attachments,
                            onAddAttachment = {
                                showAddAttachmentDialog = true
                            },
                            onDeleteAttachment = { attachment ->
                                viewModel.deleteAttachment(attachment.id)
                            },
                            onOpenAttachment = { attachment ->
                                if (attachment.mimeType?.startsWith("image/") == true) {
                                    currentViewingAttachmentPath = attachment.filePath
                                    showImageViewer = true
                                }
                            }
                        )
                    }
                }
                }
            }
        }
    }
}

/**
 * 节点类型标签
 */
@Composable
private fun NodeTypeBadge(nodeType: String, colors: ThemeColors) {
    val (label, color) = when (nodeType) {
        NODE_TYPE_ABILITY -> stringResource(R.string.node_type_ability) to colors.ability
        NODE_TYPE_RESOURCE -> stringResource(R.string.node_type_resource) to colors.resource
        else -> stringResource(R.string.node_type_unknown) to colors.textSecondary
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .accessibilityLabel(label)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 内容编辑/展示区域
 */
@Composable
private fun ContentSection(
    content: String?,
    isEditing: Boolean,
    onSave: (String) -> Unit,
    colors: ThemeColors
) {
    var editContent by remember(content) { mutableStateOf(content ?: "") }

    Text(
        text = stringResource(R.string.node_content_label),
        color = colors.textSecondary,
        fontSize = 14.sp
    )

    Spacer(modifier = Modifier.height(8.dp))

    if (isEditing) {
        OutlinedTextField(
            value = editContent,
            onValueChange = { editContent = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.textSecondary,
                cursorColor = colors.primary,
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface
            ),
            placeholder = {
                Text(stringResource(R.string.node_content_placeholder), color = colors.textSecondary)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { onSave(editContent) }
            ) {
                Text(stringResource(R.string.node_save), color = colors.primary)
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .padding(16.dp)
                .accessibilityLabel(stringResource(R.string.a11y_node_content))
        ) {
            if (content.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.node_content_empty),
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
            } else {
                MarkdownText(markdown = content)
            }
        }
    }
}

/**
 * Markdown 渲染组件
 */
@Composable
private fun MarkdownText(markdown: String) {
    val context = LocalContext.current
    val markwon = remember {
        Markwon.builder(context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(TaskListPlugin.create(context))
            .usePlugin(HtmlPlugin.create())
            .build()
    }

    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                setTextColor(android.graphics.Color.parseColor("#E6EDF3"))
                textSize = 15f
                setLineSpacing(6f, 1f)
            }
        },
        update = { textView ->
            markwon.setMarkdown(textView, markdown)
        },
        modifier = Modifier.accessibilityLabel(stringResource(R.string.a11y_markdown_content))
    )
}
