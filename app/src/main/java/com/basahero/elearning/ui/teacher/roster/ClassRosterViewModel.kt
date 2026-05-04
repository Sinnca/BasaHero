package com.basahero.elearning.ui.teacher.roster

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basahero.elearning.data.repository.*
import com.opencsv.CSVReaderBuilder
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStreamReader

class ClassRosterViewModel(
    private val classRepository: ClassRepository
) : ViewModel() {

    data class RosterUiState(
        val students: List<StudentInfo> = emptyList(),
        val isLoading: Boolean = true,
        val currentPage: Int = 0,
        val hasMore: Boolean = true,
        val isLoadingMore: Boolean = false,
        val showAddDialog: Boolean = false,
        val showImportResult: Boolean = false,
        val importedCount: Int = 0,
        val errorMessage: String? = null,
        val searchQuery: String = ""
    )

    private val _uiState = MutableStateFlow(RosterUiState())
    val uiState: StateFlow<RosterUiState> = _uiState.asStateFlow()

    private var classId = ""
    private var gradeLevel = 4

    fun loadRoster(classId: String, gradeLevel: Int) {
        this.classId = classId
        this.gradeLevel = gradeLevel
        viewModelScope.launch {
            val students = classRepository.getStudentsForClass(classId, page = 0)
            _uiState.value = RosterUiState(
                students = students,
                isLoading = false,
                hasMore = students.size == 20
            )
        }
    }

    fun loadNextPage() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val nextPage = _uiState.value.currentPage + 1
            val more = classRepository.getStudentsForClass(classId, nextPage)
            _uiState.update { state ->
                state.copy(
                    students = state.students + more,
                    currentPage = nextPage,
                    hasMore = more.size == 20,
                    isLoadingMore = false
                )
            }
        }
    }

    fun onSearchChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    val filteredStudents: List<StudentInfo>
        get() {
            val q = _uiState.value.searchQuery.lowercase().trim()
            return if (q.isEmpty()) _uiState.value.students
            else _uiState.value.students.filter {
                it.fullName.lowercase().contains(q) || it.section.lowercase().contains(q)
            }
        }

    fun showAddDialog() { _uiState.update { it.copy(showAddDialog = true) } }
    fun hideAddDialog() { _uiState.update { it.copy(showAddDialog = false) } }

    fun addStudent(fullName: String, section: String) {
        viewModelScope.launch {
            val result = classRepository.addStudent(classId, fullName, section, gradeLevel)
            result.fold(
                onSuccess = { student ->
                    _uiState.update { state ->
                        state.copy(
                            students = listOf(student) + state.students,
                            showAddDialog = false
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(errorMessage = "Failed to add student: ${e.message}") }
                }
            )
        }
    }

    fun importFromCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val reader = CSVReaderBuilder(InputStreamReader(inputStream))
                    .withSkipLines(1)           // Skip header row
                    .build()

                val csvRows = mutableListOf<CsvStudentRow>()
                var line: Array<String>?

                while (reader.readNext().also { line = it } != null) {
                    val row = line!!
                    if (row.size >= 2) {
                        val fullName = row[0].trim()
                        val section = row[1].trim()
                        if (fullName.isNotEmpty() && section.isNotEmpty()) {
                            csvRows.add(CsvStudentRow(fullName, section))
                        }
                    }
                }
                reader.close()

                val result = classRepository.importStudentsFromCsv(
                    classId = classId.toIntOrNull() ?: 0,
                    gradeLevel = gradeLevel,
                    rows = csvRows
                )

                result.fold(
                    onSuccess = { count ->
                        // Reload roster to show imported students
                        loadRoster(classId, gradeLevel)
                        _uiState.update { it.copy(showImportResult = true, importedCount = count) }
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(errorMessage = "CSV import failed: ${e.message}") }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Could not read CSV file: ${e.message}") }
            }
        }
    }

    fun dismissImportResult() { _uiState.update { it.copy(showImportResult = false) } }
    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }
}