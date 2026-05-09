package com.basahero.elearning.ui.student.game

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basahero.elearning.data.local.SessionManager
import com.basahero.elearning.data.repository.GameRepository
import com.basahero.elearning.data.repository.GameSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GameJoinUiState(
    val codeDigits: List<String> = listOf("", "", "", ""),
    val isLoading: Boolean = false,
    val error: String? = null,
    val session: GameSession? = null,
    val studentId: String? = null
)

class GameJoinViewModel(
    private val gameRepo: GameRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(GameJoinUiState())
    val uiState: StateFlow<GameJoinUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val session = sessionManager.studentSession.first()
            Log.d(TAG, "init: studentSession=$session, studentId=${session?.studentId}")
            _uiState.update { it.copy(studentId = session?.studentId) }
        }
    }

    fun updateDigit(index: Int, value: String) {
        val newDigits = _uiState.value.codeDigits.toMutableList()
        newDigits[index] = value.take(1)
        _uiState.update { it.copy(codeDigits = newDigits, error = null) }
        
        if (newDigits.all { it.isNotEmpty() }) {
            joinGame(newDigits.joinToString(""))
        }
    }

    fun joinGame(code: String) {
        Log.d(TAG, "joinGame called with code: $code")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val session = withContext(Dispatchers.IO) { gameRepo.getSessionByJoinCode(code) }
            Log.d(TAG, "joinGame: session found = ${session != null}, status = ${session?.status}")
            if (session == null) {
                Log.w(TAG, "joinGame: Game not found for code $code")
                _uiState.update { it.copy(isLoading = false, error = "Game not found") }
            } else if (session.status == "DONE") {
                Log.w(TAG, "joinGame: Game has ended")
                _uiState.update { it.copy(isLoading = false, error = "Game has ended") }
            } else if (session.status == "ACTIVE") {
                Log.w(TAG, "joinGame: Game already started")
                _uiState.update { it.copy(isLoading = false, error = "Game already started") }
            } else {
                val studentId = _uiState.value.studentId
                Log.d(TAG, "joinGame: studentId = $studentId")
                if (studentId != null) {
                    Log.d(TAG, "joinGame: Submitting JOIN answer for session=${session.id}, student=$studentId")
                    withContext(Dispatchers.IO) {
                        gameRepo.submitAnswer(session.id, studentId, GameRepository.JOIN_MARKER_ID, "", false, 0)
                    }
                    Log.d(TAG, "joinGame: JOIN answer submitted successfully")
                    _uiState.update { it.copy(isLoading = false, session = session) }
                } else {
                    Log.w(TAG, "joinGame: studentId is NULL - not logged in")
                    _uiState.update { it.copy(isLoading = false, error = "Not logged in") }
                }
            }
        }
    }

    companion object {
        private const val TAG = "GameJoinViewModel"
    }
}

@Composable
fun GameJoinScreen(
    viewModel: GameJoinViewModel,
    onJoined: (String, String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequesters = remember { List(4) { FocusRequester() } }

    LaunchedEffect(uiState.session, uiState.studentId) {
        val session = uiState.session
        val studentId = uiState.studentId
        if (session != null && studentId != null) {
            onJoined(session.id, studentId)
        }
    }

    val isTablet = LocalConfiguration.current.screenWidthDp >= 600

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF673AB7) // Deep Purple Theme
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Enter Game PIN", fontSize = if (isTablet) 42.sp else 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Look at the teacher's screen to get the 4-digit PIN.", fontSize = if (isTablet) 20.sp else 16.sp, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
            
            Spacer(modifier = Modifier.height(48.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                for (i in 0..3) {
                    OutlinedTextField(
                        value = uiState.codeDigits[i],
                        onValueChange = { newValue ->
                            if (newValue.length <= 1) {
                                viewModel.updateDigit(i, newValue)
                                if (newValue.isNotEmpty() && i < 3) {
                                    focusRequesters[i + 1].requestFocus()
                                }
                            }
                        },
                        modifier = Modifier
                            .width(if (isTablet) 96.dp else 72.dp)
                            .height(if (isTablet) 120.dp else 84.dp)
                            .focusRequester(focusRequesters[i])
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.key == Key.Backspace && uiState.codeDigits[i].isEmpty() && i > 0) {
                                    focusRequesters[i - 1].requestFocus()
                                    true
                                } else false
                            },
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontSize = if (isTablet) 48.sp else 32.sp,
                            fontWeight = FontWeight.Black
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator(color = Color.White)
            }

            if (uiState.error != null) {
                Surface(color = Color.White, shape = RoundedCornerShape(8.dp)) {
                    Text(uiState.error!!, color = Color.Red, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onBack, 
                modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth().height(64.dp), 
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f), contentColor = Color.White)
            ) {
                Text("Cancel", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
