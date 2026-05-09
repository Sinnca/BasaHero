package com.basahero.elearning.ui.common

import androidx.compose.runtime.*

// ─────────────────────────────────────────────────────────────────────────────
// AppStrings — Centralized multilingual string provider
// Supports "en" (English) and "fil" (Filipino/Tagalog)
// ─────────────────────────────────────────────────────────────────────────────

data class AppStrings(
    // ── Role Select ──────────────────────────────────────────────────────────
    val getStarted: String,
    val student: String,
    val teacher: String,
    val startYourLearningJourney: String,
    val manageClassesAndAnalytics: String,

    // ── Student Home ─────────────────────────────────────────────────────────
    val hiGreeting: (String) -> String,  // "Hi, {name}! 👋"
    val gradeAndSection: (Int, String) -> String,  // "Grade 4 · Section A"
    val loadingYourAdventure: String,
    val myQuarters: String,
    val complete: String,  // "Complete"
    val percentComplete: (Int) -> String,  // "85% Complete"
    val keepItUp: String,
    val almostThere: String,
    val allDone: String,
    val completePreviousQuarter: String,
    val lessonsProgress: (Int, Int) -> String,  // "5 of 10 lessons done ✔"
    val joinGame: String,
    val logout: String,
    val english: String,  // for language toggle

    // ── Lesson List ──────────────────────────────────────────────────────────
    val lessons: String,
    val preTest: String,
    val postTest: String,
    val takePreTest: String,
    val takePostTest: String,
    val locked: String,
    val done: String,
    val inProgress: String,

    // ── Reading Screen ───────────────────────────────────────────────────────
    val scrollDownToRead: String,
    val youveFinishedReading: String,
    val takeTheQuiz: String,
    val back: String,

    // ── Quiz Screen ──────────────────────────────────────────────────────────
    val quiz: String,
    val loadingQuestions: String,
    val questionOf: (Int, Int) -> String,  // "Question 1 of 10"
    val typeYourAnswer: String,
    val typeYourAnswerHint: String,
    val next: String,
    val submitQuiz: String,
    val longPressToReorder: String,
    val correctSequence: String,
    val tapLeftThenRight: String,
    val nowTapRight: String,

    // ── Quiz Result ──────────────────────────────────────────────────────────
    val quizComplete: String,
    val yourScore: String,
    val backToLessons: String,

    // ── Game ──────────────────────────────────────────────────────────────────
    val enterGameCode: String,
    val joinGameButton: String,
    val waitingForHost: String,

    // ── Language toggle ──────────────────────────────────────────────────────
    val languageLabel: String,  // "Language" / "Wika"
    val tagalog: String,

    // ── Grade Select ─────────────────────────────────────────────────────────
    val selectYourGrade: String,
    val grade: (Int) -> String,
    val alreadyHaveAccount: String,
    val loginHere: String,
)

val EnglishStrings = AppStrings(
    // Role Select
    getStarted = "GET STARTED",
    student = "Student",
    teacher = "Teacher",
    startYourLearningJourney = "Start your learning journey",
    manageClassesAndAnalytics = "Manage classes & analytics",

    // Student Home
    hiGreeting = { name -> "Hi, $name! 👋" },
    gradeAndSection = { grade, section -> "Grade $grade  ·  $section" },
    loadingYourAdventure = "Loading your adventure...",
    myQuarters = "My Quarters",
    complete = "Complete",
    percentComplete = { pct -> "$pct% Complete" },
    keepItUp = "Keep it up, hero! 💪",
    almostThere = "Almost there! 🌟",
    allDone = "All done! Amazing! 🎉",
    completePreviousQuarter = "🔒 Complete previous quarter first",
    lessonsProgress = { done, total -> "$done of $total lessons done ✔" },
    joinGame = "Join Game",
    logout = "Logout",
    english = "English",

    // Lesson List
    lessons = "Lessons",
    preTest = "Pre-Test",
    postTest = "Post-Test",
    takePreTest = "Take Pre-Test",
    takePostTest = "Take Post-Test",
    locked = "Locked",
    done = "Done",
    inProgress = "In Progress",

    // Reading
    scrollDownToRead = "Scroll down to read the full passage",
    youveFinishedReading = "You've finished reading! Ready for the quiz?",
    takeTheQuiz = "Take the Quiz",
    back = "Back",

    // Quiz
    quiz = "Quiz",
    loadingQuestions = "Loading questions…",
    questionOf = { current, total -> "Question $current of $total" },
    typeYourAnswer = "Type your answer",
    typeYourAnswerHint = "Type your answer in the box above.",
    next = "Next",
    submitQuiz = "Submit Quiz",
    longPressToReorder = "Long press the ≡ handle to drag and reorder:",
    correctSequence = "Correct sequence:",
    tapLeftThenRight = "Tap a word on the left, then tap its match on the right.",
    nowTapRight = "Now tap a word on the right to connect →",

    // Quiz Result
    quizComplete = "Quiz Complete!",
    yourScore = "Your Score",
    backToLessons = "Back to Lessons",

    // Game
    enterGameCode = "Enter Game Code",
    joinGameButton = "Join Game",
    waitingForHost = "Waiting for host...",

    // Language
    languageLabel = "Language",
    tagalog = "Tagalog",

    // Grade Select
    selectYourGrade = "Select Your Grade",
    grade = { level -> "Grade $level" },
    alreadyHaveAccount = "Already have an account?",
    loginHere = "Log in here",
)

