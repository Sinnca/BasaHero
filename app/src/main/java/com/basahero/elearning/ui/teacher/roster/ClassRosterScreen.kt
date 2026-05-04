package com.basahero.elearning.ui.teacher.roster

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basahero.elearning.data.repository.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassRosterScreen(
    classId: String,
    className: String,
    gradeLevel: Int,
    viewModel: ClassRosterViewModel,
    onStudentClick: (studentId: String, studentName: String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Pagination — load more when near bottom
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= uiState.students.size - 5 && uiState.hasMore
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadNextPage()
    }

    LaunchedEffect(classId) { viewModel.loadRoster(classId, gradeLevel) }

    // CSV file picker
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.importFromCsv(context, it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(className, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Grade $gradeLevel · Class Roster",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Import CSV
                    IconButton(onClick = { csvLauncher.launch("text/csv") }) {
                        Icon(Icons.Default.Upload, "Import CSV")
                    }
                    // Add student manually
                    IconButton(onClick = { viewModel.showAddDialog() }) {
                        Icon(Icons.Default.PersonAdd, "Add student")
                    }
                }
            )
        }
    ) { padding ->

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchChanged(it) },
                placeholder = { Text("Search by name or section...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            val displayed = viewModel.filteredStudents

            if (displayed.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PeopleOutline, null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(Modifier.height(12.dp))
                        Text("No students found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        TextButton(onClick = { viewModel.showAddDialog() }) {
                            Text("Add a student")
                        }
                    }
                }
                return@Column
            }

            // Student count header
            Text(
                "${displayed.size} students",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayed) { student ->
                    StudentRosterCard(
                        student = student,
                        onClick = { onStudentClick(student.id, student.fullName) }
                    )
                }

                // Load more indicator
                if (uiState.isLoadingMore) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    // Add student dialog
    if (uiState.showAddDialog) {
        AddStudentDialog(
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { name, section -> viewModel.addStudent(name, section) }
        )
    }

    // Import success snackbar
    if (uiState.showImportResult) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(3000)
            viewModel.dismissImportResult()
        }
    }
}


// ── Student roster card ───────────────────────────────────────────────────────
@Composable
fun StudentRosterCard(student: StudentInfo, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = if (student.isAtRisk)
            BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
        else null
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar circle with initials
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (student.isAtRisk) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = student.fullName.split(" ")
                        .take(2).joinToString("") { it.firstOrNull()?.uppercase() ?: "" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (student.isAtRisk) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(student.fullName, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(
                    student.section,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                student.lastActive?.let { active ->
                    Text(
                        "Last active: ${active.take(10)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (student.isAtRisk) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            "At Risk",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Icon(Icons.Default.ChevronRight, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}


// ── Add student dialog ────────────────────────────────────────────────────────
@Composable
fun AddStudentDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Student", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    placeholder = { Text("e.g. Juan dela Cruz") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = section,
                    onValueChange = { section = it },
                    label = { Text("Section") },
                    placeholder = { Text("e.g. Mabini") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
                Text(
                    "Or import multiple students from a CSV file using the ↑ button in the top bar.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (fullName.isNotBlank() && section.isNotBlank()) onConfirm(fullName, section) },
                enabled = fullName.isNotBlank() && section.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
