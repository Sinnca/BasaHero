package com.basahero.elearning.ui.student.prepost

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basahero.elearning.data.local.entity.AppConstants
import com.basahero.elearning.data.model.*
import com.basahero.elearning.data.repository.PrePostRepository
import com.basahero.elearning.domain.QuizScoringUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// 👇 REUSING YOUR EXISTING QUIZ COMPONENTS!
import com.basahero.elearning.ui.student.quiz.McqQuestion
import com.basahero.elearning.ui.student.quiz.FillInQuestion
import com.basahero.elearning.ui.student.quiz.SequencingQuestion
import com.basahero.elearning.ui.student.quiz.MatchingQuestion

object TestType {
    const val PRE = "PRE"
    const val POST = "POST"
}

// ─────────────────────────────────────────────────────────────────────────────
// PrePostViewModel
// ─────────────────────────────────────────────────────────────────────────────
class PrePostViewModel(
    private val prePostRepository: PrePostRepository,
    private val scoringUseCase: QuizScoringUseCase
) : ViewModel() {

    sealed class PrePostUiState {
        object Loading : PrePostUiState()
        object AlreadyCompleted : PrePostUiState()
        object NoContent : PrePostUiState()
        data class Ready(
            val questions: List<QuizQuestion>,
            val currentIndex: Int = 0,
            val answers: Map<String, QuizScoringUseCase.StudentAnswer> = emptyMap(),
            val isSubmitted: Boolean = false,
            val score: Int = 0,
            val total: Int = 0
        ) : PrePostUiState() {
            val currentQuestion get() = questions.getOrNull(currentIndex)
            val isLastQuestion get() = currentIndex == questions.size - 1
            val progressFraction get() = if (questions.isEmpty()) 0f else (currentIndex + 1).toFloat() / questions.size
        }
    }

    private val _uiState = MutableStateFlow<PrePostUiState>(PrePostUiState.Loading)
    val uiState: StateFlow<PrePostUiState> = _uiState.asStateFlow()

    fun load(studentId: String, quarterId: String, testType: String) {
        viewModelScope.launch {
            val alreadyDone = prePostRepository.isTestCompleted(studentId, quarterId, testType)
            if (alreadyDone) { _uiState.value = PrePostUiState.AlreadyCompleted; return@launch }

            val hasContent = prePostRepository.hasTestContent(quarterId, testType)
            if (!hasContent) { _uiState.value = PrePostUiState.NoContent; return@launch }

            val questions = prePostRepository.getTestQuestions(quarterId, testType)
            _uiState.value = PrePostUiState.Ready(questions = questions)
        }
    }

    fun answer(questionId: String, answer: QuizScoringUseCase.StudentAnswer) {
        val current = _uiState.value as? PrePostUiState.Ready ?: return
        _uiState.value = current.copy(answers = current.answers + (questionId to answer))
    }

    fun next() {
        val current = _uiState.value as? PrePostUiState.Ready ?: return
        _uiState.value = current.copy(currentIndex = current.currentIndex + 1)
    }

    fun previous() {
        val current = _uiState.value as? PrePostUiState.Ready ?: return
        if (current.currentIndex > 0) _uiState.value = current.copy(currentIndex = current.currentIndex - 1)
    }

    fun submit(studentId: String, quarterId: String, testType: String) {
        val current = _uiState.value as? PrePostUiState.Ready ?: return
        viewModelScope.launch {
            val result = scoringUseCase.calculate(
                lessonId = quarterId,
                lessonTitle = "$testType Test",
                questions = current.questions,
                studentAnswers = current.answers
            )
            prePostRepository.saveTestResult(studentId, quarterId, testType, result.score, result.total)
            _uiState.value = current.copy(isSubmitted = true, score = result.score, total = result.total)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PreTestGateScreen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreTestGateScreen(
    quarterId: String,
    quarterTitle: String,
    studentId: String,
    viewModel: PrePostViewModel,
    onPreTestComplete: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(quarterId) {
        viewModel.load(studentId, quarterId, TestType.PRE)
    }

    LaunchedEffect(uiState) {
        if (uiState is PrePostViewModel.PrePostUiState.AlreadyCompleted ||
            uiState is PrePostViewModel.PrePostUiState.NoContent) {
            onPreTestComplete()
        }
        if (uiState is PrePostViewModel.PrePostUiState.Ready) {
            val ready = uiState as PrePostViewModel.PrePostUiState.Ready
            if (ready.isSubmitted) onPreTestComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pre-Test — $quarterTitle") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is PrePostViewModel.PrePostUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is PrePostViewModel.PrePostUiState.Ready -> {
                PrePostTestContent(
                    state = state,
                    testType = TestType.PRE,
                    modifier = Modifier.padding(padding),
                    onAnswer = { qId, answer -> viewModel.answer(qId, answer) },
                    onNext = { viewModel.next() },
                    onPrevious = { viewModel.previous() },
                    onSubmit = { viewModel.submit(studentId, quarterId, TestType.PRE) }
                )
            }
            else -> Unit
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PostTestScreen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostTestScreen(
    quarterId: String,
    quarterTitle: String,
    studentId: String,
    viewModel: PrePostViewModel,
    onPostTestComplete: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(quarterId) {
        viewModel.load(studentId, quarterId, TestType.POST)
    }

    LaunchedEffect(uiState) {
        if (uiState is PrePostViewModel.PrePostUiState.AlreadyCompleted) onPostTestComplete()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post-Test — $quarterTitle") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is PrePostViewModel.PrePostUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is PrePostViewModel.PrePostUiState.Ready -> {
                if (state.isSubmitted) {
                    PostTestResultContent(state = state, modifier = Modifier.padding(padding), onContinue = onPostTestComplete)
                } else {
                    PrePostTestContent(
                        state = state,
                        testType = TestType.POST,
                        modifier = Modifier.padding(padding),
                        onAnswer = { qId, answer -> viewModel.answer(qId, answer) },
                        onNext = { viewModel.next() },
                        onPrevious = { viewModel.previous() },
                        onSubmit = { viewModel.submit(studentId, quarterId, TestType.POST) }
                    )
                }
            }
            else -> Unit
        }
    }
}

// ── Shared test content composable ────────────────────────────────────────────
@Composable
fun PrePostTestContent(
    state: PrePostViewModel.PrePostUiState.Ready,
    testType: String,
    modifier: Modifier,
    onAnswer: (String, QuizScoringUseCase.StudentAnswer) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSubmit: () -> Unit
) {
    val question = state.currentQuestion ?: return
    val currentAnswer = state.answers[question.id]

    Column(modifier = modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (testType == TestType.PRE) "Answer all questions to unlock lessons. Your score won't affect access."
                    else "This post-test measures how much you've learned. Do your best!",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        LinearProgressIndicator(
            progress = { state.progressFraction },
            modifier = Modifier.fillMaxWidth().height(5.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Question ${state.currentIndex + 1} of ${state.questions.size}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text(text = question.questionText, fontSize = 17.sp, fontWeight = FontWeight.Medium, lineHeight = 26.sp)
            Spacer(Modifier.height(24.dp))

            // 👇 ALL 4 QUESTION TYPES RESTORED!
            when (question.questionType) {
                QuestionType.MCQ -> McqQuestion(
                    question = question,
                    selectedChoiceId = currentAnswer?.answer,
                    onChoiceSelected = { choiceId -> onAnswer(question.id, QuizScoringUseCase.StudentAnswer(question.id, choiceId)) }
                )
                QuestionType.FILL_IN -> FillInQuestion(
                    question = question,
                    currentText = currentAnswer?.answer ?: "",
                    onTextChanged = { text -> onAnswer(question.id, QuizScoringUseCase.StudentAnswer(question.id, text)) }
                )
                QuestionType.SEQUENCING -> SequencingQuestion(
                    question = question,
                    currentOrder = currentAnswer?.selectedChoiceIds ?: question.choices.map { it.id }.shuffled(),
                    onOrderChanged = { newOrder -> onAnswer(question.id, QuizScoringUseCase.StudentAnswer(question.id, "", newOrder)) }
                )
                QuestionType.MATCHING -> MatchingQuestion(
                    question = question,
                    selectedIds = currentAnswer?.selectedChoiceIds ?: emptyList(),
                    onSelectionChanged = { ids -> onAnswer(question.id, QuizScoringUseCase.StudentAnswer(question.id, "", ids)) }
                )
            }
        }

        Surface(shadowElevation = 4.dp) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(onClick = onPrevious, enabled = state.currentIndex > 0) {
                    Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp)); Text("Back")
                }
                if (state.isLastQuestion) {
                    Button(onClick = onSubmit, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp)); Text("Submit")
                    }
                } else {
                    Button(onClick = onNext, enabled = state.answers.containsKey(question.id)) {
                        Text("Next"); Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// ── Post-test result screen ───────────────────────────────────────────────────
@Composable
fun PostTestResultContent(
    state: PrePostViewModel.PrePostUiState.Ready,
    modifier: Modifier,
    onContinue: () -> Unit
) {
    val percent = if (state.total == 0) 0 else (state.score * 100) / state.total
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Post-Test Complete! 🎉", fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text("$percent%", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("${state.score} / ${state.total} points", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text(
            "Your teacher will compare this with your pre-test score to see how much you've grown!",
            fontSize = 13.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp)) {
            Text("Back to Home", fontWeight = FontWeight.SemiBold)
        }
    }
}