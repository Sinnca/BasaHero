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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import com.basahero.elearning.data.repository.*

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
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Teacher Dashboard", 
                            fontSize = 20.sp, 
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E293B),
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = uiState.teacher?.fullName ?: "Administrator",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                actions = {
                    // School year filter
                    var expanded by remember { mutableStateOf(false) }
                    Surface(
                        onClick = { expanded = true },
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(uiState.selectedSchoolYear, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp), tint = Color(0xFF1E293B))
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            viewModel.schoolYears.forEach { year ->
                                DropdownMenuItem(
                                    text = { Text(year, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        viewModel.selectSchoolYear(year)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1E293B)
                )
            )
        },
        containerColor = Color(0xFFF8FAFC), // Light Slate background to make cards pop
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 0.dp,
                modifier = Modifier.border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                val items = listOf(
                    Triple("Dashboard", Icons.Default.Dashboard, 0),
                    Triple("Classes", Icons.Default.Class, 1),
                    Triple("Game", Icons.Default.SportsEsports, 2),
                    Triple("Profile", Icons.Default.Person, 3)
                )
                
                items.forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        icon = { Icon(icon, contentDescription = label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1E293B),
                            selectedTextColor = Color(0xFF1E293B),
                            unselectedIconColor = Color(0xFF94A3B8),
                            unselectedTextColor = Color(0xFF94A3B8),
                            indicatorColor = Color(0xFFF1F5F9)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.showCreateDialog() },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("New Class") },
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> DashboardOverviewContent(
                    uiState = uiState,
                    onClassClick = onClassClick,
                    onTabSwitch = { selectedTab = it }
                )
                1 -> {
                    if (uiState.classes.isEmpty()) {
                        EmptyDashboardState()
                    } else {
                        ClassListContent(
                            uiState = uiState,
                            onClassClick = onClassClick
                        )
                    }
                }
                2 -> GameTabContent(
                    uiState = uiState,
                    onHostGameClick = onHostGameClick
                )
                3 -> TeacherProfileContent(
                    uiState = uiState,
                    onLogout = { viewModel.signOut(onLogout) }
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
    onClassClick: (classId: String, className: String, gradeLevel: Int) -> Unit,
    onTabSwitch: (Int) -> Unit
) {
    val greeting = run {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }
    val firstName = uiState.teacher?.fullName?.split(" ")?.firstOrNull() ?: "Teacher"
    val totalStudents = uiState.classes.sumOf { it.studentCount }
    val totalClasses = uiState.classes.size
    val gradeLevels = uiState.classes.map { it.gradeLevel }.distinct().sorted()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Greeting Banner ──
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "$greeting,",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = firstName,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "SY ${uiState.selectedSchoolYear}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        modifier = Modifier
                            .background(Color(0xFF334155), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // ── Stats Row ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OverviewStatCard(
                    modifier = Modifier.weight(1f),
                    value = "$totalClasses",
                    label = "Classes",
                    accent = Color(0xFF3B82F6)
                )
                OverviewStatCard(
                    modifier = Modifier.weight(1f),
                    value = "$totalStudents",
                    label = "Students",
                    accent = Color(0xFF10B981)
                )
                OverviewStatCard(
                    modifier = Modifier.weight(1f),
                    value = "${gradeLevels.size}",
                    label = "Grades",
                    accent = Color(0xFFF59E0B)
                )
            }
        }

        // ── Quick Actions ──
        item {
            Text(
                "Quick Actions",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Class,
                    label = "My Classes",
                    color = Color(0xFF3B82F6),
                    onClick = { onTabSwitch(1) }
                )
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.SportsEsports,
                    label = "Host Game",
                    color = Color(0xFFEA580C),
                    onClick = { onTabSwitch(2) }
                )
            }
        }

        // ── Recent Classes ──
        if (uiState.classes.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Classes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    TextButton(onClick = { onTabSwitch(1) }) {
                        Text("See All", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                    }
                }
            }

            items(uiState.classes.take(3)) { cls ->
                RecentClassRow(cls = cls, onClick = { onClassClick(cls.id, cls.name, cls.gradeLevel) })
            }
        }

        item { Spacer(Modifier.height(60.dp)) }
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
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .then(if (isTablet) Modifier.widthIn(max = 900.dp) else Modifier.fillMaxWidth())
                .padding(if (isTablet) 32.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DashboardStatsRow(classes = uiState.classes)
                Spacer(Modifier.height(32.dp))
                Text(
                    "My Classes", 
                    fontSize = 22.sp, 
                    fontWeight = FontWeight.Black, 
                    color = Color(0xFF1E293B),
                    letterSpacing = (-0.5).sp
                )
            }

            if (isTablet) {
                item {
                    FlowRow(
                        maxItemsInEachRow = 2,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        uiState.classes.forEach { classInfo ->
                            ClassCard(
                                classInfo = classInfo,
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                onClick = { onClassClick(classInfo.id, classInfo.name, classInfo.gradeLevel) }
                            )
                        }
                    }
                }
            } else {
                items(uiState.classes) { classInfo ->
                    ClassCard(
                        classInfo = classInfo,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onClassClick(classInfo.id, classInfo.name, classInfo.gradeLevel) }
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
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
fun DashboardStatsRow(classes: List<ClassInfo>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            value = "${classes.size}",
            label = "Classes",
            icon = Icons.Default.Class,
            color = MaterialTheme.colorScheme.primary
        )
        StatCard(
            modifier = Modifier.weight(1f),
            value = "${classes.sumOf { it.studentCount }}",
            label = "Students",
            icon = Icons.Default.Group,
            color = MaterialTheme.colorScheme.tertiary
        )
        StatCard(
            modifier = Modifier.weight(1f),
            value = "${classes.map { it.gradeLevel }.distinct().size}",
            label = "Grade levels",
            icon = Icons.Default.School,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun StatCard(modifier: Modifier, value: String, label: String,
             icon: androidx.compose.ui.graphics.vector.ImageVector,
             color: androidx.compose.ui.graphics.Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                color = color.copy(alpha = 0.12f),
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = value, 
                fontSize = 26.sp, 
                fontWeight = FontWeight.Black, 
                color = Color(0xFF0F172A),
                lineHeight = 26.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label.uppercase(), 
                fontSize = 9.sp, 
                fontWeight = FontWeight.Black,
                color = Color(0xFF64748B),
                letterSpacing = 1.2.sp
            )
        }
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
    onHostGameClick: (classId: String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text("Host a Game", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF1E293B))
        Text("Select a section to play with.", fontSize = 14.sp, color = Color(0xFF64748B))
        Spacer(Modifier.height(16.dp))
        
        if (uiState.classes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No sections available.", color = Color(0xFF94A3B8))
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.classes) { cls ->
                    Card(
                        onClick = { onHostGameClick(cls.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SportsEsports, null, tint = Color(0xFFEA580C), modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cls.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E293B))
                                Text("Grade ${cls.gradeLevel} • ${cls.studentCount} Students", fontSize = 13.sp, color = Color(0xFF64748B))
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF94A3B8))
                        }
                    }
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
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        // Avatar
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(Color(0xFF3B82F6), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = uiState.teacher?.fullName?.take(1)?.uppercase() ?: "T",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = uiState.teacher?.fullName ?: "Unknown Teacher",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF1E293B)
        )
        Text(
            text = uiState.teacher?.email ?: "No email",
            fontSize = 14.sp,
            color = Color(0xFF64748B)
        )
        
        Spacer(Modifier.height(48.dp))
        
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, null)
            Spacer(Modifier.width(8.dp))
            Text("Log Out", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

