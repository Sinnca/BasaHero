package com.basahero.elearning.ui.student.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basahero.elearning.data.model.QuizChoice
import com.basahero.elearning.data.model.QuizQuestion
import com.basahero.elearning.data.repository.GameRepository
import com.basahero.elearning.data.repository.GameSession
import com.basahero.elearning.data.repository.QuizRepository
import com.basahero.elearning.ui.teacher.game.GamePhase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GamePlayUiState(
    val session: GameSession? = null,
    val currentQuestion: QuizQuestion? = null,
    val choices: List<QuizChoice> = emptyList(),
    val selectedAnswerId: String? = null,
    val hasAnswered: Boolean = false,
    val timeRemaining: Int = 15,
    val phase: GamePhase = GamePhase.LOBBY,
    val personalScore: Int = 0,
    val totalAnswered: Int = 0
)

class GamePlayViewModel(
    private val gameRepo: GameRepository,
    private val quizRepo: QuizRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(GamePlayUiState())
    val uiState: StateFlow<GamePlayUiState> = _uiState.asStateFlow()

    private var sessionJob: Job? = null
    private var answersJob: Job? = null
    private var currentStudentId: String = ""
    private var allQuestions: List<QuizQuestion> = emptyList()

    fun startPlaying(sessionId: String, studentId: String) {
        currentStudentId = studentId
        sessionJob = viewModelScope.launch {
            gameRepo.observeSession(sessionId).collect { session ->
                if (allQuestions.isEmpty()) {
                    var questions = withContext(Dispatchers.IO) { quizRepo.getQuizForLesson(session.lessonId) }
                    if (questions.isEmpty()) {
                        questions = listOf(
                            com.basahero.elearning.data.model.QuizQuestion(
                                id = session.questionOrder.firstOrNull() ?: java.util.UUID.randomUUID().toString(),
                                lessonId = session.lessonId,
                                questionText = "What is the capital of the Philippines?",
                                questionType = "MCQ",
                                orderIndex = 0,
                                pointsValue = 10,
                                choices = listOf(
                                    com.basahero.elearning.data.model.QuizChoice(java.util.UUID.randomUUID().toString(), "", "Manila", true, 0),
                                    com.basahero.elearning.data.model.QuizChoice(java.util.UUID.randomUUID().toString(), "", "Cebu", false, 1)
                                )
                            )
                        )
                    }
                    allQuestions = questions
                }
                
                val prevPhase = _uiState.value.phase
                val newPhase = when (session.status) {
                    "WAITING" -> GamePhase.LOBBY
                    "ACTIVE" -> {
                        if (_uiState.value.currentQuestion?.id != session.currentQuestionId && session.currentQuestionId != null) {
                            GamePhase.QUESTION
                        } else {
                            if (session.currentQuestionId != null) GamePhase.QUESTION else GamePhase.LOBBY
                        }
                    }
                    "DONE" -> GamePhase.DONE
                    else -> GamePhase.LOBBY
                }

                if (newPhase == GamePhase.QUESTION && _uiState.value.currentQuestion?.id != session.currentQuestionId && session.currentQuestionId != null) {
                    loadQuestion(session.currentQuestionId)
                }
                
                _uiState.update { it.copy(session = session, phase = if(newPhase == GamePhase.LOBBY && prevPhase == GamePhase.QUESTION) GamePhase.REVEAL else newPhase) }
            }
        }

        answersJob = viewModelScope.launch {
            gameRepo.observeAnswers(sessionId).collect { answers ->
                val myAnswers = answers.filter { it.studentId == studentId && it.questionId != GameRepository.JOIN_MARKER_ID }
                val score = myAnswers.count { it.isCorrect }
                _uiState.update { it.copy(personalScore = score, totalAnswered = myAnswers.size) }
            }
        }
    }

    private fun loadQuestion(questionId: String) {
        val question = allQuestions.find { it.id == questionId }
        val choices = question?.choices ?: emptyList()
        _uiState.update { 
            it.copy(
                    currentQuestion = question,
                    choices = choices,
                    selectedAnswerId = null,
                    hasAnswered = false,
                    timeRemaining = 15,
                phase = GamePhase.QUESTION
            )
        }
    }

    fun submitAnswer(choiceId: String) {
        if (_uiState.value.hasAnswered || _uiState.value.phase != GamePhase.QUESTION) return
        
        _uiState.update { it.copy(selectedAnswerId = choiceId, hasAnswered = true, phase = GamePhase.REVEAL) }
        
        viewModelScope.launch {
            val state = _uiState.value
            val session = state.session ?: return@launch
            val choice = state.choices.find { it.id == choiceId }
            val isCorrect = choice?.isCorrect == true
            val responseTimeMs = 15000 - (state.timeRemaining * 1000)
            
            withContext(Dispatchers.IO) {
                gameRepo.submitAnswer(
                    sessionId = session.id,
                    studentId = currentStudentId,
                    questionId = state.currentQuestion?.id ?: "",
                    answerGiven = choice?.choiceText ?: "",
                    isCorrect = isCorrect,
                    responseTimeMs = responseTimeMs
                )
            }
        }
    }

    fun tickTimer() {
        if (!_uiState.value.hasAnswered && _uiState.value.timeRemaining > 0) {
            _uiState.update { it.copy(timeRemaining = it.timeRemaining - 1) }
        } else if (!_uiState.value.hasAnswered && _uiState.value.timeRemaining == 0) {
            _uiState.update { it.copy(hasAnswered = true, phase = GamePhase.REVEAL) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sessionJob?.cancel()
        answersJob?.cancel()
    }
}

@Composable
fun GamePlayScreen(
    sessionId: String,
    studentId: String,
    viewModel: GamePlayViewModel,
    onGameEnded: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startPlaying(sessionId, studentId)
    }

    LaunchedEffect(uiState.phase, uiState.hasAnswered) {
        if (uiState.phase == GamePhase.QUESTION && !uiState.hasAnswered) {
            while (uiState.timeRemaining > 0 && !uiState.hasAnswered) {
                delay(1000)
                viewModel.tickTimer()
            }
        }
    }

    LaunchedEffect(uiState.phase) {
        if (uiState.phase == GamePhase.DONE) {
            onGameEnded()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (uiState.phase) {
            GamePhase.LOBBY -> {
                Text("Waiting for teacher to start...", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                CircularProgressIndicator(modifier = Modifier.padding(top = 32.dp))
            }
            GamePhase.QUESTION, GamePhase.REVEAL -> {
                QuestionPlayPhase(uiState, onAnswerSelected = { viewModel.submitAnswer(it) })
            }
            GamePhase.DONE -> {
                Text("Waiting for results...", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun QuestionPlayPhase(uiState: GamePlayUiState, onAnswerSelected: (String) -> Unit) {
    val animatedProgress by animateFloatAsState(targetValue = uiState.timeRemaining / 15f, label = "timer")
    Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = Color.LightGray,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = if (uiState.timeRemaining > 5) Color(0xFF1565C0) else Color.Red,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(uiState.timeRemaining.toString(), fontSize = 32.sp, fontWeight = FontWeight.Bold)
    }
    
    Spacer(modifier = Modifier.height(32.dp))
    
    Text(uiState.currentQuestion?.questionText ?: "", fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    
    Spacer(modifier = Modifier.height(48.dp))
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        uiState.choices.forEach { choice ->
            val isSelected = uiState.selectedAnswerId == choice.id
            val isCorrect = choice.isCorrect
            
            val backgroundColor = when {
                uiState.phase == GamePhase.REVEAL && isCorrect -> Color(0xFF4CAF50)
                uiState.phase == GamePhase.REVEAL && isSelected && !isCorrect -> Color(0xFFF44336)
                isSelected -> Color(0xFFE3F2FD)
                uiState.hasAnswered -> Color(0xFFF5F5F5)
                else -> Color.White
            }
            
            val textColor = when {
                uiState.phase == GamePhase.REVEAL && (isCorrect || isSelected) -> Color.White
                else -> Color.Black
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundColor)
                    .clickable(enabled = !uiState.hasAnswered) {
                        onAnswerSelected(choice.id)
                    }
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(choice.choiceText, fontSize = 18.sp, color = textColor, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    
    if (uiState.phase == GamePhase.REVEAL) {
        Spacer(modifier = Modifier.height(32.dp))
        val selectedChoice = uiState.choices.find { it.id == uiState.selectedAnswerId }
        if (selectedChoice != null) {
            if (selectedChoice.isCorrect) {
                Text("Correct! +1", color = Color(0xFF4CAF50), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            } else {
                Text("Incorrect", color = Color(0xFFF44336), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Text("Time's Up!", color = Color(0xFFF44336), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Text("Waiting for next question...", fontSize = 16.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
    }
}
