package com.basahero.elearning.ui.student.prepost

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalConfiguration
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

// Reusing the animated interactive quiz components
import com.basahero.elearning.ui.student.quiz.AnimatedMcqQuestion
import com.basahero.elearning.ui.student.quiz.AnimatedFillInQuestion
import com.basahero.elearning.ui.student.quiz.DragDropSequencingQuestion
import com.basahero.elearning.ui.student.quiz.CanvasMatchingQuestion
import com.basahero.elearning.ui.student.quiz.PassageQuestion

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

// ─────────────────────────────────────────────────────────────────────────────
// Shared test-question composable — phone + tablet adaptive, kid-friendly
// ─────────────────────────────────────────────────────────────────────────────
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
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    val hPad = if (isTablet) 32.dp else 16.dp

    // Animate question transitions
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(300),
        label = "q_alpha"
    )

    Column(modifier = modifier.fillMaxSize()) {

        // ── Colourful info banner ─────────────────────────────────────────────
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = hPad, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (testType == TestType.PRE) "📝" else "🏆",
                    fontSize = 18.sp
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (testType == TestType.PRE)
                        "Answer every question to unlock lessons!"
                    else
                        "Show what you've learned — do your best!",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    lineHeight = 17.sp
                )
            }
        }

        // ── Chunky progress bar ───────────────────────────────────────────────
        LinearProgressIndicator(
            progress = { state.progressFraction },
            modifier = Modifier.fillMaxWidth().height(7.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        // ── Scrollable question body ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = hPad, vertical = 20.dp)
        ) {
            // Question number badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Q ${state.currentIndex + 1} / ${state.questions.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = question.questionType.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))

            // Question text — large for kids
            Text(
                text = question.questionText,
                fontSize = if (isTablet) 20.sp else 18.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = if (isTablet) 30.sp else 27.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(28.dp))

            // ── Question component router ─────────────────────────────────────
            when (question.questionType) {
                QuestionType.MCQ -> AnimatedMcqQuestion(
                    question = question,
                    selectedChoiceId = currentAnswer?.answer,
                    isSubmitted = state.isSubmitted,
                    onChoiceSelected = { choiceId ->
                        onAnswer(question.id, QuizScoringUseCase.StudentAnswer(question.id, choiceId))
                    }
                )
                QuestionType.FILL_IN -> AnimatedFillInQuestion(
                    question = question,
                    currentText = currentAnswer?.answer ?: "",
                    isSubmitted = false,
                    onTextChanged = { text ->
                        onAnswer(question.id, QuizScoringUseCase.StudentAnswer(question.id, text))
                    }
                )
                QuestionType.SEQUENCING -> DragDropSequencingQuestion(
                    question = question,
                    currentOrder = currentAnswer?.selectedChoiceIds
                        ?: remember(question.id) { question.choices.map { it.id }.shuffled() },
                    isSubmitted = state.isSubmitted,
                    onOrderChanged = { newOrder ->
                        onAnswer(question.id, QuizScoringUseCase.StudentAnswer(question.id, "", newOrder))
                    }
                )
                QuestionType.MATCHING -> CanvasMatchingQuestion(
                    question = question,
                    connections = currentAnswer?.selectedChoiceIds ?: emptyList(),
                    isSubmitted = state.isSubmitted,
                    onConnectionMade = { ids ->
                        onAnswer(question.id, QuizScoringUseCase.StudentAnswer(question.id, "", ids))
                    }
                )
                QuestionType.PASSAGE -> PassageQuestion(
                    question = question,
                    selectedWordIds = currentAnswer?.selectedChoiceIds ?: emptyList(),
                    isSubmitted = state.isSubmitted,
                    onSelectionChanged = { ids ->
                        onAnswer(question.id, QuizScoringUseCase.StudentAnswer(question.id, "", ids))
                    }
                )
            }

            Spacer(Modifier.height(16.dp))
        }

        // ── Navigation bar ────────────────────────────────────────────────────
        Surface(
            shadowElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = hPad, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPrevious,
                    enabled = state.currentIndex > 0,
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Back", fontWeight = FontWeight.Medium)
                }

                // Step dots — phone only (too wide for many questions on tablet)
                if (!isTablet && state.questions.size <= 8) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        repeat(state.questions.size) { i ->
                            Box(
                                modifier = Modifier
                                    .size(if (i == state.currentIndex) 10.dp else 7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (i == state.currentIndex) MaterialTheme.colorScheme.primary
                                        else if (state.answers.containsKey(state.questions[i].id))
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                        }
                    }
                }

                if (state.isLastQuestion) {
                    Button(
                        onClick = onSubmit,
                        modifier = Modifier.height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Submit ✓", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onNext,
                        enabled = state.answers.containsKey(question.id),
                        modifier = Modifier.height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Next", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Post-test result — celebration screen for kids
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PostTestResultContent(
    state: PrePostViewModel.PrePostUiState.Ready,
    modifier: Modifier,
    onContinue: () -> Unit
) {
    val percent = if (state.total == 0) 0 else (state.score * 100) / state.total
    val animPercent by animateIntAsState(
        targetValue = percent,
        animationSpec = tween(1200, easing = EaseOutCubic),
        label = "score_count"
    )
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val contentMod = if (isTablet)
            Modifier.width(480.dp)
        else
            Modifier.fillMaxWidth().padding(horizontal = 28.dp)

        Column(
            modifier = contentMod,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Post-Test\nComplete!", fontSize = if (isTablet) 32.sp else 28.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
                lineHeight = 38.sp)

            Spacer(Modifier.height(24.dp))

            // Animated score bubble
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$animPercent%", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary)
                    Text("${state.score}/${state.total} pts", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                }
            }

            Spacer(Modifier.height(20.dp))

            // Star rating
            Row {
                val stars = when { percent >= 80 -> 3; percent >= 60 -> 2; else -> 1 }
                repeat(3) { i -> Text(if (i < stars) "⭐" else "☆", fontSize = 28.sp) }
            }

            Spacer(Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = "Your teacher will compare this with your pre-test score to see how much you've grown! 🌱",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("🏠  Back to Home", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}