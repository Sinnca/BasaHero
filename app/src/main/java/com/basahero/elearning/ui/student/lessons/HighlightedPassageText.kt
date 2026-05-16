//package com.basahero.elearning.ui.student.lessons
//
//import android.speech.tts.TextToSpeech
//import androidx.compose.animation.*
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.text.ClickableText
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.SpanStyle
//import androidx.compose.ui.text.TextStyle
//import androidx.compose.ui.text.buildAnnotatedString
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.text.style.TextDecoration
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import java.util.Locale
//
//@Composable
//fun HighlightedPassageText(
//    passageText: String,
//    highlightedWords: List<HighlightedWord>,
//    highlightColor: Color = MaterialTheme.colorScheme.primary,
//    modifier: Modifier = Modifier
//) {
//    var selectedWord by remember { mutableStateOf<String?>(null) }
//    var showSheet by remember { mutableStateOf(false) }
//
//    val annotatedText = remember(passageText, highlightedWords) {
//        buildAnnotatedString {
//            append(passageText)
//            highlightedWords.forEach { hw ->
//                if (hw.start >= 0 && hw.end <= passageText.length) {
//                    addStyle(
//                        style = SpanStyle(
//                            color = highlightColor,
//                            fontWeight = FontWeight.Bold,
//                            textDecoration = TextDecoration.Underline
//                        ),
//                        start = hw.start,
//                        end = hw.end
//                    )
//                    addStringAnnotation(tag = "WORD", annotation = hw.word, start = hw.start, end = hw.end)
//                }
//            }
//        }
//    }
//
//    ClickableText(
//        text = annotatedText,
//        style = TextStyle(
//            fontSize = 16.sp,
//            lineHeight = 26.sp,
//            color = MaterialTheme.colorScheme.onSurface,
//            textAlign = TextAlign.Justify
//        ),
//        modifier = modifier,
//        onClick = { offset ->
//            annotatedText.getStringAnnotations("WORD", offset, offset).firstOrNull()?.let {
//                selectedWord = it.item
//                showSheet = true
//            }
//        }
//    )
//
//    if (showSheet && selectedWord != null) {
//        WordPronunciationSheet(word = selectedWord!!, onDismiss = { showSheet = false })
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun WordPronunciationSheet(word: String, onDismiss: () -> Unit) {
//    val context = LocalContext.current
//    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
//    var isListening by remember { mutableStateOf(false) }
//
//    LaunchedEffect(Unit) {
//        tts = TextToSpeech(context) { tts?.language = Locale.US }
//    }
//    DisposableEffect(Unit) { onDispose { tts?.shutdown() } }
//
//    ModalBottomSheet(onDismissRequest = onDismiss) {
//        Column(
//            modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding(),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Text(text = word, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
//            Spacer(Modifier.height(24.dp))
//            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
//                Button(
//                    onClick = { tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, null) },
//                    modifier = Modifier.weight(1f).height(56.dp),
//                    shape = RoundedCornerShape(12.dp),
//                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
//                ) {
//                    Icon(Icons.Default.VolumeUp, null)
//                    Spacer(Modifier.width(8.dp))
//                    Text("Hear")
//                }
//                Button(
//                    onClick = { isListening = !isListening },
//                    modifier = Modifier.weight(1f).height(56.dp),
//                    shape = RoundedCornerShape(12.dp)
//                ) {
//                    Icon(if (isListening) Icons.Default.Stop else Icons.Default.Mic, null)
//                    Spacer(Modifier.width(8.dp))
//                    Text(if (isListening) "Stop" else "Record")
//                }
//            }
//            if (isListening) {
//                Text("Listening...", color = Color.Red, modifier = Modifier.padding(top = 16.dp))
//            }
//            Spacer(Modifier.height(24.dp))
//        }
//    }
//}
package com.basahero.elearning.ui.student.lessons

import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
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
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// HighlightedPassageText
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HighlightedPassageText(
    passageText: String,
    highlightedWords: List<HighlightedWord>,
    highlightColor: Color = MaterialTheme.colorScheme.primary,
    isTablet: Boolean = false,
    modifier: Modifier = Modifier,
    onPronunciationAttempt: (word: String, heard: String, isCorrect: Boolean, attemptNumber: Int) -> Unit = { _, _, _, _ -> }
) {
    var selectedWord by remember { mutableStateOf<String?>(null) }
    var showSheet by remember { mutableStateOf(false) }

    // 🚀 THE FIX: We moved the Voice Engine UP here so it stays alive!
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    // Boot up the engine once when the lesson opens
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                isTtsReady = true
            }
        }
        
        // 🚀 THE FIX: Always ensure Vosk is warming up when entering this screen!
        VoskManager.initModel(context)
    }

    // Shut it down ONLY when the student leaves the lesson entirely
    DisposableEffect(Unit) {
        onDispose { tts?.shutdown() }
    }

    val annotatedText = remember(passageText, highlightedWords) {
        val baseAnnotated = com.basahero.elearning.util.TextUtil.parseBoldText(passageText)
        val cleanedText = baseAnnotated.text
        
        buildAnnotatedString {
            append(baseAnnotated)
            
            // Apply vocabulary highlights (WORD annotations)
            highlightedWords.forEach { hw ->
                val startIndex = cleanedText.indexOf(hw.word, ignoreCase = true)
                if (startIndex >= 0) {
                    val endIndex = startIndex + hw.word.length
                    addStyle(
                        style = SpanStyle(
                            color = highlightColor,
                            fontWeight = FontWeight.Bold,
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

    ClickableText(
        text = annotatedText,
        style = TextStyle(
            fontFamily = fredokaFontFamily,
            fontSize = if (isTablet) 18.sp else 16.sp,
            lineHeight = if (isTablet) 30.sp else 26.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            textAlign = TextAlign.Start
        ),
        modifier = modifier,
        onClick = { offset ->
            annotatedText.getStringAnnotations("WORD", offset, offset).firstOrNull()?.let {
                selectedWord = it.item
                showSheet = true
            }
        }
    )

    if (showSheet && selectedWord != null) {
        WordPronunciationSheet(
            word = selectedWord!!,
            tts = tts,               // 👈 Pass the warm engine down to the sheet
            isTtsReady = isTtsReady, // 👈 Pass the ready status down
            isTablet = isTablet,
            onDismiss = { showSheet = false },
            onPronunciationAttempt = onPronunciationAttempt
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WordPronunciationSheet
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordPronunciationSheet(
    word: String,
    tts: TextToSpeech?,      // 👈 Received from above
    isTtsReady: Boolean,     // 👈 Received from above
    isTablet: Boolean,
    onDismiss: () -> Unit,
    onPronunciationAttempt: (word: String, heard: String, isCorrect: Boolean, attemptNumber: Int) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var feedbackResult by remember { mutableStateOf<WordFeedback?>(null) }
    var attemptCount by remember { mutableStateOf(0) }
    var partialText by remember { mutableStateOf("") }
    var hasHandledResult by remember { mutableStateOf(false) }

    // Read the feedback aloud automatically using TTS
    LaunchedEffect(feedbackResult) {
        feedbackResult?.let { feedback ->
            if (isTtsReady && tts != null) {
                val spokenMsg = when {
                    feedback.isCorrect && attemptCount == 1 -> "Wow! Perfect on your first try!"
                    feedback.isCorrect -> "Great pronunciation!"
                    feedback.heard == "Model is still loading, please wait..." -> feedback.heard
                    feedback.heard.contains("Failed to start") -> feedback.heard
                    feedback.heard.contains("Mic Error") -> feedback.heard
                    feedback.heard.contains("didn't hear") -> feedback.heard
                    feedback.heard.isEmpty() -> "I didn't catch that clearly, let's try again!"
                    !feedback.isCorrect && feedback.confidence in 0.01f..0.6f -> "I didn't hear that clearly, let's try again. You said: ${feedback.heard}"
                    attemptCount == 1 -> "Almost there! Keep trying! You said: ${feedback.heard}"
                    attemptCount == 2 -> "Don't give up! Try listening to it again. You said: ${feedback.heard}"
                    else -> "Practice makes perfect! Take your time. You said: ${feedback.heard}"
                }
                tts.speak(spokenMsg, TextToSpeech.QUEUE_FLUSH, null, "feedback_audio")
            }
        }
    }

    // ── Speech engine (our custom AudioRecord + DSP pipeline) ─────────────────
    val isVoskReady by VoskManager.isReady.collectAsState()
    val engine = remember { SpeechRecognitionEngine() }
    val rmsLevel by engine.rmsLevel.collectAsState()


    DisposableEffect(Unit) {
        onDispose { engine.release() }
    }

    // Permission handling
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (!isGranted) {
                feedbackResult = WordFeedback(word, "Permission denied", false, 0f)
            }
        }
    )

    fun handleRecognizedText(heard: String, confidence: Float) {
        if (hasHandledResult) return  // guard against duplicate callbacks
        hasHandledResult = true
        isListening = false
        partialText = ""
        
        if (heard.isEmpty() || heard.trim() == "[unk]") {
            feedbackResult = WordFeedback(word, "", false, 0f)
            return
        }
        
        attemptCount++
        
        // Basic match logic for kids: strip punctuation and compare
        val cleanWord  = word.lowercase().replace(Regex("[^a-z]"), "")
        val cleanHeard = heard.lowercase().replace(Regex("[^a-z]"), "")
        
        val isExactMatch = cleanWord == cleanHeard || cleanHeard.contains(cleanWord)
        val similarityScore = cleanWord.similarity(cleanHeard)
        val isCorrect = isExactMatch || similarityScore >= 0.7f
        
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

    fun extractVoskText(jsonStr: String?): String {
        if (jsonStr == null) return ""
        return try {
            org.json.JSONObject(jsonStr).getString("text")
        } catch (e: Exception) {
            ""
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

    // Auto-stop VAD logic
    LaunchedEffect(rmsLevel) {
        if (isListening && partialText.isNotEmpty()) {
            if (rmsLevel < 0.05f) { // Silence threshold
                if (silenceStartTime == 0L) silenceStartTime = System.currentTimeMillis()
                if (System.currentTimeMillis() - silenceStartTime > 1500L) { // 1.5 seconds of silence
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
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Vocabulary Word",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        // Force the audio to play immediately without queuing
                        tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "tts_$word")
                    },
                    modifier = Modifier.weight(1f).height(if (isTablet) 56.dp else 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = isTtsReady,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Hear pronunciation")
                    Spacer(Modifier.width(8.dp))
                    Text(if (isTtsReady) "Hear" else "Loading...")
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
                    modifier = Modifier.weight(1f).height(if (isTablet) 56.dp else 48.dp),
                    shape = RoundedCornerShape(12.dp),
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
                    Text(if (isListening) "Stop" else "Record")
                }
            }

            // Listening indicator
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
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            // Real-time RMS level meter
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

            // Feedback result
            AnimatedVisibility(visible = feedbackResult != null && !isListening) {
                feedbackResult?.let { feedback ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 20.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (feedback.isCorrect) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(16.dp),
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
                                    color = if (feedback.isCorrect) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                                if (attemptCount > 1) {
                                    Text(
                                        "Attempt $attemptCount",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

data class WordFeedback(
    val word: String,
    val heard: String,
    val isCorrect: Boolean,
    val confidence: Float
)

// ── String Similarity Helpers ─────────────────────────────────────────────────

fun String.levenshtein(other: String): Int {
    val lhs = this.lowercase()
    val rhs = other.lowercase()
    if (lhs == rhs) return 0
    if (lhs.isEmpty()) return rhs.length
    if (rhs.isEmpty()) return lhs.length

    var cost = IntArray(rhs.length + 1) { it }
    var newCost = IntArray(rhs.length + 1)

    for (i in 1..lhs.length) {
        newCost[0] = i
        for (j in 1..rhs.length) {
            val match = if (lhs[i - 1] == rhs[j - 1]) 0 else 1
            val costReplace = cost[j - 1] + match
            val costInsert  = cost[j] + 1
            val costDelete  = newCost[j - 1] + 1
            newCost[j] = minOf(costInsert, costDelete, costReplace)
        }
        val swap = cost; cost = newCost; newCost = swap
    }
    return cost[rhs.length]
}

fun String.similarity(other: String): Float {
    val maxLen = maxOf(this.length, other.length)
    if (maxLen == 0) return 1f
    val dist = this.levenshtein(other)
    return 1f - (dist.toFloat() / maxLen)
}