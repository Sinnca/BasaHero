package com.basahero.elearning.ui.student.lessons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basahero.elearning.data.model.Lesson
import com.basahero.elearning.data.model.LessonStatus
import com.basahero.elearning.ui.common.LocalAppStrings

// ─────────────────────────────────────────────────────────────────────────────
// LessonListScreen — wireframe accurate, centered column for tablet
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonListScreen(
    quarterId: String,
    studentId: String,
    quarterTitle: String,
    viewModel: LessonListViewModel,
    onLessonClick: (lessonId: String) -> Unit,
    onPreTestClick: () -> Unit,
    onPostTestClick: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    val strings = LocalAppStrings.current

    LaunchedEffect(quarterId) {
        viewModel.loadLessons(quarterId, studentId, quarterTitle)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = uiState.quarterTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isTablet) 22.sp else 20.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .then(
                        if (isTablet) Modifier.widthIn(max = 800.dp) else Modifier.fillMaxWidth()
                    )
                    .padding(horizontal = if (isTablet) 32.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // ── Pre-Test Card
                if (uiState.hasPrePostContent) {
                    item {
                        PrePostCard(
                            title = strings.preTest,
                            subtitle = if (uiState.isPreTestDone) strings.done else "Required to unlock lessons",
                            isDone = uiState.isPreTestDone,
                            isLocked = false, // Pre-test is always open
                            icon = Icons.Default.Assignment,
                            isTablet = isTablet,
                            onClick = onPreTestClick
                        )
                    }
                }

                // ── Lessons
                itemsIndexed(uiState.lessons) { index, lesson ->
                    LessonCard(
                        lesson = lesson,
                        lessonNumber = index + 1,
                        isTablet = isTablet,
                        onClick = {
                            if (lesson.status != LessonStatus.LOCKED) {
                                onLessonClick(lesson.id)
                            }
                        }
                    )
                }

                // ── Post-Test Card
                if (uiState.hasPrePostContent) {
                    item {
                        PrePostCard(
                            title = strings.postTest,
                            subtitle = if (uiState.isPostTestDone) strings.done else "Unlock by finishing all lessons",
                            isDone = uiState.isPostTestDone,
                            isLocked = !uiState.allLessonsDone,
                            icon = Icons.Default.EmojiEvents,
                            isTablet = isTablet,
                            onClick = { if (uiState.allLessonsDone) onPostTestClick() }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared Pre/Post Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PrePostCard(
    title: String,
    subtitle: String,
    isDone: Boolean,
    isLocked: Boolean,
    icon: ImageVector,
    isTablet: Boolean,
    onClick: () -> Unit
) {
    val successColor = Color(0xFF10B981) // Green
    val primaryColor = Color(0xFF2563EB) // Blue
    val greyColor = Color(0xFF9CA3AF)

    val containerColor = MaterialTheme.colorScheme.surface
    val borderColor = when {
        isDone -> successColor.copy(alpha = 0.5f)
        isLocked -> greyColor.copy(alpha = 0.3f)
        else -> primaryColor.copy(alpha = 0.5f)
    }

    Card(
        onClick = onClick,
        enabled = !isLocked,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLocked) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(if (isTablet) 24.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(if (isTablet) 56.dp else 48.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isDone -> successColor.copy(alpha = 0.15f)
                            isLocked -> greyColor.copy(alpha = 0.15f)
                            else -> primaryColor.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDone) Icons.Default.Check else if (isLocked) Icons.Default.Lock else icon,
                    contentDescription = null,
                    tint = when {
                        isDone -> successColor
                        isLocked -> greyColor
                        else -> primaryColor
                    },
                    modifier = Modifier.size(if (isTablet) 28.dp else 24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = if (isTablet) 18.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLocked) greyColor else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = if (isTablet) 14.sp else 13.sp,
                    color = if (isLocked) greyColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Lesson Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LessonCard(
    lesson: Lesson,
    lessonNumber: Int,
    isTablet: Boolean,
    onClick: () -> Unit
) {
    val isDone = lesson.status == LessonStatus.DONE
    val isInProgress = lesson.status == LessonStatus.IN_PROGRESS
    val isLocked = lesson.status == LessonStatus.LOCKED

    val successColor = Color(0xFF10B981) // Green
    val primaryColor = Color(0xFF2563EB) // Blue
    val warningColor = Color(0xFFF59E0B) // Orange
    val greyColor = Color(0xFF9CA3AF)

    val containerColor = MaterialTheme.colorScheme.surface
    val borderColor = when {
        isDone -> successColor.copy(alpha = 0.5f)
        isInProgress -> warningColor.copy(alpha = 0.5f)
        else -> greyColor.copy(alpha = 0.3f)
    }

    Card(
        onClick = onClick,
        enabled = !isLocked,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLocked) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(if (isTablet) 24.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(if (isTablet) 56.dp else 48.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isDone -> successColor.copy(alpha = 0.15f)
                            isInProgress -> warningColor.copy(alpha = 0.1f)
                            else -> greyColor.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isDone -> Icon(
                        Icons.Default.Check,
                        contentDescription = "Done",
                        tint = successColor,
                        modifier = Modifier.size(if (isTablet) 28.dp else 24.dp)
                    )
                    isInProgress -> Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = warningColor,
                        modifier = Modifier.size(if (isTablet) 28.dp else 24.dp)
                    )
                    else -> Icon(
                        Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = greyColor,
                        modifier = Modifier.size(if (isTablet) 24.dp else 20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Lesson $lessonNumber: ${lesson.competency}",
                    fontSize = if (isTablet) 14.sp else 12.sp,
                    color = when {
                        isDone -> successColor
                        isInProgress -> warningColor
                        else -> greyColor
                    },
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lesson.title,
                    fontSize = if (isTablet) 18.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLocked) greyColor else MaterialTheme.colorScheme.onSurface
                )
            }

            StatusChip(lesson.status)
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val strings = LocalAppStrings.current
    val (text, color) = when (status) {
        LessonStatus.DONE -> strings.done to Color(0xFF10B981)
        LessonStatus.IN_PROGRESS -> strings.inProgress to Color(0xFFF59E0B)
        else -> strings.locked to Color(0xFF9CA3AF)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}