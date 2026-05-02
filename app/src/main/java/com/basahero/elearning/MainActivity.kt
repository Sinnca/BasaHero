//package com.basahero.elearning
//
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.compose.NavHost
//import androidx.navigation.compose.composable
//import androidx.navigation.compose.rememberNavController
//
//import com.basahero.elearning.data.local.AppDatabase
//import com.basahero.elearning.data.local.DatabaseSeeder
//import com.basahero.elearning.data.repository.LessonRepository
//import com.basahero.elearning.data.repository.StudentRepository
//import com.basahero.elearning.ui.student.login.StudentLoginScreen
//import com.basahero.elearning.ui.student.login.StudentLoginViewModel
//import com.basahero.elearning.ui.student.home.StudentHomeScreen
//import com.basahero.elearning.ui.student.home.StudentHomeViewModel
//
//// 👇 NEW IMPORTS FOR LESSON LIST
//import com.basahero.elearning.ui.student.lessons.LessonListScreen
//import com.basahero.elearning.ui.student.lessons.LessonListViewModel
//import com.basahero.elearning.ui.student.lessons.ReadingScreen
//import com.basahero.elearning.ui.student.lessons.ReadingViewModel
//import com.basahero.elearning.ui.student.quiz.QuizScreen
//import com.basahero.elearning.ui.student.quiz.QuizViewModel
//import com.basahero.elearning.ui.theme.BasaHeroTheme
//
//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        installSplashScreen()
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContent {
//            BasaHeroTheme {
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    PhilIRIApp()
//                }
//            }
//        }
//    }
//}
//
//object Routes {
//    const val ROLE_SELECT = "role_select"
//    const val STUDENT_LOGIN = "student_login"
//    const val STUDENT_HOME = "student_home"
//    const val QUARTER_MENU = "quarter_menu/{gradeLevel}"
//    const val LESSON_LIST = "lesson_list/{quarterId}"
//    const val READING = "reading/{lessonId}"
//    const val QUIZ = "quiz/{lessonId}"
//    const val QUIZ_RESULT = "quiz_result/{lessonId}/{score}/{total}"
//    const val PRONUNCIATION = "pronunciation/{lessonId}"
//    const val GAME_JOIN = "game_join"
//    const val GAME_PLAY = "game_play/{sessionId}"
//    const val GAME_RESULT = "game_result/{sessionId}"
//    const val TEACHER_LOGIN = "teacher_login"
//    const val TEACHER_DASHBOARD = "teacher_dashboard"
//    const val CLASS_ROSTER = "class_roster/{classId}"
//    const val STUDENT_PROGRESS = "student_progress/{studentId}"
//    const val GAME_HOST = "game_host/{classId}"
//}
//
//@Composable
//fun PhilIRIApp() {
//    val navController = rememberNavController()
//    val context = LocalContext.current
//
//    val database = remember { AppDatabase.getInstance(context, DatabaseSeeder(context)) }
//    val studentRepository = remember { StudentRepository(database) }
//    val lessonRepository = remember { LessonRepository(database) }
//
//    val quizRepository = remember { com.basahero.elearning.data.repository.QuizRepository(database) }
//    val progressRepository = remember { com.basahero.elearning.data.repository.ProgressRepository(database) }
//
//    var loggedInStudentId by remember { mutableStateOf<String?>(null) }
//
//    NavHost(
//        navController = navController,
//        startDestination = Routes.ROLE_SELECT
//    ) {
//        composable(Routes.ROLE_SELECT) {
//            RoleSelectScreen(
//                onStudentClick = { navController.navigate(Routes.STUDENT_LOGIN) },
//                onTeacherClick = { navController.navigate(Routes.TEACHER_LOGIN) }
//            )
//        }
//
//        composable(Routes.STUDENT_LOGIN) {
//            val viewModel: StudentLoginViewModel = viewModel(
//                factory = object : ViewModelProvider.Factory {
//                    @Suppress("UNCHECKED_CAST")
//                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//                        return StudentLoginViewModel(studentRepository) as T
//                    }
//                }
//            )
//
//            StudentLoginScreen(
//                viewModel = viewModel,
//                onLoginSuccess = { student ->
//                    loggedInStudentId = student.id
//                    navController.navigate(Routes.STUDENT_HOME) {
//                        popUpTo(Routes.ROLE_SELECT) { inclusive = false }
//                    }
//                },
//                onBack = { navController.popBackStack() }
//            )
//        }
//
//        composable(Routes.STUDENT_HOME) {
//            val currentStudentId = loggedInStudentId ?: ""
//            val viewModel: StudentHomeViewModel = viewModel(
//                factory = object : ViewModelProvider.Factory {
//                    @Suppress("UNCHECKED_CAST")
//                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//                        return StudentHomeViewModel(studentRepository, lessonRepository) as T
//                    }
//                }
//            )
//
//            StudentHomeScreen(
//                studentId = currentStudentId,
//                viewModel = viewModel,
//                onQuarterClick = { quarterId, _ ->
//                    navController.navigate("lesson_list/$quarterId")
//                },
//                onLogout = {
//                    loggedInStudentId = null
//                    navController.navigate(Routes.ROLE_SELECT) {
//                        popUpTo(0)
//                    }
//                }
//            )
//        }
//
//        // 🚀 WE REPLACED THE PLACEHOLDER WITH OUR REAL SCREEN!
//        composable(Routes.LESSON_LIST) { backStackEntry ->
//            val quarterId = backStackEntry.arguments?.getString("quarterId") ?: ""
//            val currentStudentId = loggedInStudentId ?: ""
//
//            val viewModel: LessonListViewModel = viewModel(
//                factory = object : ViewModelProvider.Factory {
//                    @Suppress("UNCHECKED_CAST")
//                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//                        return LessonListViewModel(lessonRepository) as T
//                    }
//                }
//            )
//
//            LessonListScreen(
//                quarterId = quarterId,
//                studentId = currentStudentId,
//                quarterTitle = "Lessons",
//                viewModel = viewModel,
//                onLessonClick = { lessonId ->
//                    navController.navigate("reading/$lessonId")
//                },
//                onBack = { navController.popBackStack() }
//            )
//        }
//
//        // 🚀 The Reading Screen
//        composable(Routes.READING) { backStackEntry ->
//            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
//
//            val viewModel: ReadingViewModel = viewModel(
//                factory = object : ViewModelProvider.Factory {
//                    @Suppress("UNCHECKED_CAST")
//                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//                        return ReadingViewModel(lessonRepository) as T
//                    }
//                }
//            )
//
//            ReadingScreen(
//                lessonId = lessonId,
//                viewModel = viewModel,
//                onStartQuiz = { id ->
//                    navController.navigate("quiz/$id")
//                },
//                onBack = { navController.popBackStack() }
//            )
//        }
//
//        // 🚀 The Quiz Screen
//        composable(Routes.QUIZ) { backStackEntry ->
//            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
//
//            val scoringUseCase = remember { com.basahero.elearning.domain.QuizScoringUseCase() }
//
//            val viewModel: QuizViewModel = viewModel(
//                factory = object : ViewModelProvider.Factory {
//                    @Suppress("UNCHECKED_CAST")
//                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//                        // 👇 PASSING YOUR QUIZ REPOSITORY HERE
//                        return QuizViewModel(quizRepository, scoringUseCase) as T
//                    }
//                }
//            )
//
//            QuizScreen(
//                lessonId = lessonId,
//                lessonTitle = "Lesson Quiz",
//                viewModel = viewModel,
//                onQuizComplete = { result ->
//                    // Change this line inside your onQuizComplete block:
//                    navController.navigate("quiz_result/$lessonId/${result.score}/${result.total}") {
//                        popUpTo(Routes.LESSON_LIST) { inclusive = false }
//                    }
//                },
//                onBack = { navController.popBackStack() }
//            )
//        }
//        // 🚀 The Quiz Result Screen
//        composable(Routes.QUIZ_RESULT) { backStackEntry ->
//            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
//            val score = backStackEntry.arguments?.getString("score")?.toIntOrNull() ?: 0
//            val total = backStackEntry.arguments?.getString("total")?.toIntOrNull() ?: 0
//            val currentStudentId = loggedInStudentId ?: ""
//
//            // 👇 Using the clean 'quiz' package path!
//            val viewModel: com.basahero.elearning.ui.student.quiz.QuizResultViewModel = viewModel(
//                factory = object : ViewModelProvider.Factory {
//                    @Suppress("UNCHECKED_CAST")
//                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//                        return com.basahero.elearning.ui.student.quiz.QuizResultViewModel(
//                            progressRepository,
//                            lessonRepository
//                        ) as T
//                    }
//                }
//            )
//
//            com.basahero.elearning.ui.student.quiz.QuizResultScreen(
//                studentId = currentStudentId,
//                lessonId = lessonId,
//                score = score,
//                total = total,
//                viewModel = viewModel,
//                onGoHome = {
//                    navController.popBackStack(Routes.LESSON_LIST, inclusive = false)
//                },
//                onNextLesson = { nextId ->
//                    navController.navigate("reading/$nextId") {
//                        popUpTo(Routes.LESSON_LIST) { inclusive = false }
//                    }
//                },
//                onRetry = {
//                    navController.navigate("quiz/$lessonId") {
//                        popUpTo(Routes.LESSON_LIST) { inclusive = false }
//                    }
//                }
//            )
//        }
//
//        // Placeholders for remaining screens
//        composable(Routes.QUARTER_MENU) { PlaceholderScreen("Quarter Menu") }
////        composable(Routes.READING) { PlaceholderScreen("Reading Activity") }
////        composable(Routes.QUIZ) { PlaceholderScreen("Quiz") }
////        composable(Routes.QUIZ_RESULT) { PlaceholderScreen("Quiz Result") }
//        composable(Routes.PRONUNCIATION) { PlaceholderScreen("Pronunciation Checker") }
//        composable(Routes.GAME_JOIN) { PlaceholderScreen("Join Game") }
//        composable(Routes.GAME_PLAY) { PlaceholderScreen("Game Play") }
//        composable(Routes.GAME_RESULT) { PlaceholderScreen("Game Result") }
//        composable(Routes.TEACHER_LOGIN) { PlaceholderScreen("Teacher Login") }
//        composable(Routes.TEACHER_DASHBOARD) { PlaceholderScreen("Teacher Dashboard") }
//        composable(Routes.CLASS_ROSTER) { PlaceholderScreen("Class Roster") }
//        composable(Routes.STUDENT_PROGRESS) { PlaceholderScreen("Student Progress") }
//        composable(Routes.GAME_HOST) { PlaceholderScreen("Game Host") }
//    }
//}
//
//@Composable
//fun RoleSelectScreen(onStudentClick: () -> Unit, onTeacherClick: () -> Unit) {
//    Column(
//        modifier = Modifier.fillMaxSize(),
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Text("Welcome to BasaHero!", style = MaterialTheme.typography.headlineMedium)
//        Spacer(modifier = Modifier.height(32.dp))
//        Button(onClick = onStudentClick) { Text("I am a Student") }
//        Spacer(modifier = Modifier.height(16.dp))
//        Button(onClick = onTeacherClick) { Text("I am a Teacher") }
//    }
//}
//
//@Composable
//fun PlaceholderScreen(title: String) {
//    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//        Text(title, style = MaterialTheme.typography.headlineLarge)
//    }
//}
package com.basahero.elearning

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import com.basahero.elearning.ui.student.home.StudentHomeScreen
import com.basahero.elearning.ui.student.home.StudentHomeViewModel

// 👇 IMPORTS FOR LESSON LIST
import com.basahero.elearning.ui.student.lessons.LessonListScreen
import com.basahero.elearning.ui.student.lessons.LessonListViewModel
import com.basahero.elearning.ui.student.lessons.ReadingScreen
import com.basahero.elearning.ui.student.lessons.ReadingViewModel
import com.basahero.elearning.ui.student.quiz.QuizScreen
import com.basahero.elearning.ui.student.quiz.QuizViewModel
import com.basahero.elearning.ui.theme.BasaHeroTheme

// 👇 NEW IMPORT
import com.basahero.elearning.data.local.SessionManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BasaHeroTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PhilIRIApp()
                }
            }
        }
    }
}

