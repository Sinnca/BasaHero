package com.basahero.elearning.ui.teacher.analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basahero.elearning.data.repository.LessonPerformance
import com.basahero.elearning.data.repository.StudentInfo
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// ClassAnalyticsScreen
// Week 10 — Class-wide quiz results per competency
//   Tab 0: Lesson performance (sortable by score / name)
//   Tab 1: Students needing intervention (at-risk list)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassAnalyticsScreen(
    classId: String,
    className: String,
    viewModel: ClassAnalyticsViewModel,
    onStudentClick: (studentId: String, studentName: String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }

    LaunchedEffect(classId) { viewModel.load(classId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Class Analytics", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(className, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Sort button — only shown on Lessons tab
                    if (uiState.selectedTab == 0) {
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, "Sort")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("📈 Score: Low → High") },
                                    onClick = {
                                        viewModel.setSortOrder(AnalyticsSortOrder.SCORE_ASC)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (uiState.sortOrder == AnalyticsSortOrder.SCORE_ASC)
                                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("📉 Score: High → Low") },
                                    onClick = {
                                        viewModel.setSortOrder(AnalyticsSortOrder.SCORE_DESC)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (uiState.sortOrder == AnalyticsSortOrder.SCORE_DESC)
                                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🔤 Lesson Name A→Z") },
                                    onClick = {
                                        viewModel.setSortOrder(AnalyticsSortOrder.NAME_ASC)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (uiState.sortOrder == AnalyticsSortOrder.NAME_ASC)
                                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Tabs ──────────────────────────────────────────────────────────
            TabRow(selectedTabIndex = uiState.selectedTab) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text("📊 Lessons") }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚠️ Intervention")
                            if (uiState.atRiskStudents.isNotEmpty()) {
                                Spacer(Modifier.width(6.dp))
                                Badge { Text("${uiState.atRiskStudents.size}") }
                            }
                        }
                    }
                )
            }

            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.errorMessage != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            uiState.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
                uiState.selectedTab == 0 -> LessonPerformanceTab(
                    lessons = viewModel.sortedLessons(uiState)
                )
                uiState.selectedTab == 1 -> InterventionTab(
                    students = uiState.atRiskStudents,
                    onStudentClick = onStudentClick
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tab 0 — Lesson Performance
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LessonPerformanceTab(lessons: List<LessonPerformance>) {
    if (lessons.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📊", fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text("No lesson data yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Data appears once students complete quizzes.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
        return
    }

    val lowCount = lessons.count { it.isLowPerforming }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Summary chips
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryBadge("${lessons.size} Lessons", MaterialTheme.colorScheme.primaryContainer)
                if (lowCount > 0) {
                    SummaryBadge("⚠️ $lowCount Low-Scoring", MaterialTheme.colorScheme.errorContainer)
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        items(lessons) { lesson ->
            LessonPerformanceCard(lesson)
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun LessonPerformanceCard(lesson: LessonPerformance) {
    val passRate = lesson.passRate
    val isLow = lesson.isLowPerforming

    val animatedPass by animateFloatAsState(
        targetValue = passRate,
        animationSpec = tween(600),
        label = "pass_bar"
    )
    val animatedAvg by animateFloatAsState(
        targetValue = lesson.averageScore,
        animationSpec = tween(700),
        label = "avg_bar"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLow)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lesson.lessonTitle.ifBlank { lesson.lessonId.take(14) },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (lesson.competency.isNotBlank()) {
                        Text(
                            lesson.competency,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (isLow) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.error
                    ) {
                        Text(
                            "LOW",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                } else {
                    Text(
                        "${(passRate * 100).roundToInt()}% pass",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Pass rate bar
            BarRow(
                label = "Pass Rate",
                value = animatedPass,
                label2 = "${(passRate * 100).roundToInt()}%",
                color = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))
            // Average score bar
            BarRow(
                label = "Avg Score",
                value = animatedAvg,
                label2 = "${(lesson.averageScore * 100).roundToInt()}%",
                color = MaterialTheme.colorScheme.tertiary
            )

            Spacer(Modifier.height(12.dp))

            // Pass / Fail / Not Attempted chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniChip("✅ ${lesson.passCount} Passed",  Color(0xFF2E7D32), Color(0xFFE8F5E9))
                MiniChip("❌ ${lesson.failCount} Failed",  MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.errorContainer)
                if (lesson.notAttempted > 0) {
                    MiniChip("— ${lesson.notAttempted} Skipped", MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tab 1 — Intervention (At-Risk Students)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun InterventionTab(
    students: List<StudentInfo>,
    onStudentClick: (studentId: String, studentName: String) -> Unit
) {
    if (students.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎉", fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text("No at-risk students!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "All students are performing at 60% or above.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${students.size} student(s) have at least one lesson with best score below 60%. Tap to view their progress.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        items(students) { student ->
            AtRiskStudentCard(student = student, onClick = { onStudentClick(student.id, student.fullName) })
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun AtRiskStudentCard(student: StudentInfo, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = student.fullName.split(" ")
                        .take(2).joinToString("") { it.firstOrNull()?.uppercase() ?: "" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(student.fullName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(
                    student.section,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                student.lastActive?.let {
                    Text(
                        "Last active: ${it.take(10)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        "At Risk",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(4.dp))
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared helpers
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun BarRow(label: String, value: Float, label2: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            fontSize = 11.sp,
            modifier = Modifier.width(68.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LinearProgressIndicator(
            progress = { value },
            modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )
        Spacer(Modifier.width(8.dp))
        Text(label2, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(36.dp))
    }
}

@Composable
fun MiniChip(text: String, textColor: Color, bgColor: Color) {
    Surface(shape = RoundedCornerShape(20.dp), color = bgColor) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 10.sp,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SummaryBadge(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(20.dp), color = color) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
