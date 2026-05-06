package com.basahero.elearning.ui.student.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basahero.elearning.data.model.Student
import com.basahero.elearning.data.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StudentSelectionViewModel(
    private val studentRepository: StudentRepository
) : ViewModel() {

    data class SelectionState(
        val isLoading: Boolean = false,
        val students: List<Student> = emptyList(),
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(SelectionState())
    val uiState: StateFlow<SelectionState> = _uiState.asStateFlow()

    fun loadStudents(gradeLevel: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val students = studentRepository.getStudentsByGrade(gradeLevel)
            
            if (students.isEmpty()) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        students = emptyList(),
                        errorMessage = "No students found for Grade $gradeLevel."
                    )
                }
            } else {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        students = students,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun loginStudent(student: Student, onLoginSuccess: (Student) -> Unit) {
        viewModelScope.launch {
            studentRepository.updateLastActive(student.id)
            onLoginSuccess(student)
        }
    }
}
