package com.basahero.elearning.ui.student.lessons

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import com.basahero.elearning.data.model.LessonPart
import com.basahero.elearning.data.model.MiniChoice
import com.basahero.elearning.data.model.MiniQuestion
import com.basahero.elearning.ui.common.AnimatedScrollIndicator
import com.basahero.elearning.ui.common.LocalAppStrings

// ─────────────────────────────────────────────────────────────────────────────
// ReadingScreen — Multi-step lesson wizard
// Steps: [Lecture] → [Reading+MiniActivity 1] → [Reading+MiniActivity 2] → Quiz
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReadingScreen(
    lessonId: String,
    viewModel: ReadingViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val strings = LocalAppStrings.current
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val isTablet = screenWidth >= 600
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(lessonId) { viewModel.loadLesson(lessonId) }

    if (uiState.isLoading || uiState.lesson == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val lesson = uiState.lesson!!

    // Build steps list dynamically
    val steps = remember(lesson) {
        buildList {
            if (lesson.hasLecture) add(LessonStep.Lecture(lesson.lectureText))
            if (lesson.hasParts) {
                lesson.parts.forEachIndexed { i, part ->
                    val isShortIntro = part.miniQuestions.isEmpty() &&
                            part.activityQuestions.isNotEmpty() &&
                            part.passageText.length < 350 &&
                            part.passageText.contains("Activity", ignoreCase = true)

                    if (!isShortIntro) {
                        add(LessonStep.ReadingPart(i + 1, part))
                    }

                    if (part.activityQuestions.isNotEmpty()) {
                        add(LessonStep.Activity(
                            partNumber = i + 1,
                            questions = part.activityQuestions,
                            introText = if (isShortIntro) part.passageText else null
                        ))
                    }
                }
            } else {
                add(LessonStep.ReadingPart(1, LessonPart(
                    passageText = lesson.passageText,
                    highlightedWords = uiState.highlightedWords.map { it.word }
                )))
            }
        }
    }

    val lessonTotalQuestions = remember(lesson) {
        if (lesson.hasParts) {
            lesson.parts.sumOf { it.activityQuestions.size }
        } else {
            0
        }
    }

    // Track graded activity scores per step
    val activityScores = remember { mutableStateMapOf<Int, Int>() }

    var currentStepIndex by remember { mutableIntStateOf(0) }
    val currentStep = steps.getOrNull(currentStepIndex)
    val isLastStep = currentStepIndex >= steps.lastIndex
    val isReviewMode = lesson.isDone // 👈 NEW: TRUE if lesson already finished
    val scrollState = rememberScrollState()

    // Activity result full-page state
    var showActivityResult by remember { mutableStateOf(false) }
    var activityScore by remember { mutableIntStateOf(0) }
    var activityTotal by remember { mutableIntStateOf(0) }
    // Key to force-reset ActivityStepContent on retry
    var activityRetryKey by remember { mutableIntStateOf(0) }

    // Reset scroll when step changes
    LaunchedEffect(currentStepIndex) { scrollState.scrollTo(0) }

    // ── Full-page Activity Result Screen ──────────────────────────────────────
    if (showActivityResult) {
        val pct = if (activityTotal > 0) (activityScore.toFloat() / activityTotal) * 100f else 0f
        val passed = pct >= 60f

        ActivityResultScreen(
            score = activityScore,
            total = activityTotal,
            passed = passed,
            isTablet = isTablet,
            onContinue = {
                showActivityResult = false
                if (isLastStep) {
                    onBack()
                } else {
                    currentStepIndex++
                }
            },
            onRetry = {
                showActivityResult = false
                activityRetryKey++
            },
            onBackToLessons = onBack
        )
        return
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(lesson.competency, fontSize = if (isTablet) 14.sp else 12.sp, color = MaterialTheme.colorScheme.primary)
                            Text(lesson.title, fontSize = if (isTablet) 18.sp else 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (currentStepIndex > 0) currentStepIndex-- else onBack()
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (isReviewMode) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.Close, contentDescription = "Exit Review")
                            }
                        }
                    }
                )
                // Step progress indicator
                LinearProgressIndicator(
                    progress = { (currentStepIndex + 1).toFloat() / steps.size },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        },
        bottomBar = {
            // Hide bottom bar on Activity steps UNLESS in review mode
            // In review mode, we allow the "Next" button to show so they can skip activities
            if (currentStep !is LessonStep.Activity || isReviewMode) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                if (isLastStep) {
                                    onBack()
                                } else {
                                    currentStepIndex++
                                }
                            },
                            modifier = Modifier
                                .then(if (isTablet) Modifier.width(480.dp) else Modifier.fillMaxWidth())
                                .padding(16.dp)
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            val buttonText = when {
                                isReviewMode && isLastStep -> "Finish Review"
                                isLastStep -> "Next"
                                else -> "Next"
                            }
                            Text(buttonText, fontSize = if (isTablet) 18.sp else 16.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                if (isReviewMode && isLastStep) Icons.Default.Check else Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            val contentPadding = if (isTablet) 32.dp else 16.dp

            Card(
                modifier = Modifier
                    .then(
                        if (isTablet) Modifier.widthIn(max = 720.dp).padding(vertical = 24.dp)
                        else Modifier.fillMaxWidth()
                    ),
                shape = if (isTablet) RoundedCornerShape(24.dp) else RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isTablet) 4.dp else 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(contentPadding)
                ) {
                    when (val step = currentStep) {
                        is LessonStep.Lecture -> LectureStepContent(step.text, isTablet)
                        is LessonStep.ReadingPart -> ReadingPartStepContent(
                            partNumber = step.partNumber,
                            part = step.part,
                            isTablet = isTablet,
                            isReviewMode = isReviewMode,
                            onPronunciationAttempt = { word, heard, isCorrect, _ ->
                                if (!isReviewMode) {
                                    val score = if (isCorrect) 100 else 0
                                    viewModel.savePronunciationAttempt(word, heard, isCorrect, score)
                                }
                            }
                        )
                        is LessonStep.Activity -> {
                            key(activityRetryKey) {
                                ActivityStepContent(
                                    partNumber = step.partNumber,
                                    questions = step.questions,
                                    introText = step.introText,
                                    isTablet = isTablet,
                                    isReviewMode = isReviewMode,
                                    onSubmit = { correct, total ->
                                        if (!isReviewMode) {
                                            activityScores[currentStepIndex] = correct
                                            activityScore = correct
                                            activityTotal = total
                                            
                                            // Calculate current cumulative score
                                            val currentTotalCorrect = activityScores.values.sum()
                                            val safeTotal = if (lessonTotalQuestions > 0) lessonTotalQuestions else total
                                            
                                            // Save IMMEDIATELY so the attempt is registered in Supabase
                                            viewModel.saveLessonProgress(context, currentTotalCorrect, safeTotal)
                                        }
                                        showActivityResult = true
                                    }
                                )
                            }
                        }
                        null -> {}
                    }
                }
            }
        }
    }
}

