package com.basahero.elearning.ui.teacher.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basahero.elearning.data.repository.PrePostComparison
import com.basahero.elearning.data.repository.ProgressMonitorRepository
import com.basahero.elearning.data.repository.StudentProgressSummary
import com.basahero.elearning.data.repository.LessonRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudentProgressUiState(
    val isLoading: Boolean = false,
    val progressList: List<StudentProgressSummary> = emptyList(),
    val prePostList: List<PrePostComparison> = emptyList(),
    val errorMessage: String? = null,
    val selectedTab: Int = 0   // 0 = Lessons, 1 = Pre/Post
)

class StudentProgressViewModel(
    private val progressMonitorRepository: ProgressMonitorRepository,
    private val lessonRepository: LessonRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentProgressUiState())
    val uiState: StateFlow<StudentProgressUiState> = _uiState.asStateFlow()

    fun loadStudentProgress(studentId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Load both in parallel
                val progressDeferred  = async { progressMonitorRepository.getStudentProgress(studentId) }
                val prePostDeferred   = async { progressMonitorRepository.getStudentPrePostTests(studentId) }

                val progress = progressDeferred.await()
                val prePost  = prePostDeferred.await()

                val resolvedProgress = progress.map { p ->
                    val actualLesson = lessonRepository.getLessonById(p.lessonId)
                    p.copy(
                        lessonTitle = actualLesson?.title ?: p.lessonId,
                        competency = actualLesson?.competency ?: ""
                    )
                }

                val resolvedPrePost = prePost.map { p ->
                    val actualQuarter = lessonRepository.getQuarterById(p.quarterId)
                    p.copy(quarterTitle = actualQuarter?.title ?: p.quarterId)
                }

                _uiState.update {
                    it.copy(
                        isLoading    = false,
                        progressList = resolvedProgress,
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
}
