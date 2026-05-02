//package com.basahero.elearning.ui.student.login
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.basahero.elearning.data.model.Student
//import com.basahero.elearning.data.repository.StudentRepository
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.launch
//
//// ─────────────────────────────────────────────────────────────────────────────
//// StudentLoginViewModel
//// ─────────────────────────────────────────────────────────────────────────────
//class StudentLoginViewModel(
//    private val studentRepository: StudentRepository
//) : ViewModel() {
//
//    sealed class LoginState {
//        object Idle : LoginState()
//        object Loading : LoginState()
//        data class Success(val student: Student) : LoginState()
//        data class Error(val message: String) : LoginState()
//    }
//
//    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
//    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()
//
//    fun login(fullName: String, section: String) {
//        if (fullName.isBlank()) {
//            _loginState.value = LoginState.Error("Please enter your name.")
//            return
//        }
//        if (section.isBlank()) {
//            _loginState.value = LoginState.Error("Please enter your section.")
//            return
//        }
//
//        viewModelScope.launch {
//            _loginState.value = LoginState.Loading
//            val student = studentRepository.loginStudent(
//                fullName = fullName.trim(),
//                section = section.trim()
//            )
//            if (student != null) {
//                studentRepository.updateLastActive(student.id)
//                _loginState.value = LoginState.Success(student)
//            } else {
//                _loginState.value = LoginState.Error(
//                    "Student not found. Check your name and section, or ask your teacher."
//                )
//            }
//        }
//    }
//
//    fun resetState() {
//        _loginState.value = LoginState.Idle
//    }
//}
package com.basahero.elearning.ui.student.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basahero.elearning.data.model.Student
import com.basahero.elearning.data.repository.StudentRepository
import com.basahero.elearning.data.local.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// StudentLoginViewModel
// ─────────────────────────────────────────────────────────────────────────────
class StudentLoginViewModel(
    private val studentRepository: StudentRepository,
    private val sessionManager: SessionManager // 👇 ADDED SESSION MANAGER
) : ViewModel() {

    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        data class Success(val student: Student) : LoginState()
        data class Error(val message: String) : LoginState()
    }

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    fun login(fullName: String, section: String) {
        if (fullName.isBlank()) {
            _loginState.value = LoginState.Error("Please enter your name.")
            return
        }
        if (section.isBlank()) {
            _loginState.value = LoginState.Error("Please enter your section.")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val student = studentRepository.loginStudent(
                fullName = fullName.trim(),
                section = section.trim()
            )
            if (student != null) {
                studentRepository.updateLastActive(student.id)

                // 👇 NEW: Save session to DataStore!
                sessionManager.saveStudentSession(
                    studentId = student.id,
                    studentName = student.fullName,
                    gradeLevel = student.gradeLevel,
                    section = student.section
                )

                _loginState.value = LoginState.Success(student)
            } else {
                _loginState.value = LoginState.Error(
                    "Student not found. Check your name and section, or ask your teacher."
                )
            }
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}