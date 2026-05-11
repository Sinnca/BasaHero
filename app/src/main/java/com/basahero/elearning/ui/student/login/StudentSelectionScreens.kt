package com.basahero.elearning.ui.student.login

import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.ExperimentalFoundationApi

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.basahero.elearning.data.model.Student
import com.basahero.elearning.ui.common.LocalAppStrings
import com.basahero.elearning.ui.theme.fredokaFontFamily

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

    // We show grades 4, 5, 6 with their sections
    val grades = listOf(
        GradeInfo(4, Color(0xFF2177DA), "The Beginning!"),
        GradeInfo(5, Color(0xFF379F3B), "Getting Stronger!"),
        GradeInfo(6, Color(0xFFE65100), "The Masters!")
    )

    // Blue gradient background colors
    val bgTop = Color(0xFF2563EB)
    val bgBottom = Color(0xFF1E3A8A)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(bgTop, bgBottom))
            )
    ) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
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
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                modifier = Modifier
                    .then(
                        if (isTablet) Modifier.widthIn(max = 800.dp) else Modifier.fillMaxWidth()
                    )
                    .padding(horizontal = if (isTablet) 32.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "\uD83C\uDF1F", // star emoji
                            fontSize = 40.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = strings.selectYourGrade,
                            modifier = Modifier.padding(horizontal = 20.dp),
                            fontSize = if (isTablet) 26.sp else 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            style = TextStyle(fontFamily = fredokaFontFamily)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (strings == com.basahero.elearning.ui.common.getStrings("fil"))
                                "Pumili ng iyong baitang para magsimula"
                            else
                                "Choose your grade level to get started",
                            fontSize = if (isTablet) 15.sp else 13.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Each grade with its card
                grades.forEach { grade ->
                    item {
                        Spacer(Modifier.height(12.dp))
                    }

                    item {
                        Spacer(Modifier.height(8.dp))
                    }

                    // Grade card — tappable
                    item {
                        GradeSelectionCard(
                            grade = grade.level,
                            subtitle = grade.subtitle,
                            color = grade.color,
                            isTablet = isTablet,
                            onClick = { onGradeSelected(grade.level) }
                        )
                    }
                }

                // Manual login link
                item {
                    Spacer(Modifier.height(24.dp))
                    TextButton(
                        onClick = onManualLoginClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = strings.alreadyHaveAccount + " " + strings.loginHere,
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
    } // end outer Box
}

private data class GradeInfo(val level: Int, val color: Color, val subtitle: String)

@Composable
private fun GradeSelectionCard(
    grade: Int,
    subtitle: String,
    color: Color,
    isTablet: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    // Calculate a darker shade of the color for the 3D bottom edge
    val darkColor = Color(
        red = color.red * 0.8f,
        green = color.green * 0.8f,
        blue = color.blue * 0.8f,
        alpha = 1f
    )

    val emoji = when(grade) {
        4 -> "🎒"
        5 -> "🚀"
        6 -> "👑"
        else -> "📚"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // Bottom shadow (3D edge)
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = 8.dp) // Offset downwards
                .background(darkColor, RoundedCornerShape(24.dp))
        )
        
        // Main front face
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp) // Push upwards relative to shadow
                .background(color, RoundedCornerShape(24.dp))
                .padding(
                    horizontal = if (isTablet) 32.dp else 24.dp,
                    vertical = if (isTablet) 32.dp else 24.dp
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Playful Badge
                Box(
                    modifier = Modifier
                        .size(if (isTablet) 72.dp else 56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = if (isTablet) 36.sp else 28.sp
                    )
                }

                Spacer(Modifier.width(24.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val strings = LocalAppStrings.current
                    Text(
                        text = strings.grade(grade),
                        fontSize = if (isTablet) 32.sp else 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        fontSize = if (isTablet) 18.sp else 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                }

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(if (isTablet) 40.dp else 32.dp)
                )
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
        4 -> Color(0xFF2177DA)
        5 -> Color(0xFF379F3B)
        6 -> Color(0xFFE65100)
        else -> Color(0xFF1340A0)
    }
    val gradeDark = when (gradeLevel) {
        4 -> Color(0xFF1E3A8A)
        5 -> Color(0xFF1B5E20)
        6 -> Color(0xFFBF360C)
        else -> Color(0xFF07153A)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(gradeColor, gradeDark))
            )
    ) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedSection != null) {
                            selectedSection = null
                            searchQuery = ""
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
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
                    // Title
                    Text(
                        text = if (selectedSection == null) {
                            if (strings == com.basahero.elearning.ui.common.getStrings("fil"))
                                "Pumili ng iyong seksyon"
                            else
                                "Select your section"
                        } else {
                            "Who are you?"
                        },
                        fontSize = if (isTablet) 24.sp else 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        style = TextStyle(fontFamily = fredokaFontFamily)
                    )

                    Spacer(Modifier.height(12.dp))

                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                if (selectedSection == null) "Search sections..."
                                else "Search names...",
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White,
                            focusedBorderColor = Color.White.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                            focusedContainerColor = Color.White.copy(alpha = 0.1f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.1f)
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
                        if (selectedSection == null) {
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
                                selectedSection = selectedSection!!,
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
                vertical = 8.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(sections.size) { index ->
                val section = sections[index]
                val count = uiState.students.count { it.section == section }
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
                vertical = 8.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredStudents.size) { index ->
                val student = filteredStudents[index]
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

// ─────────────────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────────────────
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
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
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
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    // Darker shade for the 3D bottom edge
    val shadowColor = Color(
        red = color.red * 0.85f,
        green = color.green * 0.85f,
        blue = color.blue * 0.85f,
        alpha = 1f
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // Bottom shadow (3D edge)
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = 6.dp) // Offset downwards
                .background(shadowColor, RoundedCornerShape(20.dp))
        )
        
        // Main front face
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp) // Push upwards relative to shadow
                .background(Color.White, RoundedCornerShape(20.dp))
                .padding(if (isTablet) 24.dp else 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Section initial badge
                Box(
                    modifier = Modifier
                        .size(if (isTablet) 64.dp else 56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(color, color.copy(alpha = 0.7f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = section.take(1).uppercase(),
                        color = Color.White,
                        fontSize = if (isTablet) 28.sp else 24.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = section,
                    fontSize = if (isTablet) 20.sp else 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B), // Dark slate
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = "$studentCount student${if (studentCount != 1) "s" else ""}",
                    fontSize = if (isTablet) 14.sp else 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B), // Slate gray
                    textAlign = TextAlign.Center
                )
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
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    val initials = student.fullName.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.take(1).uppercase() }

    val shadowColor = Color(
        red = gradeColor.red * 0.85f,
        green = gradeColor.green * 0.85f,
        blue = gradeColor.blue * 0.85f,
        alpha = 1f
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // Bottom shadow (3D edge)
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = 6.dp)
                .background(shadowColor, RoundedCornerShape(20.dp))
        )
        
        // Main front face
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .background(Color.White, RoundedCornerShape(20.dp))
                .padding(if (isTablet) 24.dp else 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Initials avatar
                Box(
                    modifier = Modifier
                        .size(if (isTablet) 64.dp else 56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(gradeColor, gradeColor.copy(alpha = 0.7f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = if (isTablet) 24.sp else 20.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = student.fullName,
                    fontSize = if (isTablet) 16.sp else 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = if (isTablet) 22.sp else 18.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GradeCard — kept for backward compatibility but no longer primary
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeCard(grade: Int, color: Color, subtitle: String, onClick: () -> Unit) {
    GradeSelectionCard(
        grade = grade,
        subtitle = subtitle,
        color = color,
        isTablet = false,
        onClick = onClick
    )
}
