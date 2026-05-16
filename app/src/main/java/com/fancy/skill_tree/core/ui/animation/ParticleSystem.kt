package com.fancy.skill_tree.core.ui.animation

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 单个粒子
 *
 * @property x 当前 X 坐标
 * @property y 当前 Y 坐标
 * @property velocityX X 方向速度（像素/秒）
 * @property velocityY Y 方向速度（像素/秒）
 * @property alpha 当前透明度 (0..1)
 * @property color 粒子颜色
 * @property radius 粒子半径（像素）
 * @property life 当前生命值（秒）
 * @property maxLife 最大生命值（秒）
 */
data class Particle(
    var x: Float,
    var y: Float,
    var velocityX: Float,
    var velocityY: Float,
    var alpha: Float,
    val color: Color,
    val radius: Float,
    var life: Float,
    val maxLife: Float
)

/**
 * 粒子系统
 * 管理粒子的创建、更新和生命周期
 *
 * @property centerX 发射中心 X 坐标
 * @property centerY 发射中心 Y 坐标
 * @property particleCount 粒子数量
 * @property colors 粒子颜色列表，为空时使用默认颜色
 */
class ParticleSystem(
    private val centerX: Float,
    private val centerY: Float,
    private val particleCount: Int = AnimationConfig.PARTICLE_COUNT,
    private val colors: List<Color> = emptyList()
) {
    /**
     * 粒子列表
     */
    val particles: MutableList<Particle> = mutableListOf()

    /**
     * 粒子系统是否存活（还有存活的粒子）
     */
    var isAlive: Boolean = true
        private set

    /**
     * 初始化粒子，从中心点向外随机发射
     *
     * 粒子属性：
     * - 速度范围：50~200 px/s
     * - 角度随机：0~2π
     * - 初始透明度：1.0
     * - 半径范围：2~6 px
     * - 生命周期：0.5~1.5 秒
     * - 颜色：从 colors 列表随机选择，为空时使用默认金色
     */
    fun emit() {
        particles.clear()
        val defaultColor = Color(0xFFFFD700)

        repeat(particleCount) {
            val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
            val speed = Random.nextFloat() * 150f + 50f // 50~200 px/s
            val velocityX = cos(angle) * speed
            val velocityY = sin(angle) * speed
            val radius = Random.nextFloat() * 4f + 2f // 2~6 px
            val maxLife = Random.nextFloat() * 1f + 0.5f // 0.5~1.5 秒
            val color = colors.randomOrNull() ?: defaultColor

            particles.add(
                Particle(
                    x = centerX,
                    y = centerY,
                    velocityX = velocityX,
                    velocityY = velocityY,
                    alpha = 1f,
                    color = color,
                    radius = radius,
                    life = maxLife,
                    maxLife = maxLife
                )
            )
        }
        isAlive = true
    }

    /**
     * 更新所有粒子的位置和透明度
     *
     * @param deltaTime 帧间隔时间（秒）
     * @param gravity 重力加速度（像素/秒²），默认 100f
     * @return 是否还有存活粒子
     */
    fun update(deltaTime: Float, gravity: Float = AnimationConfig.PARTICLE_GRAVITY): Boolean {
        if (particles.isEmpty()) {
            isAlive = false
            return false
        }

        var hasAliveParticles = false

        particles.forEach { particle ->
            if (particle.life > 0f) {
                // 更新位置
                particle.x += particle.velocityX * deltaTime
                particle.y += particle.velocityY * deltaTime

                // 应用重力
                particle.velocityY += gravity * deltaTime

                // 更新生命值
                particle.life -= deltaTime

                // 根据生命值计算透明度
                particle.alpha = (particle.life / particle.maxLife).coerceIn(0f, 1f)

                if (particle.life > 0f) {
                    hasAliveParticles = true
                }
            } else {
                particle.alpha = 0f
            }
        }

        isAlive = hasAliveParticles
        return hasAliveParticles
    }
}
