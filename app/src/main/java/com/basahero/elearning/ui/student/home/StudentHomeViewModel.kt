package com.basahero.elearning.ui.student.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basahero.elearning.data.model.Quarter
import com.basahero.elearning.data.model.Student
import com.basahero.elearning.data.repository.LessonRepository
import com.basahero.elearning.data.repository.StudentRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StudentHomeViewModel(
    private val studentRepository: StudentRepository,
    private val lessonRepository: LessonRepository
) : ViewModel() {

    data class HomeUiState(
        val student: Student? = null,
        val quarters: List<Quarter> = emptyList(),
        val overallProgress: Float = 0f,
        val isLoading: Boolean = true
    )

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadHome(studentId: String) {
        viewModelScope.launch {
            val student = studentRepository.getStudentById(studentId) ?: return@launch
            studentRepository.updateLastActive(studentId)

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
                        isLoading = false
                    )
                }
        }
    }
}