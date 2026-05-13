package com.basahero.elearning.ui.student.lessons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basahero.elearning.data.model.Lesson
import com.basahero.elearning.data.model.LessonStatus
import com.basahero.elearning.ui.common.LocalAppStrings

import com.basahero.elearning.ui.student.home.StudentBottomNavBar

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
    onNavigateHome: () -> Unit,
    onNavigateGame: () -> Unit,
    onNavigateProfile: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    val strings = LocalAppStrings.current

    LaunchedEffect(quarterId) {
        viewModel.loadLessons(quarterId, studentId, quarterTitle)
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val darkColor = Color(primaryColor.red * 0.6f, primaryColor.green * 0.6f, primaryColor.blue * 0.8f)
    val totalLessons = uiState.lessons.size
    val doneLessons = uiState.lessons.count { it.status == LessonStatus.DONE }
    val progressPercent = if (totalLessons == 0) 0f else doneLessons.toFloat() / totalLessons

    Scaffold(
        bottomBar = {
            StudentBottomNavBar(
                selectedTab = 1,
                onTabSelected = { tab ->
                    when (tab) {
                        0 -> onNavigateHome()
                        1 -> onBack()
                        2 -> onNavigateGame()
                        3 -> onNavigateProfile()
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                ) {
                    // Radial background + Bubbles
                    Box(modifier = Modifier.matchParentSize().background(
                        Brush.radialGradient(listOf(primaryColor.copy(alpha=0.85f), primaryColor, darkColor), radius = 1200f)
                    ))
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(Color.White.copy(0.06f), size.width * 0.35f, androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.1f))
                        drawCircle(Color.White.copy(0.04f), size.width * 0.2f, androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.8f))
                    }

                    Column(
                        modifier = Modifier.padding(
                            top = if (isTablet) 24.dp else 16.dp,
                            bottom = if (isTablet) 32.dp else 24.dp,
                            start = if (isTablet) 32.dp else 20.dp,
                            end = if (isTablet) 32.dp else 20.dp
                        )
                    ) {
                        // Back Button & Title Row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(if (isTablet) 48.dp else 40.dp)
                                    .background(Color.White.copy(alpha=0.2f), RoundedCornerShape(12.dp))
                                    .clickable { onBack() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription="Back", tint=Color.White)
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = uiState.quarterTitle,
                                    fontSize = if (isTablet) 15.sp else 13.sp,
                                    color = Color.White.copy(0.85f),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Your Lessons",
                                    fontSize = if (isTablet) 28.sp else 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(Modifier.height(if (isTablet) 24.dp else 16.dp))

                        // Progress Row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Ring
                            Box(modifier = Modifier.size(if(isTablet) 80.dp else 64.dp), contentAlignment = Alignment.Center) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val strk = if(isTablet) 8.dp.toPx() else 6.dp.toPx()
                                    drawArc(Color.White.copy(0.3f), -90f, 360f, false, style=Stroke(strk, cap=StrokeCap.Round))
                                    drawArc(Color.White, -90f, 360f * progressPercent, false, style=Stroke(strk, cap=StrokeCap.Round))
                                }
                                Text(
                                    "${(progressPercent * 100).toInt()}%",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = if(isTablet) 20.sp else 16.sp
                                )
                            }

                            Spacer(Modifier.width(20.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "$doneLessons of $totalLessons done!",
                                    fontSize = if (isTablet) 18.sp else 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Post-Test unlocks after Pre-Test",
                                    fontSize = if (isTablet) 14.sp else 12.sp,
                                    color = Color.White.copy(0.85f)
                                )
                                Spacer(Modifier.height(8.dp))
                                // Progress Dashes
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    for (i in 0 until totalLessons) {
                                        Box(
                                            modifier = Modifier
                                                .height(6.dp)
                                                .weight(1f)
                                                .background(
                                                    if (i < doneLessons) Color.White else Color.White.copy(0.3f),
                                                    RoundedCornerShape(3.dp)
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }

            // ── Lists ────────────────────────────────────────────────────────
            val contentPad = if (isTablet) 32.dp else 20.dp

            if (uiState.hasPrePostContent) {
                item {
                    Box(modifier = Modifier.padding(horizontal = contentPad)) {
                        PrePostCard(
                            title = strings.preTest,
                            subtitle = if (uiState.isPreTestDone) "Completed" else "Start here before lessons",
                            isDone = uiState.isPreTestDone,
                            isLocked = false,
                            icon = Icons.Default.Assignment,
                            isTablet = isTablet,
                            onClick = onPreTestClick,
                            isPreTest = true
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            itemsIndexed(uiState.lessons) { index, lesson ->
                Box(modifier = Modifier.padding(horizontal = contentPad)) {
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
                Spacer(Modifier.height(16.dp))
            }

            if (uiState.hasPrePostContent) {
                item {
                    Box(modifier = Modifier.padding(horizontal = contentPad)) {
                        PrePostCard(
                            title = strings.postTest,
                            subtitle = if (uiState.isPostTestDone) "Completed" else "Ready to take",
                            isDone = uiState.isPostTestDone,
                            isLocked = !uiState.isPreTestDone,
                            icon = Icons.Default.EmojiEvents,
                            isTablet = isTablet,
                            onClick = { if (uiState.isPreTestDone) onPostTestClick() },
                            isPreTest = false
                        )
                    }
                    Spacer(Modifier.height(32.dp))
                }
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
    onClick: () -> Unit,
    isPreTest: Boolean
) {
    val bgColor = when {
        isDone -> Color(0xFFD1FAE5) // Green
        isLocked -> Color(0xFFF3F4F6) // Grey
        else -> Color(0xFFE0F2FE) // Blue for active tests
    }
    val borderColor = when {
        isDone -> Color(0xFF6EE7B7)
        isLocked -> Color(0xFFE5E7EB)
        else -> Color(0xFF7DD3FC)
    }
    val textColor = when {
        isDone -> Color(0xFF065F46)
        isLocked -> Color(0xFF9CA3AF)
        else -> Color(0xFF0369A1)
    }

    Card(
        onClick = onClick,
        enabled = !isLocked,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(2.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(if (isTablet) 24.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(if (isTablet) 64.dp else 56.dp)
                    .background(Color.White.copy(0.6f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(if (isTablet) 32.dp else 28.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = if (isTablet) 20.sp else 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor
                )
                Text(
                    text = subtitle,
                    fontSize = if (isTablet) 14.sp else 13.sp,
                    color = textColor.copy(0.75f)
                )
            }

            // Status Pill
            if (isDone) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFD1FAE5),
                    contentColor = Color(0xFF065F46)
                ) {
                    Text("Done \u2713", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            } else if (!isLocked) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = textColor,
                    contentColor = Color.White
                ) {
                    Text("Start \u25B6", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
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

    val primaryColor = MaterialTheme.colorScheme.primary
    
    // In-progress colors (Amber/Yellow)
    val inProgressBg = Color(0xFFFFFBEB)
    val inProgressBorder = Color(0xFFFCD34D)
    val inProgressText = Color(0xFFB45309)
    val inProgressIconBg = Color(0xFFF59E0B)

    val containerColor = when {
        isLocked -> Color(0xFFF8FAFC)
        isInProgress -> inProgressBg
        else -> primaryColor.copy(alpha = 0.08f)
    }
    
    val borderColor = when {
        isLocked -> Color(0xFFE2E8F0)
        isInProgress -> inProgressBorder
        else -> primaryColor
    }
    
    val iconBgColor = when {
        isLocked -> Color(0xFFE2E8F0)
        isInProgress -> inProgressIconBg
        else -> primaryColor
    }
    
    val titleColor = when {
        isLocked -> Color(0xFF94A3B8)
        isInProgress -> inProgressText
        else -> primaryColor
    }

    Card(
        onClick = onClick,
        enabled = !isLocked,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(2.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(if (isTablet) 24.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon / Number shape
            Box(
                modifier = Modifier
                    .size(if (isTablet) 64.dp else 56.dp)
                    .background(iconBgColor, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(32.dp))
                } else {
                    Text(
                        text = "$lessonNumber",
                        color = if (isLocked) Color(0xFF94A3B8) else Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                val displayTitle = if (lesson.title.startsWith("Lesson", ignoreCase = true)) {
                    lesson.title
                } else {
                    "Lesson $lessonNumber: ${lesson.title}"
                }
                Text(
                    text = displayTitle,
                    fontSize = if (isTablet) 20.sp else 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = titleColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when {
                            isDone -> "Completed"
                            isInProgress -> "Reading now \u2022 0% done"
                            else -> "Locked"
                        },
                        fontSize = if (isTablet) 14.sp else 13.sp,
                        color = titleColor.copy(0.75f)
                    )
                    
                    if (isDone || isInProgress) {
                        Spacer(Modifier.width(8.dp))
                        Row {
                            Icon(Icons.Default.Star, contentDescription=null, tint=if (isDone) Color(0xFFFFC107) else Color.LightGray, modifier=Modifier.size(16.dp))
                            Icon(Icons.Default.Star, contentDescription=null, tint=if (isDone) Color(0xFFFFC107) else Color.LightGray, modifier=Modifier.size(16.dp))
                            Icon(Icons.Default.Star, contentDescription=null, tint=if (isDone) Color(0xFFFFC107) else Color.LightGray, modifier=Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Action Pill or Score
            if (!isLocked) {
                if (isDone) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFE2E8F0),
                        contentColor = primaryColor
                    ) {
                        Text(
                            text = "Review",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = inProgressIconBg,
                        contentColor = Color.White
                    ) {
                        Text(
                            text = "Now \u25B6",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Icon(Icons.Default.Lock, contentDescription="Locked", tint=Color(0xFFCBD5E1))
            }
        }
    }
}