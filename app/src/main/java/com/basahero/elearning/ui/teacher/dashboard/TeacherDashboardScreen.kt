package com.basahero.elearning.ui.teacher.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basahero.elearning.data.repository.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(
    viewModel: TeacherDashboardViewModel,
    onClassClick: (classId: String, className: String, gradeLevel: Int) -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadDashboard() }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Teacher Dashboard", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            uiState.teacher?.fullName ?: "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // School year filter
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { expanded = true }) {
                            Text(uiState.selectedSchoolYear, fontSize = 12.sp)
                            Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            viewModel.schoolYears.forEach { year ->
                                DropdownMenuItem(
                                    text = { Text(year) },
                                    onClick = {
                                        viewModel.selectSchoolYear(year)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.signOut(onLogout) }) {
                        Icon(Icons.Default.Logout, "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showCreateDialog() },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("New Class") }
            )
        }
    ) { padding ->

        if (uiState.classes.isEmpty()) {
            // Empty state
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Class,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("No classes yet", fontSize = 18.sp, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("Tap + to create your first class",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Summary stats row
                item {
                    DashboardStatsRow(classes = uiState.classes)
                    Spacer(Modifier.height(8.dp))
                    Text("My Classes", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                // Class cards
                items(uiState.classes) { classInfo ->
                    ClassCard(
                        classInfo = classInfo,
                        onClick = { onClassClick(classInfo.id, classInfo.name, classInfo.gradeLevel) }
                    )
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}


// ── Class card ────────────────────────────────────────────────────────────────
@Composable
fun ClassCard(classInfo: ClassInfo, onClick: () -> Unit) {
    val gradeColor = when (classInfo.gradeLevel) {
        4 -> MaterialTheme.colorScheme.primary
        5 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Grade circle
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(gradeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "G${classInfo.gradeLevel}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = gradeColor
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(classInfo.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Grade ${classInfo.gradeLevel} · ${classInfo.schoolYear}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.People, null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${classInfo.studentCount} students",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
