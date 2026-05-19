package com.basahero.elearning.ui.student.lessons

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.basahero.elearning.audio.SpeechRecognitionEngine
import com.basahero.elearning.util.VoskManager
import com.basahero.elearning.ui.theme.fredokaFontFamily
import com.basahero.elearning.ui.student.lessons.components.MascotState
import com.basahero.elearning.ui.student.lessons.components.SparkleParticleState
import com.basahero.elearning.ui.common.squishClickable
import kotlinx.coroutines.launch
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// HighlightedPassageText - Premium guided passage with read-aloud tracking
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HighlightedPassageText(
    passageText: String,
    highlightedWords: List<HighlightedWord>,
    highlightColor: Color = MaterialTheme.colorScheme.primary,
    isTablet: Boolean = false,
    modifier: Modifier = Modifier,
    onPronunciationAttempt: (word: String, heard: String, isCorrect: Boolean, attemptNumber: Int) -> Unit = { _, _, _, _ -> },
    onMascotStateChanged: (MascotState, String?) -> Unit = { _, _ -> },
    particleState: SparkleParticleState? = null
) {
    var selectedWord by remember { mutableStateOf<String?>(null) }
    var showSheet by remember { mutableStateOf(false) }

    // Build the passage highlights (Moved here to support smooth coordinate highlights)
    val annotatedText = remember(passageText, highlightedWords) {
        val baseAnnotated = com.basahero.elearning.util.TextUtil.parseBoldText(passageText)
        val cleanedText = baseAnnotated.text
        
        buildAnnotatedString {
            append(baseAnnotated)
            
            highlightedWords.forEach { hw ->
                val startIndex = cleanedText.indexOf(hw.word, ignoreCase = true)
                if (startIndex >= 0) {
                    val endIndex = startIndex + hw.word.length
                    addStyle(
                        style = SpanStyle(
                            color = highlightColor,
                            fontWeight = FontWeight.ExtraBold,
                            textDecoration = TextDecoration.Underline
                        ),
                        start = startIndex,
                        end = endIndex
                    )
                    addStringAnnotation(tag = "WORD", annotation = hw.word, start = startIndex, end = endIndex)
                }
            }
        }
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // TTS Audio read-along states
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }
    var isReadingAloud by remember { mutableStateOf(false) }
    var activeSpeechRange by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    // Dynamic Speech Pacing Settings (Slow, A Bit Slow, Normal)
    var selectedSpeedRate by remember { mutableStateOf(0.65f) }
    var isSpeedMenuExpanded by remember { mutableStateOf(false) }

    // Liquid-smooth gliding highlight coordinate targets
    var targetLeft by remember { mutableStateOf(0f) }
    var targetTop by remember { mutableStateOf(0f) }
    var targetWidth by remember { mutableStateOf(0f) }
    var targetHeight by remember { mutableStateOf(0f) }

    // Compute exact bounding coordinates for the ENTIRE spoken word (start to end)
    LaunchedEffect(activeSpeechRange, textLayoutResult) {
        val layout = textLayoutResult
        val range = activeSpeechRange
        if (range != null && layout != null) {
            val start = range.first
            val end = range.second
            if (start in 0..annotatedText.length && end in 0..annotatedText.length) {
                try {
                    // Span highlight across all characters in the active word
                    val startBox = layout.getBoundingBox(start)
                    val endBox = layout.getBoundingBox((end - 1).coerceAtLeast(start))
                    targetLeft = startBox.left
                    targetTop = startBox.top
                    targetWidth = endBox.right - startBox.left
                    targetHeight = startBox.bottom - startBox.top
                } catch (e: Exception) {
                    try {
                        val path = layout.getPathForRange(start, end)
                        val bounds = path.getBounds()
                        targetLeft = bounds.left
                        targetTop = bounds.top
                        targetWidth = bounds.width
                        targetHeight = bounds.height
                    } catch (ex: Exception) {}
                }
            }
        }
    }

    // Organic sliding physics engine for guiding early reader focus
    val animatedLeft by animateFloatAsState(
        targetValue = targetLeft,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "highlightLeft"
    )
    val animatedTop by animateFloatAsState(
        targetValue = targetTop,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "highlightTop"
    )
    val animatedWidth by animateFloatAsState(
        targetValue = targetWidth,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "highlightWidth"
    )
    val animatedHeight by animateFloatAsState(
        targetValue = targetHeight,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "highlightHeight"
    )
    
    // Smooth opacity fade when starting/stopping speech
    val targetAlpha = if (activeSpeechRange != null) 0.55f else 0f
    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 200),
        label = "highlightAlpha"
    )

    // Synchronized progress listener to slide the active word highlighter
    val utteranceListener = remember {
        object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                coroutineScope.launch {
                    isReadingAloud = true
                    onMascotStateChanged(MascotState.PRACTICE_SPEECH, "Read along silently and practice following the words, Hero!")
                }
            }
            
            override fun onDone(utteranceId: String?) {
                coroutineScope.launch {
                    isReadingAloud = false
                    activeSpeechRange = null
                    onMascotStateChanged(MascotState.FOCUS_READING, null)
                }
            }
            
            override fun onError(utteranceId: String?) {
                coroutineScope.launch {
                    isReadingAloud = false
                    activeSpeechRange = null
                    onMascotStateChanged(MascotState.FOCUS_READING, null)
                }
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                coroutineScope.launch {
                    // Return range bounds to Compose thread
                    activeSpeechRange = Pair(start, end)
                }
            }
        }
    }

    // Warm up engines once when entering passage screen
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(selectedSpeedRate) // Slower speech rate for Grade 4-6 student comprehension!
                tts?.setOnUtteranceProgressListener(utteranceListener)
                isTtsReady = true
            }
        }
        VoskManager.initModel(context)
    }

    // Instantly update speech rate on-the-fly when student toggles speed options
    LaunchedEffect(selectedSpeedRate) {
        if (isTtsReady) {
            tts?.setSpeechRate(selectedSpeedRate)
        }
    }

    // Safely shutdown TTS on dispose
    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    Column(modifier = modifier) {
        // Playful Speaker Header Row (SplashLearn style!)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Lesson Passage",
                fontFamily = fredokaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = if (isTablet) 20.sp else 13.sp, // Highly compact on mobile!
                color = MaterialTheme.colorScheme.secondary
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isTablet) 8.dp else 4.dp) // Tighter spacing on mobile!
            ) {
                // Compact Dropdown Speed Selector Box
                Box(modifier = Modifier.wrapContentSize()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .squishClickable(enabled = true) {
                                isSpeedMenuExpanded = true
                            }
                            .padding(horizontal = if (isTablet) 10.dp else 6.dp, vertical = if (isTablet) 8.dp else 6.dp), // Tighter padding on mobile!
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(if (isTablet) 4.dp else 2.dp)
                        ) {
                            val speedLabel = when (selectedSpeedRate) {
                                0.52f -> "🐢 Slow"
                                0.65f -> "🐰 Medium"
                                else -> "⚡ Normal"
                            }
                            Text(
                                text = speedLabel,
                                fontFamily = fredokaFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isTablet) 12.sp else 9.sp, // Smaller font on mobile!
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select speed",
                                modifier = Modifier.size(if (isTablet) 16.dp else 12.dp), // Smaller icon on mobile!
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Compact Material DropdownMenu
                    DropdownMenu(
                        expanded = isSpeedMenuExpanded,
                        onDismissRequest = { isSpeedMenuExpanded = false },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        listOf(
                            Triple("Slow", 0.52f, "🐢"),
                            Triple("Medium", 0.65f, "🐰"),
                            Triple("Normal", 0.82f, "⚡")
                        ).forEach { (label, rate, emoji) ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(emoji, fontSize = 14.sp)
                                        Text(
                                            text = label,
                                            fontFamily = fredokaFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (selectedSpeedRate == rate) MaterialTheme.colorScheme.primary 
                                                    else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                },
                                onClick = {
                                    selectedSpeedRate = rate
                                    isSpeedMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Bouncy physical Read-Aloud Button
                Button(
                    onClick = {
                        if (isReadingAloud) {
                            tts?.stop()
                            isReadingAloud = false
                            activeSpeechRange = null
                            onMascotStateChanged(MascotState.FOCUS_READING, null)
                        } else {
                            val params = android.os.Bundle()
                            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "passage_speech")
                            val cleanTextForSpeech = annotatedText.text
                                .replace('—', ' ') // Em-dash to space
                                .replace('–', ' ') // En-dash to space
                                .replace(Regex("(?<=\\s)-(?=\\s|$)"), " ") // Standalone hyphens
                                .replace(Regex("[-_]{2,}")) { match -> " ".repeat(match.value.length) } // Multiple hyphens/underscores to spaces
                            
                            tts?.speak(cleanTextForSpeech, TextToSpeech.QUEUE_FLUSH, params, "passage_speech")
                        }
                    },
                    modifier = Modifier
                        .squishClickable(enabled = isTtsReady) {}
                        .height(if (isTablet) 44.dp else 34.dp), // Sized down to 34.dp on mobile!
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isReadingAloud) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (isReadingAloud) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = if (isTablet) 14.dp else 8.dp, vertical = 0.dp), // Tighter padding on mobile!
                    enabled = isTtsReady
                ) {
                    Icon(
                        imageVector = if (isReadingAloud) Icons.Default.Stop else Icons.Default.VolumeUp,
                        contentDescription = "Read passage aloud",
                        modifier = Modifier.size(if (isTablet) 18.dp else 14.dp) // Smaller icon on mobile!
                    )
                    Spacer(Modifier.width(if (isTablet) 6.dp else 4.dp))
                    Text(
                        text = if (isReadingAloud) "Stop" else if (isTablet) "Read Aloud" else "Read", // Shortened label to "Read" on mobile!
                        fontSize = if (isTablet) 14.sp else 10.sp, // Smaller font on mobile!
                        fontFamily = fredokaFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Active highlighted sliding capsule custom draw behind
        ClickableText(
            text = annotatedText,
            style = TextStyle(
                fontFamily = fredokaFontFamily,
                fontSize = if (isTablet) 18.sp else 16.sp,
                lineHeight = if (isTablet) 30.sp else 26.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                textAlign = TextAlign.Start
            ),
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    if (animatedAlpha > 0f) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFFF59D).copy(alpha = animatedAlpha), 
                                    Color(0xFFFFD54F).copy(alpha = animatedAlpha)
                                ) // Glowing Yellow/Gold
                            ),
                            topLeft = Offset(animatedLeft - 4.dp.toPx(), animatedTop - 2.dp.toPx()),
                            size = Size(animatedWidth + 8.dp.toPx(), animatedHeight + 4.dp.toPx()),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )
                    }
                },
            onTextLayout = { textLayoutResult = it },
            onClick = { offset ->
                annotatedText.getStringAnnotations("WORD", offset, offset).firstOrNull()?.let { annotation ->
                    // Pause active read-aloud on tap
                    if (isReadingAloud) {
                        tts?.stop()
                        isReadingAloud = false
                        activeSpeechRange = null
                    }
                    selectedWord = annotation.item
                    showSheet = true
                }
            }
        )
    }

    if (showSheet && selectedWord != null) {
        WordPronunciationSheet(
            word = selectedWord!!,
            tts = tts,
            isTtsReady = isTtsReady,
            isTablet = isTablet,
            onDismiss = {
                showSheet = false
                onMascotStateChanged(MascotState.FOCUS_READING, null)
            },
            onPronunciationAttempt = onPronunciationAttempt,
            onMascotStateChanged = onMascotStateChanged,
            particleState = particleState
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WordPronunciationSheet - Practice vocabulary bottom sheet with speech support
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordPronunciationSheet(
    word: String,
    tts: TextToSpeech?,
    isTtsReady: Boolean,
    isTablet: Boolean,
    onDismiss: () -> Unit,
    onPronunciationAttempt: (word: String, heard: String, isCorrect: Boolean, attemptNumber: Int) -> Unit = { _, _, _, _ -> },
    onMascotStateChanged: (MascotState, String?) -> Unit = { _, _ -> },
    particleState: SparkleParticleState? = null
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var feedbackResult by remember { mutableStateOf<WordFeedback?>(null) }
    var attemptCount by remember { mutableStateOf(0) }
    var partialText by remember { mutableStateOf("") }
    var hasHandledResult by remember { mutableStateOf(false) }

    // 1. Initial State Activation
    LaunchedEffect(Unit) {
        onMascotStateChanged(
            MascotState.PRACTICE_SPEECH,
            "Let's practice the vocabulary word '$word'! Listen to how it sounds, then press 'Record'!"
        )
    }

    // 2. Dynamic Audio Feedback Vocalizer (TTS)
    LaunchedEffect(feedbackResult) {
        feedbackResult?.let { feedback ->
            if (isTtsReady && tts != null) {
                val spokenMsg = when {
                    feedback.isCorrect && attemptCount == 1 -> {
                        "Wow! Perfect on your first try! Incredibly stellar!"
                    }
                    feedback.isCorrect -> "Great pronunciation! You got it!"
                    feedback.heard == "Model is still loading, please wait..." -> feedback.heard
                    feedback.heard.contains("Failed to start") -> feedback.heard
                    feedback.heard.contains("Mic Error") -> feedback.heard
                    feedback.heard.contains("didn't hear") -> feedback.heard
                    feedback.heard.isEmpty() -> "I didn't catch that clearly, let's try again!"
                    !feedback.isCorrect && feedback.confidence in 0.01f..0.6f -> {
                        "I didn't hear that clearly, let's try again. You said: ${feedback.heard}"
                    }
                    attemptCount == 1 -> "Almost there! Keep trying! You said: ${feedback.heard}"
                    attemptCount == 2 -> "Don't give up! Try listening to it again. You said: ${feedback.heard}"
                    else -> "Practice makes perfect! Take your time. You said: ${feedback.heard}"
                }
                
                // Adjust mascot expressions dynamically
                if (feedback.isCorrect) {
                    onMascotStateChanged(MascotState.SUCCESS_CHEER, "Phenomenal job! You spoke it perfectly! 🎉")
                } else if (feedback.heard.isNotEmpty()) {
                    onMascotStateChanged(MascotState.TRY_AGAIN, "Almost there, Hero! Let's try saying '$word' again! 💪")
                }
                
                tts.speak(spokenMsg, TextToSpeech.QUEUE_FLUSH, null, "feedback_audio")
            }
        }
    }

    val isVoskReady by VoskManager.isReady.collectAsState()
    val engine = remember { SpeechRecognitionEngine() }
    val rmsLevel by engine.rmsLevel.collectAsState()

    DisposableEffect(Unit) {
        onDispose { engine.release() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (!isGranted) {
                feedbackResult = WordFeedback(word, "Permission denied", false, 0f)
            }
        }
    )

    fun handleRecognizedText(heard: String, confidence: Float) {
        if (hasHandledResult) return
        hasHandledResult = true
        isListening = false
        partialText = ""
        
        if (heard.isEmpty() || heard.trim() == "[unk]") {
            feedbackResult = WordFeedback(word, "", false, 0f)
            onMascotStateChanged(MascotState.TRY_AGAIN, "I didn't hear anything. Tap 'Record' and speak into your mic!")
            return
        }
        
        attemptCount++
        
        val cleanWord  = word.lowercase().replace(Regex("[^a-z]"), "")
        val cleanHeard = heard.lowercase().replace(Regex("[^a-z]"), "")
        
        val isExactMatch = cleanWord == cleanHeard || cleanHeard.contains(cleanWord)
        val similarityScore = cleanWord.similarity(cleanHeard)
        val isCorrect = isExactMatch || similarityScore >= 0.7f
        
        // Trigger sparkling particles on correct answer
        if (isCorrect && particleState != null) {
            // Emits from the center of the sheet
            particleState.emitStarBurst(x = 500f, y = 800f, count = 25)
        }

        feedbackResult = WordFeedback(word, heard, isCorrect, confidence)
        onPronunciationAttempt(word, heard, isCorrect, attemptCount)
    }

    fun stopListening() {
        isListening = false
        partialText = ""
        engine.stopListening { finalText, conf ->
            if (finalText.isNotEmpty()) handleRecognizedText(finalText, conf)
        }
    }

    fun startListening() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        if (!isVoskReady || VoskManager.model == null) {
            feedbackResult = WordFeedback(word, "Model is still loading, please wait...", false, 0f)
            return
        }

        feedbackResult = null
        partialText    = ""
        isListening    = true
        hasHandledResult = false

        onMascotStateChanged(MascotState.PRACTICE_SPEECH, "I am listening... Speak '$word' clearly! 🎙️")

        engine.startListening(
            targetWords = listOf(word),
            onPartial = { partial -> partialText = partial.replace("[unk]", "").trim() },
            onResult  = { text, conf -> handleRecognizedText(text.replace("[unk]", "").trim(), conf) },
            onError   = { msg    ->
                isListening = false
                feedbackResult = WordFeedback(word, "Mic Error: $msg", false, 0f)
            }
        )
    }

    var silenceStartTime by remember { mutableStateOf(0L) }

    LaunchedEffect(rmsLevel) {
        if (isListening && partialText.isNotEmpty()) {
            if (rmsLevel < 0.05f) {
                if (silenceStartTime == 0L) silenceStartTime = System.currentTimeMillis()
                if (System.currentTimeMillis() - silenceStartTime > 1500L) {
                    stopListening()
                }
            } else {
                silenceStartTime = 0L
            }
        } else {
            silenceStartTime = 0L
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = word,
                fontSize = if (isTablet) 36.sp else 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = fredokaFontFamily
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Vocabulary Sound Practice",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = fredokaFontFamily
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Hear button
                Button(
                    onClick = {
                        if (isListening) stopListening()
                        feedbackResult = null
                        tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "tts_$word")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .squishClickable(enabled = isTtsReady) {}
                        .height(if (isTablet) 56.dp else 48.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = isTtsReady,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Hear pronunciation")
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isTtsReady) "Hear" else "Loading...",
                        fontFamily = fredokaFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Record button
                Button(
                    onClick = {
                        if (isListening) {
                            stopListening()
                        } else {
                            startListening()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .squishClickable {}
                        .height(if (isTablet) 56.dp else 48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isListening) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isListening) "Stop recording" else "Record"
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isListening) "Stop" else "Record",
                        fontFamily = fredokaFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(visible = isListening) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Column(
                            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.FiberManualRecord, null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (partialText.isNotEmpty()) "\"$partialText\""
                                           else "Listening... say the word",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    fontFamily = fredokaFontFamily
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { rmsLevel },
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.error,
                                trackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }

            // High-fidelity speech verification feedback
            AnimatedVisibility(visible = feedbackResult != null && !isListening) {
                feedbackResult?.let { feedback ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 20.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (feedback.isCorrect) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer,
                            border = BorderStroke(
                                2.dp,
                                if (feedback.isCorrect) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                            )
                        ) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    if (feedback.isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    null,
                                    tint = if (feedback.isCorrect) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = when {
                                        feedback.isCorrect && attemptCount == 1 -> "Wow! Perfect on your first try! 🎉"
                                        feedback.isCorrect -> "Great pronunciation! ✓"
                                        feedback.heard == "Model is still loading, please wait..." -> feedback.heard
                                        feedback.heard.contains("Failed to start") -> feedback.heard
                                        feedback.heard.contains("Mic Error") -> feedback.heard
                                        feedback.heard.contains("didn't hear") -> feedback.heard
                                        feedback.heard.isEmpty() -> "I didn't catch that clearly, let's try again! 🎙️"
                                        !feedback.isCorrect && feedback.confidence in 0.01f..0.6f -> "I didn't hear that clearly, let's try again. You said: \"${feedback.heard}\""
                                        attemptCount == 1 -> "Almost there! Keep trying! You said: \"${feedback.heard}\""
                                        attemptCount == 2 -> "Don't give up! Try listening to it again. You said: \"${feedback.heard}\""
                                        else -> "Practice makes perfect! Take your time. You said: \"${feedback.heard}\""
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (feedback.isCorrect) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                    fontFamily = fredokaFontFamily,
                                    textAlign = TextAlign.Center
                                )
                                if (attemptCount > 1) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Attempt $attemptCount",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontFamily = fredokaFontFamily
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Tap 'Hear' to listen, then 'Record' to practice",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = fredokaFontFamily
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WordFeedback - Structure containing Vosk speech recognition results
// ─────────────────────────────────────────────────────────────────────────────
data class WordFeedback(
    val word: String,
    val heard: String,
    val isCorrect: Boolean,
    val confidence: Float
)

// ─────────────────────────────────────────────────────────────────────────────
// Levenshtein String Similarity Helper
// ─────────────────────────────────────────────────────────────────────────────
private fun String.similarity(other: String): Float {
    if (this == other) return 1.0f
    if (this.isEmpty() || other.isEmpty()) return 0.0f
    
    val len0 = this.length + 1
    val len1 = other.length + 1
    
    var cost = IntArray(len0)
    var newcost = IntArray(len0)
    
    for (i in 0 until len0) cost[i] = i
    
    for (j in 1 until len1) {
        newcost[0] = j
        for (i in 1 until len0) {
            val match = if (this[i - 1] == other[j - 1]) 0 else 1
            val costReplace = cost[i - 1] + match
            val costInsert = cost[i] + 1
            val costDelete = newcost[i - 1] + 1
            newcost[i] = minOf(costInsert, minOf(costDelete, costReplace))
        }
        val swap = cost
        cost = newcost
        newcost = swap
    }
    
    val editDistance = cost[len0 - 1]
    val maxLength = maxOf(this.length, other.length)
    return 1.0f - (editDistance.toFloat() / maxLength.toFloat())
}