package com.fancy.skill_tree.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fancy.skill_tree.R
import com.fancy.skill_tree.core.data.preferences.LocaleManager
import com.fancy.skill_tree.core.data.preferences.UserPreferences
import com.fancy.skill_tree.core.ui.accessibility.accessibilityButton
import com.fancy.skill_tree.core.ui.accessibility.accessibilityLabel
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.fancy.skill_tree.ui.theme.LocalThemeColors
import com.fancy.skill_tree.ui.theme.ThemeMode
import com.fancy.skill_tree.ui.theme.ThemeColors
import android.app.Activity
import javax.inject.Inject

/**
 * 设置页 Screen
 *
 * @param onThemeChanged 主题模式变更回调
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    onThemeChanged: (ThemeMode) -> Unit = {},
    localeManager: LocaleManager? = null
) {
    val colors = LocalThemeColors.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
            )

            SettingsSection(title = "🎨 ${stringResource(R.string.settings_appearance)}", colors = colors) {
                SettingsItem(
                    icon = "🌙",
                    title = stringResource(R.string.settings_theme),
                    subtitle = stringResource(R.string.settings_theme_desc),
                    onClick = { showThemeDialog = true },
                    colors = colors
                )
                SettingsItem(
                    icon = "🌐",
                    title = stringResource(R.string.settings_language),
                    subtitle = stringResource(R.string.settings_language_desc),
                    onClick = { showLanguageDialog = true },
                    colors = colors
                )
            }

            SettingsSection(title = "📁 ${stringResource(R.string.settings_data_management)}", colors = colors) {
                SettingsItem(
                    icon = "📤",
                    title = stringResource(R.string.settings_export_markdown),
                    subtitle = stringResource(R.string.settings_export_desc),
                    onClick = {
                        viewModel.exportMarkdown { _ -> }
                    },
                    colors = colors
                )
                SettingsItem(
                    icon = "📋",
                    title = stringResource(R.string.settings_load_sample),
                    subtitle = stringResource(R.string.settings_load_sample_desc),
                    onClick = { viewModel.loadSampleData() },
                    colors = colors
                )
                SettingsItem(
                    icon = "🗑️",
                    title = stringResource(R.string.settings_clear_data),
                    subtitle = stringResource(R.string.settings_clear_data_desc),
                    textColor = MaterialTheme.colorScheme.error,
                    onClick = { showClearDataDialog = true },
                    colors = colors,
                    accessibilityHint = stringResource(R.string.settings_danger_zone)
                )
            }

            SettingsSection(title = "🏷️ ${stringResource(R.string.settings_content_management)}", colors = colors) {
                SettingsItem(
                    icon = "🏷️",
                    title = stringResource(R.string.settings_tag_management),
                    subtitle = stringResource(R.string.settings_tag_management_desc),
                    onClick = { },
                    colors = colors
                )
                SettingsItem(
                    icon = "🔗",
                    title = stringResource(R.string.settings_link_management),
                    subtitle = stringResource(R.string.settings_link_management_desc),
                    onClick = { },
                    colors = colors
                )
            }

            SettingsSection(title = "ℹ️ ${stringResource(R.string.settings_about)}", colors = colors) {
                SettingsItem(
                    icon = "📖",
                    title = stringResource(R.string.settings_restart_onboarding),
                    subtitle = stringResource(R.string.settings_restart_onboarding_desc),
                    onClick = { },
                    colors = colors
                )
                SettingsItem(
                    icon = "ℹ️",
                    title = stringResource(R.string.settings_about_app),
                    subtitle = stringResource(R.string.settings_version),
                    showArrow = false,
                    onClick = null,
                    colors = colors
                )
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        uiState.errorMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text(stringResource(R.string.common_error), color = colors.textPrimary) },
                text = { Text(message, color = colors.textSecondary) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text(stringResource(R.string.common_ok), color = MaterialTheme.colorScheme.primary)
                    }
                },
                containerColor = colors.surface
            )
        }

        if (showClearDataDialog) {
            AlertDialog(
                onDismissRequest = { showClearDataDialog = false },
                title = { Text(stringResource(R.string.settings_clear_data), color = colors.textPrimary) },
                text = { Text(stringResource(R.string.settings_clear_confirm), color = colors.textSecondary) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.clearAllData()
                        showClearDataDialog = false
                    }) {
                        Text(stringResource(R.string.settings_clear_confirm_btn), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDataDialog = false }) {
                        Text(stringResource(R.string.common_cancel), color = colors.textSecondary)
                    }
                },
                containerColor = colors.surface
            )
        }

        if (showThemeDialog) {
            ThemeSelectionDialog(
                currentMode = viewModel.userPreferences.themeMode,
                onDismiss = { showThemeDialog = false },
                onModeSelected = { mode ->
                    viewModel.userPreferences.themeMode = mode
                    onThemeChanged(mode)
                    showThemeDialog = false
                },
                colors = colors
            )
        }

        if (showLanguageDialog) {
            LanguageSelectionDialog(
                currentTag = localeManager?.languageTag,
                onDismiss = { showLanguageDialog = false },
                onLanguageSelected = { tag ->
                    localeManager?.languageTag = tag
                    (context as? Activity)?.recreate()
                    showLanguageDialog = false
                },
                colors = colors
            )
        }
    }
}

/**
 * 主题选择对话框
 */
