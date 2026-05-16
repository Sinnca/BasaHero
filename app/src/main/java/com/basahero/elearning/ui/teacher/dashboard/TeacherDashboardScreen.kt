package com.basahero.elearning.ui.teacher.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import com.basahero.elearning.data.repository.*
import com.basahero.elearning.ui.theme.fredokaFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(
    viewModel: TeacherDashboardViewModel,
    onClassClick: (classId: String, className: String, gradeLevel: Int) -> Unit,
    onHostGameClick: (classId: String) -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { viewModel.loadDashboard() }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (uiState.errorMessage != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(uiState.errorMessage!!, color = Color.Red, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { viewModel.signOut(onLogout) }) {
                    Text("Log Out")
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            if (selectedTab == 3) {
                TopAppBar(
                    title = {
                        Text(
                            text = when(selectedTab) {
                                1 -> "My Classes"
                                3 -> "Profile Settings"
                                else -> "Dashboard"
                            },
                            fontSize = 20.sp, 
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontFamily = fredokaFontFamily
                        )
                    },
                    actions = {
                        if (selectedTab == 1) {
                            IconButton(onClick = { viewModel.showCreateDialog() }) {
                                Icon(Icons.Default.Add, null, tint = Color.White)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1E293B),
                        titleContentColor = Color.White
                    )
                )
            }
        },
        containerColor = Color(0xFFF8FAFC), // Soft Tinted Slate for Modern Vibe
        bottomBar = {
            NavigationBar(
                containerColor = Color.White.copy(alpha = 0.9f),
                tonalElevation = 0.dp,
                modifier = Modifier
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            ) {
                val items = listOf(
                    Triple("Dashboard", Icons.Default.Dashboard, 0),
                    Triple("Classes", Icons.Default.Class, 1),
                    Triple("Game", Icons.Default.SportsEsports, 2),
                    Triple("Profile", Icons.Default.Person, 3)
                )
                
                items.forEach { item ->
                    NavigationBarItem(
                        selected = selectedTab == item.third,
                        onClick = { selectedTab = item.third },
                        icon = { Icon(item.second, contentDescription = item.first) },
                        label = { Text(item.first, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1E293B),
                            selectedTextColor = Color(0xFF1E293B),
                            unselectedIconColor = Color(0xFF94A3B8),
                            unselectedTextColor = Color(0xFF94A3B8),
                            indicatorColor = Color(0xFF1E293B).copy(alpha = 0.1f)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.showCreateDialog() },
                    modifier = Modifier.padding(bottom = 50.dp), // Move it higher
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("New Class") },
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> DashboardOverviewContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    onClassClick = onClassClick,
                    onTabSwitch = { selectedTab = it },
                    modifier = Modifier.padding(padding)
                )
                1 -> {
                    Box(modifier = Modifier.padding(padding)) {
                        if (uiState.classes.isEmpty()) {
                            EmptyDashboardState()
                        } else {
                            ClassListContent(
                                uiState = uiState,
                                onClassClick = onClassClick
                            )
                        }
                    }
                }
                2 -> GameTabContent(
                    uiState = uiState,
                    onHostGameClick = onHostGameClick,
                    onBack = { selectedTab = 0 }
                )
                3 -> TeacherProfileContent(
                    uiState = uiState,
                    onLogout = { viewModel.signOut(onLogout) },
                    modifier = Modifier.padding(padding)
                )
            }
        }

        // Create class dialog
        if (uiState.showCreateClassDialog) {
            CreateClassDialog(
                onDismiss = { viewModel.hideCreateDialog() },
                onConfirm = { name, grade -> viewModel.createClass(name, grade) }
            )
        }

        // Error snackbar
        uiState.errorMessage?.let { msg ->
            LaunchedEffect(msg) {
                // Error displayed inline — reset after showing
            }
        }
    }
}



@Composable
fun EmptyDashboardState() {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Class,
                null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF94A3B8).copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(16.dp))
            Text("No classes yet", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B))
            Spacer(Modifier.height(8.dp))
            Text("Tap + to create your first class",
                fontSize = 14.sp, color = Color(0xFF64748B))
        }
    }
}

