package com.basahero.elearning.ui.student.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basahero.elearning.data.model.Quarter
import com.basahero.elearning.data.model.Student
import com.basahero.elearning.data.repository.LessonRepository
import com.basahero.elearning.data.repository.ProgressRepository
import com.basahero.elearning.data.repository.StudentRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StudentHomeViewModel(
    private val studentRepository: StudentRepository,
    private val lessonRepository: LessonRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    data class HomeUiState(
        val student: Student? = null,
        val quarters: List<Quarter> = emptyList(),
        val overallProgress: Float = 0f,
        val skillStats: List<Float> = listOf(0.8f, 0.7f, 0.9f, 0.75f), // Vocabulary, Comprehension, Consistency, Accuracy
        val isLoading: Boolean = true
    )

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadHome(studentId: String) {
        viewModelScope.launch {
            val student = studentRepository.getStudentById(studentId) ?: return@launch
            studentRepository.updateLastActive(studentId)

            // Silently sync progress down from cloud to restore any lost data
            launch {
                studentRepository.syncStudentDataDown(studentId)
            }

            lessonRepository
                .getQuartersWithProgress(student.gradeLevel, studentId)
                .collect { quarters ->
                    val totalLessons = quarters.sumOf { it.totalLessons }
                    val completedLessons = quarters.sumOf { it.completedLessons }
                    val overall = if (totalLessons == 0) 0f
                    else completedLessons.toFloat() / totalLessons

                    _uiState.value = HomeUiState(
                        student = student,
                        quarters = quarters,
                        overallProgress = overall,
                        skillStats = calculateSkillStats(studentId, quarters),
                        isLoading = false
                    )
                }
        }
    }

    private suspend fun calculateSkillStats(studentId: String, quarters: List<Quarter>): List<Float> {
        val allProgress = progressRepository.getAllProgressForStudent(studentId).first()
        if (allProgress.isEmpty()) return listOf(0.1f, 0.1f, 0.1f, 0.1f)

        // 1. Comprehension: Average quiz score ratio
        val quizProgress = allProgress.filter { it.quizTotal > 0 }
        val comprehension = if (quizProgress.isEmpty()) 0.1f 
            else quizProgress.map { it.quizScore.toFloat() / it.quizTotal }.average().toFloat()

        // 2. Accuracy: Average first-attempt score ratio
        val accuracy = if (quizProgress.isEmpty()) 0.1f 
            else quizProgress.map { (it.firstScore ?: it.quizScore).toFloat() / it.quizTotal }.average().toFloat()

        // 3. Consistency: Active days in last 30 days
        val activityDays = allProgress.mapNotNull { it.completedAt }
            .plus(uiState.value.student?.lastActive ?: System.currentTimeMillis())
            .map { 
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = it
                "${cal.get(java.util.Calendar.YEAR)}-${cal.get(java.util.Calendar.DAY_OF_YEAR)}"
            }.distinct().size
        val consistency = (activityDays.toFloat() / 14f).coerceIn(0.1f, 1.0f) // Target 14 active days

        // 4. Vocabulary (Learning Breadth): Completed lessons vs total targeted
        val totalLessons = quarters.sumOf { it.totalLessons }.coerceAtLeast(1)
        val completedLessons = quarters.sumOf { it.completedLessons }
        val vocabulary = (completedLessons.toFloat() / totalLessons).coerceIn(0.1f, 1.0f)

        return listOf(
            vocabulary.coerceIn(0.1f, 1f),
            comprehension.coerceIn(0.1f, 1f),
            consistency.coerceIn(0.1f, 1f),
            accuracy.coerceIn(0.1f, 1f)
        )
    }
}