////package com.basahero.elearning
////
////import android.os.Bundle
////import androidx.activity.ComponentActivity
////import androidx.activity.compose.setContent
////import androidx.activity.enableEdgeToEdge
////import androidx.compose.foundation.layout.*
////import androidx.compose.material3.*
////import androidx.compose.runtime.*
////import androidx.compose.ui.Alignment
////import androidx.compose.ui.Modifier
////import androidx.compose.ui.platform.LocalContext
////import androidx.compose.ui.unit.dp
////import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
////import androidx.lifecycle.ViewModel
////import androidx.lifecycle.ViewModelProvider
////import androidx.lifecycle.viewmodel.compose.viewModel
////import androidx.navigation.compose.NavHost
////import androidx.navigation.compose.composable
////import androidx.navigation.compose.rememberNavController
////
////import com.basahero.elearning.data.local.AppDatabase
////import com.basahero.elearning.data.local.DatabaseSeeder
////import com.basahero.elearning.data.repository.LessonRepository
////import com.basahero.elearning.data.repository.StudentRepository
////import com.basahero.elearning.ui.student.login.StudentLoginScreen
////import com.basahero.elearning.ui.student.login.StudentLoginViewModel
////import com.basahero.elearning.ui.student.home.StudentHomeScreen
////import com.basahero.elearning.ui.student.home.StudentHomeViewModel
////
////// 👇 NEW IMPORTS FOR LESSON LIST
////import com.basahero.elearning.ui.student.lessons.LessonListScreen
////import com.basahero.elearning.ui.student.lessons.LessonListViewModel
////import com.basahero.elearning.ui.student.lessons.ReadingScreen
////import com.basahero.elearning.ui.student.lessons.ReadingViewModel
////import com.basahero.elearning.ui.student.quiz.QuizScreen
////import com.basahero.elearning.ui.student.quiz.QuizViewModel
////import com.basahero.elearning.ui.theme.BasaHeroTheme
////
////class MainActivity : ComponentActivity() {
////    override fun onCreate(savedInstanceState: Bundle?) {
////        installSplashScreen()
////        super.onCreate(savedInstanceState)
////        enableEdgeToEdge()
////        setContent {
////            BasaHeroTheme {
////                Surface(
////                    modifier = Modifier.fillMaxSize(),
////                    color = MaterialTheme.colorScheme.background
////                ) {
////                    PhilIRIApp()
////                }
////            }
////        }
////    }
////}
////
////object Routes {
////    const val ROLE_SELECT = "role_select"
////    const val STUDENT_LOGIN = "student_login"
////    const val STUDENT_HOME = "student_home"
////    const val QUARTER_MENU = "quarter_menu/{gradeLevel}"
////    const val LESSON_LIST = "lesson_list/{quarterId}"
////    const val READING = "reading/{lessonId}"
////    const val QUIZ = "quiz/{lessonId}"
////    const val QUIZ_RESULT = "quiz_result/{lessonId}/{score}/{total}"
////    const val PRONUNCIATION = "pronunciation/{lessonId}"
////    const val GAME_JOIN = "game_join"
////    const val GAME_PLAY = "game_play/{sessionId}"
////    const val GAME_RESULT = "game_result/{sessionId}"
////    const val TEACHER_LOGIN = "teacher_login"
////    const val TEACHER_DASHBOARD = "teacher_dashboard"
////    const val CLASS_ROSTER = "class_roster/{classId}"
////    const val STUDENT_PROGRESS = "student_progress/{studentId}"
////    const val GAME_HOST = "game_host/{classId}"
////}
////
////@Composable
////fun PhilIRIApp() {
////    val navController = rememberNavController()
////    val context = LocalContext.current
////
////    val database = remember { AppDatabase.getInstance(context, DatabaseSeeder(context)) }
////    val studentRepository = remember { StudentRepository(database) }
////    val lessonRepository = remember { LessonRepository(database) }
////
////    val quizRepository = remember { com.basahero.elearning.data.repository.QuizRepository(database) }
////    val progressRepository = remember { com.basahero.elearning.data.repository.ProgressRepository(database) }
////
////    var loggedInStudentId by remember { mutableStateOf<String?>(null) }
////
////    NavHost(
////        navController = navController,
////        startDestination = Routes.ROLE_SELECT
////    ) {
////        composable(Routes.ROLE_SELECT) {
////            RoleSelectScreen(
////                onStudentClick = { navController.navigate(Routes.STUDENT_LOGIN) },
////                onTeacherClick = { navController.navigate(Routes.TEACHER_LOGIN) }
////            )
////        }
////
////        composable(Routes.STUDENT_LOGIN) {
////            val viewModel: StudentLoginViewModel = viewModel(
////                factory = object : ViewModelProvider.Factory {
////                    @Suppress("UNCHECKED_CAST")
////                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
////                        return StudentLoginViewModel(studentRepository) as T
////                    }
////                }
////            )
////
////            StudentLoginScreen(
////                viewModel = viewModel,
////                onLoginSuccess = { student ->
////                    loggedInStudentId = student.id
////                    navController.navigate(Routes.STUDENT_HOME) {
////                        popUpTo(Routes.ROLE_SELECT) { inclusive = false }
////                    }
////                },
////                onBack = { navController.popBackStack() }
////            )
////        }
////
////        composable(Routes.STUDENT_HOME) {
////            val currentStudentId = loggedInStudentId ?: ""
////            val viewModel: StudentHomeViewModel = viewModel(
////                factory = object : ViewModelProvider.Factory {
////                    @Suppress("UNCHECKED_CAST")
////                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
////                        return StudentHomeViewModel(studentRepository, lessonRepository) as T
////                    }
////                }
////            )
////
////            StudentHomeScreen(
////                studentId = currentStudentId,
////                viewModel = viewModel,
////                onQuarterClick = { quarterId, _ ->
////                    navController.navigate("lesson_list/$quarterId")
////                },
////                onLogout = {
////                    loggedInStudentId = null
////                    navController.navigate(Routes.ROLE_SELECT) {
////                        popUpTo(0)
////                    }
////                }
////            )
////        }
////
////        // 🚀 WE REPLACED THE PLACEHOLDER WITH OUR REAL SCREEN!
////        composable(Routes.LESSON_LIST) { backStackEntry ->
////            val quarterId = backStackEntry.arguments?.getString("quarterId") ?: ""
////            val currentStudentId = loggedInStudentId ?: ""
////
////            val viewModel: LessonListViewModel = viewModel(
////                factory = object : ViewModelProvider.Factory {
////                    @Suppress("UNCHECKED_CAST")
////                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
////                        return LessonListViewModel(lessonRepository) as T
////                    }
////                }
////            )
////
////            LessonListScreen(
////                quarterId = quarterId,
////                studentId = currentStudentId,
////                quarterTitle = "Lessons",
////                viewModel = viewModel,
////                onLessonClick = { lessonId ->
////                    navController.navigate("reading/$lessonId")
////                },
////                onBack = { navController.popBackStack() }
////            )
////        }
////
////        // 🚀 The Reading Screen
////        composable(Routes.READING) { backStackEntry ->
////            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
////
////            val viewModel: ReadingViewModel = viewModel(
////                factory = object : ViewModelProvider.Factory {
////                    @Suppress("UNCHECKED_CAST")
////                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
////                        return ReadingViewModel(lessonRepository) as T
////                    }
////                }
////            )
////
////            ReadingScreen(
////                lessonId = lessonId,
////                viewModel = viewModel,
////                onStartQuiz = { id ->
////                    navController.navigate("quiz/$id")
////                },
////                onBack = { navController.popBackStack() }
////            )
////        }
////
////        // 🚀 The Quiz Screen
////        composable(Routes.QUIZ) { backStackEntry ->
////            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
////
////            val scoringUseCase = remember { com.basahero.elearning.domain.QuizScoringUseCase() }
////
////            val viewModel: QuizViewModel = viewModel(
////                factory = object : ViewModelProvider.Factory {
////                    @Suppress("UNCHECKED_CAST")
////                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
////                        // 👇 PASSING YOUR QUIZ REPOSITORY HERE
////                        return QuizViewModel(quizRepository, scoringUseCase) as T
////                    }
////                }
////            )
////
////            QuizScreen(
////                lessonId = lessonId,
////                lessonTitle = "Lesson Quiz",
////                viewModel = viewModel,
////                onQuizComplete = { result ->
////                    // Change this line inside your onQuizComplete block:
////                    navController.navigate("quiz_result/$lessonId/${result.score}/${result.total}") {
////                        popUpTo(Routes.LESSON_LIST) { inclusive = false }
////                    }
////                },
////                onBack = { navController.popBackStack() }
////            )
////        }
////        // 🚀 The Quiz Result Screen
////        composable(Routes.QUIZ_RESULT) { backStackEntry ->
////            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
////            val score = backStackEntry.arguments?.getString("score")?.toIntOrNull() ?: 0
////            val total = backStackEntry.arguments?.getString("total")?.toIntOrNull() ?: 0
////            val currentStudentId = loggedInStudentId ?: ""
////
////            // 👇 Using the clean 'quiz' package path!
////            val viewModel: com.basahero.elearning.ui.student.quiz.QuizResultViewModel = viewModel(
////                factory = object : ViewModelProvider.Factory {
////                    @Suppress("UNCHECKED_CAST")
////                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
////                        return com.basahero.elearning.ui.student.quiz.QuizResultViewModel(
////                            progressRepository,
////                            lessonRepository
////                        ) as T
////                    }
////                }
////            )
////
////            com.basahero.elearning.ui.student.quiz.QuizResultScreen(
////                studentId = currentStudentId,
////                lessonId = lessonId,
////                score = score,
////                total = total,
////                viewModel = viewModel,
////                onGoHome = {
////                    navController.popBackStack(Routes.LESSON_LIST, inclusive = false)
////                },
////                onNextLesson = { nextId ->
////                    navController.navigate("reading/$nextId") {
////                        popUpTo(Routes.LESSON_LIST) { inclusive = false }
////                    }
////                },
////                onRetry = {
////                    navController.navigate("quiz/$lessonId") {
////                        popUpTo(Routes.LESSON_LIST) { inclusive = false }
////                    }
////                }
////            )
////        }
////
////        // Placeholders for remaining screens
////        composable(Routes.QUARTER_MENU) { PlaceholderScreen("Quarter Menu") }
//////        composable(Routes.READING) { PlaceholderScreen("Reading Activity") }
//////        composable(Routes.QUIZ) { PlaceholderScreen("Quiz") }
//////        composable(Routes.QUIZ_RESULT) { PlaceholderScreen("Quiz Result") }
////        composable(Routes.PRONUNCIATION) { PlaceholderScreen("Pronunciation Checker") }
////        composable(Routes.GAME_JOIN) { PlaceholderScreen("Join Game") }
////        composable(Routes.GAME_PLAY) { PlaceholderScreen("Game Play") }
////        composable(Routes.GAME_RESULT) { PlaceholderScreen("Game Result") }
////        composable(Routes.TEACHER_LOGIN) { PlaceholderScreen("Teacher Login") }
////        composable(Routes.TEACHER_DASHBOARD) { PlaceholderScreen("Teacher Dashboard") }
////        composable(Routes.CLASS_ROSTER) { PlaceholderScreen("Class Roster") }
////        composable(Routes.STUDENT_PROGRESS) { PlaceholderScreen("Student Progress") }
////        composable(Routes.GAME_HOST) { PlaceholderScreen("Game Host") }
////    }
////}
////
////@Composable
////fun RoleSelectScreen(onStudentClick: () -> Unit, onTeacherClick: () -> Unit) {
////    Column(
////        modifier = Modifier.fillMaxSize(),
////        verticalArrangement = Arrangement.Center,
////        horizontalAlignment = Alignment.CenterHorizontally
////    ) {
////        Text("Welcome to BasaHero!", style = MaterialTheme.typography.headlineMedium)
////        Spacer(modifier = Modifier.height(32.dp))
////        Button(onClick = onStudentClick) { Text("I am a Student") }
////        Spacer(modifier = Modifier.height(16.dp))
////        Button(onClick = onTeacherClick) { Text("I am a Teacher") }
////    }
////}
////
////@Composable
////fun PlaceholderScreen(title: String) {
////    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
////        Text(title, style = MaterialTheme.typography.headlineLarge)
////    }
////}
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
//import com.basahero.elearning.ui.student.lessons.LessonListScreen
//import com.basahero.elearning.ui.student.lessons.LessonListViewModel
//import com.basahero.elearning.ui.student.lessons.ReadingScreen
//import com.basahero.elearning.ui.student.lessons.ReadingViewModel
//import com.basahero.elearning.ui.student.quiz.QuizScreen
//import com.basahero.elearning.ui.student.quiz.QuizViewModel
//import com.basahero.elearning.ui.theme.BasaHeroTheme
//
//import com.basahero.elearning.data.local.SessionManager
//import kotlinx.coroutines.launch
//
//// 👇 PHASE 3B NEW IMPORTS
//import com.basahero.elearning.data.repository.PrePostRepository
//import com.basahero.elearning.ui.student.prepost.PreTestGateScreen
//import com.basahero.elearning.ui.student.prepost.PostTestScreen
//import com.basahero.elearning.ui.student.prepost.PrePostViewModel
//import com.basahero.elearning.domain.QuizScoringUseCase
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
//    const val PRE_TEST = "pre_test/{quarterId}" // 👈 NEW
//    const val POST_TEST = "post_test/{quarterId}" // 👈 NEW
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
//    val coroutineScope = rememberCoroutineScope()
//
//    val database = remember { AppDatabase.getInstance(context, DatabaseSeeder(context)) }
//    val studentRepository = remember { StudentRepository(database) }
//    val lessonRepository = remember { LessonRepository(database) }
//    val quizRepository = remember { com.basahero.elearning.data.repository.QuizRepository(database) }
//    val progressRepository = remember { com.basahero.elearning.data.repository.ProgressRepository(database) }
//
//    // 👇 NEW: PrePost Repository & shared UseCase
//    val prePostRepository = remember { PrePostRepository(database) }
//    val scoringUseCase = remember { QuizScoringUseCase() }
//
//    val sessionManager = remember { SessionManager(context) }
//    val currentSession by sessionManager.studentSession.collectAsState(initial = null)
//
//    // Automatically navigate to Home if session exists
//    LaunchedEffect(currentSession) {
//        if (currentSession != null) {
//            navController.navigate(Routes.STUDENT_HOME) {
//                popUpTo(Routes.ROLE_SELECT) { inclusive = true }
//            }
//        }
//    }
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
//                        return StudentLoginViewModel(studentRepository, sessionManager) as T
//                    }
//                }
//            )
//
//            StudentLoginScreen(
//                viewModel = viewModel,
//                onLoginSuccess = { student ->
//                    navController.navigate(Routes.STUDENT_HOME) {
//                        popUpTo(Routes.ROLE_SELECT) { inclusive = false }
//                    }
//                },
//                onBack = { navController.popBackStack() }
//            )
//        }
//
//        composable(Routes.STUDENT_HOME) {
//            val currentStudentId = currentSession?.studentId ?: ""
//
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
//                    // 🚀 PHASE 3B: Route to Pre-Test first!
//                    navController.navigate("pre_test/$quarterId")
//                },
//                onLogout = {
//                    coroutineScope.launch {
//                        sessionManager.clearStudentSession()
//                    }
//                    navController.navigate(Routes.ROLE_SELECT) {
//                        popUpTo(0)
//                    }
//                }
//            )
//        }
//
//        // 🚀 PHASE 3B: Pre-Test Gate Screen
//        composable(Routes.PRE_TEST) { backStackEntry ->
//            val quarterId = backStackEntry.arguments?.getString("quarterId") ?: ""
//            val currentStudentId = currentSession?.studentId ?: ""
//
//            val viewModel: PrePostViewModel = viewModel(
//                factory = object : ViewModelProvider.Factory {
//                    @Suppress("UNCHECKED_CAST")
//                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//                        return PrePostViewModel(prePostRepository, scoringUseCase) as T
//                    }
//                }
//            )
//
//            PreTestGateScreen(
//                quarterId = quarterId,
//                quarterTitle = "Quarter Introduction",
//                studentId = currentStudentId,
//                viewModel = viewModel,
//                onPreTestComplete = {
//                    // Pre-test done (or skipped) -> Automatically bounce to Lesson List!
//                    navController.navigate("lesson_list/$quarterId") {
//                        popUpTo("pre_test/$quarterId") { inclusive = true }
//                    }
//                },
//                onBack = { navController.popBackStack() }
//            )
//        }
//
//        composable(Routes.LESSON_LIST) { backStackEntry ->
//            val quarterId = backStackEntry.arguments?.getString("quarterId") ?: ""
//            val currentStudentId = currentSession?.studentId ?: ""
//
//            val viewModel: LessonListViewModel = viewModel(
//                factory = object : ViewModelProvider.Factory {
//                    @Suppress("UNCHECKED_CAST")
//                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//                        // 👇 FIX 1: We added prePostRepository here!
//                        return LessonListViewModel(lessonRepository, prePostRepository) as T
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
//                // 👇 FIX 2: We added the navigation for the two new test cards!
//                onPreTestClick = {
//                    navController.navigate("pre_test/$quarterId")
//                },
//                onPostTestClick = {
//                    navController.navigate("post_test/$quarterId")
//                },
//                onBack = { navController.popBackStack(Routes.STUDENT_HOME, inclusive = false) }
//            )
//        }
//
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
//        composable(Routes.QUIZ) { backStackEntry ->
//            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
//
//            val viewModel: QuizViewModel = viewModel(
//                factory = object : ViewModelProvider.Factory {
//                    @Suppress("UNCHECKED_CAST")
//                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//                        // 👇 Now using the hoisted scoringUseCase
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
//                    navController.navigate("quiz_result/$lessonId/${result.score}/${result.total}") {
//                        popUpTo(Routes.LESSON_LIST) { inclusive = false }
//                    }
//                },
//                onBack = { navController.popBackStack() }
//            )
//        }
//
//        composable(Routes.QUIZ_RESULT) { backStackEntry ->
//            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
//            val score = backStackEntry.arguments?.getString("score")?.toIntOrNull() ?: 0
//            val total = backStackEntry.arguments?.getString("total")?.toIntOrNull() ?: 0
//
//            val currentStudentId = currentSession?.studentId ?: ""
//
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
//        // 🚀 PHASE 3B: Post-Test Screen
//        composable(Routes.POST_TEST) { backStackEntry ->
//            val quarterId = backStackEntry.arguments?.getString("quarterId") ?: ""
//            val currentStudentId = currentSession?.studentId ?: ""
//
//            val viewModel: PrePostViewModel = viewModel(
//                factory = object : ViewModelProvider.Factory {
//                    @Suppress("UNCHECKED_CAST")
//                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//                        return PrePostViewModel(prePostRepository, scoringUseCase) as T
//                    }
//                }
//            )
//
//            PostTestScreen(
//                quarterId = quarterId,
//                quarterTitle = "Quarter Review",
//                studentId = currentStudentId,
//                viewModel = viewModel,
//                onPostTestComplete = {
//                    // Post-test done -> Send them all the way back to the main home screen
//                    navController.popBackStack(Routes.STUDENT_HOME, inclusive = false)
//                },
//                onBack = { navController.popBackStack() }
//            )
//        }
//
//        // Placeholders for remaining screens
//        composable(Routes.QUARTER_MENU) { PlaceholderScreen("Quarter Menu") }
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
    const val STUDENT_PROGRESS = "student_progress/{studentId}"
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

    // Shared repositories
    val prePostRepository = remember { PrePostRepository(database) }
    val scoringUseCase = remember { QuizScoringUseCase() }

    val sessionManager = remember { SessionManager(context) }
    val currentSession by sessionManager.studentSession.collectAsState(initial = null)

    // Derive grade level from active session (default 4 = blue before login)
    val gradeLevel = currentSession?.gradeLevel ?: 4

    LaunchedEffect(currentSession) {
        if (currentSession != null) {
            navController.navigate(Routes.STUDENT_HOME) {
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
                onStudentClick = { navController.navigate(Routes.STUDENT_LOGIN) },
                onTeacherClick = { navController.navigate(Routes.TEACHER_LOGIN) }
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
        composable(Routes.TEACHER_LOGIN) { PlaceholderScreen("Teacher Login") }
        composable(Routes.TEACHER_DASHBOARD) { PlaceholderScreen("Teacher Dashboard") }
        composable(Routes.CLASS_ROSTER) { PlaceholderScreen("Class Roster") }
        composable(Routes.STUDENT_PROGRESS) { PlaceholderScreen("Student Progress") }
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