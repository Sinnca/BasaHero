package com.basahero.elearning

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.basahero.elearning.data.local.AppDatabase
import com.basahero.elearning.data.local.DatabaseSeeder
import com.basahero.elearning.data.repository.LessonRepository
import com.basahero.elearning.data.repository.StudentRepository
import com.basahero.elearning.ui.student.login.StudentLoginScreen
import com.basahero.elearning.ui.student.login.StudentLoginViewModel
import com.basahero.elearning.ui.student.login.StudentGradeSelectScreen
import com.basahero.elearning.ui.student.login.StudentNameSelectScreen
import com.basahero.elearning.ui.student.login.StudentSelectionViewModel
import com.basahero.elearning.ui.student.home.StudentHomeScreen
import com.basahero.elearning.ui.student.home.StudentHomeViewModel

import com.basahero.elearning.ui.student.lessons.LessonListScreen
import com.basahero.elearning.ui.student.lessons.LessonListViewModel
import com.basahero.elearning.ui.student.lessons.ReadingScreen
import com.basahero.elearning.ui.student.lessons.ReadingViewModel
import com.basahero.elearning.ui.student.quiz.QuizScreen
import com.basahero.elearning.ui.student.quiz.QuizViewModel
import com.basahero.elearning.ui.theme.PhilIRITheme
import com.basahero.elearning.data.local.SessionManager
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

// 👇 PHASE 3B REPOSITORY & SCREEN IMPORTS
import com.basahero.elearning.data.repository.PrePostRepository
import com.basahero.elearning.ui.student.prepost.PreTestGateScreen
import com.basahero.elearning.ui.student.prepost.PostTestScreen
import com.basahero.elearning.ui.student.prepost.PrePostViewModel
import com.basahero.elearning.domain.QuizScoringUseCase

import com.basahero.elearning.ui.teacher.auth.TeacherLoginScreen
import com.basahero.elearning.ui.teacher.auth.TeacherLoginViewModel
import com.basahero.elearning.ui.teacher.dashboard.TeacherDashboardScreen
import com.basahero.elearning.ui.teacher.dashboard.TeacherDashboardViewModel
import com.basahero.elearning.ui.teacher.roster.ClassRosterScreen
import com.basahero.elearning.ui.teacher.roster.ClassRosterViewModel

import com.basahero.elearning.ui.teacher.progress.StudentProgressScreen
import com.basahero.elearning.ui.teacher.progress.StudentProgressViewModel
import com.basahero.elearning.ui.teacher.analytics.ClassAnalyticsScreen
import com.basahero.elearning.ui.teacher.analytics.ClassAnalyticsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // PhilIRITheme is applied inside PhilIRIApp where gradeLevel is available
            PhilIRIApp()
        }
    }
}

object Routes {
    const val ROLE_SELECT = "role_select"
    const val GRADE_SELECT = "grade_select"
    const val NAME_SELECT = "name_select/{gradeLevel}"
    const val STUDENT_LOGIN = "student_login"
    const val STUDENT_HOME = "student_home"
    const val QUARTER_MENU = "quarter_menu/{gradeLevel}"
    const val PRE_TEST = "pre_test/{quarterId}"
    const val POST_TEST = "post_test/{quarterId}"
    const val LESSON_LIST = "lesson_list/{quarterId}"
    const val READING = "reading/{lessonId}"
    const val QUIZ = "quiz/{lessonId}"
    const val QUIZ_RESULT = "quiz_result/{lessonId}/{score}/{total}"
    const val PRONUNCIATION = "pronunciation/{lessonId}"
    const val GAME_JOIN = "game_join"
    const val GAME_PLAY = "game_play/{sessionId}"
    const val GAME_RESULT = "game_result/{sessionId}"
    const val TEACHER_LOGIN = "teacher_login"
    const val TEACHER_DASHBOARD = "teacher_dashboard"
    const val CLASS_ROSTER = "class_roster/{classId}"
    const val STUDENT_PROGRESS = "student_progress/{studentId}/{studentName}"
    const val CLASS_ANALYTICS   = "class_analytics/{classId}/{className}"
    const val GAME_HOST = "game_host/{classId}"
}

