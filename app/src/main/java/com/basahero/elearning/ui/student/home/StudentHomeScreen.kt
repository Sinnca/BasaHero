package com.basahero.elearning.ui.student.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basahero.elearning.data.model.Quarter
import kotlin.math.min

// ─────────────────────────────────────────────────────────────────────────────
// StudentHomeScreen — elementary-friendly, phone+tablet adaptive
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    studentId: String,
    viewModel: StudentHomeViewModel,
    onQuarterClick: (quarterId: String, gradeLevel: Int) -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600

    LaunchedEffect(studentId) { viewModel.loadHome(studentId) }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(strokeWidth = 4.dp, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text("Loading your adventure...", fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    val student = uiState.student ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Avatar circle
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = student.fullName.first().uppercase(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Hi, ${student.fullName.split(" ").first()}! 👋",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Grade ${student.gradeLevel}  ·  ${student.section}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (isTablet) {
            // ── Tablet: two-column grid ───────────────────────────────────────
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                item { GreetingBanner(progress = uiState.overallProgress, gradeLevel = student.gradeLevel) }
                item {
                    Text("My Quarters", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp))
                }
                // Rows of 2 cards each
                val rows = uiState.quarters.chunked(2)
                items(rows) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        row.forEach { quarter ->
                            QuarterCard(
                                quarter = quarter,
                                isTablet = true,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (quarter.isActive) onQuarterClick(quarter.id, student.gradeLevel)
                                }
                            )
                        }
                        // Filler if odd number
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        } else {
            // ── Phone: single column ──────────────────────────────────────────
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                item { GreetingBanner(progress = uiState.overallProgress, gradeLevel = student.gradeLevel) }
                item {
                    Text("My Quarters", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp))
                }
                items(uiState.quarters) { quarter ->
                    QuarterCard(
                        quarter = quarter,
                        isTablet = false,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (quarter.isActive) onQuarterClick(quarter.id, student.gradeLevel)
                        }
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Greeting banner with progress ring
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GreetingBanner(progress: Float, gradeLevel: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ProgressRing(
                progress = progress,
                size = 88.dp,
                strokeWidth = 9.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Grade $gradeLevel English",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = "${(progress * 100).toInt()}% Complete",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (progress < 0.5f) "Keep it up, hero! 💪"
                           else if (progress < 1f)  "Almost there! 🌟"
                           else                      "All done! Amazing! 🎉",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quarter card — adaptive for phone/tablet
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun QuarterCard(
    quarter: Quarter,
    isTablet: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isLocked = !quarter.isActive

    // Scale animation on click (press effect)
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isLocked) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "card_scale"
    )

    val cardMod: Modifier = modifier
        .let { m -> if (!isLocked) m.clickable(interactionSource = interactionSource, indication = null, onClick = onClick) else m }
        .scale(scale)
        .let { m -> if (!isLocked) m.shadow(3.dp, RoundedCornerShape(20.dp)) else m }

    Card(
        modifier = cardMod,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) MaterialTheme.colorScheme.surfaceVariant
                             else MaterialTheme.colorScheme.surface
        ),
        border = if (!isLocked) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)) else null
    ) {
        // Colored top strip for active cards
        if (!isLocked) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                        )
                    )
            )
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Quarter badge
                Box(
                    modifier = Modifier
                        .size(if (isTablet) 52.dp else 46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isLocked) Color.Gray.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLocked) {
                        Icon(Icons.Default.Lock, null, tint = Color.Gray, modifier = Modifier.size(22.dp))
                    } else {
                        Text(
                            "Q${quarter.quarterNumber}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = if (isTablet) 17.sp else 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quarter.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isTablet) 17.sp else 15.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    if (isLocked) {
                        Text(
                            "🔒 Complete previous quarter first",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            "${quarter.completedLessons} of ${quarter.totalLessons} lessons done ✔",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!isLocked) {
                    Icon(
                        Icons.Default.ChevronRight, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Progress bar for active quarters
            if (!isLocked) {
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(
                    progress = { quarter.progressPercent },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${(quarter.progressPercent * 100).toInt()}%",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Animated progress ring
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ProgressRing(progress: Float, size: Dp, strokeWidth: Dp, color: Color, trackColor: Color) {
    val animProg by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(900, easing = EaseOutCubic),
        label = "ring_progress"
    )
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val diameter = min(this.size.width, this.size.height) - strokePx
            val topLeft  = androidx.compose.ui.geometry.Offset((this.size.width - diameter) / 2, (this.size.height - diameter) / 2)
            val arcSize  = androidx.compose.ui.geometry.Size(diameter, diameter)
            drawArc(trackColor, -90f, 360f, false, topLeft = topLeft, size = arcSize, style = Stroke(strokePx, cap = StrokeCap.Round))
            drawArc(color,      -90f, 360f * animProg, false, topLeft = topLeft, size = arcSize, style = Stroke(strokePx, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(animProg * 100).toInt()}%", fontSize = (size.value * 0.18f).sp, fontWeight = FontWeight.ExtraBold, color = color)
            if (progress >= 1f) Text("🌟", fontSize = (size.value * 0.18f).sp)
        }
    }
}