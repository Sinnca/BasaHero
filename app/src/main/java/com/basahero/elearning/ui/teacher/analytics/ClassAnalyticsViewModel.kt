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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuarterAnalyticsSummary(
    val quarterId: String,
    val quarterTitle: String,
    val isExpanded: Boolean = false,
    val lessons: List<LessonPerformance>
)

enum class AnalyticsSortOrder { SCORE_ASC, SCORE_DESC, NAME_ASC }

data class ClassAnalyticsUiState(
    val isLoading: Boolean = false,
    val quarterPerformanceList: List<QuarterAnalyticsSummary> = emptyList(),
    val atRiskStudents: List<StudentInfo> = emptyList(),
    val errorMessage: String? = null,
    val selectedTab: Int = 0,                           // 0 = Lessons, 1 = Students
    val sortOrder: AnalyticsSortOrder = AnalyticsSortOrder.SCORE_ASC
)

class ClassAnalyticsViewModel(
    private val repo: ProgressMonitorRepository,
    private val lessonRepo: LessonRepository,
    private val gradeLevel: Int
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
                
                val allQuarters = lessonRepo.getQuartersWithProgress(gradeLevel, "").first()
                
                // Map lessonId to actual lesson title and get quarterId
                val resolvedLessons = lessons.map { lesson ->
                    val actualLesson = lessonRepo.getLessonById(lesson.lessonId)
                    val quarterId = actualLesson?.quarterId ?: "unknown"
                    val quarterTitle = lessonRepo.getQuarterById(quarterId)?.title ?: "Unknown Quarter"
                    
                    object {
                        val performance = lesson.copy(lessonTitle = actualLesson?.title ?: lesson.lessonId)
                        val qId = quarterId
                        val qTitle = quarterTitle
                    }
                }
                
                val grouped = resolvedLessons.groupBy { it.qId }
                
                val quarterList = allQuarters.map { q ->
                    val quarterLessons = grouped[q.id]?.map { it.performance } ?: emptyList()
                    QuarterAnalyticsSummary(
                        quarterId = q.id,
                        quarterTitle = q.title,
                        isExpanded = false,
                        lessons = quarterLessons
                    )
                }.sortedBy { it.quarterId }

                _uiState.update {
                    it.copy(
                        isLoading        = false,
                        quarterPerformanceList = quarterList,
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

    fun toggleQuarter(quarterId: String) {
        _uiState.update { state ->
            val updatedList = state.quarterPerformanceList.map { q ->
                if (q.quarterId == quarterId) q.copy(isExpanded = !q.isExpanded) else q
            }
            state.copy(quarterPerformanceList = updatedList)
        }
    }

    // Sorted lesson list for a specific quarter — memoised by sort order
    fun sortedLessons(lessons: List<LessonPerformance>, state: ClassAnalyticsUiState): List<LessonPerformance> =
        when (state.sortOrder) {
            AnalyticsSortOrder.SCORE_ASC  -> lessons.sortedBy  { it.averageScore }
            AnalyticsSortOrder.SCORE_DESC -> lessons.sortedByDescending { it.averageScore }
            AnalyticsSortOrder.NAME_ASC   -> lessons.sortedBy  { it.lessonTitle }
        }
}
