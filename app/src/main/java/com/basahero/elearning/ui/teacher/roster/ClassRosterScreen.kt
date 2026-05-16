package com.basahero.elearning.ui.teacher.roster

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basahero.elearning.data.repository.*
import com.basahero.elearning.ui.theme.fredokaFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassRosterScreen(
    classId: String,
    className: String,
    gradeLevel: Int,
    viewModel: ClassRosterViewModel,
    onStudentClick: (studentId: String, studentName: String) -> Unit,
    onAnalyticsClick: () -> Unit,
    onHostGameClick: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    // Responsive dimensions
    val hPad: Dp = if (isTablet) 40.dp else 20.dp
    val cardRadius: Dp = if (isTablet) 24.dp else 16.dp
    val avatarSize: Dp = if (isTablet) 56.dp else 48.dp
    val nameFontSize = if (isTablet) 18.sp else 15.sp
    val subFontSize = if (isTablet) 13.sp else 12.sp

    val gradeColor = when (gradeLevel) {
        4 -> Color(0xFF4A90E2)
        5 -> Color(0xFFFFAB40)
        6 -> Color(0xFFE53935)
        else -> Color(0xFF64748B)
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= uiState.students.size - 5 && uiState.hasMore
        }
    }

    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) viewModel.loadNextPage() }
    LaunchedEffect(classId) { viewModel.loadRoster(classId, className, gradeLevel) }

    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        uri -> uri?.let { viewModel.importFromCsv(context, it) }
    }

    Scaffold(
        containerColor = Color(0xFFF1F5F9),
        topBar = {
            // ─── HEADER ────────────────────────────────────────────────────────
            // ── Single unified header row ──
            // ── Responsive Header ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .statusBarsPadding()
                    .padding(
                        start = if (isTablet) 24.dp else 12.dp,
                        end = hPad,
                        top = if (isTablet) 48.dp else 24.dp,
                        bottom = if (isTablet) 48.dp else 24.dp
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Back button — sits higher
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(if (isTablet) 60.dp else 52.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, "Back",
                            tint = Color.White,
                            modifier = Modifier.size(if (isTablet) 28.dp else 24.dp)
                        )
                    }

                    Spacer(Modifier.width(if (isTablet) 24.dp else 16.dp))

                    // Section name + subtitle
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = if (isTablet) 12.dp else 10.dp)
                    ) {
                        Text(
                            className,
                            fontSize = if (isTablet) 42.sp else 28.sp,
                            fontFamily = fredokaFontFamily,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "CLASS ROSTER · ${uiState.students.size} STUDENTS",
                            fontSize = if (isTablet) 16.sp else 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.55f),
                            letterSpacing = 1.2.sp
                        )
                    }

                    if (isTablet) {
                        Spacer(Modifier.width(14.dp))
                        // Action chips for tablet
                        Row(
                            modifier = Modifier.padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RosterChip(icon = Icons.Default.VideogameAsset, label = "HOST", isTablet = true, onClick = onHostGameClick)
                            RosterChip(icon = Icons.Default.BarChart, label = "STATS", isTablet = true, onClick = onAnalyticsClick)
                            RosterChip(icon = Icons.Default.Upload, label = "CSV", isTablet = true, onClick = { csvLauncher.launch("*/*") })
                        }
                    }
                }
                
                // Action chips for mobile (moves to next line)
                if (!isTablet) {
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 68.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RosterChip(icon = Icons.Default.VideogameAsset, label = "HOST", isTablet = false, onClick = onHostGameClick)
                        RosterChip(icon = Icons.Default.BarChart, label = "STATS", isTablet = false, onClick = onAnalyticsClick)
                        RosterChip(icon = Icons.Default.Upload, label = "CSV", isTablet = false, onClick = { csvLauncher.launch("*/*") })
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                modifier = Modifier
                    .padding(bottom = 32.dp) // Float it higher
                    .height(if (isTablet) 64.dp else 56.dp), // Make it taller
                containerColor = Color(0xFF1E293B),
                contentColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                icon = {
                    Icon(
                        Icons.Default.PersonAdd, null,
                        modifier = Modifier.size(if (isTablet) 24.dp else 22.dp)
                    )
                },
                text = {
                    Text(
                        "Add Student",
                        fontWeight = FontWeight.Black,
                        fontSize = if (isTablet) 16.sp else 15.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            // Constrain max width on tablet for readability
            Box(
                modifier = if (isTablet)
                    Modifier.widthIn(max = 960.dp).fillMaxHeight()
                else
                    Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        start = hPad,
                        end = hPad,
                        top = if (isTablet) 28.dp else 20.dp,
                        bottom = 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp)
                ) {
                    // ── Search Field ──
                    item {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchChanged(it) },
                            placeholder = {
                                Text(
                                    "Search students by name…",
                                    color = Color(0xFFADB5BD),
                                    fontSize = if (isTablet) 15.sp else 14.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search, null,
                                    tint = Color(0xFFADB5BD),
                                    modifier = Modifier.size(if (isTablet) 22.dp else 20.dp)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isTablet) 60.dp else 54.dp),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.White,
                                focusedContainerColor = Color.White,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedBorderColor = gradeColor
                            )
                        )
                    }

                    // ── Section divider label ──
                    if (!uiState.isLoading) {
                        val displayed = viewModel.filteredStudents
                        if (displayed.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = if (isTablet) 8.dp else 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${displayed.size} STUDENTS",
                                        fontSize = if (isTablet) 12.sp else 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF94A3B8),
                                        letterSpacing = 1.5.sp
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    HorizontalDivider(
                                        modifier = Modifier.weight(1f),
                                        color = Color(0xFFE2E8F0),
                                        thickness = 1.dp
                                    )
                                }
                            }
                        }
                    }

                    // ── Loading ──
                    if (uiState.isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = gradeColor, strokeWidth = 3.dp)
                            }
                        }
                    } else {
                        val displayed = viewModel.filteredStudents
                        if (displayed.isEmpty()) {
                            // ── Empty State ──
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = if (isTablet) 80.dp else 60.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(if (isTablet) 96.dp else 80.dp)
                                            .background(Color(0xFFE2E8F0), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.PeopleOutline, null,
                                            modifier = Modifier.size(if (isTablet) 48.dp else 40.dp),
                                            tint = Color(0xFFCBD5E1)
                                        )
                                    }
                                    Spacer(Modifier.height(if (isTablet) 24.dp else 18.dp))
                                    Text(
                                        "No students yet",
                                        fontSize = if (isTablet) 22.sp else 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = fredokaFontFamily,
                                        color = Color(0xFF475569)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Tap \"Add Student\" or import from CSV",
                                        fontSize = if (isTablet) 15.sp else 13.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        } else {
                            // ── Student Cards ──
                            if (isTablet) {
                                items(displayed.chunked(2)) { pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        pair.forEach { student ->
                                            RosterStudentCard(
                                                student = student,
                                                gradeColor = gradeColor,
                                                isTablet = true,
                                                avatarSize = avatarSize,
                                                cardRadius = cardRadius,
                                                nameFontSize = nameFontSize,
                                                subFontSize = subFontSize,
                                                modifier = Modifier.weight(1f),
                                                onClick = { onStudentClick(student.id, student.fullName) }
                                            )
                                        }
                                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                }
                            } else {
                                items(displayed) { student ->
                                    RosterStudentCard(
                                        student = student,
                                        gradeColor = gradeColor,
                                        isTablet = false,
                                        avatarSize = avatarSize,
                                        cardRadius = cardRadius,
                                        nameFontSize = nameFontSize,
                                        subFontSize = subFontSize,
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = { onStudentClick(student.id, student.fullName) }
                                    )
                                }
                            }
                        }
                    }

                    // ── Load More ──
                    if (uiState.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = gradeColor,
                                    strokeWidth = 2.5.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showAddDialog) {
        AddStudentDialog(
            defaultSection = className,
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { name, section -> viewModel.addStudent(name, section) }
        )
    }
}

