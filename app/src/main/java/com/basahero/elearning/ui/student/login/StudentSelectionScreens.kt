package com.basahero.elearning.ui.student.login

import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.ExperimentalFoundationApi

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.launch
import com.basahero.elearning.data.model.Student
import com.basahero.elearning.ui.theme.fredokaFontFamily
import com.basahero.elearning.R
import com.basahero.elearning.ui.theme.PrimaryBlue
import com.basahero.elearning.ui.theme.SecondaryMint
import com.basahero.elearning.ui.theme.AccentOrange
import com.basahero.elearning.ui.theme.SoftWhite
import com.basahero.elearning.ui.common.*

// ─────────────────────────────────────────────────────────────────────────────
// 1. StudentGradeSelectScreen
//    Shows ALL grade levels with their sections grouped underneath.
//    "Select your grade level and section"
//    Matches tablet wireframe: Grade header → grid of section cards
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentGradeSelectScreen(
    onGradeSelected: (Int) -> Unit,
    onManualLoginClick: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val isTablet = screenWidth >= 600

    val grades = listOf(
        GradeInfo(4, PrimaryBlue, "Explore & Discover", "🎒"),
        GradeInfo(5, AccentOrange, "Investigate & Create", "🚀"),
        GradeInfo(6, Color(0xFFE53935), "Launch & Learn", "👑")
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Dynamic Animated Background (Floating shapes)
        PlayfulBackground()

        Scaffold(
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Section: Greeting Banner with entrance animation
                var bannerVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { bannerVisible = true }
                
                AnimatedVisibility(
                    visible = bannerVisible,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(1000))
                ) {
                    BasaHeroGreetingBanner(
                        studentName = "Future Hero!",
                        gradeLevel = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(32.dp))

                Text(
                    text = "Choose Your Grade",
                    fontSize = if (isTablet) 36.sp else 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1E293B),
                    style = TextStyle(fontFamily = fredokaFontFamily),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(40.dp))

                Column(
                    modifier = Modifier
                        .then(
                            if (isTablet) Modifier.widthIn(max = 600.dp) else Modifier.fillMaxWidth()
                        )
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    grades.forEachIndexed { index, grade ->
                        AnimatedGradeCard(
                            index = index,
                            grade = grade,
                            isTablet = isTablet,
                            onGradeSelected = onGradeSelected
                        )
                    }
                }

                Spacer(Modifier.height(64.dp))
            }
        }
    }
}
private data class GradeInfo(
    val level: Int, 
    val color: Color, 
    val subtitle: String,
    val emoji: String
)

@Composable
private fun AnimatedGradeCard(
    index: Int,
    grade: GradeInfo,
    isTablet: Boolean,
    onGradeSelected: (Int) -> Unit
) {
    var cardVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 200L)
        cardVisible = true
    }
    
    AnimatedVisibility(
        visible = cardVisible,
        enter = slideInHorizontally(initialOffsetX = { if (index % 2 == 0) -it else it }) + fadeIn(tween(800))
    ) {
        GradeSelectionCard(
            grade = grade.level,
            subtitle = grade.subtitle,
            color = grade.color,
            emoji = grade.emoji,
            isTablet = isTablet,
            onClick = { onGradeSelected(grade.level) }
        )
    }
}