// ── Dashboard Overview (Tab 0) ────────────────────────────────────────────────
@Composable
fun DashboardOverviewContent(
    uiState: TeacherDashboardViewModel.DashboardUiState,
    viewModel: TeacherDashboardViewModel,
    onClassClick: (classId: String, className: String, gradeLevel: Int) -> Unit,
    onTabSwitch: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    
    val firstName = uiState.teacher?.fullName?.split(" ")?.firstOrNull() ?: "Teacher"
    val totalStudents = uiState.classes.sumOf { it.studentCount }
    val totalClasses = uiState.classes.size
    val gradeLevels = uiState.classes.map { it.gradeLevel }.distinct().sorted()

    Box(modifier = modifier.fillMaxSize()) {
        // Subtle Atmospheric Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFF1E293B).copy(alpha = 0.015f),
                radius = 400.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.1f)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = if (isTablet) 24.dp else 16.dp, 
                bottom = 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (isTablet) 24.dp else 16.dp)
        ) {
            // ── Wireframe Header: Teacher + Profile ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isTablet) 24.dp else 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Teacher",
                            fontSize = if (isTablet) 36.sp else 28.sp,
                            fontFamily = fredokaFontFamily,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            "WELCOME BACK, ${firstName.uppercase()}",
                            fontSize = if (isTablet) 12.sp else 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = if (isTablet) 1.5.sp else 1.sp
                        )
                    }
                    
                    // Profile Anchor
                    Box(
                        modifier = Modifier
                            .size(if (isTablet) 64.dp else 44.dp)
                            .background(Color(0xFF1E293B).copy(alpha = 0.05f), CircleShape)
                            .border(1.5.dp, Color(0xFF1E293B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            firstName.take(1).uppercase(),
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E293B),
                            fontSize = 18.sp
                        )
                    }
                }
            }

            // ── Wireframe Section: My Classes ──
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = if (isTablet) 24.dp else 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "My Classes",
                            fontSize = if (isTablet) 22.sp else 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }
                    
                    Spacer(Modifier.height(if (isTablet) 16.dp else 12.dp))
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp),
                        contentPadding = PaddingValues(horizontal = if (isTablet) 24.dp else 16.dp)
                    ) {
                        items(uiState.classes) { cls ->
                            ModernClassCard(
                                cls = cls,
                                isTablet = isTablet,
                                modifier = Modifier.width(if (isTablet) 320.dp else 220.dp),
                                onClick = { onClassClick(cls.id, cls.name, cls.gradeLevel) }
                            )
                        }
                    }
                }
            }

            // ── Wireframe Section: Statistics & Activity ──
            item {
                Column(modifier = Modifier.padding(horizontal = if (isTablet) 24.dp else 16.dp)) {
                    // Secondary Duo Cards (Now at the top of this section)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp)
                    ) {
                        EditorialStatCard(
                            modifier = Modifier.weight(1f),
                            value = "$totalStudents",
                            label = "STUDENTS",
                            color = Color(0xFF1E293B),
                            icon = Icons.Default.Groups
                        )
                        EditorialStatCard(
                            modifier = Modifier.weight(1f),
                            value = "${gradeLevels.size}",
                            label = "GRADES",
                            color = Color(0xFF64748B),
                            icon = Icons.Default.Layers
                        )
                    }
                    
                    Spacer(Modifier.height(if (isTablet) 48.dp else 24.dp))
                    
                    // ── Recent Activity Header (Moved below cards) ──
                    Text(
                        "Recent Activity",
                        fontSize = if (isTablet) 20.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(Modifier.height(if (isTablet) 24.dp else 12.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        if (uiState.activities.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("NO RECENT EVENTS RECORDED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                            }
                        } else {
                            // Responsive Grid: 2 columns for tablet, 1 column for mobile
                            val columnCount = if (isTablet) 2 else 1
                            val activitiesToShow = if (isTablet) uiState.activities.take(4) else uiState.activities.take(2)
                            
                            activitiesToShow.chunked(columnCount).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                                ) {
                                    rowItems.forEach { activity ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            ModernActivityItem(activity = activity, isTablet = isTablet)
                                        }
                                    }
                                    // Filler if odd number of items in tablet mode
                                    if (isTablet && rowItems.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Compact stat card for the overview ────────────────────────────────────────
@Composable
fun OverviewStatCard(modifier: Modifier, value: String, label: String, accent: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = accent
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B)
            )
        }
    }
}

// ── Quick action card ─────────────────────────────────────────────────────────
@Composable
fun QuickActionCard(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        }
    }
}

