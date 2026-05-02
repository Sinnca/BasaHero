package com.basahero.elearning.ui.student.lessons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basahero.elearning.data.model.Lesson
import com.basahero.elearning.data.model.LessonStatus
import com.basahero.elearning.data.repository.LessonRepository
import com.basahero.elearning.data.repository.PrePostRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.background

// ─────────────────────────────────────────────────────────────────────────────
// LessonListViewModel
// ─────────────────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────────────────
// LessonListScreen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonListScreen(
    quarterId: String,
    studentId: String,
    quarterTitle: String,
    viewModel: LessonListViewModel,
    onLessonClick: (lessonId: String) -> Unit,
    onPreTestClick: () -> Unit, // 👈 NEW Action
    onPostTestClick: () -> Unit, // 👈 NEW Action
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(quarterId) {
        viewModel.loadLessons(quarterId, studentId, quarterTitle)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.quarterTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // 🚀 PHASE 3B: Pre-Test Card (Top)
            if (uiState.hasPrePostContent) {
                item {
                    PrePostCard(
                        title = "Quarter Pre-Test",
                        subtitle = if (uiState.isPreTestDone) "Completed" else "Required to unlock lessons",
                        isDone = uiState.isPreTestDone,
                        isLocked = false, // Pre-test is never locked!
                        icon = Icons.Default.Assignment,
                        onClick = onPreTestClick
                    )
                }
            }

            // Normal Lessons
            itemsIndexed(uiState.lessons) { index, lesson ->
                LessonCard(
                    lesson = lesson,
                    lessonNumber = index + 1,
                    onClick = {
                        if (lesson.status != LessonStatus.LOCKED) onLessonClick(lesson.id)
                    }
                )
            }

            // 🚀 PHASE 3B: Post-Test Card (Bottom)
            if (uiState.hasPrePostContent) {
                item {
                    PrePostCard(
                        title = "Quarter Post-Test",
                        subtitle = if (uiState.isPostTestDone) "Completed" else "Unlock by finishing all lessons",
                        isDone = uiState.isPostTestDone,
                        isLocked = !uiState.allLessonsDone, // Locked until all lessons are done!
                        icon = Icons.Default.EmojiEvents,
                        onClick = { if (uiState.allLessonsDone) onPostTestClick() }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// ── Shared Pre/Post Card ──────────────────────────────────────────────────────
@Composable
fun PrePostCard(
    title: String,
    subtitle: String,
    isDone: Boolean,
    isLocked: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val containerColor = when {
        isDone -> MaterialTheme.colorScheme.tertiaryContainer
        isLocked -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    val borderColor = when {
        isDone -> MaterialTheme.colorScheme.tertiary
        isLocked -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        onClick = onClick,
        enabled = !isLocked,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDone) Icons.Default.Check else if (isLocked) Icons.Default.Lock else icon,
                    contentDescription = null,
                    tint = if (isDone) MaterialTheme.colorScheme.tertiary else if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ── Lesson Card ───────────────────────────────────────────────────────────────
@Composable
fun LessonCard(
    lesson: Lesson,
    lessonNumber: Int,
    onClick: () -> Unit
) {
    val isDone = lesson.status == LessonStatus.DONE
    val isInProgress = lesson.status == LessonStatus.IN_PROGRESS
    val isLocked = lesson.status == LessonStatus.LOCKED

    val containerColor = when {
        isDone -> MaterialTheme.colorScheme.tertiaryContainer
        isInProgress -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val borderColor = when {
        isDone -> MaterialTheme.colorScheme.tertiary
        isInProgress -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Card(
        onClick = onClick,
        enabled = !isLocked,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isDone -> Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Done",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(44.dp)
                    )
                    isInProgress -> Text(
                        text = "$lessonNumber",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    else -> Icon(
                        Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lesson.competency,
                    fontSize = 11.sp,
                    color = when {
                        isDone -> MaterialTheme.colorScheme.onTertiaryContainer
                        isInProgress -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = lesson.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isLocked)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }

            StatusChip(lesson.status)
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (text, color) = when (status) {
        LessonStatus.DONE -> "Done" to MaterialTheme.colorScheme.tertiary
        LessonStatus.IN_PROGRESS -> "Start" to MaterialTheme.colorScheme.primary
        else -> "Locked" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}