@Composable
fun PhilIRIApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val database = remember { AppDatabase.getInstance(context, DatabaseSeeder(context)) }
    val studentRepository = remember { StudentRepository(database) }
    val lessonRepository = remember { LessonRepository(database) }
    val quizRepository = remember { com.basahero.elearning.data.repository.QuizRepository(database) }
    val progressRepository = remember { com.basahero.elearning.data.repository.ProgressRepository(database) }
    val pronunciationRepository = remember { com.basahero.elearning.data.repository.PronunciationRepository(database) }
    val teacherAuthRepository = remember { com.basahero.elearning.data.repository.TeacherAuthRepository() }
    val classRepository = remember { com.basahero.elearning.data.repository.ClassRepository() }

    // Shared repositories
    val prePostRepository = remember { PrePostRepository(database) }
    val scoringUseCase = remember { QuizScoringUseCase() }

    val sessionManager = remember { SessionManager(context) }
    val currentSession by sessionManager.studentSession.collectAsState(initial = null)

    // Derive grade level from active session (default 4 = blue before login)
    val gradeLevel = currentSession?.gradeLevel ?: 4

    val isTeacherLoggedIn by sessionManager.isTeacherLoggedIn.collectAsState(initial = false)

    LaunchedEffect(currentSession, isTeacherLoggedIn) {
        if (currentSession != null) {
            navController.navigate(Routes.STUDENT_HOME) {
                popUpTo(Routes.ROLE_SELECT) { inclusive = true }
            }
        } else if (isTeacherLoggedIn) {
            navController.navigate(Routes.TEACHER_DASHBOARD) {
                popUpTo(Routes.ROLE_SELECT) { inclusive = true }
            }
        }
    }

    // ── Apply the grade-level colour scheme to the entire app ─────────────
    PhilIRITheme(gradeLevel = gradeLevel) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {

    NavHost(
        navController = navController,
        startDestination = Routes.ROLE_SELECT
    ) {
        composable(Routes.ROLE_SELECT) {
            RoleSelectScreen(
                onStudentClick = { navController.navigate(Routes.GRADE_SELECT) },
                onTeacherClick = { navController.navigate(Routes.TEACHER_LOGIN) }
            )
        }

        composable(Routes.GRADE_SELECT) {
            StudentGradeSelectScreen(
                onGradeSelected = { grade ->
                    navController.navigate("name_select/$grade")
                },
                onManualLoginClick = { navController.navigate(Routes.STUDENT_LOGIN) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.NAME_SELECT) { backStackEntry ->
            val gradeStr = backStackEntry.arguments?.getString("gradeLevel") ?: "4"
            val gradeLevel = gradeStr.toIntOrNull() ?: 4
            
            val viewModel: StudentSelectionViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return StudentSelectionViewModel(studentRepository) as T
                    }
                }
            )

            StudentNameSelectScreen(
                gradeLevel = gradeLevel,
                viewModel = viewModel,
                onStudentSelected = { student ->
                    // Set session and navigate home
                    coroutineScope.launch {
                        sessionManager.saveStudentSession(
                            studentId = student.id,
                            studentName = student.fullName,
                            gradeLevel = student.gradeLevel,
                            section = student.section
                        )
                        navController.navigate(Routes.STUDENT_HOME) {
                            popUpTo(Routes.ROLE_SELECT) { inclusive = true }
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.STUDENT_LOGIN) {
            val viewModel: StudentLoginViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return StudentLoginViewModel(studentRepository, sessionManager) as T
                    }
                }
            )

            StudentLoginScreen(
                viewModel = viewModel,
                onLoginSuccess = { navController.navigate(Routes.STUDENT_HOME) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.STUDENT_HOME) {
            val currentStudentId = currentSession?.studentId ?: ""

            val viewModel: StudentHomeViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return StudentHomeViewModel(studentRepository, lessonRepository) as T
                    }
                }
            )

            StudentHomeScreen(
                studentId = currentStudentId,
                viewModel = viewModel,
                onQuarterClick = { quarterId, _ ->
                    // ✅ FIXED: Now goes straight to the Lesson List instead of popping up the pre-test!
                    navController.navigate("lesson_list/$quarterId")
                },
                onLogout = {
                    coroutineScope.launch { sessionManager.clearStudentSession() }
                    navController.navigate(Routes.ROLE_SELECT) { popUpTo(0) }
                }
            )
        }

        composable(Routes.LESSON_LIST) { backStackEntry ->
            val quarterId = backStackEntry.arguments?.getString("quarterId") ?: ""
            val currentStudentId = currentSession?.studentId ?: ""

            val viewModel: LessonListViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        // ✅ Pass both repos to handle Pre/Post logic in the list
                        return LessonListViewModel(lessonRepository, prePostRepository) as T
                    }
                }
            )

            LessonListScreen(
                quarterId = quarterId,
                studentId = currentStudentId,
                quarterTitle = "Lessons",
                viewModel = viewModel,
                onLessonClick = { id -> navController.navigate("reading/$id") },
                // ✅ Pass the navigation actions for the Pre/Post cards
                onPreTestClick = { navController.navigate("pre_test/$quarterId") },
                onPostTestClick = { navController.navigate("post_test/$quarterId") },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PRE_TEST) { backStackEntry ->
            val quarterId = backStackEntry.arguments?.getString("quarterId") ?: ""
            val currentStudentId = currentSession?.studentId ?: ""

            val viewModel: PrePostViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return PrePostViewModel(prePostRepository, scoringUseCase) as T
                    }
                }
            )

            PreTestGateScreen(
                quarterId = quarterId,
                quarterTitle = "Quarter Introduction",
                studentId = currentStudentId,
                viewModel = viewModel,
                onPreTestComplete = {
                    // ✅ Popping backstack returns student to the list where lessons are now UNLOCKED
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.POST_TEST) { backStackEntry ->
            val quarterId = backStackEntry.arguments?.getString("quarterId") ?: ""
            val currentStudentId = currentSession?.studentId ?: ""

            val viewModel: PrePostViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return PrePostViewModel(prePostRepository, scoringUseCase) as T
                    }
                }
            )

            PostTestScreen(
                quarterId = quarterId,
                quarterTitle = "Quarter Review",
                studentId = currentStudentId,
                viewModel = viewModel,
                onPostTestComplete = {
                    // ✅ Returns student to the list
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.READING) { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
            val viewModel: ReadingViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return ReadingViewModel(lessonRepository, pronunciationRepository, sessionManager) as T
                    }
                }
            )
            ReadingScreen(lessonId = lessonId, viewModel = viewModel, onStartQuiz = { id -> navController.navigate("quiz/$id") }, onBack = { navController.popBackStack() })
        }

        composable(Routes.QUIZ) { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
            val viewModel: QuizViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return QuizViewModel(quizRepository, scoringUseCase) as T
                    }
                }
            )
            QuizScreen(lessonId = lessonId, lessonTitle = "Lesson Quiz", viewModel = viewModel, onQuizComplete = { result -> navController.navigate("quiz_result/$lessonId/${result.score}/${result.total}") { popUpTo(Routes.LESSON_LIST) { inclusive = false } } }, onBack = { navController.popBackStack() })
        }

        composable(Routes.QUIZ_RESULT) { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
            val score = backStackEntry.arguments?.getString("score")?.toIntOrNull() ?: 0
            val total = backStackEntry.arguments?.getString("total")?.toIntOrNull() ?: 0
            val currentStudentId = currentSession?.studentId ?: ""
            val viewModel: com.basahero.elearning.ui.student.quiz.QuizResultViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return com.basahero.elearning.ui.student.quiz.QuizResultViewModel(progressRepository, lessonRepository) as T
                    }
                }
            )
            com.basahero.elearning.ui.student.quiz.QuizResultScreen(studentId = currentStudentId, lessonId = lessonId, score = score, total = total, viewModel = viewModel, onGoHome = { navController.popBackStack(Routes.LESSON_LIST, inclusive = false) }, onNextLesson = { nextId -> navController.navigate("reading/$nextId") { popUpTo(Routes.LESSON_LIST) { inclusive = false } } }, onRetry = { navController.navigate("quiz/$lessonId") { popUpTo(Routes.LESSON_LIST) { inclusive = false } } })
        }

        // Placeholders
        composable(Routes.QUARTER_MENU) { PlaceholderScreen("Quarter Menu") }
        composable(Routes.PRONUNCIATION) { PlaceholderScreen("Pronunciation Checker") }
        composable(Routes.GAME_JOIN) { PlaceholderScreen("Join Game") }
        composable(Routes.GAME_PLAY) { PlaceholderScreen("Game Play") }
        composable(Routes.GAME_RESULT) { PlaceholderScreen("Game Result") }
        composable(Routes.TEACHER_LOGIN) {
            val viewModel: TeacherLoginViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return TeacherLoginViewModel(teacherAuthRepository, sessionManager) as T
                    }
                }
            )
            TeacherLoginScreen(
                viewModel = viewModel,
                onLoginSuccess = { navController.navigate(Routes.TEACHER_DASHBOARD) { popUpTo(Routes.ROLE_SELECT) { inclusive = true } } },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.TEACHER_DASHBOARD) {
            val viewModel: TeacherDashboardViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return TeacherDashboardViewModel(teacherAuthRepository, classRepository) as T
                    }
                }
            )
            TeacherDashboardScreen(
                viewModel = viewModel,
                onClassClick = { classId, className, gradeLevel ->
                    navController.navigate("class_roster/$classId/$className/$gradeLevel")
                },
                onLogout = {
                    coroutineScope.launch { sessionManager.setTeacherLoggedIn(false) }
                    navController.navigate(Routes.ROLE_SELECT) { popUpTo(0) }
                }
            )
        }

        composable("class_roster/{classId}/{className}/{gradeLevel}") { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            val className = backStackEntry.arguments?.getString("className") ?: ""
            val gradeLevel = backStackEntry.arguments?.getString("gradeLevel")?.toIntOrNull() ?: 4
            
            val viewModel: ClassRosterViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return ClassRosterViewModel(classRepository) as T
                    }
                }
            )
            ClassRosterScreen(
                classId = classId,
                className = className,
                gradeLevel = gradeLevel,
                viewModel = viewModel,
                onStudentClick = { studentId, studentName ->
                    navController.navigate("student_progress/$studentId/$studentName")
                },
                onAnalyticsClick = {
                    navController.navigate("class_analytics/$classId/$className")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.STUDENT_PROGRESS) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
            val studentName = backStackEntry.arguments?.getString("studentName") ?: ""
            
            val viewModel: StudentProgressViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        val repo = com.basahero.elearning.data.repository.ProgressMonitorRepository()
                        return StudentProgressViewModel(repo) as T
                    }
                }
            )

            StudentProgressScreen(
                studentId = studentId,
                studentName = studentName,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CLASS_ANALYTICS) { backStackEntry ->
            val classId   = backStackEntry.arguments?.getString("classId")   ?: ""
            val className = backStackEntry.arguments?.getString("className")  ?: ""

            val viewModel: ClassAnalyticsViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        val repo = com.basahero.elearning.data.repository.ProgressMonitorRepository()
                        return ClassAnalyticsViewModel(repo, lessonRepository) as T
                    }
                }
            )

            ClassAnalyticsScreen(
                classId   = classId,
                className = className,
                viewModel = viewModel,
                onStudentClick = { studentId, studentName ->
                    navController.navigate("student_progress/$studentId/$studentName")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.GAME_HOST) { PlaceholderScreen("Game Host") }
    }           // end NavHost
    }           // end Surface
    }           // end PhilIRITheme
}