// ── Recent class compact row ──────────────────────────────────────────────────
@Composable
fun RecentClassRow(cls: ClassInfo, onClick: () -> Unit) {
    val gradeColor = when (cls.gradeLevel) {
        4 -> Color(0xFF2563EB)
        5 -> Color(0xFF059669)
        else -> Color(0xFFD97706)
    }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(gradeColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("G${cls.gradeLevel}", fontWeight = FontWeight.Black, fontSize = 14.sp, color = gradeColor)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(cls.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Text("${cls.studentCount} students", fontSize = 12.sp, color = Color(0xFF64748B))
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(20.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClassListContent(
    uiState: TeacherDashboardViewModel.DashboardUiState,
    onClassClick: (classId: String, className: String, gradeLevel: Int) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Box(
        modifier = Modifier.fillMaxSize()
            .statusBarsPadding(), // Ensure it doesn't overlap with status bar
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = if (isTablet) 24.dp else 16.dp,
                end = if (isTablet) 24.dp else 16.dp,
                top = if (isTablet) 32.dp else 16.dp,
                bottom = 120.dp // Added extra bottom space for the FAB
            ),
            verticalArrangement = Arrangement.spacedBy(if (isTablet) 28.dp else 20.dp)
        ) {
            // Header Section
            item {
                Column {
                    Text(
                        "Classroom",
                        fontSize = if (isTablet) 40.sp else 28.sp,
                        fontFamily = fredokaFontFamily,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        "Overview of all active sections",
                        fontSize = if (isTablet) 16.sp else 14.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Stats Section
            item {
                DashboardStatsRow(classes = uiState.classes, isTablet = isTablet)
            }

            // Section Label
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "MY SECTIONS",
                        fontSize = if (isTablet) 13.sp else 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.width(16.dp))
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFFF1F5F9)))
                }
            }

            // Grid Section
            if (isTablet) {
                item {
                    FlowRow(
                        maxItemsInEachRow = 3,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        uiState.classes.forEach { classInfo ->
                            ModernClassCard(
                                cls = classInfo,
                                isTablet = true,
                                modifier = Modifier.weight(1f),
                                onClick = { onClassClick(classInfo.id, classInfo.name, classInfo.gradeLevel) }
                            )
                        }
                        
                        // Fillers to maintain grid alignment
                        val fillers = (3 - (uiState.classes.size % 3)) % 3
                        repeat(fillers) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                items(uiState.classes) { classInfo ->
                    ModernClassCard(
                        cls = classInfo,
                        isTablet = false,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onClassClick(classInfo.id, classInfo.name, classInfo.gradeLevel) }
                    )
                }
            }
        }
    }
}

@Composable
fun PlaceholderTabContent(title: String, subtitle: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF1E293B))
            Spacer(Modifier.height(8.dp))
            Text(subtitle, fontSize = 14.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center)
        }
    }
}

// ── Dashboard stats row ───────────────────────────────────────────────────────
@Composable
fun DashboardStatsRow(classes: List<ClassInfo>, isTablet: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 10.dp)
    ) {
        EditorialStatCard(
            modifier = Modifier.weight(1f),
            value = "${classes.size}",
            label = "SECTIONS",
            color = Color(0xFF1E293B),
            icon = Icons.Default.Class
        )
        EditorialStatCard(
            modifier = Modifier.weight(1f),
            value = "${classes.sumOf { it.studentCount }}",
            label = "STUDENTS",
            color = Color(0xFF6366F1),
            icon = Icons.Default.Group
        )
        EditorialStatCard(
            modifier = Modifier.weight(1f),
            value = "${classes.map { it.gradeLevel }.distinct().size}",
            label = "GRADES",
            color = Color(0xFF0EA5E9),
            icon = Icons.Default.School
        )
    }
}



