package com.basahero.elearning.ui.student.quiz

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basahero.elearning.ui.common.FallbackType
import com.basahero.elearning.ui.common.QuarterCompletionScreen
import com.basahero.elearning.ui.common.SafeLottieAnimation
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// QuizResultScreen — kid-friendly, phone + tablet adaptive
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizResultScreen(
    studentId: String,
    lessonId: String,
    score: Int,
    total: Int,
    viewModel: QuizResultViewModel,
    onGoHome: () -> Unit,
    onNextLesson: (lessonId: String) -> Unit,
    onRetry: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600

    val percentage = if (total > 0) (score.toFloat() / total) * 100 else 0f
    val passed = percentage >= 60f

    // Quarter celebration overlay
    if (passed && uiState.isQuarterComplete) {
        QuarterCompletionScreen(
            quarterTitle = uiState.quarterTitle,
            completedLessons = uiState.quarterLessonsCompleted,
            averageScore = uiState.quarterAverageScore,
            onContinue = onGoHome
        )
        return
    }

    LaunchedEffect(lessonId) { viewModel.saveResult(context, studentId, lessonId, score, total) }

    // Entrance animation
    val enterAlpha = remember { Animatable(0f) }
    val enterScale = remember { Animatable(0.85f) }
    LaunchedEffect(Unit) {
        launch { enterAlpha.animateTo(1f, tween(500)) }
        launch { enterScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quiz Result", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onGoHome) {
                        Icon(Icons.Default.Home, "Home")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            if (passed) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                        )
                    )
                )
        ) {
            val contentModifier = if (isTablet)
                Modifier.width(520.dp).align(Alignment.TopCenter)
            else
                Modifier.fillMaxWidth()

            Column(
                modifier = contentModifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .graphicsLayer { alpha = enterAlpha.value; scaleX = enterScale.value; scaleY = enterScale.value },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(12.dp))

                // Celebration / result animation
                SafeLottieAnimation(
                    assetPath = if (passed) "lottie/confetti.json" else "lottie/try_again.json",
                    fallback = if (passed) FallbackType.COMPLETE else FallbackType.WRONG,
                    modifier = Modifier.size(140.dp),
                    iterations = 2
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = if (passed) "Amazing Work! 🌟" else "Good Try! 💪",
                    fontSize = if (isTablet) 30.sp else 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )

                if (!passed) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            "You need at least 60% to continue.\nYou can do it — try again!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Animated score circle
                AnimatedScoreCircle(score = score, total = total, passed = passed)

                Spacer(Modifier.height(32.dp))

                // Stars row for motivation
                if (passed) {
                    val starCount = when {
                        percentage >= 90 -> 3
                        percentage >= 75 -> 2
                        else             -> 1
                    }
                    Row(horizontalArrangement = Arrangement.Center) {
                        repeat(3) { i ->
                            Text(
                                text = if (i < starCount) "⭐" else "☆",
                                fontSize = 32.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // Action buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (passed && uiState.nextLessonId != null) {
                        Button(
                            onClick = { onNextLesson(uiState.nextLessonId!!) },
                            enabled = uiState.isSaved,
                            modifier = Modifier.fillMaxWidth().height(58.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Next Lesson", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(20.dp))
                        }
                    }

                    if (!passed) {
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.fillMaxWidth().height(58.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Try Again! 🔁", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onRetry,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Practice Again", fontSize = 15.sp)
                        }
                    }

                    TextButton(
                        onClick = onGoHome,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Default.Home, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Back to Lessons", fontSize = 15.sp)
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Animated score circle — counts up to the score
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AnimatedScoreCircle(score: Int, total: Int, passed: Boolean) {
    val percent = if (total == 0) 0 else (score * 100) / total
    val color   = if (passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    val animPercent by animateIntAsState(
        targetValue = percent,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label = "score_count"
    )

    Box(
        modifier = Modifier
            .size(160.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .border(5.dp, color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$animPercent%",
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = "$score / $total pts",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ScoreCircle kept for backward compatibility
@Composable
fun ScoreCircle(score: Int, total: Int, passed: Boolean) = AnimatedScoreCircle(score, total, passed)