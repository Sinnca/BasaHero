package com.basahero.elearning.ui.student.home

import android.annotation.SuppressLint
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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
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

@SuppressLint("Range")
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

    val totalLessons = uiState.quarters.sumOf { it.totalLessons }
    val completedLessons = uiState.quarters.sumOf { it.completedLessons }
    val greetingTime = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY).let { hour ->
        when {
            hour < 12 -> if (languageCode == "fil") "Magandang umaga," else "Good morning,"
            hour < 18 -> if (languageCode == "fil") "Magandang hapon," else "Good afternoon,"
            else -> if (languageCode == "fil") "Magandang gabi," else "Good evening,"
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val darkColor = MaterialTheme.colorScheme.onPrimaryContainer

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            StudentBottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    if (tab == 2) onJoinGameClick()
                    else selectedTab = tab
                }
            )
        }
    ) { padding ->
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

        if (selectedTab == 1) {
            QuartersListContent(
                quarters = uiState.quarters,
                studentGradeLevel = student.gradeLevel,
                isTablet = isTablet,
                onQuarterClick = onQuarterClick,
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            // Background Mascot Image at the very top (takes up ~35% of height)
            // TODO: Picture Holder for Header - Replace 'R.drawable.header_mascot' with your new picture
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.basahero.elearning.R.drawable.header_mascot),
                contentDescription = "Header Mascot",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                alignment = BiasAlignment(0f, -0.7f), // Slightly above center focus
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.35f)
            )

            // Main Content Card (Holding Profile, Play, and Progress)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.65f) // Reduced to 65% to avoid overlapping the 35% mascot area
                    .align(Alignment.BottomCenter),
                shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
                colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.08f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = if (isTablet) 32.dp else 16.dp, vertical = if (isTablet) 20.dp else 12.dp),
                    verticalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 10.dp)
                ) {
                    // User Info Card (At the top of the stack)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(if (isTablet) 24.dp else 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isTablet) 72.dp else 48.dp)
                                    .background(primaryColor.copy(alpha = 0.2f), CircleShape)
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile Picture Holder",
                                    tint = primaryColor,
                                    modifier = Modifier.fillMaxSize(0.8f)
                                )
                            }
                            
                            Spacer(Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "👋 ${if (languageCode == "fil") "Kamusta" else "Hi"}, ${student.fullName.split(" ").first()}",
                                    fontSize = if (isTablet) 26.sp else 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor // Themed greeting
                                )
                                Text(
                                    text = if (languageCode == "fil") "Baitang ${student.gradeLevel} Student" else "Grade ${student.gradeLevel} Student",
                                    fontSize = if (isTablet) 18.sp else 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                    
                    Text(
                        text = if (languageCode == "fil") "Tayo'y Maglaro!" else "Let's Play!",
                        fontSize = if (isTablet) 28.sp else 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryColor // Themed section header
                    )

                    // Let's Play Card (Orange)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f), // Flexible height
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFAB40)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Image as background layer
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.basahero.elearning.R.drawable.kids),
                                contentDescription = "Kids Play",
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxHeight(if (isTablet) 1f else 1.1f) // Shrunk for mobile
                                    .wrapContentWidth()
                                    .align(Alignment.CenterStart)
                                    .padding(start = if (isTablet) 30.dp else 10.dp, top = 8.dp, bottom = 8.dp)
                            )
                            
                            // Mission text + Play now button grouped together, moved right
                            Row(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(start = if (isTablet) 240.dp else 140.dp), // Set to 140dp for mobile as requested
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(if (isTablet) 32.dp else 4.dp)
                            ) {
                                // Text on the Left of the group
                                Column(
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        text = if (languageCode == "fil") "Simulan ang\nMisyon!" else "Mission\nStart!",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = if (isTablet) 45.sp else 22.sp,
                                        lineHeight = if (isTablet) 46.sp else 24.sp,
                                        textAlign = TextAlign.Start
                                    )
                                    Text(
                                        text = if (languageCode == "fil") "Maglaro at matuto!" else "Play and learn!",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isTablet) 20.sp else 12.sp,
                                        textAlign = TextAlign.Start
                                    )
                                }

                                // Button on the Right of the group
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFF2196F3))
                                        .clickable { onJoinGameClick() }
                                        .padding(horizontal = if (isTablet) 32.dp else 12.dp, vertical = if (isTablet) 12.dp else 6.dp)
                                ) {
                                    Text(
                                        text = if (languageCode == "fil") "Maglaro na" else "Play now",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = if (isTablet) 18.sp else 12.sp
                                    )
                                }
                            }
                        }
                    }
                    
                    // Progress and Stats Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.2f), // Reduced from 1.5f to fit in 65% height
                        horizontalArrangement = Arrangement.spacedBy(if (isTablet) 24.dp else 16.dp)
                    ) {
                        // Current Progress Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE53935)), // Red
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(if (isTablet) 24.dp else 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (languageCode == "fil") "Progreso" else "Progress",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = if (isTablet) 24.sp else 16.sp,
                                    textAlign = TextAlign.Center
                                )
                                
                                // Progress Ring and Percentage side-by-side
                                Row(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        ProgressRing(
                                            progress = uiState.overallProgress,
                                            size = if (isTablet) 90.dp else 70.dp,
                                            strokeWidth = if (isTablet) 8.dp else 6.dp,
                                            color = Color(0xFFFFD700),
                                            trackColor = Color.White.copy(alpha = 0.2f)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = "Trophy Progress",
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(if (isTablet) 45.dp else 30.dp)
                                        )
                                    }
                                    
                                    Spacer(Modifier.width(if (isTablet) 16.dp else 12.dp))
                                    
                                    Text(
                                        text = "${(uiState.overallProgress * 100).toInt()}%",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = if (isTablet) 32.sp else 22.sp
                                    )
                                }
                                if (!isTablet) Spacer(Modifier.height(10.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(if (isTablet) 1f else 0.75f) // Even smaller width on mobile
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .clickable { 
                                            // Find first active quarter to continue
                                            val activeQ = uiState.quarters.firstOrNull { it.isActive && it.progressPercent < 1f }
                                            if (activeQ != null) {
                                                onQuarterClick(activeQ.id, student.gradeLevel)
                                            } else {
                                                selectedTab = 1 // Go to quarters tab if done
                                            }
                                        }
                                        .padding(vertical = if (isTablet) 14.dp else 4.dp), // Thinner on mobile
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (languageCode == "fil") "Ipagpatuloy" else "Continue",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = if (isTablet) 20.sp else 11.sp
                                    )
                                }
                            }
                        }

                        // Quick Stats Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF673AB7)), // Purple
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(if (isTablet) 24.dp else 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (languageCode == "fil") "Istatistika" else "Stats",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = if (isTablet) 24.sp else 16.sp,
                                    textAlign = TextAlign.Center
                                )
                                
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                                    // TODO: Picture Holder for Stats - Replace 'R.drawable.pie' with your new picture
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = com.basahero.elearning.R.drawable.pie),
                                        contentDescription = "Quick Stats",
                                        modifier = Modifier.size(if (isTablet) 100.dp else 60.dp),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                    )
                                }
                                
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$completedLessons/$totalLessons",
                                        color = Color(0xFFFFD700),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = if (isTablet) 30.sp else 20.sp
                                    )
                                    Text(
                                        text = if (languageCode == "fil") "Aralin tapos" else "Lessons done",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isTablet) 18.sp else 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBadge(
    icon: ImageVector,
    iconColor: Color,
    value: String,
    label: String,
    isTablet: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.height(if (isTablet) 130.dp else 105.dp)
    ) {
        // Bottom shadow (3D edge)
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = 4.dp)
                .background(Color.Black.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
        )
        // Main front face
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(bottom = 4.dp)
                .background(Color.White.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                .padding(horizontal = 4.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(if (isTablet) 32.dp else 28.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = value,
                    fontSize = if (isTablet) 24.sp else 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = label,
                    fontSize = if (isTablet) 12.sp else 11.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun QuartersListContent(
    quarters: List<Quarter>,
    studentGradeLevel: Int,
    isTablet: Boolean,
    onQuarterClick: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val darkColor = Color(primaryColor.red * 0.6f, primaryColor.green * 0.6f, primaryColor.blue * 0.8f)

    val totalLessons = quarters.take(3).sumOf { it.totalLessons }
    val completedLessons = quarters.take(3).sumOf { it.completedLessons }
    val progressPercent = if (totalLessons == 0) 0f else completedLessons.toFloat() / totalLessons

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            listOf(primaryColor.copy(alpha = 0.85f), primaryColor, darkColor),
                            radius = 1200f
                        )
                    )
            )
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(Color.White.copy(0.06f), size.width * 0.35f, androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.1f))
                drawCircle(Color.White.copy(0.04f), size.width * 0.2f, androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.8f))
            }

            Column(
                modifier = Modifier.padding(
                    top = if (isTablet) 24.dp else 12.dp,
                    bottom = if (isTablet) 24.dp else 12.dp,
                    start = if (isTablet) 32.dp else 20.dp,
                    end = if (isTablet) 32.dp else 20.dp
                )
            ) {
                // Top Row: My Quarters & Subtitle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("\uD83D\uDCDA", fontSize = if (isTablet) 40.sp else 28.sp)
                    Spacer(Modifier.width(if (isTablet) 16.dp else 12.dp))
                    Column {
                        Text(
                            text = strings.myQuarters,
                            fontSize = if (isTablet) 32.sp else 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "Adventure through Grade $studentGradeLevel",
                            fontSize = if (isTablet) 18.sp else 13.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(if (isTablet) 24.dp else 12.dp))

                // Glass Panel - Reduced for Mobile
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(0.15f), RoundedCornerShape(20.dp))
                        .padding(if (isTablet) 24.dp else 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Progress Ring - Smaller on Mobile
                        Box(contentAlignment = Alignment.Center) {
                            ProgressRing(
                                progress = progressPercent,
                                size = if (isTablet) 80.dp else 54.dp,
                                strokeWidth = if (isTablet) 10.dp else 7.dp,
                                color = Color.White,
                                trackColor = Color.White.copy(0.2f)
                            )
                            Text(
                                text = "${(progressPercent * 100).toInt()}%",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = if (isTablet) 20.sp else 13.sp
                            )
                        }

                        Spacer(Modifier.width(if (isTablet) 20.dp else 12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("\u2B50", fontSize = if (isTablet) 14.sp else 12.sp)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Start your adventure!",
                                    fontSize = if (isTablet) 16.sp else 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Progress",
                                    fontSize = if (isTablet) 14.sp else 11.sp,
                                    color = Color.White.copy(0.85f),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$completedLessons / $totalLessons",
                                    fontSize = if (isTablet) 14.sp else 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Spacer(Modifier.height(if (isTablet) 8.dp else 4.dp))

                            // Thin Yellow Progress Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (isTablet) 8.dp else 5.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(0.2f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressPercent)
                                        .fillMaxHeight()
                                        .background(Color(0xFFFFD700)) // Yellow
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (isTablet) 32.dp else 16.dp,
                    vertical = if (isTablet) 20.dp else 12.dp
                ),
            verticalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp)
        ) {
            val quartersToShow = quarters.take(4)
            val rows = quartersToShow.chunked(2)
            
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp)
                ) {
                    rowItems.forEachIndexed { rowIndex, quarter ->
                        val globalIndex = quartersToShow.indexOf(quarter)
                        QuarterCard(
                            quarter = quarter,
                            studentGradeLevel = studentGradeLevel,
                            isTablet = isTablet,
                            index = globalIndex,
                            modifier = Modifier.weight(1f),
                            onClick = { onQuarterClick(quarter.id, studentGradeLevel) }
                        )
                    }
                    // Fill row if only 1 item
                    if (rowItems.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun QuarterCard(
    quarter: Quarter,
    studentGradeLevel: Int,
    isTablet: Boolean,
    index: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val isLocked = !quarter.isActive
    
    val imageRes = if (studentGradeLevel == 4 && index == 0) {
        com.basahero.elearning.R.drawable.grade4_q1
    } else null

    val themeGradient = when (index) {
        0 -> Brush.verticalGradient(listOf(Color(0xFFBAE6FD), Color(0xFF7DD3FC)))
        1 -> Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0EA5E9)))
        2 -> Brush.verticalGradient(listOf(Color(0xFF0284C7), Color(0xFF0369A1)))
        else -> Brush.verticalGradient(listOf(Color(0xFF60A5FA), Color(0xFF2563EB)))
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isLocked) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "card_scale"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { if (!isLocked) onClick() }
            ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Picture Holder (Top)
            Box(
                modifier = Modifier
                    .weight(if (isTablet) 1.8f else 1.5f)
                    .fillMaxWidth()
                    .then(
                        if (isLocked) Modifier.background(Color(0xFFE2E8F0))
                        else Modifier.background(themeGradient)
                    )
            ) {
                if (imageRes != null && !isLocked) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = imageRes),
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Image,
                        contentDescription = null,
                        tint = if (isLocked) Color(0xFF94A3B8) else Color.White.copy(0.4f),
                        modifier = Modifier.size(if (isTablet) 48.dp else 32.dp).align(Alignment.Center)
                    )
                }
            }

            // 2. Info Area (Bottom)
            Box(
                modifier = Modifier
                    .weight(if (isTablet) 1f else 1.3f)
                    .fillMaxWidth()
                    .padding(if (isTablet) 16.dp else 10.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(if (isTablet) 8.dp else 4.dp)
                ) {
                    // Title and Tag Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = quarter.title,
                            fontSize = if (isTablet) 22.sp else 15.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isLocked) Color(0xFF94A3B8) else Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Box(
                            modifier = Modifier
                                .background(primaryColor.copy(0.1f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Q${index + 1}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = primaryColor
                            )
                        }
                    }

                    // Bottom Section: Progress on Left, Play on Right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${quarter.completedLessons}/${quarter.totalLessons} LESSONS",
                                fontSize = if (isTablet) 12.sp else 9.sp,
                                fontWeight = FontWeight.Black,
                                color = primaryColor.copy(0.8f)
                            )
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${(quarter.progressPercent * 100).toInt()}%",
                                    fontSize = if (isTablet) 18.sp else 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isLocked) Color(0xFF94A3B8) else primaryColor
                                )
                            }
                            
                            if (isTablet) {
                                Spacer(Modifier.height(2.dp))
                                
                                // Progress Bar - Tablet Only
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .height(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF1F5F9))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(quarter.progressPercent)
                                            .fillMaxHeight()
                                            .then(
                                                if (isLocked) Modifier.background(Color(0xFFCBD5E1))
                                                else Modifier.background(Brush.horizontalGradient(listOf(primaryColor.copy(0.8f), primaryColor)))
                                            )
                                    )
                                }
                            }
                        }
                        
                        // Optimized Play Button
                        Box(
                            modifier = Modifier
                                .size(if (isTablet) 44.dp else 40.dp)
                                .shadow(if (isLocked) 0.dp else 4.dp, CircleShape, spotColor = Color(0xFF00C4CC))
                                .clip(CircleShape)
                                .background(if (isLocked) Color(0xFFE2E8F0) else Color(0xFF00C4CC)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(if (isTablet) 28.dp else 22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    student: com.basahero.elearning.data.model.Student,
    languageCode: String,
    onToggleLanguage: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(primaryColor, primaryColor.copy(alpha = 0.7f)))
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
                    tint = primaryColor,
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
                    color = primaryColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = if (languageCode == "en") "EN" else "FIL",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

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
                    Icons.AutoMirrored.Filled.ExitToApp,
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
    }
}

@Composable
private fun QuarterStatChip(label: String) {
    Row(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 0.5.sp
        )
    }
}