@Composable
fun RoleSelectScreen(onStudentClick: () -> Unit, onTeacherClick: () -> Unit) {
    // ── Animated entrance ───────────────────────────────────────────────────
    val alpha = remember { androidx.compose.animation.core.Animatable(0f) }
    val slideY = remember { androidx.compose.animation.core.Animatable(40f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.coroutineScope {
            launch { alpha.animateTo(1f, androidx.compose.animation.core.tween(700)) }
            launch { slideY.animateTo(0f, androidx.compose.animation.core.tween(700, easing = androidx.compose.animation.core.EaseOutCubic)) }
        }
    }

    // ── Deep-blue gradient background ───────────────────────────────────────
    Box(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(
                        androidx.compose.ui.graphics.Color(0xFF1A56C4),
                        androidx.compose.ui.graphics.Color(0xFF1340A0),
                        androidx.compose.ui.graphics.Color(0xFF0D2F82)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .graphicsLayer {
                    this.alpha = alpha.value
                    translationY = slideY.value
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Stacked-books logo (drawn with Canvas) ──────────────────────
            androidx.compose.foundation.Canvas(
                modifier = androidx.compose.ui.Modifier.size(80.dp)
            ) {
                val w = size.width
                val h = size.height
                val bookH = h * 0.28f
                val bookW = w * 0.72f
                val gap = h * 0.06f
                val cornerR = 10f

                // Back book — blue
                drawRoundRect(
                    color = androidx.compose.ui.graphics.Color(0xFF1565C0),
                    topLeft = androidx.compose.ui.geometry.Offset((w - bookW) / 2f + 12f, h * 0.12f),
                    size = androidx.compose.ui.geometry.Size(bookW, bookH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR)
                )
                // Middle book — red
                drawRoundRect(
                    color = androidx.compose.ui.graphics.Color(0xFFE53935),
                    topLeft = androidx.compose.ui.geometry.Offset((w - bookW) / 2f + 4f, h * 0.12f + bookH + gap),
                    size = androidx.compose.ui.geometry.Size(bookW, bookH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR)
                )
                // Front book — green
                drawRoundRect(
                    color = androidx.compose.ui.graphics.Color(0xFF43A047),
                    topLeft = androidx.compose.ui.geometry.Offset((w - bookW) / 2f, h * 0.12f + (bookH + gap) * 2f),
                    size = androidx.compose.ui.geometry.Size(bookW, bookH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR)
                )
            }

            Spacer(androidx.compose.ui.Modifier.height(20.dp))

            // ── App title ───────────────────────────────────────────────────
            Text(
                text = "BASAhero",
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.White,
                letterSpacing = 0.5.sp
            )

            Spacer(androidx.compose.ui.Modifier.height(6.dp))

            Text(
                text = "MATATAG English · Grade 4–6",
                fontSize = 14.sp,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.75f),
                letterSpacing = 0.3.sp
            )

            Spacer(androidx.compose.ui.Modifier.height(56.dp))

            // ── "Who are you?" prompt ───────────────────────────────────────
            Text(
                text = "Who are you?",
                fontSize = 14.sp,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.65f)
            )

            Spacer(androidx.compose.ui.Modifier.height(16.dp))

            // ── Student button ──────────────────────────────────────────────
            Button(
                onClick = onStudentClick,
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color.White,
                    contentColor = androidx.compose.ui.graphics.Color(0xFF1340A0)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = androidx.compose.ui.Modifier.size(20.dp)
                )
                Spacer(androidx.compose.ui.Modifier.width(10.dp))
                Text(
                    text = "I am a Student",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(androidx.compose.ui.Modifier.height(12.dp))

            // ── Teacher button ──────────────────────────────────────────────
            Button(
                onClick = onTeacherClick,
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.15f),
                    contentColor = androidx.compose.ui.graphics.Color.White
                ),
                border = BorderStroke(1.5.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.4f)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    modifier = androidx.compose.ui.Modifier.size(20.dp)
                )
                Spacer(androidx.compose.ui.Modifier.width(10.dp))
                Text(
                    text = "I am a Teacher",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(androidx.compose.ui.Modifier.height(40.dp))

            // ── Grade footer ────────────────────────────────────────────────
            Text(
                text = "Grade 4  ·  Grade 5  ·  Grade 6",
                fontSize = 12.sp,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.45f),
                letterSpacing = 0.5.sp
            )
        }
    }
}


@Composable
fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, style = MaterialTheme.typography.headlineLarge)
    }
}