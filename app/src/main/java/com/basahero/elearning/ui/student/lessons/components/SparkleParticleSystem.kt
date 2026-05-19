package com.basahero.elearning.ui.student.lessons.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ─────────────────────────────────────────────────────────────────────────────
// SparkleParticleState - Shared controller for spawning particle bursts
// ─────────────────────────────────────────────────────────────────────────────
class SparkleParticleState {
    internal val particles = mutableStateListOf<BaseParticle>()

    fun emitStarBurst(x: Float, y: Float, count: Int = 18) {
        val colors = listOf(
            Color(0xFFFFD700), // Bright Gold
            Color(0xFFFFAB40), // Orange Star
            Color(0xFF50E3C2), // Teal Sparkle
            Color(0xFFB39DDB), // Violet Star
            Color(0xFFFF8A80)  // Coral Star
        )
        repeat(count) {
            val angle = Random.nextFloat() * 2 * Math.PI
            val speed = Random.nextFloat() * 12f + 6f
            val size = Random.nextFloat() * 18f * dpToPx() + 8f * dpToPx()
            particles.add(
                BaseParticle(
                    x = x,
                    y = y,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed - 5f).toFloat(), // Upward burst bias
                    size = size,
                    color = colors.random(),
                    type = ParticleType.STAR,
                    maxAge = Random.nextInt(40, 70),
                    gravity = 0.3f,
                    friction = 0.98f
                )
            )
        }
    }

    fun emitConfetti(x: Float, y: Float, count: Int = 24) {
        val colors = listOf(
            Color(0xFF4CAF50), // Green
            Color(0xFF2196F3), // Blue
            Color(0xFFFFE082), // Amber
            Color(0xFFE91E63), // Pink
            Color(0xFF9C27B0)  // Purple
        )
        repeat(count) {
            val angle = Random.nextFloat() * 2 * Math.PI
            val speed = Random.nextFloat() * 10f + 4f
            val size = Random.nextFloat() * 14f * dpToPx() + 8f * dpToPx()
            particles.add(
                BaseParticle(
                    x = x,
                    y = y,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed - 7f).toFloat(),
                    size = size,
                    color = colors.random(),
                    type = ParticleType.CONFETTI,
                    maxAge = Random.nextInt(60, 90),
                    gravity = 0.2f,
                    friction = 0.96f
                )
            )
        }
    }

    fun emitRisingBubbles(x: Float, y: Float, count: Int = 12) {
        repeat(count) {
            val angle = (Random.nextFloat() * 0.5 * Math.PI) + (1.25 * Math.PI) // Upward cone angle
            val speed = Random.nextFloat() * 6f + 2f
            val size = Random.nextFloat() * 24f * dpToPx() + 10f * dpToPx()
            particles.add(
                BaseParticle(
                    x = x,
                    y = y,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    size = size,
                    color = Color(0x99B3E5FC), // Translucent Sky Blue
                    type = ParticleType.BUBBLE,
                    maxAge = Random.nextInt(50, 80),
                    gravity = -0.1f, // Negative gravity makes them rise!
                    friction = 0.99f
                )
            )
        }
    }

    fun update() {
        if (particles.isEmpty()) return
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.age++
            if (p.age >= p.maxAge) {
                iterator.remove()
                continue
            }
            
            p.vx *= p.friction
            p.vy = (p.vy * p.friction) + p.gravity
            
            p.x += p.vx
            p.y += p.vy
            p.rotation += p.rotationSpeed
            
            // Fading out particle logic
            val lifeRatio = p.age.toFloat() / p.maxAge
            p.alpha = (1f - lifeRatio).coerceIn(0f, 1f)
        }
    }
    
    // Pixel sizing helper (standard approx for density)
    private fun dpToPx(): Float = 2.5f 
}

@Composable
fun rememberSparkleParticleState(): SparkleParticleState {
    return remember { SparkleParticleState() }
}

