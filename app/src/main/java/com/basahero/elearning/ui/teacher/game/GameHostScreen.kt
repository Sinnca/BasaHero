package com.basahero.elearning.ui.teacher.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideogameAsset
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
import com.basahero.elearning.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.from
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
    val phase: GamePhase = GamePhase.LOBBY,
    val titleText: String = "Loading..."
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
            
            // Fetch dynamic class details
            val classDetails = withContext(Dispatchers.IO) {
                try {
                    val row = SupabaseClient.client.from("class").select { filter { eq("id", classId) } }.decodeSingleOrNull<ClassRow>()
                    if (row != null) "Grade ${row.grade_level} - ${row.name}" else "Class Game"
                } catch (e: Exception) {
                    "Class Game"
                }
            }
            _uiState.update { it.copy(titleText = classDetails) }

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

    private var isSessionHandled = false

    fun endGame() {
        if (isSessionHandled) return
        isSessionHandled = true
        viewModelScope.launch {
            val state = _uiState.value
            state.session?.id?.let { sessionId ->
                withContext(Dispatchers.IO) {
                    if (state.phase == GamePhase.LOBBY) {
                        gameRepo.deleteSession(sessionId)
                    } else {
                        gameRepo.endSession(sessionId)
                    }
                }
            }
            _uiState.update { it.copy(phase = GamePhase.DONE) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sessionJob?.cancel()
        answersJob?.cancel()
        
        if (isSessionHandled) return
        isSessionHandled = true
        
        // Clean up the session even if the ViewModel is destroyed (e.g. app closed)
        val state = _uiState.value
        state.session?.id?.let { sessionId ->
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                if (state.phase == GamePhase.LOBBY) {
                    gameRepo.deleteSession(sessionId)
                } else {
                    gameRepo.endSession(sessionId)
                }
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
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF511D89)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF511D89) // Deep Purple Background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VideogameAsset, contentDescription = null, tint = Color(0xFFB39DDB), modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Game Mode", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                    if (uiState.titleText.isNotEmpty()) {
                        Text(uiState.titleText, fontSize = 14.sp, color = Color(0xFFD1C4E9), modifier = Modifier.padding(start = 36.dp))
                    }
                }
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (uiState.phase) {
                GamePhase.LOBBY -> LobbyPhase(uiState, onStart = { viewModel.nextQuestion() })
                GamePhase.QUESTION -> QuestionPhase(uiState, onReveal = { viewModel.revealAnswer() })
                GamePhase.REVEAL -> RevealPhase(uiState, onNext = { viewModel.nextQuestion() })
                GamePhase.DONE -> DonePhase(uiState, onBack = onBack)
            }
        }
    }
}

@Composable
fun ColumnScope.LobbyPhase(uiState: GameHostUiState, onStart: () -> Unit) {
    val totalExpectedStudents = 32 // Hardcoded for demo, normally from class details

    // PIN Card
    Surface(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        color = Color(0xFF6A35A3),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 32.dp, horizontal = 16.dp)
        ) {
            Text("Join Code", fontSize = 16.sp, color = Color(0xFFD1C4E9))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = (uiState.session?.joinCode ?: "----").toList().joinToString(" "), 
                fontSize = 72.sp, 
                fontWeight = FontWeight.Black, 
                color = Color(0xFFFFEB3B), // Bright Yellow
                letterSpacing = 8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Students enter this code to join", fontSize = 14.sp, color = Color(0xFFD1C4E9))
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    // Students Joined Progress
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Students Joined (${uiState.connectedStudents.size}/$totalExpectedStudents)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        val progress = if (totalExpectedStudents > 0) uiState.connectedStudents.size.toFloat() / totalExpectedStudents else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
            color = Color(0xFF69F0AE), // Bright Green
            trackColor = Color.White
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Avatar Row
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val displayLimit = 5
        val displayedStudents = uiState.connectedStudents.take(displayLimit)
        val overflow = uiState.connectedStudents.size - displayLimit

        displayedStudents.forEach { student ->
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(40.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color(0xFF8E24AA)),
                contentAlignment = Alignment.Center
            ) {
                val initials = student.fullName.split(" ").take(2).joinToString("") { it.firstOrNull()?.uppercase() ?: "" }
                Text(initials, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (overflow > 0) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color(0xFF7E57C2)),
                contentAlignment = Alignment.Center
            ) {
                Text("+$overflow", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    Spacer(modifier = Modifier.weight(1f))

    // Start Button
    Button(
        onClick = onStart,
        enabled = uiState.connectedStudents.isNotEmpty(),
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFFD700), // Yellow
            contentColor = Color.Black,
            disabledContainerColor = Color(0xFF5E35B1),
            disabledContentColor = Color(0xFF9575CD)
        )
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Start Game", fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text("Live via Supabase Realtime • Internet required", fontSize = 12.sp, color = Color(0xFFB39DDB), modifier = Modifier.align(Alignment.CenterHorizontally))
    Spacer(modifier = Modifier.height(16.dp))
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
        onReveal() // Auto-reveal when timer hits 0
    }

    Text("Question ${uiState.session?.questionIndex?.plus(1) ?: 1}", fontSize = 20.sp, color = Color.Gray)
    Spacer(modifier = Modifier.height(16.dp))
    Text(uiState.currentQuestion?.questionText ?: "", fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    
    Spacer(modifier = Modifier.height(32.dp))
    
    LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(8.dp)))
    
    Spacer(modifier = Modifier.weight(1f))
    
    Text("$answersForCurrentQ of $totalStudents answered", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    
    Spacer(modifier = Modifier.height(32.dp))
}

