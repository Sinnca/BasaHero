package com.basahero.elearning.ui.student.lessons.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basahero.elearning.ui.theme.fredokaFontFamily
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// MascotState - Dynamic emotional and behavioral states of the companion
// ─────────────────────────────────────────────────────────────────────────────
enum class MascotState {
    GREETING,          // Wave, big smile
    FOCUS_READING,     // Wearing tiny reading glasses, focused eyes
    PRACTICE_SPEECH,   // Holding microphone, talking mouth
    SUCCESS_CHEER,     // High jumping, star eyes, huge smile
    TRY_AGAIN,         // Soft encouraging smile, head tilting
    STEP_COMPLETE      // Graduation cap, victory dancing
}

// ─────────────────────────────────────────────────────────────────────────────
// MascotGuidanceView - Main Composable providing peeking mascot + speech bubble
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MascotGuidanceView(
    state: MascotState,
    modifier: Modifier = Modifier,
    customMessage: String? = null,
    isTablet: Boolean = false
) {
    // 1. Context-aware guidance text dictionary
    val guidanceMessage = remember(state, customMessage) {
        customMessage ?: when (state) {
            MascotState.GREETING -> "Welcome, Hero! Let's read this exciting story. Read the intro card to begin! 🌟"
            MascotState.FOCUS_READING -> "Look closely at the story! Tap the golden underlined words to learn their secrets and practice! 📚"
            MascotState.PRACTICE_SPEECH -> "Listen to the pronunciation first, then press 'Record' and read the word out loud! You got this! 🎙️"
            MascotState.SUCCESS_CHEER -> "Absolutely stellar! You got it 100% correct! Have a gold star! ⭐"
            MascotState.TRY_AGAIN -> "Super try! Let's practice it together one more time. You can do it! 💪"
            MascotState.STEP_COMPLETE -> "Outstanding progress! We finished this section. Swipe or click 'Next' for our next challenge! 🚀"
        }
    }

    // 2. Continuous idle physics: floating, cape waving, and breathing loops
    val infiniteTransition = rememberInfiniteTransition(label = "mascot_physics")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -5f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(2400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "float"
    )
    val capeWave by infiniteTransition.animateFloat(
        initialValue = -8f, targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "cape"
    )
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.98f, targetValue = 1.02f,
        animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "breath"
    )
    val mouthAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(220, easing = LinearEasing), RepeatMode.Reverse),
        label = "mouth"
    )

    // 3. Dynamic blinking loop (blinks every 3-5 seconds at random)
    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay((3000..6000).random().toLong())
            isBlinking = true
            delay(150)
            isBlinking = false
        }
    }

    // 4. Spring-loaded dialogue entrance: bounce bubble whenever text changes
    var textTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(guidanceMessage) {
        textTrigger++
    }
    
    val bubbleScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bubble_spring"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTablet) 20.dp else 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // A. Visual Star Mascot peeking out of card
        Box(
            modifier = Modifier
                .size(if (isTablet) 100.dp else 72.dp)
                .offset(y = floatOffset.dp),
            contentAlignment = Alignment.Center
        ) {
            MascotCanvas(
                state = state,
                isBlinking = isBlinking,
                breathScale = breathScale,
                capeWave = capeWave,
                mouthAnim = mouthAnim,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.width(6.dp))

        // B. Glassmorphism Bouncy speech bubble
        Box(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer {
                    scaleX = bubbleScale
                    scaleY = bubbleScale
                }
                .shadow(4.dp, RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
                .clip(SpeechBubbleShape)
                .background(Color.White.copy(alpha = 0.94f))
                .border(2.dp, Color(0xFFFFF176).copy(alpha = 0.8f), SpeechBubbleShape) // Golden border accent
                .padding(
                    start = if (isTablet) 24.dp else 18.dp, // Leave slightly more padding on the left side for the speech bubble tip
                    end = if (isTablet) 16.dp else 12.dp,
                    top = if (isTablet) 12.dp else 8.dp,
                    bottom = if (isTablet) 12.dp else 8.dp
                )
        ) {
            key(textTrigger) {
                Text(
                    text = guidanceMessage,
                    fontSize = if (isTablet) 14.sp else 12.sp,
                    fontFamily = fredokaFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4E342E), // Legible cocoa dark brown text
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MascotCanvas - Renders the cute peeking star hero on Canvas
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MascotCanvas(
    state: MascotState,
    isBlinking: Boolean,
    breathScale: Float,
    capeWave: Float,
    mouthAnim: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val cy = h / 2
        val sizeVal = minOf(w, h) * breathScale

        // A. Draw Red Superhero Cape behind Star body
        val capePath = Path().apply {
            val startCapeX = cx - sizeVal * 0.35f
            val startCapeY = cy + sizeVal * 0.2f
            moveTo(startCapeX, startCapeY)
            // Left cape wing sways based on capeWave
            cubicTo(
                startCapeX - sizeVal * 0.25f, startCapeY + sizeVal * 0.1f + capeWave,
                cx - sizeVal * 0.3f, cy + sizeVal * 0.5f,
                cx, cy + sizeVal * 0.45f
            )
            // Right cape wing sways opposite
            cubicTo(
                cx + sizeVal * 0.3f, cy + sizeVal * 0.5f,
                cx + sizeVal * 0.35f + sizeVal * 0.25f, startCapeY + sizeVal * 0.1f - capeWave,
                cx + sizeVal * 0.35f, startCapeY
            )
            close()
        }
        drawPath(capePath, color = Color(0xFFE53935)) // Soft Crimson Red Cape

        // B. Draw Golden Star body with rounded soft points
        val starPath = Path().apply {
            val outerRadius = sizeVal * 0.42f
            val innerRadius = sizeVal * 0.24f
            val spikes = 5
            var rot = Math.PI / 2 * 3
            val step = Math.PI / spikes
            
            // Generate star coordinate vectors
            var xVec = (cx + cos(rot) * outerRadius).toFloat()
            var yVec = (cy + sin(rot) * outerRadius).toFloat()
            moveTo(xVec, yVec)
            
            repeat(spikes) {
                xVec = (cx + cos(rot) * outerRadius).toFloat()
                yVec = (cy + sin(rot) * outerRadius).toFloat()
                lineTo(xVec, yVec)
                rot += step

                xVec = (cx + cos(rot) * innerRadius).toFloat()
                yVec = (cy + sin(rot) * innerRadius).toFloat()
                lineTo(xVec, yVec)
                rot += step
            }
            close()
        }
        
        // Dynamic gold gradient body fill
        drawPath(
            path = starPath,
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFF176), Color(0xFFFBC02D)),
                center = Offset(cx, cy),
                radius = sizeVal * 0.5f
            )
        )
        
        // Star outer contour line
        drawPath(
            path = starPath,
            color = Color(0xFFF57F17),
            style = Stroke(width = sizeVal * 0.04f)
        )

        // C. Draw cute blushing cheeks
        drawCircle(
            color = Color(0xFFFF8A80).copy(alpha = 0.6f),
            radius = sizeVal * 0.06f,
            center = Offset(cx - sizeVal * 0.18f, cy + sizeVal * 0.08f)
        )
        drawCircle(
            color = Color(0xFFFF8A80).copy(alpha = 0.6f),
            radius = sizeVal * 0.06f,
            center = Offset(cx + sizeVal * 0.18f, cy + sizeVal * 0.08f)
        )

        // D. Draw Big Cartoon eyes
        val eyeRadius = sizeVal * 0.06f
        val leftEyeX = cx - sizeVal * 0.13f
        val rightEyeX = cx + sizeVal * 0.13f
        val eyeY = cy - sizeVal * 0.04f

        if (isBlinking) {
            // Blinking eyes are simple sleeping flat lines
            drawLine(
                color = Color(0xFF3E2723),
                start = Offset(leftEyeX - eyeRadius, eyeY),
                end = Offset(leftEyeX + eyeRadius, eyeY),
                strokeWidth = sizeVal * 0.04f
            )
            drawLine(
                color = Color(0xFF3E2723),
                start = Offset(rightEyeX - eyeRadius, eyeY),
                end = Offset(rightEyeX + eyeRadius, eyeY),
                strokeWidth = sizeVal * 0.04f
            )
        } else if (state == MascotState.SUCCESS_CHEER) {
            // Success cheering morphs eyes into stars or twinkling "^" shapes
            drawStarEye(leftEyeX, eyeY, eyeRadius)
            drawStarEye(rightEyeX, eyeY, eyeRadius)
        } else {
            // Normal black eyes with a beautiful white reflex gloss bubble
            drawCircle(color = Color(0xFF3E2723), radius = eyeRadius, center = Offset(leftEyeX, eyeY))
            drawCircle(color = Color(0xFF3E2723), radius = eyeRadius, center = Offset(rightEyeX, eyeY))

            // White reflect point
            drawCircle(color = Color.White, radius = eyeRadius * 0.35f, center = Offset(leftEyeX - eyeRadius * 0.3f, eyeY - eyeRadius * 0.3f))
            drawCircle(color = Color.White, radius = eyeRadius * 0.35f, center = Offset(rightEyeX - eyeRadius * 0.3f, eyeY - eyeRadius * 0.3f))
        }

        // E. Draw cute emotional mouth
        val mouthY = cy + sizeVal * 0.06f
        when (state) {
            MascotState.PRACTICE_SPEECH -> {
                // Animated speaking oval mouth that opens and closes dynamically!
                val animatedMouthHeight = sizeVal * 0.08f * mouthAnim
                drawArc(
                    color = Color(0xFF3E2723),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = true,
                    topLeft = Offset(cx - sizeVal * 0.04f, mouthY - animatedMouthHeight / 2),
                    size = Size(sizeVal * 0.08f, animatedMouthHeight)
                )
            }
            MascotState.TRY_AGAIN -> {
                // Soft flat cute line
                drawLine(
                    color = Color(0xFF3E2723),
                    start = Offset(cx - sizeVal * 0.05f, mouthY),
                    end = Offset(cx + sizeVal * 0.05f, mouthY),
                    strokeWidth = sizeVal * 0.03f
                )
            }
            else -> {
                // Normal happy smiling arc path
                val mouthPath = Path().apply {
                    moveTo(cx - sizeVal * 0.06f, mouthY - sizeVal * 0.01f)
                    quadraticTo(
                        cx, mouthY + sizeVal * 0.05f,
                        cx + sizeVal * 0.06f, mouthY - sizeVal * 0.01f
                    )
                }
                drawPath(
                    path = mouthPath,
                    color = Color(0xFF3E2723),
                    style = Stroke(width = sizeVal * 0.03f)
                )
            }
        }

        // F. Mascot State Custom Dress-up details (e.g. Reading Glasses, Graduation Cap)
        if (state == MascotState.FOCUS_READING) {
            // Draw cute blue vector reading glasses
            val frameRadius = sizeVal * 0.09f
            drawCircle(
                color = Color(0xFF0288D1),
                radius = frameRadius,
                center = Offset(leftEyeX, eyeY),
                style = Stroke(width = sizeVal * 0.02f)
            )
            drawCircle(
                color = Color(0xFF0288D1),
                radius = frameRadius,
                center = Offset(rightEyeX, eyeY),
                style = Stroke(width = sizeVal * 0.02f)
            )
            // Bridge line
            drawLine(
                color = Color(0xFF0288D1),
                start = Offset(leftEyeX + frameRadius, eyeY),
                end = Offset(rightEyeX - frameRadius, eyeY),
                strokeWidth = sizeVal * 0.025f
            )
        } else if (state == MascotState.STEP_COMPLETE) {
            // Draw adorable mini black graduation cap on top spike
            val capY = cy - sizeVal * 0.32f
            val capPath = Path().apply {
                moveTo(cx, capY - sizeVal * 0.08f)
                lineTo(cx + sizeVal * 0.16f, capY)
                lineTo(cx, capY + sizeVal * 0.08f)
                lineTo(cx - sizeVal * 0.16f, capY)
                close()
            }
            drawPath(capPath, color = Color(0xFF263238)) // Cap diamond body
            
            // Cap base
            val baseCapPath = Path().apply {
                moveTo(cx - sizeVal * 0.08f, capY)
                quadraticTo(cx, capY + sizeVal * 0.1f, cx + sizeVal * 0.08f, capY)
                lineTo(cx + sizeVal * 0.08f, capY + sizeVal * 0.05f)
                quadraticTo(cx, capY + sizeVal * 0.12f, cx - sizeVal * 0.08f, capY + sizeVal * 0.05f)
                close()
            }
            drawPath(baseCapPath, color = Color(0xFF37474F))

            // Cap little gold hanging tassel
            drawLine(
                color = Color(0xFFFFD54F),
                start = Offset(cx, capY),
                end = Offset(cx - sizeVal * 0.14f, capY + sizeVal * 0.06f),
                strokeWidth = sizeVal * 0.015f
            )
        }
    }
}