// ── Class card ────────────────────────────────────────────────────────────────
@Composable
fun ClassCard(classInfo: ClassInfo, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val gradeColor = when (classInfo.gradeLevel) {
        4 -> Color(0xFF2563EB) // Royal Blue
        5 -> Color(0xFF059669) // Emerald
        else -> Color(0xFFD97706) // Amber
    }

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, Color(0xFFE2E8F0)), // Increased thickness and visibility
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Grade badge with gradient
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(gradeColor.copy(alpha = 0.2f), gradeColor.copy(alpha = 0.05f))
                        )
                    )
                    .border(1.dp, gradeColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "G${classInfo.gradeLevel}",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = gradeColor
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = classInfo.name, 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "SY ${classInfo.schoolYear}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8)
                )
                
                Spacer(Modifier.height(8.dp))
                
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Groups, 
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF64748B)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${classInfo.studentCount} Students Enrolled",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFF1F5F9), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ChevronRight, 
                    null,
                    tint = Color(0xFF1E293B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


// ── Create class dialog ───────────────────────────────────────────────────────
@Composable
fun CreateClassDialog(onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) {
    var className by remember { mutableStateOf("") }
    var selectedGrade by remember { mutableStateOf(4) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Class", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it },
                    label = { Text("Section name") },
                    placeholder = { Text("e.g. Mabini") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                Text("Grade level", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(4, 5, 6).forEach { grade ->
                        FilterChip(
                            selected = selectedGrade == grade,
                            onClick = { selectedGrade = grade },
                            label = { Text("Grade $grade") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (className.isNotBlank()) onConfirm(className, selectedGrade) },
                enabled = className.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// -----------------------------------------------------------------------------
// Tab 2: Game Content
// -----------------------------------------------------------------------------
@Composable
fun GameTabContent(
    uiState: TeacherDashboardViewModel.DashboardUiState,
    onHostGameClick: (classId: String) -> Unit,
    onBack: () -> Unit
) {
    val groupedClasses = uiState.classes.groupBy { it.gradeLevel }.toSortedMap()

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))
    ) {
        // ── Unified Header (Copied from Class Roster) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B))
                .statusBarsPadding()
                .padding(
                    start = 12.dp,
                    end = 24.dp,
                    top = 40.dp,
                    bottom = 40.dp
                ),
            verticalAlignment = Alignment.Top
        ) {
            // Back button
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(52.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(20.dp))

            // Text content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 10.dp)
            ) {
                Text(
                    text = "Host a Game",
                    fontSize = 32.sp,
                    fontFamily = fredokaFontFamily,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "GAME SESSIONS · SELECT A SECTION",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.55f),
                    letterSpacing = 1.2.sp
                )
            }
        }

        if (uiState.classes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SportsEsports, null, modifier = Modifier.size(64.dp), tint = Color(0xFFCBD5E1))
                    Text("No sections ready for gaming.", color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                groupedClasses.forEach { (grade, classes) ->
                    item {
                        Surface(
                            color = Color(0xFF1E293B).copy(alpha = 0.05f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "GRADE $grade",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                letterSpacing = 1.5.sp
                            )
                        }
                    }
                    items(classes) { cls ->
                        PremiumGameCard(cls = cls, onClick = { onHostGameClick(cls.id) })
                    }
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
fun PremiumGameCard(cls: ClassInfo, onClick: () -> Unit) {
    val gradeColor = when (cls.gradeLevel) {
        4 -> Color(0xFF3B82F6) // Modern Blue
        5 -> Color(0xFFF59E0B) // Modern Amber
        6 -> Color(0xFFEF4444) // Modern Red
        else -> Color(0xFF64748B)
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = gradeColor.copy(alpha = 0.15f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.6f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle accent background
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = (-40).dp)
                    .background(gradeColor.copy(alpha = 0.03f), CircleShape)
            )

            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon with layered background
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(gradeColor.copy(alpha = 0.1f), CircleShape)
                    )
                    Icon(
                        Icons.Default.SportsEsports, 
                        null, 
                        tint = gradeColor, 
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(Modifier.width(20.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cls.name, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 20.sp, 
                        color = Color(0xFF1E293B),
                        fontFamily = fredokaFontFamily
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${cls.studentCount} Students", 
                                fontSize = 11.sp, 
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                
                // Play button indicator
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(gradeColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow, 
                        null, 
                        tint = gradeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Tab 3: Profile Content
// -----------------------------------------------------------------------------
@Composable
fun TeacherProfileContent(
    uiState: TeacherDashboardViewModel.DashboardUiState,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Color(0xFFF1F5F9)), // Light modern background
        contentPadding = PaddingValues(bottom = 120.dp) // Space for FAB/nav
    ) {
        item {
            // Header Gradient Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isTablet) 240.dp else 140.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(Color(0xFF511D89), Color(0xFF8B5CF6))
                        )
                    )
            ) {
                // Subtle decorative circles
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(Color.White.copy(alpha = 0.05f), radius = 300f, center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.1f))
                    drawCircle(Color.White.copy(alpha = 0.05f), radius = 150f, center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.8f))
                }
            }
        }

        item {
            // Profile Info Box that overlaps the header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-60).dp)
                    .padding(horizontal = if (isTablet) 48.dp else 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(if (isTablet) 140.dp else 96.dp)
                        .background(Color.White, CircleShape)
                        .padding(if (isTablet) 6.dp else 4.dp) // White border effect
                        .background(Color(0xFFFFD700), CircleShape), // BasaHero Yellow
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.teacher?.fullName?.take(1)?.uppercase() ?: "T",
                        fontSize = if (isTablet) 56.sp else 40.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = fredokaFontFamily,
                        color = Color(0xFF511D89)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = uiState.teacher?.fullName ?: "Unknown Teacher",
                    fontSize = if (isTablet) 32.sp else 24.sp,
                    fontFamily = fredokaFontFamily,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = uiState.teacher?.email ?: "No email",
                    fontSize = if (isTablet) 16.sp else 14.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(32.dp))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val totalClasses = uiState.classes.size
                    val totalStudents = uiState.classes.sumOf { it.studentCount }
                    
                    ProfileStatCard(modifier = Modifier.weight(1f), value = totalClasses.toString(), label = "Classes", icon = Icons.Default.Class, color = Color(0xFF4A90E2), isTablet = isTablet)
                    ProfileStatCard(modifier = Modifier.weight(1f), value = totalStudents.toString(), label = "Students", icon = Icons.Default.Group, color = Color(0xFF10B981), isTablet = isTablet)
                }

                Spacer(Modifier.height(32.dp))

                // Settings Cards
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("ACCOUNT", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), letterSpacing = 1.sp, modifier = Modifier.padding(start = 8.dp))
                    ProfileMenuRow(icon = Icons.Default.Person, title = "Personal Information", isTablet = isTablet)
                    ProfileMenuRow(icon = Icons.Default.Security, title = "Security & Password", isTablet = isTablet)
                    ProfileMenuRow(icon = Icons.Default.Notifications, title = "Notifications", isTablet = isTablet)
                }

                Spacer(Modifier.height(48.dp))

                // Logout Button
                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444).copy(alpha = 0.1f), 
                        contentColor = Color(0xFFEF4444)
                    ),
                    modifier = Modifier.fillMaxWidth().height(if (isTablet) 64.dp else 56.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null)
                    Spacer(Modifier.width(12.dp))
                    Text("Log Out", fontSize = if (isTablet) 18.sp else 16.sp, fontWeight = FontWeight.Bold, fontFamily = fredokaFontFamily)
                }
            }
        }
    }
}

