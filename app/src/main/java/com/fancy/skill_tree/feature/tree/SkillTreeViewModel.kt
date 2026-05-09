package com.fancy.skill_tree.feature.tree

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fancy.skill_tree.core.data.repository.SkillTreeRepository
import com.fancy.skill_tree.core.domain.entity.SkillNodeEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 技能树界面 ViewModel
 * 负责从 Repository 加载数据并暴露 UI 状态
 */
@HiltViewModel
class SkillTreeViewModel @Inject constructor(
    private val repository: SkillTreeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SkillTreeUiState())
    val uiState: StateFlow<SkillTreeUiState> = _uiState.asStateFlow()

    init {
        loadNodes()
    }

    /**
     * 从数据库加载所有节点
     */
    private fun loadNodes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            repository.getAllNodes()
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "加载数据失败"
                        )
                    }
                }
                .collect { nodes ->
                    _uiState.update {
                        it.copy(
                            nodes = nodes,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    /**
     * 插入新节点
     * @param node 要插入的节点
     */
    fun insertNode(node: SkillNodeEntity) {
        viewModelScope.launch {
            try {
                repository.insertNode(node)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "插入节点失败")
                }
            }
        }
    }

    /**
     * 更新节点
     * @param node 要更新的节点
     */
    fun updateNode(node: SkillNodeEntity) {
        viewModelScope.launch {
            try {
                repository.updateNode(node)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "更新节点失败")
                }
            }
        }
    }

    /**
     * 删除节点
     * @param nodeId 要删除的节点 ID
     */
    fun deleteNode(nodeId: String) {
        viewModelScope.launch {
            try {
                repository.deleteNode(nodeId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "删除节点失败")
                }
            }
        }
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
