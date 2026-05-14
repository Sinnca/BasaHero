package com.basahero.elearning.ui.common

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import com.basahero.elearning.ui.theme.fredokaFontFamily
import com.airbnb.lottie.compose.*
import coil.compose.AsyncImage
import coil.request.ImageRequest

// ─────────────────────────────────────────────────────────────────────────────
// STEP 9 — SafeLottieAnimation
// Crash-safe wrapper around every Lottie call in the app.
// Checks if the asset file exists before loading.
// If missing → plays a clean built-in Compose fallback animation.
// Usage:
//   SafeLottieAnimation(assetPath = "lottie/confetti.json", fallback = FallbackType.CONFETTI)
// ─────────────────────────────────────────────────────────────────────────────

enum class FallbackType { CONFETTI, CORRECT, WRONG, COMPLETE }

@Composable
fun SafeLottieAnimation(
    assetPath: String,
    fallback: FallbackType,
    modifier: Modifier = Modifier,
    iterations: Int = 1,
    autoPlay: Boolean = true
) {
    val context = LocalContext.current

    // Check if the Lottie file actually exists in assets — once, remembered
    val fileExists = remember(assetPath) {
        try {
            context.assets.open(assetPath).close()
            true
        } catch (e: Exception) {
            false
        }
    }

    if (fileExists) {
        // ── Normal Lottie path ────────────────────────────────────────────────
        val composition by rememberLottieComposition(LottieCompositionSpec.Asset(assetPath))
        val progress by animateLottieCompositionAsState(
            composition = composition,
            isPlaying = autoPlay,
            iterations = iterations
        )
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = modifier
        )
    } else {
        // ── Fallback: pure Compose animation — no crash ───────────────────────
        LottieFallbackAnimation(type = fallback, modifier = modifier)
    }
}

@Composable
private fun LottieFallbackAnimation(type: FallbackType, modifier: Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "fallback")

    when (type) {
        FallbackType.CORRECT -> {
            // Pulsing green checkmark scale
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.8f, targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    tween(400, easing = EaseInOutCubic), RepeatMode.Reverse
                ), label = "correct_scale"
            )
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                Text(
                    "✓",
                    fontSize = (48 * scale).sp,
                    color = Color(0xFF1D9E75),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        FallbackType.WRONG -> {
            // Shaking red X
            val offsetX by infiniteTransition.animateFloat(
                initialValue = -8f, targetValue = 8f,
                animationSpec = infiniteRepeatable(
                    tween(80, easing = LinearEasing), RepeatMode.Reverse
                ), label = "wrong_shake"
            )
            Box(
                modifier = modifier.offset(x = offsetX.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("✗", fontSize = 48.sp, color = Color(0xFFD85A30), fontWeight = FontWeight.Bold)
            }
        }

        FallbackType.CONFETTI -> {
            // Expanding + fading colored circles burst
            val alpha by infiniteTransition.animateFloat(
                initialValue = 1f, targetValue = 0f,
                animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Restart),
                label = "confetti_alpha"
            )
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.2f, targetValue = 1.5f,
                animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Restart),
                label = "confetti_scale"
            )
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                repeat(6) { i ->
                    val colors = listOf(Color(0xFFFFB300), Color(0xFF1D9E75), Color(0xFF1565C0),
                        Color(0xFFD85A30), Color(0xFF7F77DD), Color(0xFFE65100))
                    Box(
                        modifier = Modifier
                            .offset(
                                x = ((i % 3 - 1) * 24).dp,
                                y = ((i / 3 - 1) * 24 - 12).dp
                            )
                            .size((16 * scale).dp)
                            .clip(RoundedCornerShape(50))
                            .background(colors[i].copy(alpha = alpha))
                    )
                }
            }
        }

        FallbackType.COMPLETE -> {
            // Star bounce
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f, targetValue = 1.3f,
                animationSpec = infiniteRepeatable(
                    tween(500, easing = EaseInOutBounce), RepeatMode.Reverse
                ), label = "complete_scale"
            )
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                Text("⭐", fontSize = (48 * scale).sp)
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// STEP 7 — Shimmer Loading Placeholder
// Shows animated shimmer while content loads.
// No external library — pure Compose gradient animation.
// Usage: ShimmerBox(modifier = Modifier.fillMaxWidth().height(80.dp))
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp)
) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.surfaceVariant,
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 400f, translateAnim - 400f),
        end = Offset(translateAnim, translateAnim)
    )

    Box(modifier = modifier.clip(shape).background(brush))
}

// Pre-built shimmer layouts for common screens

