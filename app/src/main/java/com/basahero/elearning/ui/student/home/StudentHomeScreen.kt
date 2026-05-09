package com.basahero.elearning.ui.student.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basahero.elearning.data.local.SessionManager
import com.basahero.elearning.data.model.Quarter
import com.basahero.elearning.ui.common.LocalAppStrings
import kotlinx.coroutines.launch
import kotlin.math.min

// ─────────────────────────────────────────────────────────────────────────────
// StudentHomeScreen — Matches wireframe: blue header, stats, quarters, bottom nav
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    studentId: String,
    viewModel: StudentHomeViewModel,
    onQuarterClick: (quarterId: String, gradeLevel: Int) -> Unit,
    onJoinGameClick: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val isTablet = screenWidth >= 600
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val languageCode by sessionManager.language.collectAsState(initial = "en")
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(studentId) { viewModel.loadHome(studentId) }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(strokeWidth = 4.dp, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text(
                    strings.loadingYourAdventure,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val student = uiState.student ?: return

    // Computed stats
    val totalLessons = uiState.quarters.sumOf { it.totalLessons }
    val completedLessons = uiState.quarters.sumOf { it.completedLessons }
    val greetingTime = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY).let { hour ->
        when {
            hour < 12 -> if (languageCode == "fil") "Magandang umaga," else "Good morning,"
            hour < 18 -> if (languageCode == "fil") "Magandang hapon," else "Good afternoon,"
            else -> if (languageCode == "fil") "Magandang gabi," else "Good evening,"
        }
    }

    // Grade colors
    val primaryBlue = Color(0xFF2563EB)
    val darkBlue = Color(0xFF1E40AF)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // ── Bottom Navigation Bar ────────────────────────────────────────
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = primaryBlue,
                        selectedTextColor = primaryBlue,
                        indicatorColor = primaryBlue.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        // Navigate to first active quarter's lessons
                        val activeQ = uiState.quarters.firstOrNull { it.isActive }
                        if (activeQ != null) {
                            onQuarterClick(activeQ.id, student.gradeLevel)
                        }
                    },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                    label = { Text(strings.lessons, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = primaryBlue,
                        selectedTextColor = primaryBlue,
                        indicatorColor = primaryBlue.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        onJoinGameClick()
                    },
                    icon = { Icon(Icons.Default.SportsEsports, contentDescription = null) },
                    label = { Text("Game", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = primaryBlue,
                        selectedTextColor = primaryBlue,
                        indicatorColor = primaryBlue.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Profile", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = primaryBlue,
                        selectedTextColor = primaryBlue,
                        indicatorColor = primaryBlue.copy(alpha = 0.1f)
                    )
                )
            }
        }
    ) { padding ->
        // ── Profile tab ─────────────────────────────────────────────────
        if (selectedTab == 3) {
            ProfileContent(
                student = student,
                languageCode = languageCode,
                onToggleLanguage = {
                    val newLang = if (languageCode == "en") "fil" else "en"
                    coroutineScope.launch { sessionManager.setLanguage(newLang) }
                },
                onLogout = onLogout,
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        // ── Home tab content ────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .then(
                        if (isTablet) Modifier.widthIn(max = 720.dp) else Modifier.fillMaxWidth()
                    ),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // ── Blue gradient header card ────────────────────────────
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(primaryBlue, darkBlue)
                                ),
                                shape = RoundedCornerShape(
                                    bottomStart = 28.dp,
                                    bottomEnd = 28.dp
                                )
                            )
                            .padding(
                                start = 20.dp, end = 20.dp,
                                top = 20.dp, bottom = 24.dp
                            )
                    ) {
                        Column {
                            // Top row: Avatar + Greeting + Settings
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val initials = student.fullName.split(" ")
                                        .filter { it.isNotBlank() }
                                        .take(2)
                                        .joinToString("") { it.take(1).uppercase() }
                                    Text(
                                        text = initials,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }

                                Spacer(Modifier.width(14.dp))

                                // Greeting text
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = greetingTime,
                                        fontSize = 14.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = student.fullName,
                                        fontSize = if (isTablet) 22.sp else 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                // Settings gear
                                IconButton(
                                    onClick = { selectedTab = 3 },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            Color.White.copy(alpha = 0.15f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            // Stats row: Day streak, XP, Grade level
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatBadge(
                                    icon = Icons.Default.Whatshot,
                                    iconColor = Color(0xFFFF6B35),
                                    value = "$completedLessons",
                                    label = if (languageCode == "fil") "Aralin\ntapos" else "Lessons\ndone",
                                    modifier = Modifier.weight(1f)
                                )
                                StatBadge(
                                    icon = Icons.Default.Star,
                                    iconColor = Color(0xFFFFD700),
                                    value = "${(uiState.overallProgress * 100).toInt()}%",
                                    label = if (languageCode == "fil") "Progreso" else "Progress",
                                    modifier = Modifier.weight(1f)
                                )
                                StatBadge(
                                    icon = Icons.Default.School,
                                    iconColor = primaryBlue,
                                    value = "Gr.${student.gradeLevel}",
                                    label = if (languageCode == "fil") "Baitang" else "Grade\nlevel",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // ── Overall Progress section ────────────────────────────
                item {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = 20.dp,
                            vertical = 16.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (languageCode == "fil") "Kabuuang Progreso" else "Overall Progress",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$completedLessons/$totalLessons ${if (languageCode == "fil") "tapos" else "done"}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { uiState.overallProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = primaryBlue,
                            trackColor = primaryBlue.copy(alpha = 0.12f)
                        )
                    }
                }

                // ── My Quarters heading ─────────────────────────────────
                item {
                    Text(
                        text = strings.myQuarters,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                }

                // ── Quarter cards ───────────────────────────────────────
                items(uiState.quarters) { quarter ->
                    QuarterCard(
                        quarter = quarter,
                        isTablet = isTablet,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        onClick = {
                            if (quarter.isActive) onQuarterClick(quarter.id, student.gradeLevel)
                        }
                    )
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stat badge — used in the blue header (Lessons done, Progress, Grade)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StatBadge(
    icon: ImageVector,
    iconColor: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quarter card — with progress ring, matching wireframe
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun QuarterCard(
    quarter: Quarter,
    isTablet: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isLocked = !quarter.isActive
    val isActive = quarter.isActive && quarter.progressPercent < 1f
    val isDone = quarter.isActive && quarter.progressPercent >= 1f

    // Press animation
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isLocked) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "card_scale"
    )

    val primaryBlue = Color(0xFF2563EB)

    val cardMod: Modifier = modifier
        .let { m -> if (!isLocked) m.clickable(interactionSource = interactionSource, indication = null, onClick = onClick) else m }
        .scale(scale)

    Card(
        modifier = cardMod,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) primaryBlue
            else MaterialTheme.colorScheme.surface
        ),
        border = if (!isActive) BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ) else null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isActive) 6.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Progress ring or lock
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLocked) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    // Progress ring
                    val progressColor = if (isActive) Color.White else primaryBlue
                    val trackColor = if (isActive) Color.White.copy(alpha = 0.2f)
                    else primaryBlue.copy(alpha = 0.12f)

                    ProgressRing(
                        progress = quarter.progressPercent,
                        size = 56.dp,
                        strokeWidth = 5.dp,
                        color = progressColor,
                        trackColor = trackColor
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // Quarter info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quarter.title,
                    fontSize = if (isTablet) 18.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color.White
                    else if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = when {
                        isLocked -> strings.completePreviousQuarter
                        isDone -> "✔ ${strings.done}"
                        else -> strings.lessonsProgress(quarter.completedLessons, quarter.totalLessons) +
                                " · ${strings.inProgress}"
                    },
                    fontSize = 12.sp,
                    color = if (isActive) Color.White.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Chevron for active
            if (!isLocked) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (isActive) Color.White.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Profile tab content — settings, language toggle, logout
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ProfileContent(
    student: com.basahero.elearning.data.model.Student,
    languageCode: String,
    onToggleLanguage: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val primaryBlue = Color(0xFF2563EB)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        // Avatar
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(primaryBlue, primaryBlue.copy(alpha = 0.7f)))
                ),
            contentAlignment = Alignment.Center
        ) {
            val initials = student.fullName.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.take(1).uppercase() }
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = student.fullName,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = strings.gradeAndSection(student.gradeLevel, student.section),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        // Language toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleLanguage)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Language,
                    contentDescription = null,
                    tint = primaryBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.languageLabel,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (languageCode == "en") "English" else "Tagalog",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = primaryBlue.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = if (languageCode == "en") "EN" else "FIL",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryBlue
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Logout
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLogout)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = strings.logout,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
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
            val topLeft = androidx.compose.ui.geometry.Offset(
                (this.size.width - diameter) / 2,
                (this.size.height - diameter) / 2
            )
            val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)
            drawArc(trackColor, -90f, 360f, false, topLeft = topLeft, size = arcSize, style = Stroke(strokePx, cap = StrokeCap.Round))
            drawArc(color, -90f, 360f * animProg, false, topLeft = topLeft, size = arcSize, style = Stroke(strokePx, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${(animProg * 100).toInt()}%",
                fontSize = (size.value * 0.22f).sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}