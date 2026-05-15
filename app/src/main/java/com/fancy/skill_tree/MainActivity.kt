package com.fancy.skill_tree

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fancy.skill_tree.core.data.preferences.LocaleManager
import com.fancy.skill_tree.core.data.preferences.UserPreferences
import com.fancy.skill_tree.core.ui.animation.AnimationConfig
import com.fancy.skill_tree.core.ui.error.DialogEvent
import com.fancy.skill_tree.core.ui.error.ErrorStateManager
import com.fancy.skill_tree.feature.node.NodeDetailScreen
import com.fancy.skill_tree.feature.onboarding.OnboardingScreen
import com.fancy.skill_tree.feature.onboarding.OnboardingManager
import com.fancy.skill_tree.feature.statistics.StatisticsScreen
import com.fancy.skill_tree.feature.settings.SettingsScreen
import com.fancy.skill_tree.feature.tree.SkillTreeRoute
import com.fancy.skill_tree.feature.tree.SkillTreeScreen
import com.fancy.skill_tree.ui.theme.LocalThemeColors
import com.fancy.skill_tree.ui.theme.SkilltreeTheme
import com.fancy.skill_tree.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var onboardingManager: OnboardingManager

    @Inject
    lateinit var errorStateManager: ErrorStateManager

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var localeManager: LocaleManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var themeMode by remember { mutableStateOf(userPreferences.themeMode) }
            SkilltreeTheme(themeMode = themeMode) {
                SkilltreeApp(
                    onboardingManager = onboardingManager,
                    errorStateManager = errorStateManager,
                    onThemeChanged = { themeMode = it },
                    localeManager = localeManager
                )
            }
        }
    }
}

/**
 * 应用入口 Composable
 * @param onboardingManager 引导页管理器
 * @param errorStateManager 全局错误状态管理器
 */
@Composable
fun SkilltreeApp(
    onboardingManager: OnboardingManager,
    errorStateManager: ErrorStateManager,
    onThemeChanged: (ThemeMode) -> Unit = {},
    localeManager: LocaleManager? = null
) {
    var showOnboarding by remember { mutableStateOf(onboardingManager.shouldShowOnboarding) }

    if (showOnboarding) {
        OnboardingScreen(onComplete = { showOnboarding = false })
    } else {
        MainApp(errorStateManager = errorStateManager, onThemeChanged = onThemeChanged, localeManager = localeManager)
    }
}

/**
 * 主应用 Composable，包含导航和全局错误展示
 * @param errorStateManager 全局错误状态管理器
 */
@Composable
fun MainApp(
    errorStateManager: ErrorStateManager,
    onThemeChanged: (ThemeMode) -> Unit = {},
    localeManager: LocaleManager? = null
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    var dialogEvent by remember { mutableStateOf<DialogEvent?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        errorStateManager.snackbarEvents.collect { event ->
            val message = context.getString(event.messageResId, *event.messageArgs)
            val actionLabel = event.actionLabelResId?.let { context.getString(it) }
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = if (event.isError) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                event.action?.invoke()
            }
        }
    }

    LaunchedEffect(Unit) {
        errorStateManager.dialogEvents.collect { event ->
            dialogEvent = event
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            painterResource(it.icon),
                            contentDescription = stringResource(it.labelResId)
                        )
                    },
                    label = { Text(stringResource(it.labelResId)) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentDestination) {
                    AppDestinations.HOME -> {
                        NavHost(
                            navController = navController,
                            startDestination = SkillTreeRoute.Tree.route,
                            enterTransition = {
                                slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Start,
                                    animationSpec = tween(AnimationConfig.PAGE_TRANSITION_DURATION)
                                ) + fadeIn(
                                    animationSpec = tween(AnimationConfig.PAGE_TRANSITION_DURATION)
                                )
                            },
                            exitTransition = {
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Start,
                                    animationSpec = tween(AnimationConfig.PAGE_TRANSITION_DURATION)
                                ) + fadeOut(
                                    animationSpec = tween(AnimationConfig.PAGE_TRANSITION_DURATION)
                                )
                            },
                            popEnterTransition = {
                                slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.End,
                                    animationSpec = tween(AnimationConfig.PAGE_TRANSITION_DURATION)
                                ) + fadeIn(
                                    animationSpec = tween(AnimationConfig.PAGE_TRANSITION_DURATION)
                                )
                            },
                            popExitTransition = {
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.End,
                                    animationSpec = tween(AnimationConfig.PAGE_TRANSITION_DURATION)
                                ) + fadeOut(
                                    animationSpec = tween(AnimationConfig.PAGE_TRANSITION_DURATION)
                                )
                            }
                        ) {
                            composable(SkillTreeRoute.Tree.route) {
                                SkillTreeScreen(
                                    onNodeClick = { nodeId ->
                                        navController.navigate(SkillTreeRoute.NodeDetail.createRoute(nodeId))
                                    }
                                )
                            }

                            composable(
                                route = SkillTreeRoute.NodeDetail.route,
                                arguments = listOf(
                                    navArgument("nodeId") { type = NavType.StringType }
                                )
                            ) {
                                NodeDetailScreen(
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToNode = { nodeId ->
                                        navController.navigate(SkillTreeRoute.NodeDetail.createRoute(nodeId))
                                    }
                                )
                            }
                        }
                    }

                    AppDestinations.STATISTICS -> {
                        StatisticsScreen()
                    }

                    AppDestinations.SETTINGS -> {
                        SettingsScreen(onThemeChanged = onThemeChanged, localeManager = localeManager)
                    }
                }
            }
        }
    }

    dialogEvent?.let { event ->
        val colors = LocalThemeColors.current
        AlertDialog(
            onDismissRequest = { dialogEvent = null },
            title = { Text(context.getString(event.titleResId), color = colors.textPrimary) },
            text = { Text(context.getString(event.messageResId, *event.messageArgs), color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    event.onConfirm?.invoke()
                    dialogEvent = null
                }) {
                    Text(context.getString(event.confirmLabelResId))
                }
            },
            containerColor = colors.surface
        )
    }
}

enum class AppDestinations(
    @StringRes val labelResId: Int,
    val icon: Int,
) {
    HOME(R.string.nav_skill_tree, R.drawable.ic_home),
    STATISTICS(R.string.nav_statistics, R.drawable.ic_statistics),
    SETTINGS(R.string.nav_settings, R.drawable.ic_settings),
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SkilltreeTheme {
        SkillTreeScreen(
            onNodeClick = {}
        )
    }
}
