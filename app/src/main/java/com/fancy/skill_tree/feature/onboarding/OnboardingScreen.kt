package com.fancy.skill_tree.feature.onboarding

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.fancy.skill_tree.R
import com.fancy.skill_tree.ui.theme.LocalThemeColors
import kotlinx.coroutines.launch

private const val NODE_TYPE_ABILITY = "ABILITY"
private const val NODE_TYPE_RESOURCE = "RESOURCE"

data class FeatureItem(val icon: String, val title: String, val description: String)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val colors = LocalThemeColors.current
    val currentStep by viewModel.currentStep.collectAsState()
    val keepSampleData by viewModel.keepSampleData.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentStep) {
        pagerState.animateScrollToPage(currentStep)
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> WelcomeStep(
                    onNext = { viewModel.nextStep() },
                    onSkip = {
                        viewModel.skip()
                        onComplete()
                    }
                )
                1 -> CreateFirstNodeStep(
                    onNext = { title, type ->
                        viewModel.createFirstNode(title, type)
                    },
                    onSkip = { viewModel.nextStep() },
                    onBack = { viewModel.prevStep() }
                )
                2 -> SampleTreeStep(
                    keepSampleData = keepSampleData,
                    onKeepSampleData = { viewModel.setKeepSampleData(it) },
                    onNext = { viewModel.nextStep() },
                    onSkip = { viewModel.nextStep() },
                    onBack = { viewModel.prevStep() }
                )
                3 -> FeatureOverviewStep(
                    onFinish = {
                        viewModel.finish(keepSampleData)
                        onComplete()
                    },
                    onBack = { viewModel.prevStep() }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (currentStep > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${currentStep + 1}/4",
                        color = colors.textSecondary,
                        fontSize = 14.sp
                    )
                    TextButton(onClick = { viewModel.finish(keepSampleData); onComplete() }) {
                        Text(stringResource(R.string.onboarding_skip), color = colors.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeStep(onNext: () -> Unit, onSkip: () -> Unit) {
    val colors = LocalThemeColors.current
    var logoScale by remember { mutableStateOf(0.5f) }
    val featureItems = listOf(
        FeatureItem("📝", stringResource(R.string.onboard_feat_quick_record), stringResource(R.string.onboard_feat_quick_record_desc)),
        FeatureItem("🌳", stringResource(R.string.onboard_feat_tree_viz), stringResource(R.string.onboard_feat_tree_viz_desc)),
        FeatureItem("🔗", stringResource(R.string.onboard_feat_smart_link), stringResource(R.string.onboard_feat_smart_link_desc)),
        FeatureItem("🔒", stringResource(R.string.onboard_feat_local_storage), stringResource(R.string.onboard_feat_local_storage_desc))
    )

    LaunchedEffect(Unit) {
        logoScale = 1f
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🌳",
            fontSize = 72.sp,
            modifier = Modifier.graphicsLayer {
                scaleX = logoScale
                scaleY = logoScale
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.onboard_app_name),
            color = colors.textPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.onboard_app_slogan),
            color = colors.textSecondary,
            fontSize = 16.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        featureItems.forEachIndexed { index, item ->
            FeatureRow(item)
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.onboard_explore), fontSize = 18.sp, modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onSkip) {
            Text(stringResource(R.string.onboarding_skip), color = colors.textSecondary)
        }
    }
}

@Composable
fun FeatureRow(item: FeatureItem) {
    val colors = LocalThemeColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = item.icon, fontSize = 28.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = item.title, color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(text = item.description, color = colors.textSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
fun CreateFirstNodeStep(
    onNext: (String, String) -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalThemeColors.current
    var title by remember { mutableStateOf("") }
    var nodeType by remember { mutableStateOf(NODE_TYPE_ABILITY) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("✨", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Text(stringResource(R.string.onboard_create_first_node), color = colors.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(stringResource(R.string.onboard_node_types_desc), color = colors.textSecondary, fontSize = 16.sp)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.onboard_example_ability), color = colors.textSecondary) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.textSecondary,
                cursorColor = colors.primary,
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TypeCard(
                type = NODE_TYPE_ABILITY,
                selected = nodeType == NODE_TYPE_ABILITY,
                onClick = { nodeType = NODE_TYPE_ABILITY }
            )
            TypeCard(
                type = NODE_TYPE_RESOURCE,
                selected = nodeType == NODE_TYPE_RESOURCE,
                onClick = { nodeType = NODE_TYPE_RESOURCE }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text(
                stringResource(R.string.onboard_type_tip),
                color = colors.textSecondary,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { if (title.isNotBlank()) onNext(title, nodeType) },
            enabled = title.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.create_node_button), fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onSkip) {
            Text(stringResource(R.string.onboard_create_later), color = colors.textSecondary)
        }
    }
}

@Composable
fun TypeCard(type: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalThemeColors.current
    val (emoji, label) = when (type) {
        NODE_TYPE_ABILITY -> "⚔️" to stringResource(R.string.node_type_ability)
        else -> "💎" to stringResource(R.string.node_type_resource)
    }

    Surface(
        modifier = Modifier
            .size(120.dp)
            .clickable(onClick = onClick),
        color = if (selected) colors.primary.copy(alpha = 0.2f) else colors.surface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 2.dp,
            color = if (selected) colors.primary else colors.textSecondary
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = emoji, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun SampleTreeStep(
    keepSampleData: Boolean,
    onKeepSampleData: (Boolean) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalThemeColors.current
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🌳", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Text(stringResource(R.string.onboard_your_tree), color = colors.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(colors.surface, RoundedCornerShape(16.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.onboard_sample_tree), color = colors.textSecondary, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(stringResource(R.string.onboard_tip_label), color = colors.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.onboard_tip_zoom), color = colors.textSecondary, fontSize = 13.sp)
                Text(stringResource(R.string.onboard_tip_pan), color = colors.textSecondary, fontSize = 13.sp)
                Text(stringResource(R.string.onboard_tip_tap), color = colors.textSecondary, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Checkbox(
                checked = keepSampleData,
                onCheckedChange = onKeepSampleData,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(stringResource(R.string.onboard_keep_sample), color = colors.textPrimary, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.onboard_enter_home), fontSize = 18.sp)
        }
    }
}

@Composable
fun FeatureOverviewStep(
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalThemeColors.current
    val features = listOf(
        FeatureItem("🔍", stringResource(R.string.onboard_feat_search), stringResource(R.string.onboard_feat_search_desc)),
        FeatureItem("➕", stringResource(R.string.onboard_feat_create), stringResource(R.string.onboard_feat_create_desc)),
        FeatureItem("✏️", stringResource(R.string.onboard_feat_edit), stringResource(R.string.onboard_feat_edit_desc)),
        FeatureItem("🔗", stringResource(R.string.onboard_feat_link), stringResource(R.string.onboard_feat_link_desc)),
        FeatureItem("📤", stringResource(R.string.onboard_feat_export), stringResource(R.string.onboard_feat_export_desc))
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎉", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Text(stringResource(R.string.onboard_ready), color = colors.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(12.dp))

        Text(stringResource(R.string.onboard_ready_desc), color = colors.textSecondary, fontSize = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column {
                features.forEach { item ->
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = item.icon, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = item.title, color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text(text = item.description, color = colors.textSecondary, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onFinish,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.onboarding_get_started), fontSize = 18.sp)
        }
    }
}