@Composable
fun ProfileStatCard(modifier: Modifier, value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, isTablet: Boolean) {
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(if (isTablet) 24.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(if (isTablet) 48.dp else 36.dp).background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(if (isTablet) 24.dp else 18.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(value, fontSize = if (isTablet) 28.sp else 20.sp, fontWeight = FontWeight.Black, fontFamily = fredokaFontFamily, color = Color(0xFF1E293B))
            Text(label, fontSize = if (isTablet) 14.sp else 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
        }
    }
}

@Composable
fun ProfileMenuRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, isTablet: Boolean) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(if (isTablet) 20.dp else 12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(if (isTablet) 40.dp else 32.dp).background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color(0xFF64748B), modifier = Modifier.size(if (isTablet) 20.dp else 16.dp))
            }
            Spacer(Modifier.width(if (isTablet) 16.dp else 12.dp))
            Text(title, fontSize = if (isTablet) 18.sp else 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B), modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(if (isTablet) 24.dp else 20.dp))
        }
    }
}

// ── Refined Editorial Components ──────────────────────────────────────────

@Composable
fun ModernClassCard(cls: ClassInfo, isTablet: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val gradeColor = when (cls.gradeLevel) {
        4 -> Color(0xFF4A90E2) // Grade 4 Blue
        5 -> Color(0xFFFFAB40) // Grade 5 Orange
        6 -> Color(0xFFE53935) // Grade 6 Red
        else -> Color(0xFF64748B) // Default Slate
    }
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(if (isTablet) 28.dp else 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, gradeColor.copy(alpha = 0.6f)) // Colored border based on grade
    ) {
        Column(modifier = Modifier.padding(if (isTablet) 32.dp else 16.dp)) {
            // Compact Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isTablet) 32.dp else 24.dp)
                        .background(gradeColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${cls.gradeLevel}", 
                        color = gradeColor, 
                        fontWeight = FontWeight.Black, 
                        fontSize = if (isTablet) 12.sp else 10.sp
                    )
                }
                Text(
                    "G${cls.gradeLevel} SECTION", 
                    fontSize = if (isTablet) 9.sp else 8.sp, 
                    fontWeight = FontWeight.Black, 
                    color = Color(0xFF94A3B8),
                    letterSpacing = 1.sp
                )
            }
            
            Spacer(Modifier.height(if (isTablet) 16.dp else 12.dp))
            
            Text(
                cls.name, 
                fontSize = if (isTablet) 26.sp else 16.sp, 
                fontFamily = fredokaFontFamily, 
                fontWeight = FontWeight.Black, 
                color = Color(0xFF1E293B),
                maxLines = 1
            )
            
            Spacer(Modifier.height(4.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${cls.studentCount} Students", 
                    fontSize = if (isTablet) 12.sp else 11.sp, 
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.height(if (isTablet) 20.dp else 16.dp))
            
            // Ultra-minimalist Progress
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isTablet) 4.dp else 3.dp)
                    .background(Color(0xFFF1F5F9), CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .fillMaxHeight()
                        .background(gradeColor, CircleShape)
                )
            }
        }
    }
}