val FilipinoStrings = AppStrings(
    // Role Select
    getStarted = "MAGSIMULA",
    student = "Mag-aaral",
    teacher = "Guro",
    startYourLearningJourney = "Simulan ang iyong pag-aaral",
    manageClassesAndAnalytics = "Pamahalaan ang klase at datos",

    // Student Home
    hiGreeting = { name -> "Kumusta, $name! 👋" },
    gradeAndSection = { grade, section -> "Baitang $grade  ·  $section" },
    loadingYourAdventure = "Nilo-load ang iyong adventure...",
    myQuarters = "Mga Markahan",
    complete = "Kumpleto",
    percentComplete = { pct -> "$pct% Kumpleto" },
    keepItUp = "Kaya mo 'yan, hero! 💪",
    almostThere = "Konti na lang! 🌟",
    allDone = "Tapos na lahat! Ang galing! 🎉",
    completePreviousQuarter = "🔒 Tapusin muna ang nakaraang markahan",
    lessonsProgress = { done, total -> "$done sa $total aralin ang tapos ✔" },
    joinGame = "Sumali sa Laro",
    logout = "Mag-logout",
    english = "Ingles",

    // Lesson List
    lessons = "Mga Aralin",
    preTest = "Paunang Pagsusulit",
    postTest = "Panghuling Pagsusulit",
    takePreTest = "Kumuha ng Paunang Pagsusulit",
    takePostTest = "Kumuha ng Panghuling Pagsusulit",
    locked = "Naka-lock",
    done = "Tapos",
    inProgress = "Kasalukuyan",

    // Reading
    scrollDownToRead = "Mag-scroll pababa para basahin ang buong teksto",
    youveFinishedReading = "Tapos ka nang bumasa! Handa ka na ba sa pagsusulit?",
    takeTheQuiz = "Simulan ang Pagsusulit",
    back = "Bumalik",

    // Quiz
    quiz = "Pagsusulit",
    loadingQuestions = "Nilo-load ang mga tanong…",
    questionOf = { current, total -> "Tanong $current sa $total" },
    typeYourAnswer = "I-type ang iyong sagot",
    typeYourAnswerHint = "I-type ang iyong sagot sa kahon sa itaas.",
    next = "Susunod",
    submitQuiz = "Ipasa ang Pagsusulit",
    longPressToReorder = "Pindutin nang matagal ang ≡ at i-drag para ayusin:",
    correctSequence = "Tamang pagkakasunod-sunod:",
    tapLeftThenRight = "Pindutin ang salita sa kaliwa, pagkatapos sa kanan.",
    nowTapRight = "Ngayon pindutin ang salita sa kanan para ikonekta →",

    // Quiz Result
    quizComplete = "Tapos na ang Pagsusulit!",
    yourScore = "Iyong Iskor",
    backToLessons = "Bumalik sa Mga Aralin",

    // Game
    enterGameCode = "Ilagay ang Code ng Laro",
    joinGameButton = "Sumali sa Laro",
    waitingForHost = "Naghihintay sa host...",

    // Language
    languageLabel = "Wika",
    tagalog = "Tagalog",

    // Grade Select
    selectYourGrade = "Pumili ng Baitang",
    grade = { level -> "Baitang $level" },
    alreadyHaveAccount = "May account ka na?",
    loginHere = "Mag-log in dito",
)

fun getStrings(languageCode: String): AppStrings = when (languageCode) {
    "fil" -> FilipinoStrings
    else -> EnglishStrings
}

// CompositionLocal so any composable in the tree can access strings
val LocalAppStrings = staticCompositionLocalOf { EnglishStrings }
