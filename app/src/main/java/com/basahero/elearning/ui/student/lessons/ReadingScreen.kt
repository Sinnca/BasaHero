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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basahero.elearning.ui.common.AnimatedScrollIndicator
import com.basahero.elearning.ui.common.LocalAppStrings

// ─────────────────────────────────────────────────────────────────────────────
// ReadingScreen — tablet + phone responsive
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
    val strings = LocalAppStrings.current
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val isTablet = screenWidth >= 600

    LaunchedEffect(lessonId) {
        viewModel.loadLesson(lessonId)
    }

    // Detect when scrolled near bottom (within 200px)
    val isAtBottom by remember {
        derivedStateOf {
            scrollState.maxValue == 0 || scrollState.value >= scrollState.maxValue - 200
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

    // Adaptive sizes
    val contentPadding = if (isTablet) 32.dp else 16.dp
    val titleFontSize = if (isTablet) 28.sp else 22.sp
    val passageFontSize = if (isTablet) 18.sp else 15.sp
    val imageHeight = if (isTablet) 220.dp else 150.dp

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                lesson.competency,
                                fontSize = if (isTablet) 14.sp else 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                lesson.title,
                                fontSize = if (isTablet) 18.sp else 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
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
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { onStartQuiz(lessonId) },
                            modifier = Modifier
                                .then(
                                    if (isTablet) Modifier.width(480.dp) else Modifier.fillMaxWidth()
                                )
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
                                strings.takeTheQuiz,
                                fontSize = if (isTablet) 18.sp else 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
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
                            strings.scrollDownToRead,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { padding ->
        // Center-constrained content for tablet
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                modifier = Modifier
                    .then(
                        if (isTablet) Modifier
                            .widthIn(max = 720.dp)
                            .padding(vertical = 24.dp)
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
                // Placeholder for Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = "Story Icon",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(if (isTablet) 64.dp else 48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(if (isTablet) 28.dp else 20.dp))

                // Competency badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = lesson.competency,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        fontSize = if (isTablet) 14.sp else 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Lesson title
                Text(
                    text = lesson.title,
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Passage text — interactive highlighted text
                HighlightedPassageText(
                    passageText = lesson.passageText,
                    highlightedWords = uiState.highlightedWords,
                    modifier = Modifier.fillMaxWidth(),
                    onPronunciationAttempt = { word, heard, isCorrect, _ ->
                        val score = if (isCorrect) 100 else 0
                        viewModel.savePronunciationAttempt(word, heard, isCorrect, score)
                    }
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
                            modifier = Modifier.size(if (isTablet) 24.dp else 20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            strings.youveFinishedReading,
                            fontSize = if (isTablet) 16.sp else 14.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            } // end Card
        }
    }
}