@Composable
fun ModernActivityItem(activity: TeacherDashboardViewModel.ActivityItem, isTablet: Boolean = true) {
    val color = when(activity.type) {
        TeacherDashboardViewModel.ActivityType.CLASS_CREATED -> Color(0xFF3B82F6)
        TeacherDashboardViewModel.ActivityType.STUDENT_JOINED -> Color(0xFF10B981)
        TeacherDashboardViewModel.ActivityType.PERFORMANCE -> Color(0xFF6366F1)
        TeacherDashboardViewModel.ActivityType.GAME_ENDED -> Color(0xFFF59E0B)
    }
    
    val icon = when(activity.type) {
        TeacherDashboardViewModel.ActivityType.CLASS_CREATED -> Icons.Default.Layers
        TeacherDashboardViewModel.ActivityType.STUDENT_JOINED -> Icons.Default.PersonAdd
        TeacherDashboardViewModel.ActivityType.PERFORMANCE -> Icons.AutoMirrored.Filled.TrendingUp
        TeacherDashboardViewModel.ActivityType.GAME_ENDED -> Icons.Default.SportsEsports
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isTablet) 8.dp else 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Compact Timeline
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(if (isTablet) 12.dp else 6.dp)
                    .background(Color(0xFF1E293B).copy(alpha = 0.1f), CircleShape)
                    .border(1.dp, Color(0xFF1E293B), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(if (isTablet) 5.dp else 2.dp).background(Color(0xFF1E293B), CircleShape))
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(if (isTablet) 60.dp else 40.dp)
                    .background(Color(0xFFF1F5F9))
            )
        }
        
        Spacer(Modifier.width(if (isTablet) 16.dp else 8.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(if (isTablet) 16.dp else 10.dp))
                Spacer(Modifier.width(if (isTablet) 8.dp else 4.dp))
                Text(
                    activity.time.uppercase(), 
                    fontSize = if (isTablet) 11.sp else 7.sp, 
                    fontWeight = FontWeight.Black, 
                    color = Color(0xFF94A3B8), 
                    letterSpacing = if (isTablet) 1.2.sp else 0.5.sp
                )
            }
            Text(
                activity.title, 
                fontSize = if (isTablet) 18.sp else 12.sp, 
                fontWeight = FontWeight.Bold, 
                color = Color(0xFF1E293B)
            )
            Text(
                activity.desc, 
                fontSize = if (isTablet) 14.sp else 10.sp, 
                color = Color(0xFF64748B), 
                lineHeight = if (isTablet) 20.sp else 14.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun EditorialStatCard(modifier: Modifier, value: String, label: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(if (isTablet) 24.dp else 14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFF50E3C2).copy(alpha = 0.6f)) // Use SecondaryMint color for border
    ) {
        Column(modifier = Modifier.padding(if (isTablet) 32.dp else 12.dp)) {
            Box(
                modifier = Modifier
                    .size(if (isTablet) 48.dp else 32.dp)
                    .background(color.copy(alpha = 0.08f), RoundedCornerShape(if (isTablet) 12.dp else 8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(if (isTablet) 24.dp else 16.dp))
            }
            Spacer(Modifier.height(if (isTablet) 12.dp else 8.dp))
            Text(
                value, 
                fontSize = if (isTablet) 40.sp else 24.sp, 
                fontFamily = fredokaFontFamily, 
                fontWeight = FontWeight.Black, 
                color = Color(0xFF1E293B)
            )
            Text(
                label, 
                fontSize = 12.sp, 
                fontWeight = FontWeight.Black, 
                color = Color(0xFF94A3B8), 
                letterSpacing = 1.5.sp
            )
        }
    }
}