@Composable
fun LessonCardShimmer(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ShimmerBox(modifier = Modifier.size(44.dp), shape = RoundedCornerShape(22.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.4f).height(12.dp))
                Spacer(Modifier.height(6.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f).height(16.dp))
            }
        }
    }
}

@Composable
fun LessonListShimmer() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        repeat(5) { LessonCardShimmer() }
    }
}

@Composable
fun QuizShimmer() {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.3f).height(22.dp))
        Spacer(Modifier.height(16.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(60.dp))
        Spacer(Modifier.height(24.dp))
        repeat(4) {
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(10.dp))
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// STEP 7 — Haptic Feedback Helpers
// Single call to trigger correct or wrong haptic vibration.
// Wraps platform API differences so callers don't need to check API level.
// Usage:
//   val haptic = LocalHapticFeedback.current
//   haptic.triggerCorrect()   // long press pattern = success feel
//   haptic.triggerWrong()     // reject pattern = error feel
// ─────────────────────────────────────────────────────────────────────────────

fun androidx.compose.ui.hapticfeedback.HapticFeedback.triggerCorrect() {
    performHapticFeedback(HapticFeedbackType.LongPress)
}

fun HapticFeedback.triggerWrong() {
    // Reject is API 34+. Fall back to TextHandleMove on older devices.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        performHapticFeedback(HapticFeedbackType.TextHandleMove)
    } else {
        performHapticFeedback(HapticFeedbackType.LongPress)
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// STEP 7 — Quarter Completion Screen
// Full-screen celebration shown when all lessons in a quarter are DONE.
// Shows confetti animation + score summary + "Continue" button.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun QuarterCompletionScreen(
    quarterTitle: String,
    completedLessons: Int,
    averageScore: Float,
    onContinue: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SafeLottieAnimation(
                assetPath = "lottie/confetti.json",
                fallback = FallbackType.CONFETTI,
                modifier = Modifier.size(200.dp),
                iterations = 3
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Quarter Complete! 🎉",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = quarterTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            // Score summary card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CompletionStat(label = "Lessons\nDone", value = "$completedLessons")
                    CompletionStat(label = "Average\nScore", value = "${(averageScore * 100).toInt()}%")
                    CompletionStat(
                        label = "Status",
                        value = if (averageScore >= 0.6f) "Passed ✓" else "Keep\nPracticing"
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CompletionStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// STEP 7 — Animated Quiz Answer Feedback
// Call after student submits an answer to show correct/wrong animation.
// Wraps content children — apply to the answer card or button.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AnimatedAnswerCard(
    isRevealed: Boolean,
    isCorrect: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    // Trigger haptic when answer is revealed
    LaunchedEffect(isRevealed, isCorrect) {
        if (isRevealed) {
            if (isCorrect) haptic.triggerCorrect() else haptic.triggerWrong()
        }
    }

    // Shake offset for wrong answers
    val shakeOffset by animateFloatAsState(
        targetValue = if (isRevealed && !isCorrect) 1f else 0f,
        animationSpec = if (isRevealed && !isCorrect)
            keyframes {
                durationMillis = 400
                0f at 0
                -12f at 50
                12f at 100
                -12f at 150
                12f at 200
                -8f at 250
                8f at 300
                0f at 400
            }
        else spring(),
        label = "shake"
    )

    // Scale pop for correct answers
    val scaleAnim by animateFloatAsState(
        targetValue = if (isRevealed && isCorrect) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "correct_scale"
    )

    val bgColor by animateColorAsState(
        targetValue = when {
            isRevealed && isCorrect -> MaterialTheme.colorScheme.tertiaryContainer
            isRevealed && !isCorrect -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(300),
        label = "bg_color"
    )

    Box(
        modifier = modifier
            .offset(x = shakeOffset.dp)
            .graphicsLayer(scaleX = scaleAnim, scaleY = scaleAnim)
            .background(bgColor, RoundedCornerShape(12.dp))
    ) {
        content()
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// STEP 7 — Fill-in-the-blank Shake Animation
// Shakes the text field when the answer is wrong.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ShakingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isWrong: Boolean,
    modifier: Modifier = Modifier
) {
    val shakeOffset by animateFloatAsState(
        targetValue = if (isWrong) 1f else 0f,
        animationSpec = if (isWrong)
            keyframes {
                durationMillis = 400
                0f at 0; -10f at 50; 10f at 100; -10f at 150; 10f at 200; 0f at 400
            }
        else spring(),
        label = "shake_field"
    )

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = isWrong,
        modifier = modifier
            .fillMaxWidth()
            .offset(x = shakeOffset.dp),
        shape = RoundedCornerShape(12.dp),
        supportingText = if (isWrong) {
            { Text("Incorrect — try again", color = MaterialTheme.colorScheme.error) }
        } else null
    )
}


// ─────────────────────────────────────────────────────────────────────────────
// STEP 7 — Tablet Two-Pane Layout
// On tablets (width >= 600dp): lesson list and reading pane side by side.
// On phones: standard single-column layout.
// Usage: wrap your content in TwoPaneLayout and provide both panes.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TwoPaneLayout(
    isTablet: Boolean,
    listPane: @Composable () -> Unit,
    detailPane: @Composable () -> Unit,
    showDetail: Boolean = false
) {
    if (isTablet) {
        // Side-by-side on tablet
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.width(340.dp).fillMaxHeight()) {
                listPane()
            }
            VerticalDivider(modifier = Modifier.fillMaxHeight())
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                detailPane()
            }
        }
    } else {
        // Single pane on phone — show detail when selected
        if (showDetail) {
            detailPane()
        } else {
            listPane()
        }
    }
}

