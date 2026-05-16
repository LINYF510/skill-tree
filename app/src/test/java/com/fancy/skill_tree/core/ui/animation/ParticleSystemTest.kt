package com.fancy.skill_tree.core.ui.animation

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * ParticleSystem 单元测试
 */
@DisplayName("ParticleSystem")
class ParticleSystemTest {

    @Nested
    @DisplayName("emit")
    inner class Emit {

        @Test
        @DisplayName("发射正确数量的粒子")
        fun emitsCorrectNumberOfParticles() {
            val system = ParticleSystem(
                centerX = 100f,
                centerY = 100f,
                particleCount = 30
            )
            system.emit()

            assertThat(system.particles).hasSize(30)
        }

        @Test
        @DisplayName("粒子从中心点发射")
        fun particlesStartFromCenter() {
            val system = ParticleSystem(
                centerX = 100f,
                centerY = 200f,
                particleCount = 10
            )
            system.emit()

            system.particles.forEach { particle ->
                assertThat(particle.x).isEqualTo(100f)
                assertThat(particle.y).isEqualTo(200f)
            }
        }

        @Test
        @DisplayName("发射后系统处于存活状态")
        fun systemIsAliveAfterEmit() {
            val system = ParticleSystem(
                centerX = 100f,
                centerY = 100f,
                particleCount = 10
            )
            system.emit()

            assertThat(system.isAlive).isTrue()
        }

        @Test
        @DisplayName("使用自定义颜色")
        fun usesCustomColors() {
            val customColors = listOf(Color.Red, Color.Green, Color.Blue)
            val system = ParticleSystem(
                centerX = 100f,
                centerY = 100f,
                particleCount = 30,
                colors = customColors
            )
            system.emit()

            system.particles.forEach { particle ->
                assertThat(particle.color).isIn(customColors)
            }
        }

        @Test
        @DisplayName("颜色为空时使用默认金色")
        fun usesDefaultColorWhenEmpty() {
            val system = ParticleSystem(
                centerX = 100f,
                centerY = 100f,
                particleCount = 10,
                colors = emptyList()
            )
            system.emit()

            val defaultColor = Color(0xFFFFD700)
            system.particles.forEach { particle ->
                assertThat(particle.color).isEqualTo(defaultColor)
            }
        }
    }

    @Nested
    @DisplayName("update")
    inner class Update {

        @Test
        @DisplayName("更新后粒子位置发生变化")
        fun particlePositionChangesAfterUpdate() {
            val system = ParticleSystem(
                centerX = 100f,
                centerY = 100f,
                particleCount = 10
            )
            system.emit()

            val initialX = system.particles[0].x
            val initialY = system.particles[0].y

            system.update(deltaTime = 0.1f)

            assertThat(system.particles[0].x).isNotEqualTo(initialX)
            assertThat(system.particles[0].y).isNotEqualTo(initialY)
        }

        @Test
        @DisplayName("应用重力后 Y 方向速度增加")
        fun gravityIncreasesYVelocity() {
            val system = ParticleSystem(
                centerX = 100f,
                centerY = 100f,
                particleCount = 10
            )
            system.emit()

            val initialVelocityY = system.particles[0].velocityY

            system.update(deltaTime = 0.1f, gravity = 100f)

            assertThat(system.particles[0].velocityY).isGreaterThan(initialVelocityY)
        }

        @Test
        @DisplayName("生命值随时间减少")
        fun lifeDecreasesOverTime() {
            val system = ParticleSystem(
                centerX = 100f,
                centerY = 100f,
                particleCount = 10
            )
            system.emit()

            val initialLife = system.particles[0].life

            system.update(deltaTime = 0.1f)

            assertThat(system.particles[0].life).isLessThan(initialLife)
        }

        @Test
        @DisplayName("透明度随生命值衰减")
        fun alphaDecreasesWithLife() {
            val system = ParticleSystem(
                centerX = 100f,
                centerY = 100f,
                particleCount = 10
            )
            system.emit()

            val initialAlpha = system.particles[0].alpha

            system.update(deltaTime = 0.5f)

            assertThat(system.particles[0].alpha).isLessThan(initialAlpha)
        }

        @Test
        @DisplayName("所有粒子消亡后返回 false")
        fun returnsFalseWhenAllParticlesDead() {
            val system = ParticleSystem(
                centerX = 100f,
                centerY = 100f,
                particleCount = 10
            )
            system.emit()

            // 模拟足够长的时间让所有粒子消亡
            var hasAliveParticles = true
            repeat(100) {
                hasAliveParticles = system.update(deltaTime = 0.1f)
                if (!hasAliveParticles) return@repeat
            }

            assertThat(hasAliveParticles).isFalse()
            assertThat(system.isAlive).isFalse()
        }

        @Test
        @DisplayName("还有存活粒子时返回 true")
        fun returnsTrueWhenParticlesAlive() {
            val system = ParticleSystem(
                centerX = 100f,
                centerY = 100f,
                particleCount = 10
            )
            system.emit()

            val hasAliveParticles = system.update(deltaTime = 0.01f)

            assertThat(hasAliveParticles).isTrue()
            assertThat(system.isAlive).isTrue()
        }

        @Test
        @DisplayName("粒子生命值为 0 时透明度为 0")
        fun alphaIsZeroWhenLifeIsZero() {
            val system = ParticleSystem(
                centerX = 100f,
                centerY = 100f,
                particleCount = 10
            )
            system.emit()

            // 让所有粒子消亡
            repeat(100) {
                system.update(deltaTime = 0.1f)
            }

            system.particles.forEach { particle ->
                if (particle.life <= 0f) {
                    assertThat(particle.alpha).isEqualTo(0f)
                }
            }
        }
    }

    @Nested
    @DisplayName("粒子属性范围")
    inner class ParticleProperties {

        @Test
        @DisplayName("粒子半径在 2~6 范围内")
        fun radiusInRange() {
            val system = ParticleSystem(
                centerX = 100f,
                centerY = 100f,
                particleCount = 100
            )
            system.emit()

            system.particles.forEach { particle ->
                assertThat(particle.radius).isAtLeast(2f)
                assertThat(particle.radius).isAtMost(6f)
            }
        }

        @Test
        @DisplayName("粒子生命周期在 0.5~1.5 范围内")
        fun maxLifeInRange() {
            val system = ParticleSystem(
                centerX = 100f,
                centerY = 100f,
                particleCount = 100
            )
            system.emit()

            system.particles.forEach { particle ->
                assertThat(particle.maxLife).isAtLeast(0.5f)
                assertThat(particle.maxLife).isAtMost(1.5f)
            }
        }

        @Test
        @DisplayName("初始透明度为 1.0")
        fun initialAlphaIsOne() {
            val system = ParticleSystem(
                centerX = 100f,
                centerY = 100f,
                particleCount = 10
            )
            system.emit()

            system.particles.forEach { particle ->
                assertThat(particle.alpha).isEqualTo(1f)
            }
        }
    }
}
