package com.fancy.skill_tree.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fancy.skill_tree.core.domain.usecase.node.CreateNodeUseCase
import com.fancy.skill_tree.core.domain.usecase.node.LoadSampleDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingManager: OnboardingManager,
    private val loadSampleDataUseCase: LoadSampleDataUseCase,
    private val createNodeUseCase: CreateNodeUseCase
) : ViewModel() {

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _keepSampleData = MutableStateFlow(true)
    val keepSampleData: StateFlow<Boolean> = _keepSampleData.asStateFlow()

    fun nextStep() { _currentStep.update { it + 1 } }
    fun prevStep() { _currentStep.update { (it - 1).coerceAtLeast(0) } }

    fun skip() {
        viewModelScope.launch {
            loadSampleDataUseCase()
            onboardingManager.completeOnboarding()
        }
    }

    fun finish(keepSample: Boolean) {
        viewModelScope.launch {
            if (!keepSample) {
                // 清理示例数据（这里先简化处理）
            } else {
                loadSampleDataUseCase()
            }
            onboardingManager.completeOnboarding()
        }
    }

    fun setKeepSampleData(keep: Boolean) {
        _keepSampleData.update { keep }
    }

    fun createFirstNode(title: String, nodeType: String) {
        viewModelScope.launch {
            createNodeUseCase(title, nodeType, null)
            nextStep()
        }
    }
}
