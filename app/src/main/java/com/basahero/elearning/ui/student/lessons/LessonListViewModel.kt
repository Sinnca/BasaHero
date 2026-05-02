package com.basahero.elearning.ui.student.lessons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basahero.elearning.data.model.Lesson
import com.basahero.elearning.data.model.LessonStatus
import com.basahero.elearning.data.repository.LessonRepository
import com.basahero.elearning.data.repository.PrePostRepository // 👈 Import here
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// LessonListViewModel
// ─────────────────────────────────────────────────────────────────────────────
class LessonListViewModel(
    private val lessonRepository: LessonRepository,
    private val prePostRepository: PrePostRepository // 👈 Injected here
) : ViewModel() {

    data class LessonListUiState(
        val lessons: List<Lesson> = emptyList(),
        val quarterTitle: String = "",
        val hasPrePostContent: Boolean = false,
        val isPreTestDone: Boolean = false,
        val isPostTestDone: Boolean = false,
        val allLessonsDone: Boolean = false,
        val isLoading: Boolean = true
    )

    private val _uiState = MutableStateFlow(LessonListUiState())
    val uiState: StateFlow<LessonListUiState> = _uiState.asStateFlow()

    fun loadLessons(quarterId: String, studentId: String, quarterTitle: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(quarterTitle = quarterTitle) }

            val hasContent = prePostRepository.hasTestContent(quarterId, "PRE")

            combine(
                lessonRepository.getLessonsWithStatus(quarterId, studentId),
                prePostRepository.getAllResultsForStudent(studentId)
            ) { lessons, testResults ->
                val preDone = testResults.any { it.quarterId == quarterId && it.testType == "PRE" }
                val postDone = testResults.any { it.quarterId == quarterId && it.testType == "POST" }

                val preTestCleared = !hasContent || preDone
                val allLessonsDone = lessons.isNotEmpty() && lessons.all { it.isDone }

                val finalLessons = if (!preTestCleared) {
                    lessons.map { it.copy(status = LessonStatus.LOCKED) }
                } else {
                    lessons
                }

                LessonListUiState(
                    lessons = finalLessons,
                    quarterTitle = quarterTitle,
                    hasPrePostContent = hasContent,
                    isPreTestDone = preDone,
                    isPostTestDone = postDone,
                    allLessonsDone = allLessonsDone,
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }
}