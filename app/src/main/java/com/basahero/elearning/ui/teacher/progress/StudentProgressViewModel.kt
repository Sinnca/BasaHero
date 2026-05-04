package com.basahero.elearning.ui.teacher.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basahero.elearning.data.repository.ProgressMonitorRepository
import com.basahero.elearning.data.repository.StudentProgressSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudentProgressUiState(
    val isLoading: Boolean = false,
    val progressList: List<StudentProgressSummary> = emptyList(),
    val errorMessage: String? = null
)

class StudentProgressViewModel(
    private val progressMonitorRepository: ProgressMonitorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentProgressUiState())
    val uiState: StateFlow<StudentProgressUiState> = _uiState.asStateFlow()

    fun loadStudentProgress(studentId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val progress = progressMonitorRepository.getStudentProgress(studentId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        progressList = progress
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load progress: ${e.message}"
                    )
                }
            }
        }
    }
}
