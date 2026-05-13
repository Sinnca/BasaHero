package com.basahero.elearning.ui.student.prepost

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.basahero.elearning.domain.SyncPrePostUseCase
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
            val currentPageIndex: Int = 0,
            val answers: Map<String, QuizScoringUseCase.StudentAnswer> = emptyMap(),
            val isSubmitted: Boolean = false,
            val score: Int = 0,
            val total: Int = 0
        ) : PrePostUiState() {
            val questionsPerPage = 10
            val totalPages = (questions.size + questionsPerPage - 1) / questionsPerPage
            val currentPageQuestions get() = questions.drop(currentPageIndex * questionsPerPage).take(questionsPerPage)
            val isLastPage get() = currentPageIndex == totalPages - 1
            val progressFraction get() = if (questions.isEmpty()) 0f else (answers.size).toFloat() / questions.size
            
            fun isPageComplete(): Boolean {
                return currentPageQuestions.all { answers.containsKey(it.id) }
            }
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
        if (!current.isLastPage) {
            _uiState.value = current.copy(currentPageIndex = current.currentPageIndex + 1)
        }
    }

    fun previous() {
        val current = _uiState.value as? PrePostUiState.Ready ?: return
        if (current.currentPageIndex > 0) {
            _uiState.value = current.copy(currentPageIndex = current.currentPageIndex - 1)
        }
    }

    fun submit(context: android.content.Context, studentId: String, quarterId: String, testType: String) {
        val current = _uiState.value as? PrePostUiState.Ready ?: return
        viewModelScope.launch {
            val result = scoringUseCase.calculate(
                lessonId = quarterId,
                lessonTitle = "$testType Test",
                questions = current.questions,
                studentAnswers = current.answers
            )
            prePostRepository.saveTestResult(studentId, quarterId, testType, result.score, result.total)
            
            // Trigger sync immediately!
            SyncPrePostUseCase().execute(context)

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
    val context = LocalContext.current

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
                    onSubmit = { viewModel.submit(context, studentId, quarterId, TestType.PRE) }
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
    val context = LocalContext.current

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
                        onSubmit = { viewModel.submit(context, studentId, quarterId, TestType.POST) }
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
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    val hPad = if (isTablet) 32.dp else 16.dp

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
                        "Answer every question to unlock lessons! (Page ${state.currentPageIndex + 1} of ${state.totalPages})"
                    else
                        "Show what you've learned! (Page ${state.currentPageIndex + 1} of ${state.totalPages})",
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

        // ── Scrollable question list for this page ────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = hPad, vertical = 20.dp)
        ) {
            state.currentPageQuestions.forEachIndexed { index, question ->
                val qNumber = (state.currentPageIndex * state.questionsPerPage) + index + 1
                val currentAnswer = state.answers[question.id]

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Column(Modifier.padding(if (isTablet) 20.dp else 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Q $qNumber",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            if (state.answers.containsKey(question.id)) {
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = question.questionText,
                            fontSize = if (isTablet) 18.sp else 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 24.sp
                        )

                        Spacer(Modifier.height(16.dp))

                        // ── Question component router ─────────────────────────────────────
                        when (question.questionType) {
                            QuestionType.MCQ -> AnimatedMcqQuestion(
                                question = question,
                                selectedChoiceId = currentAnswer?.answer,
                                isSubmitted = false,
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
                            else -> Text("Unsupported type in paginated view")
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(32.dp))
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
                    enabled = state.currentPageIndex > 0,
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Back", fontWeight = FontWeight.Medium)
                }

                if (state.isLastPage) {
                    Button(
                        onClick = onSubmit,
                        enabled = state.answers.size == state.questions.size,
                        modifier = Modifier.height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Submit Pre-Test ✓", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onNext,
                        enabled = state.isPageComplete(),
                        modifier = Modifier.height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Next Page", fontWeight = FontWeight.Bold)
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