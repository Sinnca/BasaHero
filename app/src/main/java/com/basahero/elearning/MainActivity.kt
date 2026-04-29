package com.basahero.elearning

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.* // Added for mutableStateOf
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
import com.basahero.elearning.data.repository.LessonRepository // Added
import com.basahero.elearning.data.repository.StudentRepository
import com.basahero.elearning.ui.student.login.StudentLoginScreen
import com.basahero.elearning.ui.student.login.StudentLoginViewModel
import com.basahero.elearning.ui.student.home.StudentHomeScreen // Added
import com.basahero.elearning.ui.student.home.StudentHomeViewModel // Added
import com.basahero.elearning.ui.theme.BasaHeroTheme

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

    val database = remember { AppDatabase.getInstance(context, DatabaseSeeder(context)) }
    val studentRepository = remember { StudentRepository(database) }
    val lessonRepository = remember { LessonRepository(database) } // Added

    // This state remembers which student is currently logged in
    var loggedInStudentId by remember { mutableStateOf<String?>(null) }

    NavHost(
        navController = navController,
        startDestination = Routes.ROLE_SELECT
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
                        return StudentLoginViewModel(studentRepository) as T
                    }
                }
            )

            StudentLoginScreen(
                viewModel = viewModel,
                onLoginSuccess = { student ->
                    loggedInStudentId = student.id // Store the ID here
                    navController.navigate(Routes.STUDENT_HOME) {
                        popUpTo(Routes.ROLE_SELECT) { inclusive = false }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // 🚀 UPDATED: Now points to your actual Home Dashboard!
        composable(Routes.STUDENT_HOME) {
            val currentStudentId = loggedInStudentId ?: ""
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
                    // Go to lesson list for that quarter
                    navController.navigate("lesson_list/$quarterId")
                },
                onLogout = {
                    loggedInStudentId = null
                    navController.navigate(Routes.ROLE_SELECT) {
                        popUpTo(0)
                    }
                }
            )
        }

        // Placeholders for remaining screens
        composable(Routes.QUARTER_MENU) { PlaceholderScreen("Quarter Menu") }
        composable(Routes.LESSON_LIST) { PlaceholderScreen("Lesson List") }
        composable(Routes.READING) { PlaceholderScreen("Reading Activity") }
        composable(Routes.QUIZ) { PlaceholderScreen("Quiz") }
        composable(Routes.QUIZ_RESULT) { PlaceholderScreen("Quiz Result") }
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