// Draw twinkling "^" vector lines for eyes
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStarEye(x: Float, y: Float, size: Float) {
    drawLine(
        color = Color(0xFF3E2723),
        start = Offset(x - size, y + size * 0.3f),
        end = Offset(x, y - size * 0.5f),
        strokeWidth = size * 0.3f
    )
    drawLine(
        color = Color(0xFF3E2723),
        start = Offset(x, y - size * 0.5f),
        end = Offset(x + size, y + size * 0.3f),
        strokeWidth = size * 0.3f
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Custom SpeechBubble Shape (Glassmorphic Container with peek triangle tip)
// ─────────────────────────────────────────────────────────────────────────────
private val SpeechBubbleShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val r = 40f // Corner radius px
    val tipWidth = 20f
    val tipHeight = 25f
    val tipTopY = 30f

    // Draw speech rectangle with left peeking tip
    moveTo(tipWidth + r, 0f)
    lineTo(w - r, 0f)
    quadraticTo(w, 0f, w, r)
    lineTo(w, h - r)
    quadraticTo(w, h, w - r, h)
    lineTo(tipWidth + r, h)
    quadraticTo(tipWidth, h, tipWidth, h - r)
    
    // Bubble side tip pointing left towards mascot
    lineTo(tipWidth, tipTopY + tipHeight)
    lineTo(0f, tipTopY + (tipHeight / 2))
    lineTo(tipWidth, tipTopY)
    
    lineTo(tipWidth, r)
    quadraticTo(tipWidth, 0f, tipWidth + r, 0f)
    close()
}
