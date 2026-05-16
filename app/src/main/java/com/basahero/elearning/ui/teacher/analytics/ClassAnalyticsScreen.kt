package com.basahero.elearning.ui.teacher.analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basahero.elearning.data.repository.LessonPerformance
import com.basahero.elearning.data.repository.StudentInfo
import com.basahero.elearning.ui.theme.fredokaFontFamily
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
        containerColor = Color(0xFFF1F5F9)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 40.dp)
            ) {
                // Integrated Back Button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Column(modifier = Modifier.padding(start = 64.dp)) {
                    Text(
                        text = "CLASS ANALYTICS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Performance Insights",
                        fontFamily = fredokaFontFamily,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 36.sp
                    )
                    Text(
                        text = "Academic Year 2024-2025",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }
            }

            // ── Tabs ──────────────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = Color.Transparent,
                divider = { },
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab]),
                        color = MaterialTheme.colorScheme.primary,
                        height = 4.dp
                    )
                }
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { 
                        Text(
                            "📊 Lessons", 
                            fontFamily = fredokaFontFamily, 
                            fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                        ) 
                    }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "⚠️ Intervention", 
                                fontFamily = fredokaFontFamily,
                                fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                            )
                            if (uiState.atRiskStudents.isNotEmpty()) {
                                Spacer(Modifier.width(6.dp))
                                Badge(containerColor = MaterialTheme.colorScheme.error) { 
                                    Text("${uiState.atRiskStudents.size}") 
                                }
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
                    uiState = uiState,
                    viewModel = viewModel
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
fun LessonPerformanceTab(
    uiState: ClassAnalyticsUiState,
    viewModel: ClassAnalyticsViewModel
) {
    val quarters = uiState.quarterPerformanceList

    if (quarters.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.QueryStats, 
                    contentDescription = null, 
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "No analytics data yet.", 
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = fredokaFontFamily,
                    fontSize = 18.sp
                )
            }
        }
        return
    }

    val allLessons = quarters.flatMap { it.lessons }
    val lowCount = allLessons.count { it.isLowPerforming }
    val totalPassRate = if (allLessons.isNotEmpty()) allLessons.sumOf { it.passRate.toDouble() } / allLessons.size else 0.0

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Class Health Dashboard
        item {
            Card(
                modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Row(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnalyticsStatColumn("Avg Pass Rate", "${(totalPassRate * 100).roundToInt()}%", "Class Health", Icons.Default.Favorite)
                    VerticalDivider(modifier = Modifier.height(40.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    AnalyticsStatColumn("At Risk", "$lowCount", "Needs Review", Icons.Default.PriorityHigh, isError = lowCount > 0)
                }
            }
        }

        items(quarters) { quarter ->
            QuarterAnalyticsCard(quarter = quarter, uiState = uiState, viewModel = viewModel)
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
fun AnalyticsStatColumn(label: String, value: String, unit: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isError: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon, 
            contentDescription = null, 
            modifier = Modifier.size(16.dp),
            tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value, 
            fontSize = 28.sp, 
            fontFamily = fredokaFontFamily, 
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Text(unit, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun QuarterAnalyticsCard(
    quarter: QuarterAnalyticsSummary,
    uiState: ClassAnalyticsUiState,
    viewModel: ClassAnalyticsViewModel
) {
    val rotation by animateFloatAsState(
        targetValue = if (quarter.isExpanded) 180f else 0f,
        label = "rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (quarter.isExpanded) 8.dp else 0.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
            )
            .clip(RoundedCornerShape(24.dp))
            .clickable { viewModel.toggleQuarter(quarter.quarterId) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (quarter.isExpanded) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(
            width = 1.dp, 
            color = if (quarter.isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = quarter.quarterId.takeLast(1), 
                            fontFamily = fredokaFontFamily,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = quarter.quarterTitle,
                            fontFamily = fredokaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${quarter.lessons.size} Active Lessons",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.graphicsLayer { rotationZ = rotation }
                )
            }

            AnimatedVisibility(
                visible = quarter.isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    val sorted = viewModel.sortedLessons(quarter.lessons, uiState)
                    if (sorted.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No lesson analytics for this period.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontFamily = fredokaFontFamily
                            )
                        }
                    } else {
                        sorted.forEach { lesson ->
                            LessonPerformanceCard(lesson)
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LessonPerformanceCard(lesson: LessonPerformance) {
    val passRate = lesson.passRate
    val isLow = lesson.isLowPerforming

    val animatedPass by animateFloatAsState(
        targetValue = passRate,
        animationSpec = tween(1200),
        label = "pass_bar"
    )
    val animatedAvg by animateFloatAsState(
        targetValue = lesson.averageScore,
        animationSpec = tween(1400),
        label = "avg_bar"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLow)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = if (isLow) BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.1f)) else null,
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lesson.lessonTitle.ifBlank { lesson.lessonId.take(14) },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (lesson.competency.isNotBlank()) {
                        Text(
                            lesson.competency,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )
                    }
                }
                
                if (isLow) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.error
                    ) {
                        Text(
                            "LOW PERFORMANCE",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                } else {
                    Text(
                        "${(passRate * 100).roundToInt()}% PASS",
                        fontFamily = fredokaFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Premium Bar Metrics
            PremiumMetricBar(
                label = "Passing Students",
                value = animatedPass,
                percentText = "${(passRate * 100).roundToInt()}%",
                primaryColor = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            
            Spacer(Modifier.height(8.dp))
            
            PremiumMetricBar(
                label = "Class Average",
                value = animatedAvg,
                percentText = "${(lesson.averageScore * 100).roundToInt()}%",
                primaryColor = MaterialTheme.colorScheme.tertiary
            )

            Spacer(Modifier.height(16.dp))

            // Participation chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusBadge("✅ ${lesson.passCount}", Color(0xFF2E7D32).copy(alpha = 0.1f), Color(0xFF2E7D32))
                StatusBadge("❌ ${lesson.failCount}", MaterialTheme.colorScheme.error.copy(alpha = 0.1f), MaterialTheme.colorScheme.error)
                if (lesson.notAttempted > 0) {
                    StatusBadge("⏳ ${lesson.notAttempted} left", MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun PremiumMetricBar(label: String, value: Float, percentText: String, primaryColor: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(percentText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = primaryColor)
        }
        Spacer(Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth().height(8.dp)) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(4.dp)))
            Box(
                modifier = Modifier
                    .fillMaxWidth(value)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.horizontalGradient(listOf(primaryColor.copy(alpha = 0.7f), primaryColor)),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

@Composable
fun StatusBadge(text: String, bgColor: Color, textColor: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = bgColor) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
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
                Box(
                    modifier = Modifier.size(80.dp).background(Color(0xFFE8F5E9), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎉", fontSize = 40.sp)
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    "Pure Excellence!", 
                    fontFamily = fredokaFontFamily, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 22.sp,
                    color = Color(0xFF2E7D32)
                )
                Text(
                    "All students are performing above 60%.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with initials
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = student.fullName.split(" ")
                        .take(2).joinToString("") { it.firstOrNull()?.uppercase() ?: "" },
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = fredokaFontFamily,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    student.fullName, 
                    fontWeight = FontWeight.ExtraBold, 
                    fontSize = 16.sp,
                    fontFamily = fredokaFontFamily
                )
                Text(
                    student.section,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                student.lastActive?.let {
                    Text(
                        "Last activity: ${it.take(10)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                ) {
                    Text(
                        "AT RISK",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(8.dp))
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
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