@Composable
private fun GradeSelectionCard(
    grade: Int,
    subtitle: String,
    color: Color,
    emoji: String,
    isTablet: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "card_float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    // Vibrant Gradients for cards (Slightly stronger)
    val cardGradient = when (grade) {
        4 -> Brush.linearGradient(listOf(Color(0xFF90CAF9), Color(0xFF64B5F6))) // Blue
        5 -> Brush.linearGradient(listOf(Color(0xFFFFCC80), Color(0xFFFFB74D))) // Orange (Moved from G6)
        6 -> Brush.linearGradient(listOf(Color(0xFFEF9A9A), Color(0xFFE57373))) // New Red Gradient
        else -> Brush.linearGradient(listOf(color.copy(alpha = 0.5f), color.copy(alpha = 0.7f)))
    }
    
    val accentColor = when (grade) {
        4 -> Color(0xFF4A90E2)
        5 -> Color(0xFFE67E22) // Orange Accent
        6 -> Color(0xFFC62828) // Strong Red Accent
        else -> color
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = floatOffset
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // Main front face
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardGradient, RoundedCornerShape(if (isTablet) 40.dp else 32.dp))
                .border(3.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(if (isTablet) 40.dp else 32.dp))
                .padding(if (isTablet) 40.dp else 28.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Section: Large Illustration Slot (Responsive Size)
                Box(
                    modifier = Modifier
                        .size(if (isTablet) 180.dp else 110.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    val imageRes = when(grade) {
                        4 -> R.drawable.ill_grade4
                        5 -> R.drawable.ill_grade5
                        6 -> R.drawable.ill_grade6
                        else -> R.drawable.ic_launcher_foreground
                    }
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = "Grade $grade Illustration",
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    )
                }

                Spacer(Modifier.width(if (isTablet) 40.dp else 24.dp))

                // Right Section: Content
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Grade $grade",
                        fontSize = if (isTablet) 44.sp else 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A), 
                        fontFamily = fredokaFontFamily
                    )
                    Text(
                        text = subtitle,
                        fontSize = if (isTablet) 22.sp else 16.sp,
                        color = Color(0xFF334155),
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = if (isTablet) 24.dp else 16.dp)
                    )

                    // "Start Learning!" Pill Button (Bigger)
                    Surface(
                        color = accentColor,
                        shape = RoundedCornerShape(50),
                        shadowElevation = 8.dp, // Re-added shadow for pop
                        tonalElevation = 4.dp
                    ) {
                        Text(
                            text = "Start Learning!",
                            modifier = Modifier.padding(
                                horizontal = if (isTablet) 32.dp else 24.dp,
                                vertical = if (isTablet) 12.dp else 8.dp
                            ),
                            color = Color.White,
                            fontSize = if (isTablet) 20.sp else 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. StudentNameSelectScreen
//    Two-stage:
//      Stage 1 — Pick section (grid of section cards)
//      Stage 2 — "Who are you?" (grid of name cards + search)
//    Matches tablet wireframe: clean white cards in a responsive grid
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StudentNameSelectScreen(
    gradeLevel: Int,
    viewModel: StudentSelectionViewModel,
    onStudentSelected: (Student) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val strings = LocalAppStrings.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedSection by remember { mutableStateOf<String?>(null) }

    val screenWidth = LocalConfiguration.current.screenWidthDp
    val isTablet = screenWidth >= 600
    // How many columns for the grid
    val gridColumns = when {
        screenWidth >= 900 -> 4
        screenWidth >= 600 -> 3
        else -> 2
    }

    BackHandler(enabled = selectedSection != null) {
        selectedSection = null
        searchQuery = ""
    }

    LaunchedEffect(gradeLevel) {
        viewModel.loadStudents(gradeLevel)
    }

    val gradeColor = when (gradeLevel) {
        4 -> Color(0xFF2177DA) // Blue
        5 -> Color(0xFFE67E22) // Orange (Moved from G6)
        6 -> Color(0xFFC62828) // New Red
        else -> Color(0xFF1340A0)
    }
    val gradeDark = when (gradeLevel) {
        4 -> Color(0xFF1E3A8A)
        5 -> Color(0xFF935116) // Darker Orange
        6 -> Color(0xFF8B0000) // Darker Red
        else -> Color(0xFF07153A)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Dynamic Animated Background (Floating shapes/letters)
        PlayfulBackground()

        Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectedSection != null) {
                                selectedSection = null
                                searchQuery = ""
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier
                            .padding(8.dp)
                            .size(if (isTablet) 56.dp else 48.dp)
                            .shadow(4.dp, CircleShape)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1E293B), // Visible dark slate
                            modifier = Modifier.size(if (isTablet) 32.dp else 24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .then(
                        if (isTablet) Modifier.widthIn(max = 900.dp) else Modifier.fillMaxWidth()
                    )
            ) {
                // ── Header ──────────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (isTablet) 32.dp else 16.dp,
                            vertical = 8.dp
                        )
                ) {
                    // Title: Grade X Heroes!
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Grade $gradeLevel Heroes!",
                            fontSize = if (isTablet) 48.sp else 30.sp, // Slightly smaller for mobile
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF2C6BBF),
                            fontFamily = fredokaFontFamily,
                            textAlign = TextAlign.Center,
                            lineHeight = if (isTablet) 56.sp else 36.sp
                        )
                        
                        // Subtitle: Select your section to find your name
                        val subtitle = if (selectedSection == null) {
                            buildAnnotatedString {
                                append("Select your ")
                                withStyle(style = SpanStyle(color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)) {
                                    append("section")
                                }
                                append(" to find your ")
                                withStyle(style = SpanStyle(color = Color(0xFFFBC02D), fontWeight = FontWeight.Bold)) {
                                    append("name")
                                }
                            }
                        } else {
                            buildAnnotatedString {
                                append("Find your ")
                                withStyle(style = SpanStyle(color = Color(0xFFFBC02D), fontWeight = FontWeight.Bold)) {
                                    append("name")
                                }
                                append(" in ")
                                withStyle(style = SpanStyle(color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)) {
                                    append(selectedSection!!)
                                }
                            }
                        }
                        
                        Text(
                            text = subtitle,
                            fontSize = if (isTablet) 20.sp else 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF475569),
                            fontFamily = fredokaFontFamily,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Search bar - clean and white
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                if (selectedSection == null) "Search sections..."
                                else "Search names...",
                                color = Color(0xFF94A3B8)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF64748B)
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(if (isTablet) 64.dp else 56.dp),
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1E293B),
                            unfocusedTextColor = Color(0xFF1E293B),
                            cursorColor = Color(0xFF2C6BBF),
                            focusedBorderColor = Color(0xFF2C6BBF),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Spacer(Modifier.height(16.dp))
                }

                // ── Content Grid ────────────────────────────────────────────
                Box(modifier = Modifier.weight(1f)) {
                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (uiState.errorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.errorMessage ?: "",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        AnimatedContent(
                            targetState = selectedSection,
                            transitionSpec = {
                                fadeIn(tween(400)) togetherWith fadeOut(tween(400))
                            },
                            label = "content_transition"
                        ) { targetSection ->
                            if (targetSection == null) {
                                // ── Section Grid ────────────────────────────────
                                SectionGrid(
                                    uiState = uiState,
                                    searchQuery = searchQuery,
                                    gradeColor = gradeColor,
                                    gridColumns = gridColumns,
                                    isTablet = isTablet,
                                    onSelect = {
                                        selectedSection = it
                                        searchQuery = ""
                                    }
                                )
                            } else {
                                // ── Name Grid ───────────────────────────────────
                                NameGrid(
                                    uiState = uiState,
                                    selectedSection = targetSection,
                                    searchQuery = searchQuery,
                                    gradeColor = gradeColor,
                                    gridColumns = gridColumns,
                                    isTablet = isTablet,
                                    onSelect = { student ->
                                        viewModel.loginStudent(student, onStudentSelected)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    } // end outer Box
}

// ─────────────────────────────────────────────────────────────────────────────
// Section Grid — Cards in a responsive grid
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SectionGrid(
    uiState: StudentSelectionViewModel.SelectionState,
    searchQuery: String,
    gradeColor: Color,
    gridColumns: Int,
    isTablet: Boolean,
    onSelect: (String) -> Unit
) {
    val sections = uiState.sections
        .filter { it.contains(searchQuery, ignoreCase = true) }

    if (sections.isEmpty()) {
        EmptyState(
            if (searchQuery.isEmpty()) "No sections available."
            else "No section matches '$searchQuery'"
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            contentPadding = PaddingValues(
                horizontal = if (isTablet) 32.dp else 16.dp,
                vertical = 24.dp // More vertical breathing room
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(sections.size) { index ->
                val section = sections[index]
                val count = uiState.students.count { it.section == section }
                
                // ── Animated Wrapper for Staggered Entrance ──
                AnimatedSectionCard(index = index) {
                    SectionCard(
                        section = section,
                        studentCount = count,
                        color = gradeColor,
                        isTablet = isTablet,
                        onClick = { onSelect(section) }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Name Grid — Student cards in a responsive grid
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NameGrid(
    uiState: StudentSelectionViewModel.SelectionState,
    selectedSection: String,
    searchQuery: String,
    gradeColor: Color,
    gridColumns: Int,
    isTablet: Boolean,
    onSelect: (Student) -> Unit
) {
    val filteredStudents = uiState.students
        .filter { it.section == selectedSection }
        .filter { it.fullName.contains(searchQuery, ignoreCase = true) }
        .sortedBy { it.fullName.uppercase() }

    if (filteredStudents.isEmpty()) {
        EmptyState("No name matches '$searchQuery' in $selectedSection.")
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            contentPadding = PaddingValues(
                horizontal = if (isTablet) 32.dp else 16.dp,
                vertical = 24.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredStudents.size) { index ->
                val student = filteredStudents[index]
                
                // ── Animated Wrapper for Staggered Entrance ──
                AnimatedStudentCard(index = index) {
                    StudentNameCard(
                        student = student,
                        gradeColor = gradeColor,
                        isTablet = isTablet,
                        onClick = { onSelect(student) }
                    )
                }
            }
        }
    }
}

// ── Animated Wrappers for Staggered Entrance ──

@Composable
private fun AnimatedSectionCard(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 100L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(tween(500)),
        modifier = Modifier.fillMaxWidth()
    ) {
        content()
    }
}

@Composable
private fun AnimatedStudentCard(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 60L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it / 3 }) + fadeIn(tween(400)),
        modifier = Modifier.fillMaxWidth()
    ) {
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section Card — clean Material3 style
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SectionCard(
    section: String,
    studentCount: Int,
    color: Color,
    isTablet: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    val cardColors = listOf(
        Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFF9C27B0),
        Color(0xFFFF9800), Color(0xFFE91E63), Color(0xFF00BCD4)
    )
    val circleColor = cardColors[section.length % cardColors.size]

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isTablet) 280.dp else 190.dp) // Optimized mobile height
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // ── Corner Decorations ──
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(if (isTablet) 24.dp else 18.dp)
                        .graphicsLayer { rotationZ = 15f },
                    tint = circleColor.copy(alpha = 0.15f)
                )
                
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .size(if (isTablet) 32.dp else 24.dp)
                ) {
                    val spacing = 8.dp.toPx()
                    for (i in 0..2) {
                        for (j in 0..2) {
                            drawCircle(
                                color = circleColor.copy(alpha = 0.1f),
                                radius = 2.dp.toPx(),
                                center = Offset(i * spacing, size.height - (j * spacing))
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize().padding(if (isTablet) 32.dp else 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isTablet) 90.dp else 60.dp) // Optimized mobile icon size
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(circleColor, circleColor.copy(alpha = 0.8f)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = section.take(1).uppercase(),
                            color = Color.White,
                            fontSize = if (isTablet) 40.sp else 28.sp, // Optimized font size
                            fontWeight = FontWeight.Black,
                            fontFamily = fredokaFontFamily
                        )
                    }

                    Spacer(Modifier.height(if (isTablet) 20.dp else 12.dp))

                    Text(
                        text = section,
                        fontSize = if (isTablet) 28.sp else 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1E293B),
                        fontFamily = fredokaFontFamily,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "$studentCount Students",
                        fontSize = if (isTablet) 16.sp else 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B),
                        fontFamily = fredokaFontFamily,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Student Name Card — clean Material3 grid card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun StudentNameCard(
    student: Student,
    gradeColor: Color,
    isTablet: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    val initials = student.fullName.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.take(1).uppercase() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isTablet) 200.dp else 145.dp) // Optimized mobile height
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // ── More Design Elements ──
                // Top-Right: Sparkle
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(18.dp),
                    tint = gradeColor.copy(alpha = 0.2f)
                )
                
                // Bottom-Right: Subtle Shield Pattern
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .graphicsLayer { alpha = 0.05f },
                    tint = gradeColor
                )

                // Decorative Bottom Stripe
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.horizontalGradient(
                                listOf(gradeColor.copy(alpha = 0.3f), gradeColor.copy(alpha = 0.1f))
                            )
                        )
                )

                Column(
                    modifier = Modifier.fillMaxSize().padding(if (isTablet) 24.dp else 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Fun "Bubble Squircle" initials
                    Box(
                        modifier = Modifier
                            .size(if (isTablet) 85.dp else 56.dp)
                            .graphicsLayer {
                                shadowElevation = if (isTablet) 8f else 4f
                                shape = RoundedCornerShape(percent = 35)
                                clip = true
                            }
                            .background(
                                Brush.verticalGradient(
                                    listOf(gradeColor, gradeColor.copy(alpha = 0.7f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Subtle Background Pattern (Stars)
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(if (isTablet) 12.dp else 8.dp).graphicsLayer { alpha = 0.1f },
                            tint = Color.White
                        )
                        
                        Text(
                            text = initials,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = if (isTablet) 28.sp else 18.sp,
                            fontFamily = fredokaFontFamily,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.3f),
                                    offset = Offset(2f, 2f),
                                    blurRadius = 4f
                                )
                            )
                        )
                    }

                    Spacer(Modifier.height(if (isTablet) 14.dp else 8.dp))

                    Text(
                        text = student.fullName,
                        fontSize = if (isTablet) 18.sp else 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        fontFamily = fredokaFontFamily,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        lineHeight = if (isTablet) 22.sp else 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.White.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fredokaFontFamily
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GradeCard — kept for backward compatibility but no longer primary
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeCard(grade: Int, color: Color, subtitle: String, onClick: () -> Unit) {
    val emoji = when(grade) {
        5 -> "🚀"
        6 -> "👑"
        else -> "🎒"
    }
    GradeSelectionCard(
        grade = grade,
        subtitle = subtitle,
        color = color,
        emoji = emoji,
        isTablet = false,
        onClick = onClick
    )
}
