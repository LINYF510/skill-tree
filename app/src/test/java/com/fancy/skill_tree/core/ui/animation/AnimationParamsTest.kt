package com.fancy.skill_tree.core.ui.animation

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * AnimationParams 单元测试
 */
@DisplayName("AnimationParams")
class AnimationParamsTest {

    @Test
    @DisplayName("默认构造函数创建正确的默认值")
    fun defaultValuesAreCorrect() {
        val params = AnimationParams()

        assertThat(params.newlyCreatedNodeId).isNull()
        assertThat(params.creationScale).isEqualTo(1f)
        assertThat(params.growthProgress).isEqualTo(100f)
        assertThat(params.searchPulseAlpha).isEqualTo(0f)
        assertThat(params.selectedPulseAlpha).isEqualTo(0f)
        assertThat(params.selectedNodeId).isNull()
        assertThat(params.isSearchActive).isFalse()
        assertThat(params.unlockingNodeId).isNull()
        assertThat(params.unlockProgress).isEqualTo(0f)
        assertThat(params.particleSystems).isEmpty()
    }

    @Test
    @DisplayName("可以设置所有字段")
    fun canSetAllFields() {
        val particleSystem = ParticleSystem(100f, 100f, 10)
        particleSystem.emit()

        val params = AnimationParams(
            newlyCreatedNodeId = "node1",
            creationScale = 0.5f,
            growthProgress = 50f,
            searchPulseAlpha = 0.8f,
            selectedPulseAlpha = 0.7f,
            selectedNodeId = "node2",
            isSearchActive = true,
            unlockingNodeId = "node3",
            unlockProgress = 0.6f,
            particleSystems = mapOf("node1" to particleSystem)
        )

        assertThat(params.newlyCreatedNodeId).isEqualTo("node1")
        assertThat(params.creationScale).isEqualTo(0.5f)
        assertThat(params.growthProgress).isEqualTo(50f)
        assertThat(params.searchPulseAlpha).isEqualTo(0.8f)
        assertThat(params.selectedPulseAlpha).isEqualTo(0.7f)
        assertThat(params.selectedNodeId).isEqualTo("node2")
        assertThat(params.isSearchActive).isTrue()
        assertThat(params.unlockingNodeId).isEqualTo("node3")
        assertThat(params.unlockProgress).isEqualTo(0.6f)
        assertThat(params.particleSystems).hasSize(1)
        assertThat(params.particleSystems["node1"]).isEqualTo(particleSystem)
    }

    @Test
    @DisplayName("copy 方法可以修改特定字段")
    fun copyCanModifySpecificFields() {
        val params = AnimationParams()
        val modified = params.copy(
            unlockingNodeId = "node1",
            unlockProgress = 0.5f
        )

        assertThat(modified.unlockingNodeId).isEqualTo("node1")
        assertThat(modified.unlockProgress).isEqualTo(0.5f)
        // 其他字段保持不变
        assertThat(modified.newlyCreatedNodeId).isNull()
        assertThat(modified.creationScale).isEqualTo(1f)
    }
}
