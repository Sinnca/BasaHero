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
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// HighlightedPassageText
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HighlightedPassageText(
    passageText: String,
    highlightedWords: List<HighlightedWord>,
    highlightColor: Color = MaterialTheme.colorScheme.primary,
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
        buildAnnotatedString {
            append(passageText)
            highlightedWords.forEach { hw ->
                if (hw.start >= 0 && hw.end <= passageText.length) {
                    addStyle(
                        style = SpanStyle(
                            color = highlightColor,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        ),
                        start = hw.start,
                        end = hw.end
                    )
                    addStringAnnotation(tag = "WORD", annotation = hw.word, start = hw.start, end = hw.end)
                }
            }
        }
    }

    ClickableText(
        text = annotatedText,
        style = TextStyle(
            fontSize = 16.sp,
            lineHeight = 26.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Justify
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
    onDismiss: () -> Unit,
    onPronunciationAttempt: (word: String, heard: String, isCorrect: Boolean, attemptNumber: Int) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var feedbackResult by remember { mutableStateOf<WordFeedback?>(null) }
    var attemptCount by remember { mutableStateOf(0) }
    var partialText by remember { mutableStateOf("") }
    var hasHandledResult by remember { mutableStateOf(false) }

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

    fun handleRecognizedText(heard: String) {
        if (hasHandledResult) return  // guard against duplicate callbacks
        hasHandledResult = true
        isListening = false
        partialText = ""
        
        if (heard.isEmpty()) {
            feedbackResult = WordFeedback(word, "I didn't hear anything clearly, try again!", false, 0f)
            return
        }
        
        attemptCount++
        
        // Basic match logic for kids: strip punctuation and compare
        val cleanWord  = word.lowercase().replace(Regex("[^a-z]"), "")
        val cleanHeard = heard.lowercase().replace(Regex("[^a-z]"), "")
        
        val isCorrect = cleanWord == cleanHeard || cleanHeard.contains(cleanWord)
        feedbackResult = WordFeedback(word, heard, isCorrect, 1f)
        onPronunciationAttempt(word, heard, isCorrect, attemptCount)
    }

    fun stopListening() {
        isListening = false
        partialText = ""
        engine.stopListening { finalText ->
            if (finalText.isNotEmpty()) handleRecognizedText(finalText)
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
            onPartial = { partial -> partialText = partial },
            onResult  = { text   -> handleRecognizedText(text) },
            onError   = { msg    ->
                isListening = false
                feedbackResult = WordFeedback(word, "Mic Error: $msg", false, 0f)
            }
        )
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
                fontSize = 36.sp,
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
                    modifier = Modifier.weight(1f).height(52.dp),
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
                    modifier = Modifier.weight(1f).height(52.dp),
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
                                        feedback.isCorrect -> "Great pronunciation! ✓"
                                        feedback.heard == "Model is still loading, please wait..." -> feedback.heard
                                        feedback.heard.contains("Failed to start") -> feedback.heard
                                        feedback.heard.contains("Mic Error") -> feedback.heard
                                        feedback.heard.contains("didn't hear") -> feedback.heard
                                        else -> "Keep trying! You said: \"${feedback.heard}\""
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