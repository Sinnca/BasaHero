package com.basahero.elearning.ui.teacher.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basahero.elearning.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TeacherDashboardViewModel(
    private val authRepository: TeacherAuthRepository,
    private val classRepository: ClassRepository
) : ViewModel() {

    data class DashboardUiState(
        val teacher: TeacherProfile? = null,
        val classes: List<ClassInfo> = emptyList(),
        val selectedSchoolYear: String = "2025-2026",
        val isLoading: Boolean = true,
        val showCreateClassDialog: Boolean = false,
        val errorMessage: String? = null
    )

    val schoolYears = listOf("2025-2026", "2026-2027", "2027-2028")

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val teacher = authRepository.getCurrentTeacher()
            if (teacher != null) {
                val classes = classRepository.getClassesForTeacher(teacher.id)
                _uiState.value = DashboardUiState(
                    teacher = teacher,
                    classes = classes,
                    isLoading = false
                )
            } else {
                _uiState.value = DashboardUiState(isLoading = false, errorMessage = "Session expired. Please log in again.")
            }
        }
    }

    fun selectSchoolYear(year: String) {
        _uiState.update { it.copy(selectedSchoolYear = year) }
    }

    fun showCreateDialog() { _uiState.update { it.copy(showCreateClassDialog = true) } }
    fun hideCreateDialog() { _uiState.update { it.copy(showCreateClassDialog = false) } }

    fun createClass(name: String, gradeLevel: Int) {
        val teacherId = _uiState.value.teacher?.id ?: return
        val schoolYear = _uiState.value.selectedSchoolYear
        viewModelScope.launch {
            val result = classRepository.createClass(teacherId, name, gradeLevel, schoolYear)
            result.fold(
                onSuccess = { newClass ->
                    _uiState.update { state ->
                        state.copy(
                            classes = state.classes + newClass,
                            showCreateClassDialog = false
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = "Failed to create class: ${e.message}") }
                }
            )
        }
    }

    fun signOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onComplete()
        }
    }
}
