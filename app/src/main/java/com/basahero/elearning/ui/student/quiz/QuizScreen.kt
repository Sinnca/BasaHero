package com.basahero.elearning.ui.student.quiz

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basahero.elearning.data.model.*
import com.basahero.elearning.data.model.QuizResult
import com.basahero.elearning.domain.QuizScoringUseCase

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

    LaunchedEffect(lessonId) {
        viewModel.loadQuiz(lessonId, lessonTitle)
    }

    LaunchedEffect(uiState.result) {
        uiState.result?.let { onQuizComplete(it) }
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
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
            // Progress bar
            LinearProgressIndicator(
                progress = { uiState.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            val question = uiState.currentQuestion ?: return@Column
            val currentAnswer = uiState.answers[question.id]

            // Question content
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

                // Render correct question type component
                when (question.questionType) {
                    "MCQ" -> McqQuestion(
                        question = question,
                        selectedChoiceId = currentAnswer?.answer,
                        onChoiceSelected = { choiceId ->
                            viewModel.answerQuestion(
                                question.id,
                                QuizScoringUseCase.StudentAnswer(question.id, choiceId)
                            )
                        }
                    )
                    "FILL_IN" -> FillInQuestion(
                        question = question,
                        currentText = currentAnswer?.answer ?: "",
                        onTextChanged = { text ->
                            viewModel.answerQuestion(
                                question.id,
                                QuizScoringUseCase.StudentAnswer(question.id, text)
                            )
                        }
                    )
                    "SEQUENCING" -> SequencingQuestion(
                        question = question,
                        currentOrder = currentAnswer?.selectedChoiceIds ?: question.choices.map { it.id }.shuffled(),
                        onOrderChanged = { newOrder ->
                            viewModel.answerQuestion(
                                question.id,
                                QuizScoringUseCase.StudentAnswer(question.id, "", newOrder)
                            )
                        }
                    )
                    "MATCHING" -> MatchingQuestion(
                        question = question,
                        selectedIds = currentAnswer?.selectedChoiceIds ?: emptyList(),
                        onSelectionChanged = { ids ->
                            viewModel.answerQuestion(
                                question.id,
                                QuizScoringUseCase.StudentAnswer(question.id, "", ids)
                            )
                        }
                    )
                }
            }

            // Navigation buttons
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
                            onClick = { viewModel.submitQuiz(lessonId) },
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
                            onClick = { viewModel.nextQuestion() },
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


// ─────────────────────────────────────────────────────────────────────────────
// MCQ Question Component
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun McqQuestion(
    question: QuizQuestion,
    selectedChoiceId: String?,
    onChoiceSelected: (String) -> Unit
) {
    val shuffledChoices = remember(question.id) { question.choices.shuffled() }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        shuffledChoices.forEach { choice ->
            val isSelected = choice.id == selectedChoiceId
            Card(
                onClick = { onChoiceSelected(choice.id) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onChoiceSelected(choice.id) }
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = choice.choiceText,
                        fontSize = 15.sp,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// Fill-in-the-blank Question Component
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun FillInQuestion(
    question: QuizQuestion,
    currentText: String,
    onTextChanged: (String) -> Unit
) {
    Column {
        OutlinedTextField(
            value = currentText,
            onValueChange = onTextChanged,
            label = { Text("Your answer") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = false,
            minLines = 2
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Type your answer in the box above.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// Sequencing Question Component
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SequencingQuestion(
    question: QuizQuestion,
    currentOrder: List<String>,
    onOrderChanged: (List<String>) -> Unit
) {
    val orderedChoices = remember(currentOrder) {
        currentOrder.mapNotNull { id -> question.choices.firstOrNull { it.id == id } }
    }.toMutableStateList()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Use ↑↓ buttons to reorder:",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        orderedChoices.forEachIndexed { index, choice ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "${index + 1}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = choice.choiceText,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Column {
                        IconButton(
                            onClick = {
                                if (index > 0) {
                                    val newList = orderedChoices.toMutableList()
                                    val temp = newList[index]
                                    newList[index] = newList[index - 1]
                                    newList[index - 1] = temp
                                    onOrderChanged(newList.map { it.id })
                                }
                            },
                            enabled = index > 0,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up",
                                modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = {
                                if (index < orderedChoices.size - 1) {
                                    val newList = orderedChoices.toMutableList()
                                    val temp = newList[index]
                                    newList[index] = newList[index + 1]
                                    newList[index + 1] = temp
                                    onOrderChanged(newList.map { it.id })
                                }
                            },
                            enabled = index < orderedChoices.size - 1,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down",
                                modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// Matching Question Component
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MatchingQuestion(
    question: QuizQuestion,
    selectedIds: List<String>,
    onSelectionChanged: (List<String>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Select all the correct matches:",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        question.choices.forEach { choice ->
            val isSelected = choice.id in selectedIds
            Card(
                onClick = {
                    val newSelection = selectedIds.toMutableList()
                    if (isSelected) newSelection.remove(choice.id)
                    else newSelection.add(choice.id)
                    onSelectionChanged(newSelection)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { checked ->
                            val newSelection = selectedIds.toMutableList()
                            if (checked) newSelection.add(choice.id)
                            else newSelection.remove(choice.id)
                            onSelectionChanged(newSelection)
                        }
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = choice.choiceText,
                        fontSize = 14.sp,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}