object Routes {
    const val ROLE_SELECT = "role_select"
    const val STUDENT_LOGIN = "student_login"
    const val STUDENT_HOME = "student_home"
    const val QUARTER_MENU = "quarter_menu/{gradeLevel}"
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
    const val STUDENT_PROGRESS = "student_progress/{studentId}"
    const val GAME_HOST = "game_host/{classId}"
}

@Composable
fun PhilIRIApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope() // 👇 NEW: Needed for logout

    val database = remember { AppDatabase.getInstance(context, DatabaseSeeder(context)) }
    val studentRepository = remember { StudentRepository(database) }
    val lessonRepository = remember { LessonRepository(database) }

    val quizRepository = remember { com.basahero.elearning.data.repository.QuizRepository(database) }
    val progressRepository = remember { com.basahero.elearning.data.repository.ProgressRepository(database) }

    // 👇 NEW: Setup SessionManager and observe it
    val sessionManager = remember { SessionManager(context) }
    val currentSession by sessionManager.studentSession.collectAsState(initial = null)

    // Automatically navigate to Home if session exists
    LaunchedEffect(currentSession) {
        if (currentSession != null) {
            navController.navigate(Routes.STUDENT_HOME) {
                popUpTo(Routes.ROLE_SELECT) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.ROLE_SELECT // Default to role select
    ) {
        composable(Routes.ROLE_SELECT) {
            RoleSelectScreen(
                onStudentClick = { navController.navigate(Routes.STUDENT_LOGIN) },
                onTeacherClick = { navController.navigate(Routes.TEACHER_LOGIN) }
            )
        }

        composable(Routes.STUDENT_LOGIN) {
            val viewModel: StudentLoginViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        // 👇 NEW: Passed sessionManager to factory
                        return StudentLoginViewModel(studentRepository, sessionManager) as T
                    }
                }
            )

            StudentLoginScreen(
                viewModel = viewModel,
                onLoginSuccess = { student ->
                    // 👇 We don't need loggedInStudentId anymore, SessionManager handles it!
                    navController.navigate(Routes.STUDENT_HOME) {
                        popUpTo(Routes.ROLE_SELECT) { inclusive = false }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.STUDENT_HOME) {
            // 👇 NEW: Fetch ID from the active session
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
                    navController.navigate("lesson_list/$quarterId")
                },
                onLogout = {
                    // 👇 NEW: Clear DataStore on logout
                    coroutineScope.launch {
                        sessionManager.clearStudentSession()
                    }
                    navController.navigate(Routes.ROLE_SELECT) {
                        popUpTo(0)
                    }
                }
            )
        }

        composable(Routes.LESSON_LIST) { backStackEntry ->
            val quarterId = backStackEntry.arguments?.getString("quarterId") ?: ""
            // 👇 NEW: Fetch ID from the active session
            val currentStudentId = currentSession?.studentId ?: ""

            val viewModel: LessonListViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return LessonListViewModel(lessonRepository) as T
                    }
                }
            )

            LessonListScreen(
                quarterId = quarterId,
                studentId = currentStudentId,
                quarterTitle = "Lessons",
                viewModel = viewModel,
                onLessonClick = { lessonId ->
                    navController.navigate("reading/$lessonId")
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
                        return ReadingViewModel(lessonRepository) as T
                    }
                }
            )

            ReadingScreen(
                lessonId = lessonId,
                viewModel = viewModel,
                onStartQuiz = { id ->
                    navController.navigate("quiz/$id")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.QUIZ) { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""

            val scoringUseCase = remember { com.basahero.elearning.domain.QuizScoringUseCase() }

            val viewModel: QuizViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return QuizViewModel(quizRepository, scoringUseCase) as T
                    }
                }
            )

            QuizScreen(
                lessonId = lessonId,
                lessonTitle = "Lesson Quiz",
                viewModel = viewModel,
                onQuizComplete = { result ->
                    navController.navigate("quiz_result/$lessonId/${result.score}/${result.total}") {
                        popUpTo(Routes.LESSON_LIST) { inclusive = false }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.QUIZ_RESULT) { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
            val score = backStackEntry.arguments?.getString("score")?.toIntOrNull() ?: 0
            val total = backStackEntry.arguments?.getString("total")?.toIntOrNull() ?: 0

            // 👇 NEW: Fetch ID from the active session
            val currentStudentId = currentSession?.studentId ?: ""

            val viewModel: com.basahero.elearning.ui.student.quiz.QuizResultViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return com.basahero.elearning.ui.student.quiz.QuizResultViewModel(
                            progressRepository,
                            lessonRepository
                        ) as T
                    }
                }
            )

            com.basahero.elearning.ui.student.quiz.QuizResultScreen(
                studentId = currentStudentId,
                lessonId = lessonId,
                score = score,
                total = total,
                viewModel = viewModel,
                onGoHome = {
                    navController.popBackStack(Routes.LESSON_LIST, inclusive = false)
                },
                onNextLesson = { nextId ->
                    navController.navigate("reading/$nextId") {
                        popUpTo(Routes.LESSON_LIST) { inclusive = false }
                    }
                },
                onRetry = {
                    navController.navigate("quiz/$lessonId") {
                        popUpTo(Routes.LESSON_LIST) { inclusive = false }
                    }
                }
            )
        }

        // Placeholders for remaining screens
        composable(Routes.QUARTER_MENU) { PlaceholderScreen("Quarter Menu") }
        composable(Routes.PRONUNCIATION) { PlaceholderScreen("Pronunciation Checker") }
        composable(Routes.GAME_JOIN) { PlaceholderScreen("Join Game") }
        composable(Routes.GAME_PLAY) { PlaceholderScreen("Game Play") }
        composable(Routes.GAME_RESULT) { PlaceholderScreen("Game Result") }
        composable(Routes.TEACHER_LOGIN) { PlaceholderScreen("Teacher Login") }
        composable(Routes.TEACHER_DASHBOARD) { PlaceholderScreen("Teacher Dashboard") }
        composable(Routes.CLASS_ROSTER) { PlaceholderScreen("Class Roster") }
        composable(Routes.STUDENT_PROGRESS) { PlaceholderScreen("Student Progress") }
        composable(Routes.GAME_HOST) { PlaceholderScreen("Game Host") }
    }
}

@Composable
fun RoleSelectScreen(onStudentClick: () -> Unit, onTeacherClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome to BasaHero!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onStudentClick) { Text("I am a Student") }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onTeacherClick) { Text("I am a Teacher") }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, style = MaterialTheme.typography.headlineLarge)
    }
}