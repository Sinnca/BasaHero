package com.basahero.elearning.ui.teacher.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basahero.elearning.data.repository.LessonPerformance
import com.basahero.elearning.data.repository.ProgressMonitorRepository
import com.basahero.elearning.data.repository.StudentInfo
import com.basahero.elearning.data.repository.LessonRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AnalyticsSortOrder { SCORE_ASC, SCORE_DESC, NAME_ASC }

data class ClassAnalyticsUiState(
    val isLoading: Boolean = false,
    val lessonPerformance: List<LessonPerformance> = emptyList(),
    val atRiskStudents: List<StudentInfo> = emptyList(),
    val errorMessage: String? = null,
    val selectedTab: Int = 0,                           // 0 = Lessons, 1 = Students
    val sortOrder: AnalyticsSortOrder = AnalyticsSortOrder.SCORE_ASC
)

class ClassAnalyticsViewModel(
    private val repo: ProgressMonitorRepository,
    private val lessonRepo: LessonRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClassAnalyticsUiState())
    val uiState: StateFlow<ClassAnalyticsUiState> = _uiState.asStateFlow()

    fun load(classId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val lessonsDeferred   = async { repo.getLessonPerformance(classId) }
                val atRiskDeferred    = async { repo.getAtRiskStudents(classId) }

                val lessons  = lessonsDeferred.await()
                val atRisk   = atRiskDeferred.await()
                
                // Map lessonId to actual lesson title using LessonRepository
                val resolvedLessons = lessons.map { lesson ->
                    val actualLesson = lessonRepo.getLessonById(lesson.lessonId)
                    lesson.copy(lessonTitle = actualLesson?.title ?: lesson.lessonId)
                }

                _uiState.update {
                    it.copy(
                        isLoading        = false,
                        lessonPerformance = resolvedLessons,
                        atRiskStudents    = atRisk
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun selectTab(tab: Int) = _uiState.update { it.copy(selectedTab = tab) }

    fun setSortOrder(order: AnalyticsSortOrder) = _uiState.update { it.copy(sortOrder = order) }

    // Sorted lesson list — memoised by sort order
    fun sortedLessons(state: ClassAnalyticsUiState): List<LessonPerformance> =
        when (state.sortOrder) {
            AnalyticsSortOrder.SCORE_ASC  -> state.lessonPerformance.sortedBy  { it.averageScore }
            AnalyticsSortOrder.SCORE_DESC -> state.lessonPerformance.sortedByDescending { it.averageScore }
            AnalyticsSortOrder.NAME_ASC   -> state.lessonPerformance.sortedBy  { it.lessonTitle }
        }
}
