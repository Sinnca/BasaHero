package com.basahero.elearning.ui.teacher.progress

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
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
import com.basahero.elearning.data.repository.PrePostComparison
import com.basahero.elearning.data.repository.StudentProgressSummary
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
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = studentName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Progress Monitor",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Tabs ──────────────────────────────────────────────────────────
            TabRow(selectedTabIndex = uiState.selectedTab) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text("📘 Lessons") }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("📝 Pre/Post Tests") }
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
                        Text(uiState.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                    }
                }
                uiState.selectedTab == 0 -> LessonsTab(uiState.progressList)
                uiState.selectedTab == 1 -> PrePostTab(uiState.prePostList)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tab 1 — Lesson progress
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun LessonsTab(progressList: List<StudentProgressSummary>) {
    if (progressList.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No lesson progress yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val atRiskCount = progressList.count { it.isAtRisk }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Summary chip row
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryChip("${progressList.size} Lessons", MaterialTheme.colorScheme.primaryContainer)
                if (atRiskCount > 0) {
                    SummaryChip("⚠️ $atRiskCount At-Risk", MaterialTheme.colorScheme.errorContainer)
                }
            }
        }

        items(progressList) { progress ->
            LessonProgressCard(progress = progress)
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun LessonProgressCard(progress: StudentProgressSummary) {
    val bestPct = progress.bestPercent
    val firstPct = progress.firstPercent
    val isAtRisk = progress.isAtRisk

    val animatedBest by animateFloatAsState(
        targetValue = bestPct,
        animationSpec = tween(600),
        label = "best_bar"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAtRisk)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = progress.lessonTitle.ifBlank { progress.lessonId.take(12) },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                if (isAtRisk) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            "AT RISK",
                            color = MaterialTheme.colorScheme.onError,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "${(bestPct * 100).roundToInt()}%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Best score progress bar
            Text("Best Score", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(3.dp))
            LinearProgressIndicator(
                progress = { animatedBest },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = if (isAtRisk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Spacer(Modifier.height(12.dp))

            // Three score columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ScoreStatColumn(
                    label = "First Score",
                    value = "${progress.firstScore}/${progress.quizTotal}",
                    pct = firstPct
                )
                ScoreStatColumn(
                    label = "Best Score",
                    value = "${progress.bestScore}/${progress.quizTotal}",
                    pct = bestPct,
                    highlight = true
                )
                ScoreStatColumn(
                    label = "Attempts",
                    value = "${progress.attemptCount}",
                    pct = null
                )
            }

            // Improvement chip
            if (progress.improvement > 0) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "+${progress.improvement} pts improvement",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
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

    val quarterLabel = comparison.quarterId
        .replace("q-gr", "Grade ")
        .replace("-q", " · Quarter ")
        .replaceFirstChar { it.uppercase() }

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
