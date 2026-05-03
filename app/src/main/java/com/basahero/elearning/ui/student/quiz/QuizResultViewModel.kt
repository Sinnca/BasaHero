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
        val isSyncing: Boolean = false,
        // Quarter-complete celebration fields
        val isQuarterComplete: Boolean = false,
        val quarterTitle: String = "",
        val quarterLessonsCompleted: Int = 0,
        val quarterAverageScore: Float = 0f
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

            // ── Step 3: Find next lesson + check quarter completion ────────────
            val currentLesson = lessonRepository.getLessonById(lessonId)
            if (currentLesson != null) {
                val lessons = lessonRepository
                    .getLessonsWithStatus(currentLesson.quarterId, studentId)
                    .first()

                val currentIndex = lessons.indexOfFirst { it.id == lessonId }
                val nextLesson = lessons.getOrNull(currentIndex + 1)

                // Check whether all lessons in this quarter are now complete
                val allDone = lessons.all { it.isDone }
                val completedCount = lessons.count { it.isDone }
                val averageScore = if (completedCount > 0) score.toFloat() / total else 0f

                _uiState.update {
                    it.copy(
                        isSaved = true,
                        nextLessonId = nextLesson?.id,
                        isQuarterComplete = allDone,
                        quarterTitle = "Quarter ${currentLesson.quarterId}",
                        quarterLessonsCompleted = completedCount,
                        quarterAverageScore = averageScore
                    )
                }
            } else {
                _uiState.update { it.copy(isSaved = true) }
            }
        }
    }
}