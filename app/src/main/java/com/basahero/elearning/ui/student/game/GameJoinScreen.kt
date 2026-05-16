package com.basahero.elearning.ui.student.game

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideogameAsset
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
import com.basahero.elearning.data.repository.StudentRepository
import com.basahero.elearning.data.model.Student
import com.basahero.elearning.ui.theme.fredokaFontFamily
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GameJoinUiState(
    val codeDigits: List<String> = listOf("", "", "", ""),
    val isLoading: Boolean = false,
    val error: String? = null,
    val session: GameSession? = null,
    val studentId: String? = null,
    val studentName: String? = null,
    val section: String? = null,
    val classId: String? = null
)

class GameJoinViewModel(
    private val gameRepo: GameRepository,
    private val sessionManager: SessionManager,
    private val studentRepo: StudentRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(GameJoinUiState())
    val uiState: StateFlow<GameJoinUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val session = sessionManager.studentSession.first()
            Log.d(TAG, "init: studentSession=$session, studentId=${session?.studentId}")
            _uiState.update { it.copy(
                studentId = session?.studentId, 
                studentName = session?.studentName, 
                section = session?.section,
                classId = session?.classId
            ) }
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
            } else if (session.classId != _uiState.value.classId) {
                Log.w(TAG, "joinGame: Student classId (${_uiState.value.classId}) does not match session classId (${session.classId})")
                _uiState.update { it.copy(isLoading = false, error = "This game is for another section!") }
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
    val scrollState = rememberScrollState()

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
        color = Color(0xFF1C122B) // Slightly lightened deep purple
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle star background simulation
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(Color.White.copy(0.1f), 4f, androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.15f))
                drawCircle(Color.White.copy(0.15f), 6f, androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.2f))
                drawCircle(Color.White.copy(0.08f), 3f, androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.35f))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(vertical = if (isTablet) 64.dp else 48.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${uiState.section?.uppercase() ?: "4A"} • GAME JOIN — ENTER CODE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(if (isTablet) 80.dp else 48.dp))

                // Gamepad Icon with glow effect
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.VideogameAsset,
                        contentDescription = "Game",
                        tint = Color(0xFF8B5CF6),
                        modifier = Modifier.size(if (isTablet) 80.dp else 64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "GAME TIME!",
                    fontSize = if (isTablet) 48.sp else 36.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = fredokaFontFamily,
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.White.copy(alpha = 0.5f),
                            offset = Offset(0f, 0f),
                            blurRadius = 30f
                        )
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enter the code from your teacher's screen",
                    fontSize = if (isTablet) 18.sp else 16.sp,
                    color = Color(0xFFA090B0),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(if (isTablet) 64.dp else 40.dp))

                if (uiState.session == null) {
                    // PIN INPUT
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(if (isTablet) 24.dp else 16.dp),
                        modifier = Modifier.wrapContentSize()
                    ) {
                        for (i in 0..3) {
                            val digit = uiState.codeDigits[i]
                            
                            Box(
                                modifier = Modifier
                                    .width(if (isTablet) 80.dp else 64.dp)
                                    .height(if (isTablet) 110.dp else 88.dp)
                                    .background(Color(0xFF251A35), RoundedCornerShape(16.dp))
                                    .border(
                                        width = if (digit.isNotEmpty()) 3.dp else if (uiState.codeDigits.indexOfFirst { it.isEmpty() } == i) 2.dp else 2.dp,
                                        color = if (digit.isNotEmpty()) Color(0xFFFFD700) else if (uiState.codeDigits.indexOfFirst { it.isEmpty() } == i) Color(0xFF8B5CF6) else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (digit.isEmpty() && uiState.codeDigits.indexOfFirst { it.isEmpty() } != i) {
                                    Canvas(modifier = Modifier.matchParentSize()) {
                                        drawRoundRect(
                                            color = Color(0xFF4B3A65),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                width = 4f,
                                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f))
                                            ),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                                        )
                                    }
                                }

                                BasicTextField(
                                    value = digit,
                                    onValueChange = { newValue ->
                                        if (newValue.length <= 1) {
                                            viewModel.updateDigit(i, newValue)
                                            if (newValue.isNotEmpty() && i < 3) {
                                                focusRequesters[i + 1].requestFocus()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .focusRequester(focusRequesters[i])
                                        .onKeyEvent { keyEvent ->
                                            if (keyEvent.key == Key.Backspace && digit.isEmpty() && i > 0) {
                                                focusRequesters[i - 1].requestFocus()
                                                true
                                            } else false
                                        },
                                    textStyle = TextStyle(
                                        fontFamily = fredokaFontFamily,
                                        textAlign = TextAlign.Center,
                                        fontSize = if (isTablet) 48.sp else 36.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFFFD700)
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    decorationBox = { innerTextField ->
                                        Box(contentAlignment = Alignment.Center) {
                                            innerTextField()
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Success State
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            color = Color(0xFF22C55E).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "SUCCESSFULLY JOINED!",
                                color = Color(0xFF22C55E),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                letterSpacing = 1.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 0.7f,
                            targetValue = 1.3f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulseAnim"
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .graphicsLayer {
                                        scaleX = pulseScale
                                        scaleY = pulseScale
                                    }
                                    .background(Color(0xFF22C55E), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Waiting for teacher to start...", 
                                color = Color.White, 
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "Playing as: ",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = uiState.studentName ?: "Student",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(48.dp))

                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color(0xFFFFD700))
                } else if (uiState.error != null) {
                    Text(uiState.error!!, color = Color.Red, fontWeight = FontWeight.Bold)
                }
                
                // Bottom padding for scrollability
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
