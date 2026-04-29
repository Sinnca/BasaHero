package com.basahero.elearning.ui.student.lessons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basahero.elearning.data.model.Lesson
import com.basahero.elearning.data.repository.LessonRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// LessonListViewModel
// ─────────────────────────────────────────────────────────────────────────────
class LessonListViewModel(
    private val lessonRepository: LessonRepository
) : ViewModel() {

    data class LessonListUiState(
        val lessons: List<Lesson> = emptyList(),
        val quarterTitle: String = "",
        val isLoading: Boolean = true
    )

    private val _uiState = MutableStateFlow(LessonListUiState())
    val uiState: StateFlow<LessonListUiState> = _uiState.asStateFlow()

    fun loadLessons(quarterId: String, studentId: String, quarterTitle: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(quarterTitle = quarterTitle) }
            lessonRepository.getLessonsWithStatus(quarterId, studentId).collect { lessons ->
                _uiState.value = LessonListUiState(
                    lessons = lessons,
                    quarterTitle = quarterTitle,
                    isLoading = false
                )
            }
        }
    }
}