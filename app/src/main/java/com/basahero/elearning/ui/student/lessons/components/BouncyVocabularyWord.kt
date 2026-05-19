package com.basahero.elearning.ui.student.lessons.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basahero.elearning.ui.theme.fredokaFontFamily
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow

// ─────────────────────────────────────────────────────────────────────────────
// BouncyVocabularyWord - Highly tactile game-like pill card for highlights
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun BouncyVocabularyWord(
    word: String,
    onClick: (x: Float, y: Float) -> Unit,
    modifier: Modifier = Modifier,
    isTablet: Boolean = false,
    gradientColors: List<Color> = listOf(Color(0xFFFFF176), Color(0xFFFFD54F)) // Warm gold/yellow
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 1. Idle Floating & Breathing Loop (Infinite Transition)
    val infiniteTransition = rememberInfiniteTransition(label = "idle_loops")
    val idleOffsetY by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating_offset"
    )
    val idleScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // 2. Click Squish Physics (Spring Animation)
    val targetScale = if (isPressed) 0.88f else 1.0f
    val targetedOffset = if (isPressed) 3.dp else 0.dp
    
    val clickScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale_physics"
    )
    
    val clickOffsetY by animateDpAsState(
        targetValue = targetedOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "offset_physics"
    )

    // Layout values
    val verticalPadding = if (isTablet) 8.dp else 6.dp
    val horizontalPadding = if (isTablet) 16.dp else 12.dp
    val fontSize = if (isTablet) 18.sp else 16.sp

    // Get card global coordinates on click to trigger sparkle particles
    var pxX by remember { mutableStateOf(0f) }
    var pxY by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .padding(horizontal = 3.dp, vertical = 4.dp)
            .graphicsLayer {
                // Apply combined continuous idle physics + touch squish physics
                scaleX = clickScale * idleScale
                scaleY = clickScale * idleScale
                translationY = (clickOffsetY.toPx() + idleOffsetY)
            }
            .shadow(
                elevation = if (isPressed) 1.dp else 3.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = Color(0xFFFFD54F).copy(alpha = 0.5f),
                spotColor = Color(0xFFFFD54F).copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(gradientColors)
            )
            .border(
                width = 2.dp,
                color = Color(0xFFFFB300), // Rich amber boundary line
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null, // No standard gray ripple, keep it 100% custom
                onClick = {
                    onClick(pxX, pxY)
                }
            )
            .onGloballyPositioned { layoutCoordinates ->
                val bounds = layoutCoordinates.boundsInWindow()
                pxX = bounds.center.x
                pxY = bounds.center.y
            }
            .padding(vertical = verticalPadding, horizontal = horizontalPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = word,
                fontSize = fontSize,
                fontFamily = fredokaFontFamily,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF5D4037) // Rich cocoa brown text for legibility
            )
        }
    }
}


