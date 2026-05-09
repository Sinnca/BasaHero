package com.basahero.elearning.ui.student.login

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
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
                // Title card
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 1.dp
                    ) {
                        Text(
                            text = strings.selectYourGrade,
                            modifier = Modifier.padding(
                                horizontal = 20.dp,
                                vertical = 16.dp
                            ),
                            fontSize = if (isTablet) 22.sp else 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Each grade with its card
                grades.forEach { grade ->
                    item {
                        Spacer(Modifier.height(8.dp))
                    }

                    // Grade header label
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = grade.color.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, grade.color.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = strings.grade(grade.level),
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 8.dp
                                ),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = grade.color
                            )
                        }
                    }

                    item {
                        Spacer(Modifier.height(4.dp))
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
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

private data class GradeInfo(val level: Int, val color: Color, val subtitle: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GradeSelectionCard(
    grade: Int,
    subtitle: String,
    color: Color,
    isTablet: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isTablet) 100.dp else 88.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.5.dp, color.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Number badge
            Box(
                modifier = Modifier
                    .size(if (isTablet) 64.dp else 52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(color, color.copy(alpha = 0.7f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = grade.toString(),
                    fontSize = if (isTablet) 28.sp else 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                val strings = LocalAppStrings.current
                Text(
                    text = strings.grade(grade),
                    fontSize = if (isTablet) 22.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = if (isTablet) 14.sp else 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = color.copy(alpha = 0.5f),
                modifier = Modifier.size(28.dp)
            )
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
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
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
                        text = if (selectedSection == null)
                            strings.selectYourGrade
                        else
                            "Who are you?",
                        fontSize = if (isTablet) 24.sp else 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = gradeColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
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
    val sections = uiState.students
        .map { it.section }
        .distinct()
        .filter { it.contains(searchQuery, ignoreCase = true) }
        .sortedBy { it.uppercase() }

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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionCard(
    section: String,
    studentCount: Int,
    color: Color,
    isTablet: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isTablet) 20.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Section initial badge
            Box(
                modifier = Modifier
                    .size(if (isTablet) 56.dp else 48.dp)
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
                    fontSize = if (isTablet) 22.sp else 18.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = section,
                fontSize = if (isTablet) 16.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = "$studentCount student${if (studentCount != 1) "s" else ""}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Student Name Card — clean Material3 grid card
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentNameCard(
    student: Student,
    gradeColor: Color,
    isTablet: Boolean = false,
    onClick: () -> Unit
) {
    val initials = student.fullName.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.take(1).uppercase() }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, gradeColor.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isTablet) 20.dp else 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Initials avatar
            Box(
                modifier = Modifier
                    .size(if (isTablet) 56.dp else 48.dp)
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
                    fontSize = if (isTablet) 20.sp else 16.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = student.fullName,
                fontSize = if (isTablet) 14.sp else 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 18.sp
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
    GradeSelectionCard(
        grade = grade,
        subtitle = subtitle,
        color = color,
        isTablet = false,
        onClick = onClick
    )
}