// ─── Step types ──────────────────────────────────────────────────────────────
sealed class LessonStep {
    data class Lecture(val text: String) : LessonStep()
    data class ReadingPart(val partNumber: Int, val part: LessonPart) : LessonStep()
    data class Activity(val partNumber: Int, val questions: List<MiniQuestion>, val introText: String? = null) : LessonStep()
}

// ─── Lecture Step ─────────────────────────────────────────────────────────────
@Composable
private fun LectureStepContent(text: String, isTablet: Boolean) {
    // Header icon
    Box(
        modifier = Modifier.fillMaxWidth().height(if (isTablet) 180.dp else 120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.School,
                contentDescription = "Lecture",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(if (isTablet) 64.dp else 48.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Lecture",
                fontSize = if (isTablet) 20.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }

    Spacer(Modifier.height(24.dp))

    Text(
        text = text,
        fontSize = if (isTablet) 18.sp else 15.sp,
        lineHeight = if (isTablet) 32.sp else 26.sp,
        color = MaterialTheme.colorScheme.onSurface
    )
}

// ─── Reading Part Step (Passage + Mini Activity on same page) ────────────────
@Composable
private fun ReadingPartStepContent(
    partNumber: Int,
    part: LessonPart,
    isTablet: Boolean,
    isReviewMode: Boolean,
    onPronunciationAttempt: (String, String, Boolean, Int) -> Unit
) {
    // Header
    Box(
        modifier = Modifier.fillMaxWidth().height(if (isTablet) 140.dp else 100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Book,
                contentDescription = "Reading",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(if (isTablet) 48.dp else 36.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Reading Passage $partNumber",
                fontSize = if (isTablet) 18.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }

    Spacer(Modifier.height(20.dp))

    // Build HighlightedWord list from string list
    val highlightedWordObjects = remember(part.passageText, part.highlightedWords) {
        part.highlightedWords.mapNotNull { word ->
            val startIndex = part.passageText.indexOf(word, ignoreCase = true)
            if (startIndex >= 0) HighlightedWord(word, startIndex, startIndex + word.length) else null
        }
    }

    // Passage text with highlighted words
    HighlightedPassageText(
        passageText = part.passageText,
        highlightedWords = highlightedWordObjects,
        modifier = Modifier.fillMaxWidth(),
        onPronunciationAttempt = onPronunciationAttempt
    )

    // Mini Activity Section
    if (part.miniQuestions.isNotEmpty()) {
        Spacer(Modifier.height(24.dp))
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            "Mini Activity",
            fontSize = if (isTablet) 20.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary
        )
        Spacer(Modifier.height(12.dp))

        part.miniQuestions.forEachIndexed { index, question ->
            MiniQuestionCard(
                questionNumber = index + 1,
                question = question,
                isTablet = isTablet,
                isReviewMode = isReviewMode
            )
            if (index < part.miniQuestions.lastIndex) {
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    Spacer(Modifier.height(32.dp))
}

// ─── Mini Question Card (practice, not graded) ──────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MiniQuestionCard(
    questionNumber: Int,
    question: MiniQuestion,
    isTablet: Boolean,
    isReviewMode: Boolean
) {
    var selectedChoiceId by remember(question.id) { mutableStateOf<String?>(null) }
    var showFeedback by remember(question.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val cleanQuestionText = question.questionText
                .replace(Regex("^Q\\d+[:.]?\\s*", RegexOption.IGNORE_CASE), "")
                .replace(Regex("^\\d+[:.]?\\s*"), "")

            Text(
                "Q$questionNumber: $cleanQuestionText",
                fontSize = if (isTablet) 16.sp else 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(8.dp))

            if (question.questionType == "SEQUENCING") {
                // Drag to reorder for MiniQuestionCard
                var currentChoices by remember(question.id) {
                    mutableStateOf(if (isReviewMode) question.choices.sortedBy { it.orderIndex } else question.choices.shuffled())
                }
                val state = rememberReorderableLazyListState(onMove = { from, to ->
                    currentChoices = currentChoices.toMutableList().apply {
                        add(to.index, removeAt(from.index))
                    }
                })

                if (!isReviewMode) {
                    Text(
                        "Hold and drag the items to arrange them in the correct order:",
                        fontSize = if (isTablet) 14.sp else 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                }

                LazyColumn(
                    state = state.listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = (currentChoices.size * 110).dp) // Increased multiplier for text wrapping
                        .reorderable(state)
                        .then(if (isReviewMode) Modifier else Modifier.detectReorderAfterLongPress(state)),
                    userScrollEnabled = false
                ) {
                    items(currentChoices, { it.id }) { choice ->
                        ReorderableItem(state, key = choice.id) { isDragging ->
                            val elevation = if (isDragging) 8.dp else 0.dp
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = elevation,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!isReviewMode) {
                                        Icon(Icons.Default.DragHandle, contentDescription = "Drag")
                                        Spacer(Modifier.width(12.dp))
                                    }
                                    Text(
                                        choice.choiceText,
                                        fontSize = if (isTablet) 16.sp else 14.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                if (!isReviewMode) {
                    Button(
                        onClick = { showFeedback = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Check Answer")
                    }
                }

                if (showFeedback) {
                    val correctOrder = question.choices.sortedBy { it.orderIndex }.map { it.id }
                    val currentOrder = currentChoices.map { it.id }
                    val isCorrect = correctOrder == currentOrder
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (isCorrect) "✓ Correct!" else "✗ Incorrect. The right order is: \n" + question.choices.sortedBy { it.orderIndex }.joinToString("\n") { it.choiceText },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isCorrect) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                }
            } else if (question.questionType == "FILL_IN") {
                // 🚀 NEW: FILL_IN UI for MiniQuestionCard
                val correctText = question.choices.firstOrNull { it.isCorrect }?.choiceText ?: ""
                var textValue by remember(question.id) { mutableStateOf("") }

                Column {
                    Text(
                        "Fill in the blank:",
                        fontSize = if (isTablet) 14.sp else 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { if (!showFeedback && !isReviewMode) textValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = showFeedback || isReviewMode,
                        placeholder = { Text("Type here...") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        trailingIcon = {
                            if (!showFeedback && !isReviewMode) {
                                IconButton(onClick = { showFeedback = true }) {
                                    Icon(Icons.Default.Check, "Check")
                                }
                            }
                        }
                    )

                    if (showFeedback || isReviewMode) {
                        val isMatch = textValue.trim().equals(correctText.trim(), ignoreCase = true)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (isMatch || isReviewMode) "✓ Correct: $correctText" else "✗ Incorrect. The answer is: $correctText",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMatch || isReviewMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else if (question.questionType == "OPEN_ENDED" || question.questionType == "REFLECTION") {
                // 🚀 NEW: REFLECTION UI for MiniQuestionCard
                var textValue by remember(question.id) { mutableStateOf("") }

                Column {
                    Text(
                        "Share your thoughts:",
                        fontSize = if (isTablet) 14.sp else 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { if (!showFeedback && !isReviewMode) textValue = it },
                        modifier = Modifier.fillMaxWidth().height(if (isTablet) 120.dp else 100.dp),
                        readOnly = showFeedback || isReviewMode,
                        placeholder = { Text("Type your thoughts here...") },
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (!showFeedback && !isReviewMode) {
                                IconButton(onClick = { showFeedback = true }) {
                                    Icon(Icons.Default.Send, "Send")
                                }
                            }
                        }
                    )

                    if (showFeedback || isReviewMode) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "✓ Thoughts shared! Great reflection.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            } else if (question.questionType == "MATCHING") {
                // 🚀 NEW: MATCHING UI for MiniQuestionCard
                val half = question.choices.size / 2
                val leftSide = remember(question.id) { question.choices.take(half) }
                val rightSide = remember(question.id) { question.choices.drop(half) }
                
                var selectedLeftId by remember(question.id) { mutableStateOf<String?>(null) }
                val matches = remember(question.id) { mutableStateMapOf<String, String>() }

                if (!isReviewMode && !showFeedback) {
                    Text(
                        "Tap a word on the left, then its match on the right:",
                        fontSize = if (isTablet) 14.sp else 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        leftSide.forEach { item ->
                            val isMatched = matches.containsKey(item.id) || showFeedback || isReviewMode
                            val isSelected = selectedLeftId == item.id
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable(enabled = !isMatched) {
                                    selectedLeftId = if (isSelected) null else item.id
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else if (isMatched) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.choiceText, fontSize = if (isTablet) 14.sp else 12.sp, modifier = Modifier.weight(1f))
                                    if (isMatched) Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        rightSide.forEach { item ->
                            val isMatched = matches.containsValue(item.id) || showFeedback || isReviewMode
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable(enabled = !isMatched && selectedLeftId != null) {
                                    val leftId = selectedLeftId ?: return@clickable
                                    matches[leftId] = item.id
                                    selectedLeftId = null
                                    if (matches.size == half) showFeedback = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isMatched) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.choiceText, fontSize = if (isTablet) 14.sp else 12.sp, modifier = Modifier.weight(1f))
                                    if (isMatched) Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                if (showFeedback || isReviewMode) {
                    var allCorrect = true
                    leftSide.forEachIndexed { i, leftItem ->
                        if (matches[leftItem.id] != rightSide[i].id) allCorrect = false
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (allCorrect || isReviewMode) "✓ Correct Matches!" else "✗ Not quite. Try to match them correctly next time!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (allCorrect || isReviewMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                }
            } else if (question.questionType == "IDENTIFICATION") {
                // 🚀 NEW: IDENTIFICATION UI for MiniQuestionCard
                var textValue by remember(question.id) { mutableStateOf("") }
                val correctAnswer = question.choices.find { it.isCorrect }?.choiceText ?: ""

                Column {
                    Text(
                        "Type your answer:",
                        fontSize = if (isTablet) 14.sp else 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { if (!showFeedback && !isReviewMode) textValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = showFeedback || isReviewMode,
                        placeholder = { Text("Enter text...") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        trailingIcon = {
                            if (!showFeedback && !isReviewMode) {
                                IconButton(onClick = { showFeedback = true }) {
                                    Icon(Icons.Default.Check, "Check")
                                }
                            }
                        }
                    )

                    if (showFeedback || isReviewMode) {
                        val isMatch = textValue.trim().equals(correctAnswer.trim(), ignoreCase = true)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (isMatch || isReviewMode) "✓ Correct: $correctAnswer" else "✗ Incorrect. The answer is: $correctAnswer",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMatch || isReviewMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else if (question.questionType == "HIGHLIGHT") {
                // HIGHLIGHT logic for MiniQuestionCard
                val selectedIds = remember(question.id) { mutableStateListOf<String>() }
                val correctIds = remember(question.id) { question.choices.filter { it.isCorrect }.map { it.id }.toSet() }

                if (!isReviewMode) {
                    Text(
                        "Tap the correct words/sentences to highlight them:",
                        fontSize = if (isTablet) 14.sp else 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    question.choices.forEach { choice ->
                        val isSelected = if (isReviewMode) choice.isCorrect else selectedIds.contains(choice.id)
                        val isCorrect = choice.isCorrect
                        
                        val bgColor = when {
                            isReviewMode && isCorrect -> MaterialTheme.colorScheme.tertiaryContainer
                            !showFeedback -> if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            isCorrect -> MaterialTheme.colorScheme.tertiaryContainer
                            isSelected && !isCorrect -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surface
                        }

                        Surface(
                            modifier = Modifier
                                .clickable(enabled = !showFeedback && !isReviewMode) {
                                    if (selectedIds.contains(choice.id)) {
                                        selectedIds.remove(choice.id)
                                    } else {
                                        selectedIds.add(choice.id)
                                    }
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = bgColor,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected && !showFeedback) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                choice.choiceText,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontSize = if (isTablet) 16.sp else 14.sp
                            )
                        }
                    }
                }

                if (!isReviewMode && !showFeedback) {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { showFeedback = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedIds.isNotEmpty()
                    ) {
                        Text("Check Answer")
                    }
                }

                if (showFeedback) {
                    val currentSelectedSet = selectedIds.toSet()
                    val isFullyCorrect = currentSelectedSet == correctIds
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (isFullyCorrect) "✓ Correct!" else "✗ Not quite. The correct highlights are shown in green.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isFullyCorrect) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                }
            } else {
                // MCQ choices
                val displayChoices = remember(question.id) {
                    if (isReviewMode) question.choices else question.choices.shuffled()
                }

                displayChoices.forEach { choice ->
                    val isSelected = if (isReviewMode) choice.isCorrect else selectedChoiceId == choice.id
                    val bgColor = when {
                        isReviewMode && choice.isCorrect -> MaterialTheme.colorScheme.tertiaryContainer
                        !showFeedback -> if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        choice.isCorrect -> MaterialTheme.colorScheme.tertiaryContainer
                        isSelected && !choice.isCorrect -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surface
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable(enabled = !showFeedback && !isReviewMode) {
                                selectedChoiceId = choice.id
                                showFeedback = true
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = bgColor,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected && !showFeedback) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                choice.choiceText,
                                fontSize = if (isTablet) 14.sp else 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (showFeedback && choice.isCorrect) {
                                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                            }
                            if (showFeedback && isSelected && !choice.isCorrect) {
                                Icon(Icons.Default.Cancel, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Feedback text
                if (showFeedback || isReviewMode) {
                    val correct = if (isReviewMode) true else question.choices.find { it.id == selectedChoiceId }?.isCorrect == true
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (correct) "✓ Correct!" else "✗ Not quite. The correct answer is shown above.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (correct) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// ─── Activity Step (Graded questions for the preceding passage) ─────────────
@Composable
private fun ActivityStepContent(
    partNumber: Int,
    questions: List<MiniQuestion>,
    introText: String?,
    isTablet: Boolean,
    isReviewMode: Boolean,
    onSubmit: (correct: Int, total: Int) -> Unit
) {
    // Header
    Box(
        modifier = Modifier.fillMaxWidth().height(if (isTablet) 140.dp else 100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Assignment,
                contentDescription = "Activity",
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(if (isTablet) 48.dp else 36.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Activity $partNumber",
                fontSize = if (isTablet) 18.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }

    Spacer(Modifier.height(24.dp))

    if (!introText.isNullOrBlank()) {
        Text(
            text = introText,
            fontSize = if (isTablet) 18.sp else 16.sp,
            lineHeight = if (isTablet) 28.sp else 24.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        )
    } else {
        Text(
            "Let's see what you remember from Reading Passage $partNumber!",
            fontSize = if (isTablet) 18.sp else 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
    }

    val shuffledQuestions = remember(questions) {
        if (isReviewMode) questions else questions.shuffled()
    }
    val answers = remember { mutableStateMapOf<String, Boolean>() }

    // Show questions
    run {
        // Show questions
        shuffledQuestions.forEachIndexed { index, question ->
            ActivityQuestionCard(
                questionNumber = index + 1,
                question = question,
                isTablet = isTablet,
                isReviewMode = isReviewMode,
                onAnswered = { isCorrect ->
                    answers[question.id] = isCorrect
                }
            )
            Spacer(Modifier.height(16.dp))
        }

        Spacer(Modifier.height(24.dp))

        if (!isReviewMode) {
            // Submit Button
            Button(
                onClick = {
                    val correct = answers.values.count { it }
                    onSubmit(correct, questions.size)
                },
                enabled = answers.size == questions.size, // Must answer all
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (answers.size == questions.size) "Submit Answers" else "Answer all questions to submit",
                    fontSize = if (isTablet) 18.sp else 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ─── Full-page Activity Result Screen ────────────────────────────────────────
@Composable
private fun ActivityResultScreen(
    score: Int,
    total: Int,
    passed: Boolean,
    isTablet: Boolean,
    onContinue: () -> Unit,
    onRetry: () -> Unit,
    onBackToLessons: () -> Unit
) {
    val pct = if (total > 0) (score * 100) / total else 0
    val circleColor = if (passed) Color(0xFF4CAF50) else Color(0xFFF44336)
    val bgGradient = if (passed) {
        Brush.verticalGradient(listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFFFEBEE), Color(0xFFFFCDD2)))
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(horizontal = if (isTablet) 48.dp else 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Emoji header
            Text(
                text = if (passed) "🌟" else "💪",
                fontSize = 64.sp
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = if (passed) "Great Job!" else "Keep Trying!",
                fontSize = if (isTablet) 36.sp else 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (passed) Color(0xFF2E7D32) else Color(0xFFC62828)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = if (passed) "You passed the activity!" else "You need at least 60% to continue.",
                fontSize = if (isTablet) 18.sp else 15.sp,
                color = Color.DarkGray,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))

            // Score Circle
            Box(
                modifier = Modifier
                    .size(if (isTablet) 200.dp else 160.dp)
                    .clip(CircleShape)
                    .background(circleColor.copy(alpha = 0.12f))
                    .border(6.dp, circleColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$pct%",
                        fontSize = if (isTablet) 48.sp else 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = circleColor
                    )
                    Text(
                        text = "$score / $total",
                        fontSize = if (isTablet) 18.sp else 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                }
            }

            Spacer(Modifier.height(48.dp))

            // Buttons
            Column(
                modifier = Modifier.widthIn(max = if (isTablet) 400.dp else 600.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (passed) {
                    Button(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Continue", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Retry Again", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                OutlinedButton(
                    onClick = onBackToLessons,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Back to Lessons", fontSize = 16.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActivityQuestionCard(
    questionNumber: Int,
    question: MiniQuestion,
    isTablet: Boolean,
    isReviewMode: Boolean,
    onAnswered: (Boolean) -> Unit
) {
    var selectedChoiceId by remember(question.id) { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (selectedChoiceId != null) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
    ) {
        val enabled = !isReviewMode
        Column(modifier = Modifier.padding(16.dp)) {
            val cleanQuestionText = question.questionText
                .replace(Regex("^Q\\d+[:.]?\\s*", RegexOption.IGNORE_CASE), "")
                .replace(Regex("^\\d+[:.]?\\s*"), "")

            Text(
                "Q$questionNumber: $cleanQuestionText",
                fontSize = if (isTablet) 18.sp else 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(12.dp))

            if (question.questionType == "FILL_IN") {
                // FILL_IN UI
                val correctText = question.choices.firstOrNull { c -> c.isCorrect }?.choiceText ?: ""
                var textValue by remember(question.id) {
                    mutableStateOf(if (isReviewMode) correctText else "")
                }
                var hasAnswered by remember(question.id) { mutableStateOf(isReviewMode) }
                OutlinedTextField(
                    value = textValue,
                    onValueChange = {
                        if (enabled) {
                            textValue = it
                            hasAnswered = it.isNotBlank()
                            // Case-insensitive match against the correct choice
                            val correctText = question.choices.firstOrNull { c -> c.isCorrect }?.choiceText ?: ""
                            val isMatch = textValue.trim().equals(correctText.trim(), ignoreCase = true)
                            onAnswered(isMatch)
                        }
                    },
                    readOnly = !enabled,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    placeholder = { Text("Type your answer here...") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            } else if (question.questionType == "SEQUENCING") {
                // SEQUENCING UI (Drag to reorder)
                var currentChoices by remember(question.id) {
                    mutableStateOf(if (isReviewMode) question.choices.sortedBy { it.orderIndex } else question.choices.shuffled())
                }
                val state = rememberReorderableLazyListState(onMove = { from, to ->
                    currentChoices = currentChoices.toMutableList().apply {
                        add(to.index, removeAt(from.index))
                    }
                })

                if (!isReviewMode) {
                    Text(
                        "Hold and drag the items to arrange them in the correct order:",
                        fontSize = if (isTablet) 14.sp else 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                }

                LazyColumn(
                    state = state.listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = (currentChoices.size * 110).dp) // Increased multiplier for text wrapping
                        .reorderable(state)
                        .then(if (enabled) Modifier.detectReorderAfterLongPress(state) else Modifier),
                    userScrollEnabled = false
                ) {
                    items(currentChoices, { it.id }) { choice ->
                        ReorderableItem(state, key = choice.id) { isDragging ->
                            val elevation = if (isDragging) 8.dp else 0.dp
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = elevation,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (enabled) {
                                        Icon(Icons.Default.DragHandle, contentDescription = "Drag")
                                        Spacer(Modifier.width(12.dp))
                                    }
                                    Text(
                                        choice.choiceText,
                                        fontSize = if (isTablet) 16.sp else 14.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Notify onAnswered whenever the order changes
                LaunchedEffect(currentChoices) {
                    val correctOrder = question.choices.sortedBy { it.orderIndex }.map { it.id }
                    val currentOrder = currentChoices.map { it.id }
                    onAnswered(correctOrder == currentOrder)
                }

                if (isReviewMode) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "✓ Correct Order",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            } else if (question.questionType == "HIGHLIGHT") {
                // HIGHLIGHT logic for ActivityQuestionCard
                val selectedIds = remember(question.id) { mutableStateListOf<String>() }
                val correctIds = remember(question.id) { question.choices.filter { it.isCorrect }.map { it.id }.toSet() }

                if (!isReviewMode) {
                    Text(
                        "Tap the correct words/sentences to highlight them:",
                        fontSize = if (isTablet) 14.sp else 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    question.choices.forEach { choice ->
                        val isSelected = if (isReviewMode) choice.isCorrect else selectedIds.contains(choice.id)
                        
                        val bgColor = when {
                            isReviewMode && choice.isCorrect -> MaterialTheme.colorScheme.tertiaryContainer
                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        }

                        Surface(
                            modifier = Modifier
                                .clickable(enabled = enabled) {
                                    if (selectedIds.contains(choice.id)) {
                                        selectedIds.remove(choice.id)
                                    } else {
                                        selectedIds.add(choice.id)
                                    }
                                    
                                    // Validation: Correct if ALL correct items selected AND NO incorrect items selected
                                    val currentSelectedSet = selectedIds.toSet()
                                    val isMatch = currentSelectedSet == correctIds
                                    onAnswered(isMatch)
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = bgColor,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        ) {
                            Text(
                                choice.choiceText,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontSize = if (isTablet) 16.sp else 14.sp
                            )
                        }
                    }
                }
                
                if (isReviewMode) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "✓ Correct Highlights",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            } else if (question.questionType == "MATCHING") {
                // 🚀 NEW: MATCHING UI (Column A | Column B)
                val half = question.choices.size / 2
                val leftSide = remember(question.id) { question.choices.take(half) }
                val rightSide = remember(question.id) { question.choices.drop(half) }
                
                var selectedLeftId by remember(question.id) { mutableStateOf<String?>(null) }
                val matches = remember(question.id) { mutableStateMapOf<String, String>() } // LeftId -> RightId

                if (!isReviewMode) {
                    Text(
                        "Tap a word on the left, then its match on the right:",
                        fontSize = if (isTablet) 14.sp else 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Column A
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        leftSide.forEach { item ->
                            val isMatched = matches.containsKey(item.id) || isReviewMode
                            val isSelected = selectedLeftId == item.id
                            
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable(enabled = enabled && !isMatched) {
                                    selectedLeftId = if (isSelected) null else item.id
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else if (isMatched) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.choiceText, fontSize = if (isTablet) 15.sp else 13.sp, modifier = Modifier.weight(1f))
                                    if (isMatched) Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Column B
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        rightSide.forEach { item ->
                            val isMatched = matches.containsValue(item.id) || isReviewMode
                            val isSelected = false // We don't select right side alone

                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable(enabled = enabled && !isMatched && selectedLeftId != null) {
                                    val leftId = selectedLeftId ?: return@clickable
                                    matches[leftId] = item.id
                                    selectedLeftId = null
                                    
                                    // Validation
                                    if (matches.size == half) {
                                        var allCorrect = true
                                        leftSide.forEachIndexed { i, leftItem ->
                                            val matchedRightId = matches[leftItem.id]
                                            val expectedRightId = rightSide[i].id
                                            if (matchedRightId != expectedRightId) allCorrect = false
                                        }
                                        onAnswered(allCorrect)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isMatched) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.choiceText, fontSize = if (isTablet) 15.sp else 13.sp, modifier = Modifier.weight(1f))
                                    if (isMatched) Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
                
                if (matches.isNotEmpty() && !isReviewMode) {
                    TextButton(onClick = { matches.clear(); onAnswered(false) }, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Reset Matches", color = MaterialTheme.colorScheme.error)
                    }
                }
            } else if (question.questionType == "OPEN_ENDED" || question.questionType == "REFLECTION") {
                // 🚀 NEW: OPEN_ENDED UI (Free Text)
                var textValue by remember(question.id) { mutableStateOf("") }

                Column {
                    Text(
                        "Share your thoughts below:",
                        fontSize = if (isTablet) 14.sp else 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = {
                            if (enabled) {
                                textValue = it
                                onAnswered(it.length >= 5) // Valid if they typed something meaningful
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(if (isTablet) 150.dp else 120.dp),
                        readOnly = !enabled,
                        placeholder = { Text("Type your answer here...") },
                        shape = RoundedCornerShape(16.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                    )
                }
            } else if (question.questionType == "IDENTIFICATION") {
                // 🚀 NEW: IDENTIFICATION UI (Type the exact answer)
                var textValue by remember(question.id) { mutableStateOf("") }
                val correctAnswer = question.choices.find { it.isCorrect }?.choiceText ?: ""

                Column {
                    Text(
                        "Type your answer carefully:",
                        fontSize = if (isTablet) 14.sp else 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = {
                            if (enabled) {
                                textValue = it
                                // Validation: Check if it matches the correct answer (case-insensitive)
                                onAnswered(it.trim().equals(correctAnswer.trim(), ignoreCase = true))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = !enabled,
                        placeholder = { Text("Enter text...") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                    )

                    if (isReviewMode) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "✓ Correct Answer: $correctAnswer",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            } else {
                // MCQ choices
                val displayChoices = remember(question.id) {
                    if (isReviewMode) question.choices else question.choices.shuffled()
                }

                displayChoices.forEach { choice ->
                    val isSelected = if (isReviewMode) choice.isCorrect else selectedChoiceId == choice.id
                    val bgColor = when {
                        isReviewMode && choice.isCorrect -> MaterialTheme.colorScheme.tertiaryContainer
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(enabled = enabled) {
                                selectedChoiceId = choice.id
                                onAnswered(choice.isCorrect)
                            },
                        shape = RoundedCornerShape(8.dp),
                        color = bgColor,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null, // handled by Surface click
                                modifier = Modifier.size(20.dp),
                                colors = if (isReviewMode && choice.isCorrect) RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.tertiary) else RadioButtonDefaults.colors()
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                choice.choiceText,
                                fontSize = if (isTablet) 16.sp else 14.sp,
                                modifier = Modifier.weight(1f) // Ensure text wraps nicely
                            )
                        }
                    }
                }
            }
        }
    }
}