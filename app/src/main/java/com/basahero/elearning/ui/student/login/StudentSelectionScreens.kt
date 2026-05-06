package com.basahero.elearning.ui.student.login

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.basahero.elearning.data.model.Student

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentGradeSelectScreen(
    onGradeSelected: (Int) -> Unit,
    onManualLoginClick: () -> Unit,
    onBack: () -> Unit
) {
    val alpha = remember { androidx.compose.animation.core.Animatable(0f) }
    val scale = remember { androidx.compose.animation.core.Animatable(0.9f) }
    
    // Floating animation for background
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "float")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(2000, easing = androidx.compose.animation.core.EaseInOutSine),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "floatAnim"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.coroutineScope {
            launch { alpha.animateTo(1f, androidx.compose.animation.core.tween(600)) }
            launch { 
                scale.animateTo(
                    1f, 
                    androidx.compose.animation.core.spring(
                        dampingRatio = 0.6f, 
                        stiffness = 150f
                    )
                ) 
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(
                        Color(0xFF2C74F3),
                        Color(0xFF1340A0),
                        Color(0xFF091C4E)
                    )
                )
            )
    ) {
        // Magical Background (same as landing page)
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = 200f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.2f + offsetY)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = 300f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.7f - offsetY)
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .graphicsLayer {
                        this.alpha = alpha.value
                        this.scaleX = scale.value
                        this.scaleY = scale.value
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Pick Your Grade",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color(0x66000000),
                            offset = androidx.compose.ui.geometry.Offset(0f, 4f),
                            blurRadius = 8f
                        )
                    )
                )
                
                Spacer(modifier = Modifier.height(48.dp))

                GradeCard(
                    grade = 4,
                    color = Color(0xFF2177DA),
                    subtitle = "The Beginning!",
                    onClick = { onGradeSelected(4) }
                )
                Spacer(modifier = Modifier.height(20.dp))
                GradeCard(
                    grade = 5,
                    color = Color(0xFF379F3B),
                    subtitle = "Getting Stronger!",
                    onClick = { onGradeSelected(5) }
                )
                Spacer(modifier = Modifier.height(20.dp))
                GradeCard(
                    grade = 6,
                    color = Color(0xFFE65100),
                    subtitle = "The Masters!",
                    onClick = { onGradeSelected(6) }
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                TextButton(onClick = onManualLoginClick) {
                    Text(
                        text = "I have a different name/section",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeCard(grade: Int, color: Color, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Custom Grade Badge (No Emoji)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = grade.toString(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = "Grade $grade",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StudentNameSelectScreen(
    gradeLevel: Int,
    viewModel: StudentSelectionViewModel,
    onStudentSelected: (Student) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    var selectedSection by remember { mutableStateOf<String?>(null) }
    
    // Floating background animation
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "float")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(2500, easing = androidx.compose.animation.core.EaseInOutSine),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "floatAnim"
    )

    BackHandler(enabled = selectedSection != null) {
        selectedSection = null
    }
    
    LaunchedEffect(gradeLevel) {
        viewModel.loadStudents(gradeLevel)
    }
    
    val themeColor = when (gradeLevel) {
        4 -> Color(0xFF2177DA)
        5 -> Color(0xFF379F3B)
        6 -> Color(0xFFE65100)
        else -> Color(0xFF1340A0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(
                        themeColor.copy(alpha = 0.9f),
                        themeColor,
                        themeColor.copy(alpha = 0.8f)
                    )
                )
            )
    ) {
        // Background elements
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = 150f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.15f + offsetY)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = 250f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.85f - offsetY)
            )
        }

        Scaffold(
            containerColor = Color.Transparent
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color(0xFF2563EB), Color(0xFF1E40AF), Color(0xFF1E3A8A))
                        )
                    )
            ) {
                // Magical Floating Blobs
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = canvasWidth * 0.4f,
                        center = androidx.compose.ui.geometry.Offset(canvasWidth * 0.8f, canvasHeight * 0.1f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.03f),
                        radius = canvasWidth * 0.6f,
                        center = androidx.compose.ui.geometry.Offset(canvasWidth * 0.2f, canvasHeight * 0.9f)
                    )
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { 
                                    if (selectedSection != null) selectedSection = null 
                                    else onBack() 
                                },
                                modifier = Modifier.background(Color.White.copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                            }
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = if (selectedSection == null) "Select Section" else "Select Your Name",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = (-1).sp
                            )
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        
                        // Glassmorphic Search Bar
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { 
                                    Text(
                                        if (selectedSection == null) "Find your section..." else "Find your name...",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Medium
                                    ) 
                                },
                                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = Color.White,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true
                            )
                        }
                    }

                    // Adaptive Grid Content
                    Box(modifier = Modifier.weight(1f)) {
                        if (uiState.isLoading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        } else if (uiState.errorMessage != null) {
                            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(text = uiState.errorMessage ?: "", color = Color.White, textAlign = TextAlign.Center)
                            }
                        } else {
                            if (selectedSection == null) {
                                SectionGridContent(
                                    uiState = uiState,
                                    searchQuery = searchQuery,
                                    themeColor = themeColor,
                                    onSelect = { selectedSection = it }
                                )
                            } else {
                                StudentGridContent(
                                    uiState = uiState,
                                    selectedSection = selectedSection!!,
                                    searchQuery = searchQuery,
                                    themeColor = themeColor,
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
}

@Composable
private fun SectionGridContent(
    uiState: StudentSelectionViewModel.SelectionState,
    searchQuery: String,
    themeColor: Color,
    onSelect: (String) -> Unit
) {
    val sections = uiState.students
        .map { it.section }
        .distinct()
        .filter { it.contains(searchQuery, ignoreCase = true) }
        .sortedBy { it.uppercase() }

    if (sections.isEmpty()) {
        EmptyState(if (searchQuery.isEmpty()) "No sections available." else "No section matches '$searchQuery'")
    } else {
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 300.dp),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(sections.size) { index ->
                val section = sections[index]
                val count = uiState.students.count { it.section == section }
                SectionCard(
                    section = section,
                    studentCount = count,
                    color = themeColor,
                    onClick = { onSelect(section) }
                )
            }
        }
    }
}

