package com.fancy.skill_tree.feature.statistics

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fancy.skill_tree.R
import com.fancy.skill_tree.core.domain.model.Achievement
import com.fancy.skill_tree.core.ui.accessibility.accessibilityLabel
import com.fancy.skill_tree.ui.theme.LocalThemeColors
import com.fancy.skill_tree.ui.theme.ThemeColors

/**
 * 统计面板 Screen
 */
@Composable
fun StatisticsScreen(
    modifier: Modifier = Modifier,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val colors = LocalThemeColors.current
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            uiState.errorMessage != null -> {
                Text(
                    text = uiState.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            else -> {
                StatisticsContent(
                    statistics = uiState.statistics,
                    achievements = uiState.achievements,
                    unlockedAchievements = uiState.unlockedAchievements,
                    isUnlocked = viewModel::isAchievementUnlocked,
                    colors = colors
                )
            }
        }
    }
}

/**
 * 统计内容
 */
@Composable
private fun StatisticsContent(
    statistics: com.fancy.skill_tree.core.domain.usecase.node.GetStatisticsUseCase.Statistics?,
    achievements: List<Achievement>,
    unlockedAchievements: List<Achievement>,
    isUnlocked: (Achievement) -> Boolean,
    colors: ThemeColors
) {
    val stats = statistics ?: return

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.stats_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatCard(
                    title = stringResource(R.string.stats_total_nodes),
                    value = stats.totalNodes.toString(),
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                    colors = colors
                )
                StatCard(
                    title = stringResource(R.string.stats_max_depth),
                    value = stats.maxDepth.toString(),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    colors = colors
                )
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatCard(
                    title = stringResource(R.string.stats_ability_nodes),
                    value = stats.abilityNodes.toString(),
                    color = colors.ability,
                    modifier = Modifier.weight(1f),
                    colors = colors
                )
                StatCard(
                    title = stringResource(R.string.stats_resource_nodes),
                    value = stats.resourceNodes.toString(),
                    color = colors.resource,
                    modifier = Modifier.weight(1f),
                    colors = colors
                )
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatCard(
                    title = stringResource(R.string.stats_unlocked_achievements),
                    value = "${unlockedAchievements.size}/${achievements.size}",
                    color = Color(0xFFFFD700),
                    modifier = Modifier.weight(1f),
                    colors = colors
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            ProgressSection(
                title = stringResource(R.string.stats_node_distribution),
                abilityCount = stats.abilityNodes,
                resourceCount = stats.resourceNodes,
                totalCount = stats.totalNodes,
                colors = colors
            )
        }

        item {
            AchievementsSection(
                achievements = achievements,
                isUnlocked = isUnlocked,
                colors = colors
            )
        }
    }
}

/**
 * 成就展示区域
 */
@Composable
private fun AchievementsSection(
    achievements: List<Achievement>,
    isUnlocked: (Achievement) -> Boolean,
    colors: ThemeColors
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, shape = RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "🏆 ${stringResource(R.string.stats_achievements)}",
            color = colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            achievements.forEach { achievement ->
                AchievementItem(
                    achievement = achievement,
                    isUnlocked = isUnlocked(achievement),
                    colors = colors
                )
            }
        }
    }
}

/**
 * 单个成就项
 */
@Composable
private fun AchievementItem(
    achievement: Achievement,
    isUnlocked: Boolean,
    colors: ThemeColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .accessibilityLabel("${stringResource(achievement.titleResId)}: ${stringResource(achievement.descriptionResId)}${if (isUnlocked) "，${stringResource(R.string.a11y_unlocked)}" else "，${stringResource(R.string.a11y_locked)}"}")
            .background(
                if (isUnlocked) Color(0x20FFD700) else Color(0x10808080),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isUnlocked) achievement.emoji else "🔒",
            fontSize = 24.sp,
            modifier = Modifier.padding(end = 12.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(achievement.titleResId),
                color = if (isUnlocked) colors.textPrimary else colors.textSecondary,
                fontSize = 14.sp,
                fontWeight = if (isUnlocked) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                text = stringResource(achievement.descriptionResId),
                color = colors.textSecondary,
                fontSize = 12.sp,
                maxLines = 1
            )
        }

        if (isUnlocked) {
            Text(
                text = "✅",
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

/**
 * 统计卡片
 */
@Composable
private fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    colors: ThemeColors
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .accessibilityLabel("$title: $value")
            .background(colors.surface, shape = RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = title,
                color = colors.textSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = color,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * 进度条展示
 */
@Composable
private fun ProgressSection(
    title: String,
    abilityCount: Int,
    resourceCount: Int,
    totalCount: Int,
    colors: ThemeColors
) {
    if (totalCount <= 0) {
        Text(
            text = stringResource(R.string.stats_no_data),
            color = colors.textSecondary,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .accessibilityLabel("$title: ${stringResource(R.string.stats_ability_label)} $abilityCount, ${stringResource(R.string.stats_resource_label)} $resourceCount, ${stringResource(R.string.stats_total_label)} $totalCount")
            .background(colors.surface, shape = RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val abilityRatio = abilityCount.toFloat() / totalCount
            val resourceRatio = resourceCount.toFloat() / totalCount

            if (abilityCount > 0) {
                Box(
                    modifier = Modifier
                        .weight(abilityRatio)
                        .fillMaxSize()
                        .background(colors.ability, shape = RoundedCornerShape(8.dp))
                )
            }

            if (resourceCount > 0) {
                Box(
                    modifier = Modifier
                        .weight(resourceRatio)
                        .fillMaxSize()
                        .background(colors.resource, shape = RoundedCornerShape(8.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            LegendItem(
                color = colors.ability,
                label = stringResource(R.string.stats_ability_label),
                count = abilityCount,
                colors = colors
            )
            LegendItem(
                color = colors.resource,
                label = stringResource(R.string.stats_resource_label),
                count = resourceCount,
                colors = colors
            )
        }
    }
}

/**
 * 图例项
 */
@Composable
private fun LegendItem(
    color: Color,
    label: String,
    count: Int,
    colors: ThemeColors
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = RoundedCornerShape(4.dp))
        )
        Text(
            text = "$label ($count)",
            color = colors.textSecondary,
            fontSize = 12.sp
        )
    }
}
