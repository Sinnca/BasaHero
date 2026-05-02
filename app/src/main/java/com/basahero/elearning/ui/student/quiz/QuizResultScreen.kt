package com.basahero.elearning.ui.student.quiz

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// QuizResultScreen
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

    // Phase 3B Logic: 60% threshold Check
    val percentage = if (total > 0) (score.toFloat() / total) * 100 else 0f
    val passed = percentage >= 60f

    LaunchedEffect(lessonId) {
        viewModel.saveResult(context, studentId, lessonId, score, total)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quiz Result") },
                navigationIcon = {
                    IconButton(onClick = onGoHome) {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(150.dp)
                    .background(
                        color = if (passed) Color(0xFFFFD700).copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Trophy",
                    modifier = Modifier.size(80.dp),
                    tint = if (passed) Color(0xFFFFB300) else MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = if (passed) "Great job! 🎉" else "Keep trying! 💪",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (passed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            // Show a specific prompt if they failed
            if (!passed) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "You need at least 60% to unlock the next lesson.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(32.dp))

            ScoreCircle(
                score = score,
                total = total,
                passed = passed
            )

            Spacer(Modifier.height(48.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 🚀 PHASE 3B UPDATE: Only show Next Lesson button IF they passed!
                if (passed && uiState.nextLessonId != null) {
                    Button(
                        onClick = { onNextLesson(uiState.nextLessonId!!) },
                        enabled = uiState.isSaved,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Next Lesson", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }

                // If they failed, highlight the Retry button by making it filled instead of outlined
                if (!passed) {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Try Again", fontSize = 16.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = onRetry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Take Quiz Again", fontSize = 16.sp)
                    }
                }

                TextButton(
                    onClick = onGoHome,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Back to Lessons", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun ScoreCircle(score: Int, total: Int, passed: Boolean) {
    val percent = if (total == 0) 0 else (score * 100) / total
    val color = if (passed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error

    Box(
        modifier = Modifier
            .size(140.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .border(4.dp, color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$percent%",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
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