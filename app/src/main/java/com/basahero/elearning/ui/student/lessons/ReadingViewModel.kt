package com.basahero.elearning.ui.student.lessons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basahero.elearning.data.model.Lesson
import com.basahero.elearning.data.repository.LessonRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// ReadingViewModel
// ─────────────────────────────────────────────────────────────────────────────
class ReadingViewModel(
    private val lessonRepository: LessonRepository
) : ViewModel() {

    data class ReadingUiState(
        val lesson: Lesson? = null,
        val isLoading: Boolean = true,
        val readingComplete: Boolean = false
    )

    private val _uiState = MutableStateFlow(ReadingUiState())
    val uiState: StateFlow<ReadingUiState> = _uiState.asStateFlow()

    fun loadLesson(lessonId: String) {
        viewModelScope.launch {
            val lesson = lessonRepository.getLessonById(lessonId)
            _uiState.value = ReadingUiState(
                lesson = lesson,
                isLoading = false
            )
        }
    }

    fun onScrolledToBottom() {
        _uiState.update { it.copy(readingComplete = true) }
    }
}