package com.basahero.elearning.ui.student.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basahero.elearning.data.model.Quarter
import androidx.compose.ui.unit.Dp
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    studentId: String,
    viewModel: StudentHomeViewModel,
    onQuarterClick: (quarterId: String, gradeLevel: Int) -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(studentId) {
        viewModel.loadHome(studentId)
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val student = uiState.student ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Hi, ${student.fullName.split(" ").first()}! 👋",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Grade ${student.gradeLevel} · ${student.section}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OverallProgressCard(
                    progress = uiState.overallProgress,
                    gradeLevel = student.gradeLevel
                )
            }

            item {
                Text(
                    text = "Quarters",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            items(uiState.quarters) { quarter ->
                QuarterCard(
                    quarter = quarter,
                    onClick = {
                        if (quarter.isActive) {
                            onQuarterClick(quarter.id, student.gradeLevel)
                        }
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun OverallProgressCard(progress: Float, gradeLevel: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProgressRing(
                progress = progress,
                size = 80.dp,
                strokeWidth = 8.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.width(20.dp))

            Column {
                Text(text = "Overall Progress", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                Text(text = "${(progress * 100).toInt()}% Complete", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(text = "Grade $gradeLevel English", fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun QuarterCard(quarter: Quarter, onClick: () -> Unit) {
    val isLocked = !quarter.isActive
    Card(
        modifier = Modifier.fillMaxWidth().then(if (!isLocked) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isLocked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface),
        border = if (!isLocked) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)) else null
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(if (isLocked) Color.Gray.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (isLocked) Icon(Icons.Default.Lock, null, tint = Color.Gray)
                else Text("Q${quarter.quarterNumber}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = quarter.title, fontWeight = FontWeight.SemiBold)
                if (!isLocked) {
                    Text("${quarter.completedLessons} of ${quarter.totalLessons} lessons done", fontSize = 12.sp)
                    LinearProgressIndicator(progress = { quarter.progressPercent }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)))
                } else {
                    Text("Complete previous quarter to unlock", fontSize = 12.sp)
                }
            }
            if (!isLocked) Icon(Icons.Default.ChevronRight, null)
        }
    }
}

@Composable
fun ProgressRing(progress: Float, size: Dp, strokeWidth: Dp, color: Color, trackColor: Color) {
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(800, easing = EaseOutCubic))
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val diameter = min(this.size.width, this.size.height) - strokePx
            drawArc(trackColor, -90f, 360f, false, topLeft = androidx.compose.ui.geometry.Offset((this.size.width - diameter) / 2, (this.size.height - diameter) / 2), size = androidx.compose.ui.geometry.Size(diameter, diameter), style = Stroke(strokePx, cap = StrokeCap.Round))
            drawArc(color, -90f, 360f * animatedProgress, false, topLeft = androidx.compose.ui.geometry.Offset((this.size.width - diameter) / 2, (this.size.height - diameter) / 2), size = androidx.compose.ui.geometry.Size(diameter, diameter), style = Stroke(strokePx, cap = StrokeCap.Round))
        }
        Text("${(animatedProgress * 100).toInt()}%", fontSize = (size.value * 0.2f).sp, fontWeight = FontWeight.Bold, color = color)
    }
}