package com.basahero.elearning.ui.teacher.progress

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import com.basahero.elearning.data.repository.PrePostComparison
import com.basahero.elearning.data.repository.StudentProgressSummary
import com.basahero.elearning.ui.theme.fredokaFontFamily
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// StudentProgressScreen — tabbed: Lessons | Pre/Post Tests
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentProgressScreen(
    studentId: String,
    studentName: String,
    viewModel: StudentProgressViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(studentId) {
        viewModel.loadStudentProgress(studentId)
    }

    Scaffold(
        containerColor = Color(0xFFF1F5F9)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // ── Hero Header ──────────────────────────────────────────────────
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
                        text = "STUDENT PROFILE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = studentName,
                        fontFamily = fredokaFontFamily,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 36.sp
                    )
                    Text(
                        text = "Academic Progress Monitoring",
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
                            "Lessons", 
                            fontFamily = fredokaFontFamily, 
                            fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                        ) 
                    }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { 
                        Text(
                            "Assessments", 
                            fontFamily = fredokaFontFamily, 
                            fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                        ) 
                    }
                )
            }

            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeCap = StrokeCap.Round)
                    }
                }
                uiState.errorMessage != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = uiState.errorMessage ?: "", 
                            color = MaterialTheme.colorScheme.error,
                            fontFamily = fredokaFontFamily
                        )
                    }
                }
                uiState.selectedTab == 0 -> LessonsTab(
                    quarterProgressList = uiState.quarterProgressList,
                    onToggleQuarter = { viewModel.toggleQuarter(it) }
                )
                uiState.selectedTab == 1 -> PrePostTab(uiState.prePostList)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tab 1 — Lesson progress
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LessonsTab(
    quarterProgressList: List<QuarterProgressSummary>,
    onToggleQuarter: (String) -> Unit
) {
    if (quarterProgressList.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Analytics, 
                    contentDescription = null, 
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "No lesson progress yet.", 
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = fredokaFontFamily,
                    fontSize = 18.sp
                )
            }
        }
        return
    }

    val totalLessons = quarterProgressList.sumOf { it.lessons.size }
    val atRiskCount = quarterProgressList.sumOf { q -> q.lessons.count { it.isAtRisk } }
    val completedCount = quarterProgressList.sumOf { q -> q.lessons.count { it.status == "DONE" } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Dashboard
        item {
            Card(
                modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Row(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    BigStatColumn("Total", "$totalLessons", "Lessons", Icons.AutoMirrored.Filled.MenuBook)
                    VerticalDivider(modifier = Modifier.height(40.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    BigStatColumn("Done", "$completedCount", "Completed", Icons.Default.CheckCircle)
                    VerticalDivider(modifier = Modifier.height(40.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    BigStatColumn("Alerts", "$atRiskCount", "At Risk", Icons.Default.ErrorOutline, isError = atRiskCount > 0)
                }
            }
        }

        items(quarterProgressList) { quarter ->
            QuarterProgressCard(
                quarter = quarter,
                onToggle = { onToggleQuarter(quarter.quarterId) }
            )
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
fun BigStatColumn(label: String, value: String, unit: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isError: Boolean = false) {
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
fun QuarterProgressCard(
    quarter: QuarterProgressSummary,
    onToggle: () -> Unit
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
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
            .clip(RoundedCornerShape(24.dp))
            .clickable { onToggle() },
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
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = quarter.quarterId.takeLast(1), 
                            fontFamily = fredokaFontFamily,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = quarter.quarterTitle,
                            fontFamily = fredokaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "${quarter.lessons.size} Lessons in this period",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
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
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (quarter.lessons.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No progress records found",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontFamily = fredokaFontFamily
                            )
                        }
                    } else {
                        quarter.lessons.forEach { progress ->
                            LessonProgressCard(progress = progress)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LessonProgressCard(progress: StudentProgressSummary) {
    val bestPct = progress.bestPercent
    val isAtRisk = progress.isAtRisk

    val animatedBest by animateFloatAsState(
        targetValue = bestPct,
        animationSpec = tween(1000),
        label = "best_bar"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAtRisk)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = if (isAtRisk) BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)) else null,
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = progress.lessonTitle.ifBlank { progress.lessonId.take(12) },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (progress.competency.isNotBlank()) {
                        Text(
                            text = progress.competency,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${(bestPct * 100).roundToInt()}%",
                        fontFamily = fredokaFontFamily,
                        fontSize = 20.sp,
                        color = if (isAtRisk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    if (isAtRisk) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.error
                        ) {
                            Text(
                                "CRITICAL",
                                color = MaterialTheme.colorScheme.onError,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Premium Progress Bar
            Box(modifier = Modifier.fillMaxWidth().height(10.dp)) {
                // Track
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(5.dp))
                )
                // Fill
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedBest)
                        .fillMaxHeight()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = if (isAtRisk) 
                                    listOf(MaterialTheme.colorScheme.error.copy(alpha = 0.7f), MaterialTheme.colorScheme.error)
                                else 
                                    listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), MaterialTheme.colorScheme.primary)
                            ),
                            shape = RoundedCornerShape(5.dp)
                        )
                )
            }

            Spacer(Modifier.height(14.dp))

            // Score Stat Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScoreBadge("First: ${progress.firstScore}/${progress.quizTotal}")
                    Spacer(Modifier.width(8.dp))
                    ScoreBadge("Best: ${progress.bestScore}/${progress.quizTotal}", isPrimary = true)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text("${progress.attemptCount} tries", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Improvement Trend
            if (progress.improvement > 0) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.TrendingUp, 
                        null, 
                        tint = Color(0xFF2E7D32), 
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Up by ${progress.improvement} points since first try", 
                        fontSize = 11.sp, 
                        color = Color(0xFF2E7D32), 
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ScoreBadge(text: String, isPrimary: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isPrimary) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tab 2 — Pre/Post Test comparison
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PrePostTab(prePostList: List<PrePostComparison>) {
    if (prePostList.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📝", fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "No pre/post test data yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Text(
                    "Data will appear once the student completes a test.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Pre-Test vs Post-Test per Quarter",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }

        items(prePostList) { comparison ->
            PrePostComparisonCard(comparison)
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun PrePostComparisonCard(comparison: PrePostComparison) {
    val prePct  = comparison.prePercent
    val postPct = comparison.postPercent
    val improvement = comparison.improvement

    val animatedPre  by animateFloatAsState(targetValue = prePct,  animationSpec = tween(600), label = "pre")
    val animatedPost by animateFloatAsState(targetValue = postPct, animationSpec = tween(700), label = "post")

    val quarterLabel = comparison.quarterTitle

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Quarter title + trend icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = quarterLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                val (trendIcon, trendColor) = when {
                    comparison.postScore == null -> Icons.AutoMirrored.Filled.TrendingFlat to MaterialTheme.colorScheme.onSurfaceVariant
                    improvement > 0.05f          -> Icons.AutoMirrored.Filled.TrendingUp   to Color(0xFF2E7D32)
                    improvement < -0.05f         -> Icons.AutoMirrored.Filled.TrendingDown  to MaterialTheme.colorScheme.error
                    else                         -> Icons.AutoMirrored.Filled.TrendingFlat  to MaterialTheme.colorScheme.onSurfaceVariant
                }
                Icon(trendIcon, contentDescription = null, tint = trendColor, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.height(14.dp))

            // Pre-test row
            TestBarRow(
                label       = "Pre-Test",
                score       = comparison.preScore,
                total       = comparison.preTotal,
                animated    = animatedPre,
                barColor    = MaterialTheme.colorScheme.tertiary,
                notDoneText = "Not taken"
            )

            Spacer(Modifier.height(10.dp))

            // Post-test row
            TestBarRow(
                label       = "Post-Test",
                score       = comparison.postScore,
                total       = comparison.postTotal,
                animated    = animatedPost,
                barColor    = MaterialTheme.colorScheme.primary,
                notDoneText = "Not taken yet"
            )

            // Improvement badge
            if (comparison.preScore != null && comparison.postScore != null) {
                Spacer(Modifier.height(10.dp))
                val pctChange = (improvement * 100).roundToInt()
                val label = when {
                    pctChange > 0  -> "📈 +${pctChange}% improvement"
                    pctChange < 0  -> "📉 ${pctChange}% decrease"
                    else           -> "➡️ No change"
                }
                val badgeColor = when {
                    pctChange > 0  -> MaterialTheme.colorScheme.secondaryContainer
                    pctChange < 0  -> MaterialTheme.colorScheme.errorContainer
                    else           -> MaterialTheme.colorScheme.surfaceVariant
                }
                val textColor = when {
                    pctChange > 0  -> MaterialTheme.colorScheme.secondary
                    pctChange < 0  -> MaterialTheme.colorScheme.error
                    else           -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Surface(shape = RoundedCornerShape(20.dp), color = badgeColor) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        fontSize = 12.sp,
                        color = textColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun TestBarRow(
    label: String,
    score: Int?,
    total: Int?,
    animated: Float,
    barColor: Color,
    notDoneText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(72.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (score != null && total != null) {
            LinearProgressIndicator(
                progress = { animated },
                modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$score/$total  (${(animated * 100).roundToInt()}%)",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        } else {
            Text(
                text = notDoneText,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared helpers
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SummaryChip(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(20.dp), color = color) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ScoreStatColumn(label: String, value: String, pct: Float?, highlight: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        pct?.let {
            Text(
                text = "(${(it * 100).roundToInt()}%)",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