// ── Header Action Chip ────────────────────────────────────────────────────────
@Composable
fun RosterChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isTablet: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.12f),
        modifier = Modifier.height(if (isTablet) 44.dp else 38.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (isTablet) 16.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon, null,
                tint = Color.White,
                modifier = Modifier.size(if (isTablet) 18.dp else 16.dp)
            )
            Text(
                label,
                color = Color.White,
                fontSize = if (isTablet) 11.sp else 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp
            )
        }
    }
}

// ── Student Card ──────────────────────────────────────────────────────────────
@Composable
fun RosterStudentCard(
    student: StudentInfo,
    gradeColor: Color,
    isTablet: Boolean,
    avatarSize: Dp,
    cardRadius: Dp,
    nameFontSize: androidx.compose.ui.unit.TextUnit,
    subFontSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isAtRisk = student.isAtRisk
    val borderColor = if (isAtRisk) Color(0xFFEF4444).copy(alpha = 0.5f)
                      else Color(0xFF50E3C2).copy(alpha = 0.5f)
    val avatarBg = if (isAtRisk) Color(0xFFEF4444).copy(alpha = 0.08f)
                   else gradeColor.copy(alpha = 0.08f)
    val avatarTextColor = if (isAtRisk) Color(0xFFEF4444) else gradeColor

    val initials = student.fullName.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(cardRadius),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (isTablet) 20.dp else 16.dp,
                    vertical = if (isTablet) 18.dp else 14.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Avatar ──
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .background(avatarBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    fontWeight = FontWeight.Black,
                    fontSize = if (isTablet) 18.sp else 15.sp,
                    color = avatarTextColor
                )
            }

            Spacer(Modifier.width(if (isTablet) 16.dp else 12.dp))

            // ── Text ──
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    student.fullName,
                    fontSize = nameFontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    student.section,
                    fontSize = subFontSize,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8),
                    maxLines = 1
                )
                student.lastActive?.let {
                    Text(
                        "Active · ${it.take(10)}",
                        fontSize = if (isTablet) 11.sp else 10.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // ── Status / Chevron ──
            if (isAtRisk) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.08f)
                ) {
                    Text(
                        "AT RISK",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = Color(0xFFEF4444),
                        fontSize = if (isTablet) 11.sp else 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            } else {
                Icon(
                    Icons.Default.ChevronRight, null,
                    tint = Color(0xFFCBD5E1),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ── Add Student Dialog ────────────────────────────────────────────────────────
@Composable
fun AddStudentDialog(
    defaultSection: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    val section = defaultSection

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Text(
                "Add Student",
                fontWeight = FontWeight.Black,
                fontFamily = fredokaFontFamily,
                fontSize = 22.sp,
                color = Color(0xFF1E293B)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    placeholder = { Text("e.g. Juan dela Cruz") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = section,
                    onValueChange = {},
                    label = { Text("Section") },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
                Text(
                    "Student will be enrolled in this section.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (fullName.isNotBlank()) onConfirm(fullName, section) },
                enabled = fullName.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.height(48.dp)
            ) {
                Text("Add Student", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        }
    )
}
