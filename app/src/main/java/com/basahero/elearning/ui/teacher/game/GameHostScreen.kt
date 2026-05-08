package com.basahero.elearning.ui.teacher.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.basahero.elearning.data.model.QuizQuestion
import com.basahero.elearning.data.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class GamePhase { LOBBY, QUESTION, REVEAL, DONE }

data class LeaderboardEntry(
    val studentId: String,
    val studentName: String,
    val correctCount: Int,
    val totalAnswered: Int,
    val averageResponseMs: Int
)

data class GameHostUiState(
    val session: GameSession? = null,
    val connectedStudents: List<StudentInfo> = emptyList(),
    val currentQuestion: QuizQuestion? = null,
    val answers: List<GameAnswer> = emptyList(),
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val isLoading: Boolean = false,
    val phase: GamePhase = GamePhase.LOBBY
)

class GameHostViewModel(
    private val gameRepo: GameRepository,
    private val classRepo: ClassRepository,
    private val quizRepo: QuizRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(GameHostUiState())
    val uiState: StateFlow<GameHostUiState> = _uiState.asStateFlow()

    private var sessionJob: Job? = null
    private var answersJob: Job? = null
    private var allQuestions: List<QuizQuestion> = emptyList()

    fun startGame(classId: String, lessonId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            var questions = withContext(Dispatchers.IO) {
                quizRepo.getQuizForLesson(lessonId).filter { it.questionType == "MCQ" }
            }
            if (questions.isEmpty()) {
                questions = listOf(
                    com.basahero.elearning.data.model.QuizQuestion(
                        id = java.util.UUID.randomUUID().toString(),
                        lessonId = lessonId,
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
            
            val session = withContext(Dispatchers.IO) {
                gameRepo.createGameSession(classId, lessonId, questions.map { it.id })
            }
            
            _uiState.update { it.copy(session = session, isLoading = false) }
            
            sessionJob = launch {
                gameRepo.observeSession(session.id).collect { updatedSession ->
                    _uiState.update { it.copy(session = updatedSession) }
                }
            }
            
            answersJob = launch {
                gameRepo.observeAnswers(session.id).collect { answers ->
                    val students = withContext(Dispatchers.IO) { classRepo.getStudentsForClass(classId) }
                    val joinedStudentIds = answers.filter { it.questionId == GameRepository.JOIN_MARKER_ID }.map { it.studentId }.distinct()
                    
                    val connected: List<StudentInfo> = joinedStudentIds.map { sId ->
                        students.find { it.id == sId } ?: StudentInfo(
                            id = sId,
                            classId = classId,
                            fullName = "Guest ${sId.take(4)}",
                            section = "Unknown",
                            gradeLevel = 0,
                            lastActive = null,
                            isAtRisk = false
                        )
                    }
                    
                    _uiState.update { it.copy(
                        answers = answers,
                        connectedStudents = connected
                    ) }
                    calculateLeaderboard()
                }
            }
        }
    }

    private fun calculateLeaderboard() {
        val state = _uiState.value
        val answers = state.answers.filter { it.questionId != GameRepository.JOIN_MARKER_ID }
        val students = state.connectedStudents
        
        val leaderboard = students.map { student ->
            val studentAnswers = answers.filter { it.studentId == student.id }
            val correctCount = studentAnswers.count { it.isCorrect }
            val avgTime = if (studentAnswers.isEmpty()) 0 else studentAnswers.map { it.responseTimeMs }.average().toInt()
            LeaderboardEntry(student.id, student.fullName, correctCount, studentAnswers.size, avgTime)
        }.sortedWith(compareByDescending<LeaderboardEntry> { it.correctCount }.thenBy { it.averageResponseMs })
        
        _uiState.update { it.copy(leaderboard = leaderboard) }
    }

    fun nextQuestion() {
        viewModelScope.launch {
            val session = _uiState.value.session ?: return@launch
            val nextIndex = if (session.currentQuestionId == null) 0 else session.questionIndex + 1
            if (nextIndex < session.questionOrder.size) {
                val nextQId = session.questionOrder[nextIndex]
                val question = allQuestions.find { it.id == nextQId }
                _uiState.update { it.copy(currentQuestion = question, phase = GamePhase.QUESTION) }
                withContext(Dispatchers.IO) {
                    gameRepo.advanceQuestion(session.id, nextQId, nextIndex)
                }
            } else {
                endGame()
            }
        }
    }

    fun revealAnswer() {
        _uiState.update { it.copy(phase = GamePhase.REVEAL) }
    }

    fun endGame() {
        viewModelScope.launch {
            _uiState.value.session?.id?.let { withContext(Dispatchers.IO) { gameRepo.endSession(it) } }
            _uiState.update { it.copy(phase = GamePhase.DONE) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sessionJob?.cancel()
        answersJob?.cancel()
        // End the session even if the ViewModel is destroyed (e.g. app closed)
        _uiState.value.session?.id?.let { sessionId ->
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                gameRepo.endSession(sessionId)
            }
        }
    }
}

@Composable
fun GameHostScreen(
    classId: String,
    lessonId: String,
    viewModel: GameHostViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startGame(classId, lessonId)
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (uiState.phase) {
            GamePhase.LOBBY -> LobbyPhase(uiState, onStart = { viewModel.nextQuestion() })
            GamePhase.QUESTION -> QuestionPhase(uiState, onReveal = { viewModel.revealAnswer() })
            GamePhase.REVEAL -> RevealPhase(uiState, onNext = { viewModel.nextQuestion() })
            GamePhase.DONE -> DonePhase(uiState, onBack = onBack)
        }
    }
}

@Composable
fun ColumnScope.LobbyPhase(uiState: GameHostUiState, onStart: () -> Unit) {
    Text("Join Code", fontSize = 24.sp, color = Color.Gray)
    Text(uiState.session?.joinCode ?: "----", fontSize = 64.sp, fontWeight = FontWeight.Bold, letterSpacing = 8.sp)
    
    Spacer(modifier = Modifier.height(32.dp))
    
    Box(modifier = Modifier.size(200.dp).background(Color.LightGray, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
        Text("QR Code Placeholder", color = Color.DarkGray)
    }
    
    Spacer(modifier = Modifier.height(32.dp))
    
    Text("${uiState.connectedStudents.size} Students Joined", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    
    LazyColumn(modifier = Modifier.weight(1f).padding(vertical = 16.dp)) {
        items(uiState.connectedStudents) { student ->
            Text(student.fullName, fontSize = 18.sp, modifier = Modifier.padding(4.dp))
        }
    }
    
    Button(
        onClick = onStart,
        enabled = uiState.connectedStudents.isNotEmpty(),
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Text("Start Game", fontSize = 18.sp)
    }
}

@Composable
fun ColumnScope.QuestionPhase(uiState: GameHostUiState, onReveal: () -> Unit) {
    val answersForCurrentQ = uiState.answers.count { it.questionId == uiState.currentQuestion?.id }
    val totalStudents = uiState.connectedStudents.size

    var timeLeft by remember { mutableFloatStateOf(15f) }
    val animatedProgress by animateFloatAsState(targetValue = timeLeft / 15f, label = "timer")
    
    LaunchedEffect(uiState.currentQuestion) {
        timeLeft = 15f
        while (timeLeft > 0) {
            kotlinx.coroutines.delay(100)
            timeLeft -= 0.1f
        }
    }

    Text("Question ${uiState.session?.questionIndex?.plus(1) ?: 1}", fontSize = 20.sp, color = Color.Gray)
    Spacer(modifier = Modifier.height(16.dp))
    Text(uiState.currentQuestion?.questionText ?: "", fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    
    Spacer(modifier = Modifier.height(32.dp))
    
    LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(8.dp)))
    
    Spacer(modifier = Modifier.weight(1f))
    
    Text("$answersForCurrentQ of $totalStudents answered", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    
    Spacer(modifier = Modifier.height(32.dp))
    
    Button(onClick = onReveal, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Text("Reveal Answer", fontSize = 18.sp)
    }
}

@Composable
fun ColumnScope.RevealPhase(uiState: GameHostUiState, onNext: () -> Unit) {
    Text("Correct Answer", fontSize = 20.sp, color = Color.Gray)
    Spacer(modifier = Modifier.height(16.dp))
    
    Text(uiState.currentQuestion?.questionText ?: "", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    Spacer(modifier = Modifier.height(24.dp))
    
    Text("Answer revealed on student devices", fontSize = 18.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
    
    Spacer(modifier = Modifier.height(32.dp))
    
    Text("Current Top 5", fontSize = 20.sp, fontWeight = FontWeight.Bold)
    LazyColumn(modifier = Modifier.weight(1f).padding(vertical = 16.dp)) {
        items(uiState.leaderboard.take(5)) { entry ->
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(entry.studentName, fontSize = 18.sp)
                Text("${entry.correctCount} pts", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
    
    val isLast = (uiState.session?.questionIndex ?: 0) >= (uiState.session?.questionOrder?.size?.minus(1) ?: 0)
    Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Text(if (isLast) "End Game" else "Next Question", fontSize = 18.sp)
    }
}

@Composable
fun ColumnScope.DonePhase(uiState: GameHostUiState, onBack: () -> Unit) {
    Text("Final Leaderboard", fontSize = 32.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(24.dp))
    
    LazyColumn(modifier = Modifier.weight(1f)) {
        items(uiState.leaderboard.withIndex().toList()) { (index, entry) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).background(if (index < 3) Color(0xFFFFF8E1) else Color.Transparent, RoundedCornerShape(8.dp)).padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("#${index + 1}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (index == 0) Color(0xFFFFD700) else Color.DarkGray, modifier = Modifier.width(40.dp))
                    Text(entry.studentName, fontSize = 18.sp)
                }
                Text("${entry.correctCount} correct", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
    
    Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Text("Back to Dashboard", fontSize = 18.sp)
    }
}