@Composable
fun ColumnScope.RevealPhase(uiState: GameHostUiState, onNext: () -> Unit) {
    LaunchedEffect(uiState.currentQuestion) {
        kotlinx.coroutines.delay(5000) // Show leaderboard for 5 seconds
        onNext() // Auto-advance to next question or end game
    }

    Text("Correct Answer", fontSize = 24.sp, color = Color(0xFFD1C4E9))
    Spacer(modifier = Modifier.height(16.dp))
    
    Text(uiState.currentQuestion?.questionText ?: "", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
    Spacer(modifier = Modifier.height(24.dp))
    
    Surface(
        color = Color(0xFF6A35A3),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
    ) {
        Text("Answer revealed on student devices", fontSize = 18.sp, color = Color(0xFFFFEB3B), textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
    }
    
    Spacer(modifier = Modifier.height(48.dp))
    
    Text("Current Top 5", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFD700))
    LazyColumn(
        modifier = Modifier.weight(1f).padding(vertical = 16.dp).widthIn(max = 600.dp)
    ) {
        items(uiState.leaderboard.take(5).withIndex().toList()) { (index, entry) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(16.dp), 
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("#${index + 1}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFEB3B), modifier = Modifier.width(40.dp))
                    Text(entry.studentName, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color.White)
                }
                Text("${entry.correctCount} pts", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun ColumnScope.DonePhase(uiState: GameHostUiState, onBack: () -> Unit) {
    Text("Final Leaderboard", fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFD700))
    Spacer(modifier = Modifier.height(32.dp))
    
    LazyColumn(
        modifier = Modifier.weight(1f).widthIn(max = 800.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        items(uiState.leaderboard.withIndex().toList()) { (index, entry) ->
            val isTop3 = index < 3
            val (bgColor, textColor, badgeColor) = when (index) {
                0 -> Triple(Color(0xFFFFD700).copy(alpha = 0.2f), Color(0xFFFFD700), Color(0xFFFFD700)) // Gold
                1 -> Triple(Color(0xFFE0E0E0).copy(alpha = 0.2f), Color.White, Color(0xFFE0E0E0)) // Silver
                2 -> Triple(Color(0xFFFF8A65).copy(alpha = 0.2f), Color.White, Color(0xFFFF8A65)) // Bronze
                else -> Triple(Color(0xFF7E57C2).copy(alpha = 0.3f), Color.White, Color.Transparent)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .background(bgColor, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(if (isTop3) badgeColor else Color.Transparent, androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "#${index + 1}", 
                            fontSize = 24.sp, 
                            fontWeight = FontWeight.Black, 
                            color = if (isTop3) Color(0xFF511D89) else Color(0xFFB39DDB)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(entry.studentName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColor)
                }
                Text("${entry.correctCount} pts", fontSize = 28.sp, fontWeight = FontWeight.Black, color = textColor)
            }
        }
    }
    
    Button(
        onClick = onBack, 
        modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF511D89))
    ) {
        Text("Back to Dashboard", fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}
