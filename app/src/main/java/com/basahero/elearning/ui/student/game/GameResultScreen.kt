package com.basahero.elearning.ui.student.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basahero.elearning.data.repository.GameRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GameResultUiState(
    val score: Int = 0,
    val totalQuestions: Int = 0,
    val rank: Int = 0,
    val totalStudents: Int = 0,
    val isLoading: Boolean = true
)

class GameResultViewModel(private val gameRepo: GameRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(GameResultUiState())
    val uiState: StateFlow<GameResultUiState> = _uiState.asStateFlow()

    fun loadResults(sessionId: String, studentId: String) {
        viewModelScope.launch {
            gameRepo.observeSession(sessionId).take(1).collect { session ->
                gameRepo.observeAnswers(sessionId).take(1).collect { answers ->
                     val allStudents = answers.filter { it.questionId != GameRepository.JOIN_MARKER_ID }.map { it.studentId }.distinct()
                     val leaderboard = allStudents.map { sId ->
                         val sAnswers = answers.filter { it.studentId == sId && it.questionId != GameRepository.JOIN_MARKER_ID }
                         val correct = sAnswers.count { it.isCorrect }
                         val time = if (sAnswers.isEmpty()) 0 else sAnswers.map { it.responseTimeMs }.average().toInt()
                         Triple(sId, correct, time)
                     }.sortedWith(compareByDescending<Triple<String, Int, Int>> { it.second }.thenBy { it.third })
     
                     val myIndex = leaderboard.indexOfFirst { it.first == studentId }
                     val rank = if (myIndex != -1) myIndex + 1 else 0
     
                     val myAnswers = answers.filter { it.studentId == studentId && it.questionId != GameRepository.JOIN_MARKER_ID }
                     val score = myAnswers.count { it.isCorrect }
                     
                     _uiState.update { it.copy(
                         score = score,
                         totalQuestions = session.questionOrder.size,
                         rank = rank,
                         totalStudents = allStudents.size,
                         isLoading = false
                     ) }
                }
            }
        }
    }
}

@Composable
fun GameResultScreen(
    sessionId: String,
    studentId: String,
    viewModel: GameResultViewModel,
    onBackToHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadResults(sessionId, studentId)
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(if (uiState.rank in 1..3) "🎉" else "🏆", fontSize = 80.sp)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Game Over!", fontSize = 36.sp, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Your Score: ${uiState.score} / ${uiState.totalQuestions}", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (uiState.rank > 0) {
            Text("You ranked #${uiState.rank} out of ${uiState.totalStudents} students", fontSize = 20.sp, color = Color.DarkGray)
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(onClick = onBackToHome, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Back to Home", fontSize = 18.sp)
        }
    }
}
