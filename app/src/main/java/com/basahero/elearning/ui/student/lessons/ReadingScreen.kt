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
            if (lesson.hasLecture) add(LessonStep.Lecture(lesson.lectureText, lesson.imagePath))
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
    
    // Track answered mini-questions to enforce completion
    val answeredMiniQuestions = remember { mutableStateMapOf<String, Boolean>() }
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

    Box(modifier = Modifier.fillMaxSize()) {
        com.basahero.elearning.ui.common.PlayfulBackground(densityMultiplier = 2f)

        Scaffold(
            containerColor = Color.Transparent,
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
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.Close, contentDescription = "Exit")
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
                        val isNextEnabled = remember(currentStep, answeredMiniQuestions.size, isReviewMode) {
                            if (isReviewMode) true
                            else if (currentStep is LessonStep.ReadingPart) {
                                // All mini questions in this part must be answered
                                currentStep.part.miniQuestions.all { answeredMiniQuestions.containsKey(it.id) }
                            } else {
                                true
                            }
                        }

                        Button(
                            onClick = {
                                if (isLastStep) {
                                    onBack()
                                } else {
                                    currentStepIndex++
                                }
                            },
                            enabled = isNextEnabled,
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
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            val contentPadding = if (isTablet) 32.dp else 16.dp

            Card(
                modifier = Modifier
                    .then(
                        if (isTablet) Modifier.widthIn(max = 720.dp).padding(vertical = 24.dp)
                        else Modifier.fillMaxWidth().padding(16.dp)
                    ),
                shape = if (isTablet) RoundedCornerShape(24.dp) else RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isTablet) 4.dp else 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(contentPadding)
                ) {
                    when (val step = currentStep) {
                        is LessonStep.Lecture -> LectureStepContent(step.text, step.imagePath, isTablet)
                        is LessonStep.ReadingPart -> ReadingPartStepContent(
                            partNumber = step.partNumber,
                            part = step.part,
                            isTablet = isTablet,
                            isReviewMode = isReviewMode,
                            onQuestionAnswered = { qId -> answeredMiniQuestions[qId] = true },
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
}

// ─── Step types ──────────────────────────────────────────────────────────────
sealed class LessonStep {
    data class Lecture(val text: String, val imagePath: String? = null) : LessonStep()
    data class ReadingPart(val partNumber: Int, val part: LessonPart) : LessonStep()
    data class Activity(val partNumber: Int, val questions: List<MiniQuestion>, val introText: String? = null) : LessonStep()
}

// ─── Lecture Step ─────────────────────────────────────────────────────────────
@Composable
private fun LectureStepContent(text: String, imagePath: String?, isTablet: Boolean) {
    // Playful Notebook-style Header for Lecture
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isTablet) 260.dp else 180.dp)
            .padding(vertical = 8.dp)
    ) {
        // Decorative shadow/offset background for 3D effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = 4.dp, y = 4.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        )

        // Main Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
                .border(
                    width = 3.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!imagePath.isNullOrBlank()) {
                coil.compose.AsyncImage(
                    model = imagePath,
                    contentDescription = "Lesson Illustration",
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Larger, more playful icon
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        modifier = Modifier.size(if (isTablet) 100.dp else 72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(if (isTablet) 56.dp else 40.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Learn Something New!",
                        fontSize = if (isTablet) 22.sp else 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = com.basahero.elearning.ui.theme.fredokaFontFamily,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(24.dp))

    Text(
        text = com.basahero.elearning.util.TextUtil.parseBoldText(text),
        fontSize = if (isTablet) 20.sp else 16.sp,
        lineHeight = if (isTablet) 32.sp else 28.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
        fontFamily = com.basahero.elearning.ui.theme.fredokaFontFamily,
        textAlign = TextAlign.Start
    )
}

// ─── Reading Part Step (Passage + Mini Activity on same page) ────────────────
@Composable
private fun ReadingPartStepContent(
    partNumber: Int,
    part: LessonPart,
    isTablet: Boolean,
    isReviewMode: Boolean,
    onQuestionAnswered: (String) -> Unit,
    onPronunciationAttempt: (String, String, Boolean, Int) -> Unit
) {
    // Playful Reading Holder
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isTablet) 260.dp else 180.dp)
            .padding(vertical = 8.dp)
    ) {
        // Decorative shadow/offset
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = (-4).dp, y = 4.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
        )

        // Main Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .border(
                    width = 3.dp,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!part.imagePath.isNullOrBlank()) {
                coil.compose.AsyncImage(
                    model = part.imagePath,
                    contentDescription = "Reading Illustration",
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                        modifier = Modifier.size(if (isTablet) 100.dp else 72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(if (isTablet) 56.dp else 40.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Reading Time!",
                        fontSize = if (isTablet) 22.sp else 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = com.basahero.elearning.ui.theme.fredokaFontFamily,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
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
        isTablet = isTablet,
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
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(12.dp))

        part.miniQuestions.forEachIndexed { index, question ->
            MiniQuestionCard(
                questionNumber = index + 1,
                question = question,
                isTablet = isTablet,
                isReviewMode = isReviewMode,
                onAnswered = { onQuestionAnswered(question.id) }
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
    isReviewMode: Boolean,
    onAnswered: () -> Unit
) {
    var selectedChoiceId by remember(question.id) { mutableStateOf<String?>(null) }
    var showFeedback by remember(question.id) { mutableStateOf(false) }

    LaunchedEffect(showFeedback) {
        if (showFeedback) onAnswered()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(if (isTablet) 16.dp else 12.dp)) {
            val cleanQuestionText = question.questionText
                .replace(Regex("^Q\\d+[:.]?\\s*", RegexOption.IGNORE_CASE), "")
                .replace(Regex("^\\d+[:.]?\\s*"), "")

            Text(
                text = com.basahero.elearning.util.TextUtil.parseBoldText("Q$questionNumber: $cleanQuestionText"),
                fontSize = if (isTablet) 16.sp else 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(if (isTablet) 8.dp else 4.dp))

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
                        .heightIn(max = (currentChoices.size * 110).dp)
                        .reorderable(state)
                        .then(if (isReviewMode) Modifier else Modifier.detectReorderAfterLongPress(state)),
                    userScrollEnabled = false
                ) {
                    items(currentChoices, { it.id }) { choice ->
                        ReorderableItem(state, key = choice.id) { isDragging ->
                            val index = currentChoices.indexOf(choice) + 1
                            SequencingItemCard(
                                index = index,
                                text = choice.choiceText,
                                isDragging = isDragging,
                                isTablet = isTablet,
                                enabled = !isReviewMode
                            )
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
                    
                    Spacer(Modifier.height(12.dp))
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        border = BorderStroke(1.dp, if (isCorrect) Color(0xFF2E7D32) else Color(0xFFD32F2F))
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (isCorrect) "Excellent!" else "Wait, not quite...",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                                )
                            }
                            
                            if (!isCorrect) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "The correct order is:",
                                    fontSize = 13.sp,
                                    color = Color(0xFFD32F2F).copy(alpha = 0.8f)
                                )
                                Text(
                                    question.choices.sortedBy { it.orderIndex }.joinToString("\n") { "• ${it.choiceText}" },
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFD32F2F)
                                )
                            }
                        }
                    }
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
                            color = if (isMatch || isReviewMode) Color(0xFF2E7D32) else Color(0xFFD32F2F)
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
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            } else if (question.questionType == "MATCHING") {
                // 🚀 NEW: DRAG & DROP MATCHING UI for MiniQuestionCard
                if (!isReviewMode && !showFeedback) {
                    Text(
                        "Drag a colored rope from the left to its match on the right:",
                        fontSize = if (isTablet) 14.sp else 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                }

                var currentMatches by remember { mutableStateOf(mapOf<String, String>()) }

                MatchingDragAndDropUI(
                    question = question,
                    isTablet = isTablet,
                    isReviewMode = isReviewMode,
                    showFeedback = showFeedback,
                    onMatchesChanged = { m ->
                        currentMatches = m
                        val half = question.choices.size / 2
                        if (m.size == half && !showFeedback && !isReviewMode) {
                            showFeedback = true
                        }
                    }
                )

                if (showFeedback || isReviewMode) {
                    val half = question.choices.size / 2
                    val leftSide = question.choices.take(half)
                    val rightSide = question.choices.drop(half)
                    var allCorrect = true
                    leftSide.forEachIndexed { i, leftItem ->
                        if (currentMatches[leftItem.id] != rightSide[i].id) allCorrect = false
                    }
                    val isCorrect = allCorrect || isReviewMode
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        color = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isCorrect) Color(0xFF81C784) else Color(0xFFE57373))
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (isCorrect) "Excellent!" else "Wait, not quite...",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                                )
                            }
                            
                            if (!isCorrect) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "The correct matches are:",
                                    fontSize = 13.sp,
                                    color = Color(0xFFD32F2F).copy(alpha = 0.8f)
                                )
                                Text(
                                    leftSide.mapIndexed { index, miniChoice -> 
                                        "• ${miniChoice.choiceText} → ${rightSide[index].choiceText}" 
                                    }.joinToString("\n"),
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFD32F2F)
                                )
                            }
                        }
                    }
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
                            color = if (isMatch || isReviewMode) Color(0xFF2E7D32) else Color(0xFFD32F2F)
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
                            isReviewMode && isCorrect -> Color(0xFFE8F5E9)
                            !showFeedback -> if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            isCorrect -> Color(0xFFE8F5E9) // Light Green
                            isSelected && !isCorrect -> Color(0xFFFFEBEE) // Light Red
                            else -> MaterialTheme.colorScheme.surface
                        }

                        Surface(
                            modifier = Modifier
                                .then(if (question.id.contains("-04-")) Modifier.fillMaxWidth() else Modifier)
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
                                text = com.basahero.elearning.util.TextUtil.parseBoldText(choice.choiceText),
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
                        color = if (isFullyCorrect) Color(0xFF2E7D32) else Color(0xFFD32F2F)
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
                        isReviewMode && choice.isCorrect -> Color(0xFFE8F5E9)
                        !showFeedback -> if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        choice.isCorrect -> Color(0xFFE8F5E9)
                        isSelected && !choice.isCorrect -> Color(0xFFFFEBEE)
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
                                text = com.basahero.elearning.util.TextUtil.parseBoldText(choice.choiceText),
                                fontSize = if (isTablet) 14.sp else 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (showFeedback && choice.isCorrect) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                            }
                            if (showFeedback && isSelected && !choice.isCorrect) {
                                Icon(Icons.Default.Cancel, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
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
                        color = if (correct) Color(0xFF2E7D32) else Color(0xFFD32F2F)
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
    // Simplified Title (Holder Removed as requested)
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Activity $partNumber",
            fontSize = if (isTablet) 28.sp else 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        )
    }

    Spacer(Modifier.height(if (isTablet) 24.dp else 16.dp))

    if (!introText.isNullOrBlank()) {
        Text(
            text = com.basahero.elearning.util.TextUtil.parseBoldText(introText),
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
        Spacer(Modifier.height(if (isTablet) 24.dp else 16.dp))
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
                fontSize = if (isTablet) 64.sp else 48.sp
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
                    .size(if (isTablet) 200.dp else 140.dp)
                    .clip(CircleShape)
                    .background(circleColor.copy(alpha = 0.12f))
                    .border(if (isTablet) 6.dp else 4.dp, circleColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$pct%",
                        fontSize = if (isTablet) 48.sp else 32.sp,
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
        Column(modifier = Modifier.padding(if (isTablet) 16.dp else 12.dp)) {
            val cleanQuestionText = question.questionText
                .replace(Regex("^Q\\d+[:.]?\\s*", RegexOption.IGNORE_CASE), "")
                .replace(Regex("^\\d+[:.]?\\s*"), "")

            Text(
                text = com.basahero.elearning.util.TextUtil.parseBoldText("Q$questionNumber: $cleanQuestionText"),
                fontSize = if (isTablet) 18.sp else 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(if (isTablet) 12.dp else 8.dp))

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
                        .heightIn(max = (currentChoices.size * 110).dp)
                        .reorderable(state)
                        .then(if (enabled) Modifier.detectReorderAfterLongPress(state) else Modifier),
                    userScrollEnabled = false
                ) {
                    items(currentChoices, { it.id }) { choice ->
                        ReorderableItem(state, key = choice.id) { isDragging ->
                            val index = currentChoices.indexOf(choice) + 1
                            SequencingItemCard(
                                index = index,
                                text = choice.choiceText,
                                isDragging = isDragging,
                                isTablet = isTablet,
                                enabled = enabled
                            )
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
                        color = Color(0xFF2E7D32)
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
                            isReviewMode && choice.isCorrect -> Color(0xFFE8F5E9)
                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        }

                        Surface(
                            modifier = Modifier
                                .then(if (question.id.contains("-04-")) Modifier.fillMaxWidth() else Modifier)
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
                                text = com.basahero.elearning.util.TextUtil.parseBoldText(choice.choiceText),
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
                        color = Color(0xFF2E7D32)
                    )
                }
            } else if (question.questionType == "MATCHING") {
                // 🚀 NEW: DRAG & DROP MATCHING UI (Column A | Column B)
                var currentMatches by remember { mutableStateOf(mapOf<String, String>()) }
                var resetKey by remember { mutableIntStateOf(0) }
                
                if (!isReviewMode) {
                    Text(
                        "Drag a colored rope from the left to its match on the right:",
                        fontSize = if (isTablet) 14.sp else 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))
                }

                key(resetKey) {
                    MatchingDragAndDropUI(
                        question = question,
                        isTablet = isTablet,
                        isReviewMode = isReviewMode,
                        showFeedback = false,
                        onMatchesChanged = { m ->
                            currentMatches = m
                            val half = question.choices.size / 2
                            if (m.size == half && !isReviewMode) {
                                val leftSide = question.choices.take(half)
                                val rightSide = question.choices.drop(half)
                                var allCorrect = true
                                leftSide.forEachIndexed { i, leftItem ->
                                    if (m[leftItem.id] != rightSide[i].id) allCorrect = false
                                }
                                onAnswered(allCorrect)
                            }
                        }
                    )
                }
                
                if (currentMatches.isNotEmpty() && !isReviewMode) {
                    TextButton(onClick = { 
                        currentMatches = emptyMap()
                        resetKey++
                        onAnswered(false) 
                    }, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Reset Matches", color = Color(0xFFD32F2F))
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
                            color = Color(0xFF2E7D32)
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
                        isReviewMode && choice.isCorrect -> Color(0xFFE8F5E9)
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = if (isTablet) 4.dp else 2.dp)
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
                            modifier = Modifier.padding(if (isTablet) 14.dp else 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null, // handled by Surface click
                                modifier = Modifier.size(20.dp),
                                colors = if (isReviewMode && choice.isCorrect) RadioButtonDefaults.colors(selectedColor = Color(0xFF2E7D32)) else RadioButtonDefaults.colors()
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = com.basahero.elearning.util.TextUtil.parseBoldText(choice.choiceText),
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
@Composable
private fun SequencingItemCard(
    index: Int,
    text: String,
    isDragging: Boolean,
    isTablet: Boolean,
    enabled: Boolean
) {
    val elevation = if (isDragging) 8.dp else 2.dp
    val bgColor = if (isDragging) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        shadowElevation = elevation,
        border = BorderStroke(
            width = if (isDragging) 2.dp else 1.dp,
            color = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = if (isTablet) 12.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Number Badge
            Surface(
                modifier = Modifier.size(if (isTablet) 32.dp else 28.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = index.toString(),
                        fontSize = if (isTablet) 16.sp else 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(Modifier.width(12.dp))
            
            Text(
                text = com.basahero.elearning.util.TextUtil.parseBoldText(text),
                fontSize = if (isTablet) 16.sp else 14.sp,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (enabled) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Default.DragHandle,
                    contentDescription = "Drag",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
 }
 }
 }
}