// Helper composable to detect if running on tablet
@Composable
fun rememberIsTablet(): Boolean {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    return remember(configuration) {
        configuration.screenWidthDp >= 600
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// STEP 7 — Animated Scroll Progress Indicator
// Thin coloured bar + bouncing "scroll down" chevron shown while the student
// has not yet reached the bottom of the passage.
// Usage: AnimatedScrollIndicator(scrollFraction = scrollState.value / maxValue)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AnimatedScrollIndicator(
    scrollFraction: Float,           // 0f = top, 1f = bottom
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scroll_chevron")
    val chevronOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            tween(600, easing = EaseInOutSine),
            RepeatMode.Reverse
        ),
        label = "chevron_bounce"
    )

    Column(modifier = modifier) {
        // Coloured scroll-progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(scrollFraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
            )
        }

        // Bouncing chevron — only show when not near the bottom
        androidx.compose.animation.AnimatedVisibility(
            visible = scrollFraction < 0.92f,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .offset(y = chevronOffset.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Scroll down to read more",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
// ─────────────────────────────────────────────────────────────────────────────
// BasaHero Greeting Banner
// Shows a friendly greeting + student avatar in a circular frame.
// Optimized for tablet portrait top section.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BasaHeroGreetingBanner(
    studentName: String,
    gradeLevel: Int,
    avatarUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val bannerGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFE0B2).copy(alpha = 0.4f), // Soft Orange
            Color(0xFFC8E6C9).copy(alpha = 0.4f), // Soft Green
            Color(0xFFB3E5FC).copy(alpha = 0.4f)  // Soft Blue
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(bannerGradient)
            .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(32.dp))
            .padding(horizontal = 32.dp, vertical = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Color(0xFF4A90E2), fontWeight = FontWeight.Bold)) {
                            append("Welcome back, ")
                        }
                        withStyle(style = SpanStyle(color = Color(0xFFE67E22), fontWeight = FontWeight.Black)) {
                            append("Hero!")
                        }
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = fredokaFontFamily
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Ready for a new adventure?",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.width(24.dp))

            StudentProfileAvatar(
                imageUrl = avatarUrl,
                gradeLevel = gradeLevel,
                size = 90.dp
            )
        }
    }
}

@Composable
fun StudentProfileAvatar(
    imageUrl: String?,
    gradeLevel: Int,
    size: Dp = 64.dp
) {
    val borderColor = when (gradeLevel) {
        5 -> Color(0xFF4CAF50)
        6 -> Color(0xFFFF9800)
        else -> Color(0xFF2196F3)
    }

    Box(
        modifier = Modifier
            .size(size)
            .border(4.dp, borderColor, CircleShape)
            .padding(4.dp)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Student Avatar",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Default playful hero icon/emoji
            Text(
                text = "🦸",
                fontSize = (size.value * 0.5f).sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BasaHero Bottom Navigation
// Premium colorful bottom navigation with rounded icons.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BasaHeroBottomNavigation(
    selectedRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        NavigationItem("Home", "student_home", "🏠", Color(0xFF4A90E2)),
        NavigationItem("Lessons", "lesson_list/current", "📚", Color(0xFF50E3C2)),
        NavigationItem("Games", "game_join", "🎮", Color(0xFFFFAB40)),
        NavigationItem("Profile", "profile", "👤", Color(0xFFB39DDB))
    )

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp,
        modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        items.forEach { item ->
            val isSelected = selectedRoute.startsWith(item.route)
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) item.color.copy(alpha = 0.2f) else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.icon,
                            fontSize = 24.sp
                        )
                    }
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) item.color else Color.Gray
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