@Composable
private fun ThemeSelectionDialog(
    currentMode: ThemeMode,
    onDismiss: () -> Unit,
    onModeSelected: (ThemeMode) -> Unit,
    colors: ThemeColors
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_theme), color = colors.textPrimary) },
        text = {
            Column {
                ThemeOption(
                    label = stringResource(R.string.settings_theme_dark),
                    selected = currentMode == ThemeMode.DARK,
                    onClick = { onModeSelected(ThemeMode.DARK) },
                    colors = colors
                )
                ThemeOption(
                    label = stringResource(R.string.settings_theme_light),
                    selected = currentMode == ThemeMode.LIGHT,
                    onClick = { onModeSelected(ThemeMode.LIGHT) },
                    colors = colors
                )
                ThemeOption(
                    label = stringResource(R.string.settings_theme_system),
                    selected = currentMode == ThemeMode.SYSTEM,
                    onClick = { onModeSelected(ThemeMode.SYSTEM) },
                    colors = colors
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close), color = colors.textSecondary)
            }
        },
        containerColor = colors.surface
    )
}

/**
 * 语言选择对话框
 *
 * @param currentTag 当前语言标签，null 表示跟随系统
 * @param onDismiss 关闭回调
 * @param onLanguageSelected 语言选择回调
 * @param colors 主题颜色
 */
@Composable
private fun LanguageSelectionDialog(
    currentTag: String?,
    onDismiss: () -> Unit,
    onLanguageSelected: (String?) -> Unit,
    colors: ThemeColors
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_language), color = colors.textPrimary) },
        text = {
            Column {
                ThemeOption(
                    label = stringResource(R.string.settings_language_zh),
                    selected = currentTag == LocaleManager.LANG_ZH,
                    onClick = { onLanguageSelected(LocaleManager.LANG_ZH) },
                    colors = colors
                )
                ThemeOption(
                    label = "English",
                    selected = currentTag == LocaleManager.LANG_EN,
                    onClick = { onLanguageSelected(LocaleManager.LANG_EN) },
                    colors = colors
                )
                ThemeOption(
                    label = stringResource(R.string.settings_language_system),
                    selected = currentTag == null,
                    onClick = { onLanguageSelected(LocaleManager.LANG_SYSTEM) },
                    colors = colors
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close), color = colors.textSecondary)
            }
        },
        containerColor = colors.surface
    )
}

/**
 * 主题选项行
 */
@Composable
private fun ThemeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    colors: ThemeColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { }
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = colors.textPrimary,
            fontSize = 15.sp
        )
    }
}

/**
 * 设置分组
 */
@Composable
private fun SettingsSection(
    title: String,
    colors: ThemeColors,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            text = title,
            color = colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
        ) {
            content()
        }
    }
}

/**
 * 设置项
 */
@Composable
private fun SettingsItem(
    icon: String,
    title: String,
    subtitle: String? = null,
    showArrow: Boolean = true,
    textColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    onClick: (() -> Unit)? = null,
    colors: ThemeColors,
    accessibilityHint: String? = null
) {
    val resolvedTextColor = if (textColor != androidx.compose.ui.graphics.Color.Unspecified) textColor else colors.textPrimary
    val clickModifier = if (onClick != null) {
        Modifier
            .semantics(mergeDescendants = true) {
                if (accessibilityHint != null) {
                    stateDescription = accessibilityHint
                }
            }
            .clickable(onClick = onClick)
    } else Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickModifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = resolvedTextColor,
                fontSize = 15.sp
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = colors.textSecondary,
                    fontSize = 13.sp
                )
            }
        }

        if (showArrow && onClick != null) {
            Text(
                text = ">",
                color = colors.textSecondary,
                fontSize = 16.sp
            )
        }
    }
}
