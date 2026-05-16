package com.basahero.elearning.ui.teacher.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basahero.elearning.data.repository.PrePostComparison
import com.basahero.elearning.data.repository.ProgressMonitorRepository
import com.basahero.elearning.data.repository.StudentProgressSummary
import com.basahero.elearning.data.repository.LessonRepository
import com.basahero.elearning.data.repository.StudentRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuarterProgressSummary(
    val quarterId: String,
    val quarterTitle: String,
    val isExpanded: Boolean = false,
    val lessons: List<StudentProgressSummary>
)

data class StudentProgressUiState(
    val isLoading: Boolean = false,
    val quarterProgressList: List<QuarterProgressSummary> = emptyList(),
    val prePostList: List<PrePostComparison> = emptyList(),
    val errorMessage: String? = null,
    val selectedTab: Int = 0   // 0 = Lessons, 1 = Pre/Post
)

class StudentProgressViewModel(
    private val progressMonitorRepository: ProgressMonitorRepository,
    private val lessonRepository: LessonRepository,
    private val studentRepository: StudentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentProgressUiState())
    val uiState: StateFlow<StudentProgressUiState> = _uiState.asStateFlow()

    fun loadStudentProgress(studentId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Load data in parallel
                val studentDeferred   = async { studentRepository.getStudentById(studentId) }
                val progressDeferred  = async { progressMonitorRepository.getStudentProgress(studentId) }
                val prePostDeferred   = async { progressMonitorRepository.getStudentPrePostTests(studentId) }

                val student  = studentDeferred.await()
                val progress = progressDeferred.await()
                val prePost  = prePostDeferred.await()

                val gradeLevel = student?.gradeLevel ?: 4
                val allQuarters = lessonRepository.getQuartersWithProgress(gradeLevel, studentId).first()

                val resolvedProgress = progress.map { p ->
                    val actualLesson = lessonRepository.getLessonById(p.lessonId)
                    val quarterId = actualLesson?.quarterId ?: "unknown"
                    val quarterTitle = lessonRepository.getQuarterById(quarterId)?.title ?: "Unknown Quarter"
                    
                    // Attach quarter info to a temporary structure for grouping
                    object {
                        val summary = p.copy(
                            lessonTitle = actualLesson?.title ?: p.lessonId,
                            competency = actualLesson?.competency ?: ""
                        )
                        val qId = quarterId
                        val qTitle = quarterTitle
                    }
                }

                // Group existing progress by quarterId
                val grouped = resolvedProgress.groupBy { it.qId }
                
                // Map ALL quarters for the grade level so they always appear
                val quarterList = allQuarters.map { q ->
                    val quarterLessons = grouped[q.id]?.map { it.summary } ?: emptyList()
                    QuarterProgressSummary(
                        quarterId = q.id,
                        quarterTitle = q.title,
                        isExpanded = false,
                        lessons = quarterLessons
                    )
                }.sortedBy { it.quarterId }

                val resolvedPrePost = prePost.map { p ->
                    val actualQuarter = lessonRepository.getQuarterById(p.quarterId)
                    p.copy(quarterTitle = actualQuarter?.title ?: p.quarterId)
                }

                _uiState.update {
                    it.copy(
                        isLoading    = false,
                        quarterProgressList = quarterList,
                        prePostList  = resolvedPrePost
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading    = false,
                        errorMessage = "Failed to load progress: ${e.message}"
                    )
                }
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun toggleQuarter(quarterId: String) {
        _uiState.update { state ->
            val updatedList = state.quarterProgressList.map { quarter ->
                if (quarter.quarterId == quarterId) {
                    quarter.copy(isExpanded = !quarter.isExpanded)
                } else {
                    quarter
                }
            }
            state.copy(quarterProgressList = updatedList)
        }
    }
}