private data class NavigationItem(
    val label: String,
    val route: String,
    val icon: String,
    val color: Color
)

// ─────────────────────────────────────────────────────────────────────────────
// PlayfulBackground
// Animated floating shapes (circles/stars) for a dynamic, non-minimalist feel.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PlayfulBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "floating_shapes")
    val config = LocalConfiguration.current
    val screenWidth = config.screenWidthDp
    val screenHeight = config.screenHeightDp
    
    // ── Animated Shifting Gradient (Stronger Colors) ──
    val color1 by infiniteTransition.animateColor(
        initialValue = Color(0xFFE0F2FE), // Light Blue
        targetValue = Color(0xFFBAE6FD), // Sky Blue (Stronger)
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse),
        label = "c1"
    )
    val color2 by infiniteTransition.animateColor(
        initialValue = Color(0xFFF1F5F9), // Slate
        targetValue = Color(0xFFE2E8F0), // Stronger Slate
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Reverse),
        label = "c2"
    )
    val color3 by infiniteTransition.animateColor(
        initialValue = Color(0xFFDBEAFE), // Soft Blue
        targetValue = Color(0xFFBFDBFE), // Stronger Blue
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse),
        label = "c3"
    )

    // Configuration for floating shapes and letters (Scaled for responsiveness)
    val isTablet = screenWidth >= 600
    val itemScale = if (isTablet) 1f else 0.6f
    val shapeCount = if (isTablet) 15 else 8
    val letterCount = if (isTablet) 40 else 20
    
    val items = remember(screenWidth, screenHeight) {
        val list = mutableListOf<FloatingItem>()
        // Banner area height estimate (220dp for tablet, 160dp for phone)
        val bannerPadding = if (isTablet) 220 else 160
        
        // Add Shapes
        repeat(shapeCount) {
            list.add(FloatingItem(
                type = ItemType.SHAPE,
                initialX = (0..screenWidth).random().toFloat(),
                initialY = (bannerPadding..screenHeight).random().toFloat(),
                size = ((30..100).random() * itemScale).dp,
                color = listOf(
                    Color(0xFF60A5FA), Color(0xFF4ADE80), Color(0xFFFBBF24), Color(0xFFF472B6)
                ).random().copy(alpha = 0.5f), // Stronger opacity
                duration = (5000..10000).random()
            ))
        }
        // Add Letters (Equally scattered left and right)
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val vibrantColors = listOf(
            Color(0xFF2563EB), Color(0xFF16A34A), Color(0xFFD97706), 
            Color(0xFFDB2777), Color(0xFF7C3AED), Color(0xFFEAB308)
        )
        val midPoint = screenWidth / 2
        
        repeat(letterCount) { i ->
            val isLeftSide = i < (letterCount / 2)
            val xRange = if (isLeftSide) (0..midPoint) else (midPoint..screenWidth)
            
            list.add(FloatingItem(
                type = ItemType.LETTER,
                text = alphabet.random().toString(),
                initialX = xRange.random().toFloat(),
                initialY = (bannerPadding..screenHeight).random().toFloat(),
                size = ((40..80).random() * itemScale).dp,
                color = vibrantColors.random().copy(alpha = 0.6f), // Stronger opacity
                duration = (8000..18000).random()
            ))
        }
        list
    }

    Box(modifier = modifier.fillMaxSize().background(Brush.verticalGradient(listOf(color1, color2, color3)))) {
        items.forEach { item ->
            val floatAnim by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 120f,
                animationSpec = infiniteRepeatable(
                    animation = tween(item.duration, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "floating"
            )
            
            val rotateAnim by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(item.duration * 3, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotate"
            )

            Box(
                modifier = Modifier
                    .offset(
                        x = item.initialX.dp,
                        y = item.initialY.dp + floatAnim.dp
                    )
                    .graphicsLayer(rotationZ = rotateAnim),
                contentAlignment = Alignment.Center
            ) {
                if (item.type == ItemType.SHAPE) {
                    Box(
                        modifier = Modifier
                            .size(item.size)
                            .background(item.color, RoundedCornerShape(item.size / 4))
                    )
                } else {
                    Text(
                        text = item.text ?: "",
                        fontSize = item.size.value.sp,
                        fontWeight = FontWeight.Black,
                        color = item.color,
                        fontFamily = fredokaFontFamily
                    )
                }
            }
        }
    }
}

private enum class ItemType { SHAPE, LETTER }

private data class FloatingItem(
    val type: ItemType,
    val initialX: Float,
    val initialY: Float,
    val size: Dp,
    val color: Color,
    val duration: Int,
    val text: String? = null
)