@Composable
private fun StudentGridContent(
    uiState: StudentSelectionViewModel.SelectionState,
    selectedSection: String,
    searchQuery: String,
    themeColor: Color,
    onSelect: (Student) -> Unit
) {
    val filteredStudents = uiState.students
        .filter { it.section == selectedSection }
        .filter { it.fullName.contains(searchQuery, ignoreCase = true) }
        .sortedBy { it.fullName.uppercase() }

    if (filteredStudents.isEmpty()) {
        EmptyState("No name matches '$searchQuery' in $selectedSection.")
    } else {
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 280.dp),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredStudents.size) { index ->
                val student = filteredStudents[index]
                StudentNameCard(
                    student = student,
                    gradeColor = themeColor,
                    onClick = { onSelect(student) }
                )
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionCard(section: String, studentCount: Int, color: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        border = BorderStroke(1.5.dp, color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Magical Gradient Avatar
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(color, color.copy(alpha = 0.7f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = section.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = section,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "$studentCount Student${if (studentCount != 1) "s" else ""}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = color.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentNameCard(student: Student, gradeColor: Color, onClick: () -> Unit) {
    val initials = student.fullName.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.take(1).uppercase() }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp), // More rounded for "Magical" feel
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        border = BorderStroke(1.5.dp, gradeColor.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // High-impact Initials Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(gradeColor, gradeColor.copy(alpha = 0.7f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.2f),
                            offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                            blurRadius = 4f
                        )
                    )
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.fullName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1E293B),
                    letterSpacing = (-0.2).sp
                )
                Text(
                    text = "Tap to enter classroom",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = gradeColor.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
