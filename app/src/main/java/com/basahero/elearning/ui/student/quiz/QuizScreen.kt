package com.basahero.elearning.ui.student.quiz

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basahero.elearning.data.model.*
import com.basahero.elearning.data.model.QuizResult
import com.basahero.elearning.domain.QuizScoringUseCase
import com.basahero.elearning.ui.common.QuizShimmer
import com.basahero.elearning.ui.common.triggerCorrect
import com.basahero.elearning.ui.common.triggerWrong

// ─────────────────────────────────────────────────────────────────────────────
// QuizScreen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    lessonId: String,
    lessonTitle: String,
    viewModel: QuizViewModel,
    onQuizComplete: (QuizResult) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(lessonId) {
        viewModel.loadQuiz(lessonId, lessonTitle)
    }

    LaunchedEffect(uiState.result) {
        uiState.result?.let { onQuizComplete(it) }
    }

    // ── Shimmer while loading ──────────────────────────────────────────────────
    if (uiState.isLoading) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Quiz", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Loading questions…", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.Close, contentDescription = "Exit Quiz")
                        }
                    }
                )
            }
        ) { padding ->
            QuizShimmer()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Quiz", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Question ${uiState.currentIndex + 1} of ${uiState.questions.size}",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Exit Quiz")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Animated progress bar ─────────────────────────────────────────
            val animatedProgress by animateFloatAsState(
                targetValue = uiState.progressFraction,
                animationSpec = tween(durationMillis = 400, easing = EaseOutCubic),
                label = "quiz_progress"
            )
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            val question = uiState.currentQuestion ?: return@Column
            val currentAnswer = uiState.answers[question.id]

            // ── Question content ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Points badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "${question.pointsValue} pt${if (question.pointsValue > 1) "s" else ""}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = question.questionType.replace("_", " "),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Question text
                Text(
                    text = question.questionText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 26.sp
                )

                Spacer(Modifier.height(24.dp))

                // ── Route to the correct animated question component ──────────
                when (question.questionType) {
                    QuestionType.MCQ -> AnimatedMcqQuestion(
                        question = question,
                        selectedChoiceId = currentAnswer?.answer,
                        isSubmitted = uiState.isSubmitted,
                        onChoiceSelected = { choiceId ->
                            if (!uiState.isSubmitted) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.answerQuestion(
                                    question.id,
                                    QuizScoringUseCase.StudentAnswer(question.id, choiceId)
                                )
                            }
                        }
                    )

                    QuestionType.FILL_IN -> AnimatedFillInQuestion(
                        question = question,
                        currentText = currentAnswer?.answer ?: "",
                        isSubmitted = uiState.isSubmitted,
                        onTextChanged = { text ->
                            viewModel.answerQuestion(
                                question.id,
                                QuizScoringUseCase.StudentAnswer(question.id, text)
                            )
                        }
                    )

                    QuestionType.SEQUENCING -> DragDropSequencingQuestion(
                        question = question,
                        currentOrder = currentAnswer?.selectedChoiceIds
                            ?: remember(question.id) { question.choices.map { it.id }.shuffled() },
                        isSubmitted = uiState.isSubmitted,
                        onOrderChanged = { newOrder ->
                            viewModel.answerQuestion(
                                question.id,
                                QuizScoringUseCase.StudentAnswer(question.id, "", newOrder)
                            )
                        }
                    )

                    QuestionType.MATCHING -> CanvasMatchingQuestion(
                        question = question,
                        connections = currentAnswer?.selectedChoiceIds ?: emptyList(),
                        isSubmitted = uiState.isSubmitted,
                        onConnectionMade = { ids ->
                            viewModel.answerQuestion(
                                question.id,
                                QuizScoringUseCase.StudentAnswer(question.id, "", ids)
                            )
                        }
                    )

                    QuestionType.PASSAGE -> PassageQuestion(
                        question = question,
                        selectedWordIds = currentAnswer?.selectedChoiceIds ?: emptyList(),
                        isSubmitted = uiState.isSubmitted,
                        onSelectionChanged = { ids ->
                            viewModel.answerQuestion(
                                question.id,
                                QuizScoringUseCase.StudentAnswer(question.id, "", ids)
                            )
                        }
                    )

                    // Fallback for legacy string types still in DB
                    "MCQ" -> AnimatedMcqQuestion(
                        question = question,
                        selectedChoiceId = currentAnswer?.answer,
                        isSubmitted = uiState.isSubmitted,
                        onChoiceSelected = { choiceId ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.answerQuestion(
                                question.id,
                                QuizScoringUseCase.StudentAnswer(question.id, choiceId)
                            )
                        }
                    )
                    "FILL_IN" -> AnimatedFillInQuestion(
                        question = question,
                        currentText = currentAnswer?.answer ?: "",
                        isSubmitted = uiState.isSubmitted,
                        onTextChanged = { text ->
                            viewModel.answerQuestion(
                                question.id,
                                QuizScoringUseCase.StudentAnswer(question.id, text)
                            )
                        }
                    )
                    "SEQUENCING" -> DragDropSequencingQuestion(
                        question = question,
                        currentOrder = currentAnswer?.selectedChoiceIds
                            ?: remember(question.id) { question.choices.map { it.id }.shuffled() },
                        isSubmitted = uiState.isSubmitted,
                        onOrderChanged = { newOrder ->
                            viewModel.answerQuestion(
                                question.id,
                                QuizScoringUseCase.StudentAnswer(question.id, "", newOrder)
                            )
                        }
                    )
                    "MATCHING" -> CanvasMatchingQuestion(
                        question = question,
                        connections = currentAnswer?.selectedChoiceIds ?: emptyList(),
                        isSubmitted = uiState.isSubmitted,
                        onConnectionMade = { ids ->
                            viewModel.answerQuestion(
                                question.id,
                                QuizScoringUseCase.StudentAnswer(question.id, "", ids)
                            )
                        }
                    )
                }
            }

            // ── Navigation buttons ────────────────────────────────────────────
            Surface(shadowElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { viewModel.previousQuestion() },
                        enabled = uiState.currentIndex > 0
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Back")
                    }

                    if (uiState.isLastQuestion) {
                        Button(
                            onClick = {
                                // Haptic feedback on submit
                                if (uiState.answers.containsKey(question.id)) {
                                    haptic.triggerCorrect()
                                }
                                viewModel.submitQuiz(lessonId)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Submit Quiz")
                        }
                    } else {
                        Button(
                            onClick = {
                                if (uiState.answers.containsKey(question.id)) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                viewModel.nextQuestion()
                            },
                            enabled = uiState.answers.containsKey(question.id)
                        ) {
                            Text("Next")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null,
                                modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}