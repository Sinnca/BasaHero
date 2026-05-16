package com.basahero.elearning.ui.teacher.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basahero.elearning.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TeacherDashboardViewModel(
    private val authRepository: TeacherAuthRepository,
    private val classRepository: ClassRepository,
    private val progressRepository: ProgressMonitorRepository
) : ViewModel() {

    data class ActivityItem(
        val title: String,
        val desc: String,
        val time: String,
        val type: ActivityType
    )

    enum class ActivityType {
        PERFORMANCE, STUDENT_JOINED, GAME_ENDED, CLASS_CREATED
    }

    data class DashboardUiState(
        val teacher: TeacherProfile? = null,
        val classes: List<ClassInfo> = emptyList(),
        val activities: List<ActivityItem> = emptyList(),
        val selectedSchoolYear: String = "2025-2026",
        val isLoading: Boolean = true,
        val showCreateClassDialog: Boolean = false,
        val errorMessage: String? = null,
        
        // Student Directory State
        val directoryStudents: List<StudentInfo> = emptyList(),
        val directoryGradeFilter: Int = 4, // Default to Grade 4
        val isDirectoryLoading: Boolean = false
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
                val classIds = classes.map { it.id }
                
                // Fetch Real Activities
                val recentActivities = mutableListOf<ActivityItem>()
                
                if (classIds.isNotEmpty()) {
                    // 1. Get recent students (Enrollments)
                    val recentStudents = classRepository.getStudentsForTeacherByGrade(teacher.id, 4) + 
                                       classRepository.getStudentsForTeacherByGrade(teacher.id, 5)
                    
                    val sortedStudents = recentStudents.filter { it.lastActive != null }
                        .sortedByDescending { it.lastActive }
                        .take(5)
                        
                    sortedStudents.forEach { s ->
                        val className = classes.find { it.id == s.classId }?.name ?: "Section"
                        recentActivities.add(ActivityItem(
                            title = "New Learner in $className",
                            desc = "${s.fullName} has joined the class.",
                            time = formatTimeAgo(s.lastActive),
                            type = ActivityType.STUDENT_JOINED
                        ))
                    }
                    
                    // 2. Get recent performance (Progress)
                    // For simplicity, we fetch progress for a subset of students
                    val allRecentProgress = mutableListOf<StudentProgressSummary>()
                    recentStudents.take(20).forEach { student ->
                        val progress = progressRepository.getStudentProgress(student.id)
                        allRecentProgress.addAll(progress)
                    }
                    
                    val sortedProgress = allRecentProgress.sortedByDescending { it.latestScore }.take(5)
                    sortedProgress.forEach { p ->
                        val studentName = recentStudents.find { it.id == p.studentId }?.fullName ?: "A student"
                        val statusDesc = if (p.bestPercent >= 0.8f) "Excellent performance!" else "Completed a lesson."
                        recentActivities.add(ActivityItem(
                            title = "Performance: $studentName",
                            desc = "$statusDesc Score: ${p.bestScore}/${p.quizTotal}",
                            time = "Recently",
                            type = ActivityType.PERFORMANCE
                        ))
                    }
                }
                
                val finalActivities = recentActivities.sortedBy { it.time }.take(10)

                _uiState.update { 
                    it.copy(
                        teacher = teacher,
                        classes = classes,
                        activities = finalActivities,
                        isLoading = false
                    ) 
                }
                // Automatically load students for the default directory grade
                loadDirectoryStudents(teacher.id, _uiState.value.directoryGradeFilter)
            } else {
                _uiState.value = DashboardUiState(isLoading = false, errorMessage = "Session expired. Please log in again.")
            }
        }
    }

    fun setDirectoryGradeFilter(gradeLevel: Int) {
        val teacherId = _uiState.value.teacher?.id ?: return
        _uiState.update { it.copy(directoryGradeFilter = gradeLevel) }
        loadDirectoryStudents(teacherId, gradeLevel)
    }

    private fun loadDirectoryStudents(teacherId: String, gradeLevel: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDirectoryLoading = true) }
            val students = classRepository.getStudentsForTeacherByGrade(teacherId, gradeLevel)
            _uiState.update { 
                it.copy(
                    directoryStudents = students,
                    isDirectoryLoading = false
                ) 
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

    private fun formatTimeAgo(timestamp: String?): String {
        if (timestamp == null) return "Unknown"
        // Simple mock for now, ideally use a real relative time library
        return "1h ago" 
    }
}
