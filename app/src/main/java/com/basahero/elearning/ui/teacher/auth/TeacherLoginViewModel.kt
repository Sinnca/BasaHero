package com.basahero.elearning.ui.teacher.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basahero.elearning.data.local.SessionManager
import com.basahero.elearning.data.repository.TeacherAuthRepository
import com.basahero.elearning.data.repository.TeacherProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TeacherLoginViewModel(
    private val authRepository: TeacherAuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Success(val teacher: TeacherProfile) : AuthState()
        data class Error(val message: String) : AuthState()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    var isSignUpMode by mutableStateOf(false)
        private set

    fun toggleMode() {
        isSignUpMode = !isSignUpMode
        _authState.value = AuthState.Idle
    }

    fun signIn(email: String, password: String) {
        if (!validateInputs(email, password)) return
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signIn(email.trim(), password)
            result.fold(
                onSuccess = { teacher ->
                    sessionManager.setTeacherLoggedIn(true)
                    _authState.value = AuthState.Success(teacher)
                },
                onFailure = { e ->
                    _authState.value = AuthState.Error(
                        when {
                            e.message?.contains("Invalid login") == true ->
                                "Incorrect email or password. Please try again."
                            e.message?.contains("network") == true ->
                                "No internet connection. Please check your Wi-Fi."
                            else -> "Login failed. Please try again."
                        }
                    )
                }
            )
        }
    }

    fun signUp(email: String, password: String, fullName: String) {
        if (fullName.isBlank()) {
            _authState.value = AuthState.Error("Please enter your full name.")
            return
        }
        if (!validateInputs(email, password)) return
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signUp(email.trim(), password, fullName.trim())
            result.fold(
                onSuccess = { teacher ->
                    sessionManager.setTeacherLoggedIn(true)
                    _authState.value = AuthState.Success(teacher)
                },
                onFailure = { e ->
                    _authState.value = AuthState.Error(
                        if (e.message?.contains("already registered") == true)
                            "This email is already registered. Please sign in instead."
                        else "Registration failed: ${e.localizedMessage ?: e.message}"
                    )
                }
            )
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        return when {
            email.isBlank() -> {
                _authState.value = AuthState.Error("Please enter your email address.")
                false
            }
            !email.contains("@") -> {
                _authState.value = AuthState.Error("Please enter a valid email address.")
                false
            }
            password.length < 6 -> {
                _authState.value = AuthState.Error("Password must be at least 6 characters.")
                false
            }
            else -> true
        }
    }

    fun resetState() { _authState.value = AuthState.Idle }
}
