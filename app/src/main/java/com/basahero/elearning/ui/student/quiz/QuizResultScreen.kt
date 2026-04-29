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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basahero.elearning.data.repository.LessonRepository
import com.basahero.elearning.data.repository.ProgressRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// QuizResultViewModel
// ─────────────────────────────────────────────────────────────────────────────
class QuizResultViewModel(
    private val progressRepository: ProgressRepository,
    private val lessonRepository: LessonRepository
) : ViewModel() {

    data class ResultUiState(
        val isSaved: Boolean = false,
        val nextLessonId: String? = null
    )

    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    fun saveResult(studentId: String, lessonId: String, score: Int, total: Int) {
        viewModelScope.launch {
            // 1. Save quiz result to Room
            progressRepository.saveQuizResult(
                studentId = studentId,
                lessonId = lessonId,
                score = score,
                total = total
            )

            // 2. Find next lesson to unlock
            val currentLesson = lessonRepository.getLessonById(lessonId)
            if (currentLesson != null) {
                val lessons = lessonRepository
                    .getLessonsWithStatus(currentLesson.quarterId, studentId)
                    .first()

                val currentIndex = lessons.indexOfFirst { it.id == lessonId }
                val nextLesson = lessons.getOrNull(currentIndex + 1)

                _uiState.value = ResultUiState(
                    isSaved = true,
                    nextLessonId = nextLesson?.id
                )
            }
        }
    }
}

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

    // Calculate pass/fail
    val percentage = if (total > 0) (score.toFloat() / total) * 100 else 0f
    val passed = percentage >= 60f

    // Save result once on screen entry
    LaunchedEffect(lessonId) {
        viewModel.saveResult(studentId, lessonId, score, total)
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

            // Static Trophy Icon (Replaces Lottie to prevent crashes)
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

            // Pass/Fail indicator
            Text(
                text = if (passed) "Great job! 🎉" else "Keep trying! 💪",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (passed) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Score circle
            ScoreCircle(
                score = score,
                total = total,
                passed = passed
            )

            Spacer(Modifier.height(48.dp))

            // Action buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Next lesson button
                if (uiState.nextLessonId != null) {
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

                // Retry button
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Try Again", fontSize = 16.sp)
                }

                // Go home
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

// ── Score Circle ──────────────────────────────────────────────────────────────
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