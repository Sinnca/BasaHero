package com.basahero.elearning.ui.student.lessons

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basahero.elearning.ui.common.AnimatedScrollIndicator

// ─────────────────────────────────────────────────────────────────────────────
// ReadingScreen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(
    lessonId: String,
    viewModel: ReadingViewModel,
    onStartQuiz: (lessonId: String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(lessonId) {
        viewModel.loadLesson(lessonId)
    }

    // Detect when scrolled near bottom (within 200px)
//    LaunchedEffect(scrollState.value) {
//        if (scrollState.value > 0 &&
//            scrollState.value >= scrollState.maxValue - 200) {
//            viewModel.onScrolledToBottom()
//        }
//    }
    // ✅ FIXED: Calculates the bottom threshold WITHOUT recomposing the UI on every pixel!
    val isAtBottom by remember {
        derivedStateOf {
            scrollState.maxValue > 0 && scrollState.value >= scrollState.maxValue - 200
        }
    }

    LaunchedEffect(isAtBottom) {
        if (isAtBottom && !uiState.readingComplete) {
            viewModel.onScrolledToBottom()
        }
    }

    if (uiState.isLoading || uiState.lesson == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val lesson = uiState.lesson!!

    // Compute scroll fraction for the animated indicator (avoid divide-by-zero)
    val scrollFraction = if (scrollState.maxValue > 0)
        scrollState.value.toFloat() / scrollState.maxValue
    else 0f

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(lesson.competency, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Text(lesson.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
                // Animated reading-progress bar + bouncing scroll hint
                AnimatedScrollIndicator(
                    scrollFraction = scrollFraction,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        bottomBar = {
            // Quiz button appears once student scrolls through the passage
            AnimatedVisibility(visible = uiState.readingComplete) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp
                ) {
                    Button(
                        onClick = { onStartQuiz(lessonId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Quiz,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Take the Quiz",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Scroll hint while not at bottom
            AnimatedVisibility(visible = !uiState.readingComplete) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Scroll down to read the full passage",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Placeholder for Image (Replaces AsyncImage to prevent crashes)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = "Story Icon",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Competency badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = lesson.competency,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lesson title
            Text(
                text = lesson.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Passage text (Changed from passageText to content)
            // 🚀 NEW INTERACTIVE TEXT
            HighlightedPassageText(
                passageText = lesson.passageText,
                highlightedWords = uiState.highlightedWords,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // End of passage indicator
            if (uiState.readingComplete) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "You've finished reading! Ready for the quiz?",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}