// ─────────────────────────────────────────────────────────────────────────────
// SparkleParticleSystem Composable View
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SparkleParticleSystem(
    state: SparkleParticleState,
    modifier: Modifier = Modifier
) {
    // 60 FPS animation ticker loop
    LaunchedEffect(state) {
        while (isActive) {
            withFrameMillis {
                state.update()
            }
        }
    }

    if (state.particles.isNotEmpty()) {
        Canvas(modifier = modifier.fillMaxSize()) {
            state.particles.forEach { p ->
                val size = p.size
                val path = when (p.type) {
                    ParticleType.STAR -> createStarPath(size)
                    ParticleType.CONFETTI -> createConfettiPath(size, p.age)
                    ParticleType.BUBBLE -> null
                }

                if (path != null) {
                    rotate(degrees = p.rotation, pivot = Offset(p.x, p.y)) {
                        drawPath(
                            path = path.apply {
                                // Shift path to center on particle's coordinates
                                translate(Offset(p.x - size / 2, p.y - size / 2))
                            },
                            color = p.color.copy(alpha = p.alpha)
                        )
                    }
                } else if (p.type == ParticleType.BUBBLE) {
                    // Draw dynamic soap bubble on canvas
                    val bubbleColor = p.color.copy(alpha = p.alpha * 0.4f)
                    drawCircle(
                        color = bubbleColor,
                        radius = size / 2,
                        center = Offset(p.x, p.y)
                    )
                    // High-quality white border highlight stroke
                    drawCircle(
                        color = Color.White.copy(alpha = p.alpha * 0.7f),
                        radius = size / 2,
                        center = Offset(p.x, p.y),
                        style = Stroke(width = 1.5f)
                    )
                    // Bubbly reflection crescent highlight
                    drawCircle(
                        color = Color.White.copy(alpha = p.alpha * 0.8f),
                        radius = size / 6,
                        center = Offset(p.x - size / 4, p.y - size / 4)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Particle Geometry Helpers
// ─────────────────────────────────────────────────────────────────────────────

enum class ParticleType { STAR, CONFETTI, BUBBLE }

class BaseParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val size: Float,
    val color: Color,
    val type: ParticleType,
    val maxAge: Int,
    val gravity: Float,
    val friction: Float
) {
    var age = 0
    var alpha = 1f
    var rotation = Random.nextFloat() * 360f
    val rotationSpeed = Random.nextFloat() * 8f - 4f
}

// Prepares a standard symmetric 5-pointed star geometry
private fun createStarPath(size: Float): Path {
    val path = Path()
    val cx = size / 2
    val cy = size / 2
    val spikes = 5
    val outerRadius = size / 2
    val innerRadius = size / 4
    
    var rot = Math.PI / 2 * 3
    var x = cx
    var y = cy
    val step = Math.PI / spikes

    path.moveTo(cx, cy - outerRadius)
    repeat(spikes) {
        x = (cx + cos(rot) * outerRadius).toFloat()
        y = (cy + sin(rot) * outerRadius).toFloat()
        path.lineTo(x, y)
        rot += step

        x = (cx + cos(rot) * innerRadius).toFloat()
        y = (cy + sin(rot) * innerRadius).toFloat()
        path.lineTo(x, y)
        rot += step
    }
    path.close()
    return path
}

// Creates an elongated rectangle mimicking a piece of spinning ribbon confetti
private fun createConfettiPath(size: Float, age: Int): Path {
    val path = Path()
    // Introduce 3D flip distortion based on particle age (flips along width)
    val widthFactor = cos(age * 0.15f)
    val halfW = (size / 2) * widthFactor
    val halfH = size / 4
    
    path.moveTo(-halfW, -halfH)
    path.lineTo(halfW, -halfH)
    path.lineTo(halfW, halfH)
    path.lineTo(-halfW, halfH)
    path.close()
    return path
}
