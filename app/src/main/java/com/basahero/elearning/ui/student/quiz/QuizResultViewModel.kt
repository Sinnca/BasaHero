package com.basahero.elearning.ui.student.quiz

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basahero.elearning.data.repository.LessonRepository
import com.basahero.elearning.data.repository.ProgressRepository
import com.basahero.elearning.domain.SyncProgressUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// QuizResultViewModel
// Saves to Room → triggers WorkManager sync → finds next lesson
// ─────────────────────────────────────────────────────────────────────────────
class QuizResultViewModel(
    private val progressRepository: ProgressRepository,
    private val lessonRepository: LessonRepository
) : ViewModel() {

    data class ResultUiState(
        val isSaved: Boolean = false,
        val nextLessonId: String? = null,
        val isSyncing: Boolean = false
    )

    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    fun saveResult(
        context: Context,
        studentId: String,
        lessonId: String,
        score: Int,
        total: Int
    ) {
        viewModelScope.launch {
            // ── Step 1: Save quiz result to Room (instant, offline-safe) ──────
            progressRepository.saveQuizResult(
                studentId = studentId,
                lessonId = lessonId,
                score = score,
                total = total
            )

            // ── Step 2: Trigger WorkManager sync (with jitter + retry) ────────
            SyncProgressUseCase().execute(context)
            _uiState.update { it.copy(isSyncing = true) }

            // ── Step 3: Find next lesson to show "Next Lesson" button ─────────
            val currentLesson = lessonRepository.getLessonById(lessonId)
            if (currentLesson != null) {
                val lessons = lessonRepository
                    .getLessonsWithStatus(currentLesson.quarterId, studentId)
                    .first()

                val currentIndex = lessons.indexOfFirst { it.id == lessonId }
                val nextLesson = lessons.getOrNull(currentIndex + 1)

                _uiState.update {
                    it.copy(
                        isSaved = true,
                        nextLessonId = nextLesson?.id
                    )
                }
            } else {
                _uiState.update { it.copy(isSaved = true) }
            }
        }
    }
}