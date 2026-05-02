package com.basahero.elearning.ui.student.lessons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basahero.elearning.data.model.Lesson
import com.basahero.elearning.data.repository.LessonRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// 🚀 NEW DATA CLASS for Step 6
data class HighlightedWord(
    val word: String,
    val start: Int,
    val end: Int
)

class ReadingViewModel(
    private val lessonRepository: LessonRepository
) : ViewModel() {

    data class ReadingUiState(
        val lesson: Lesson? = null,
        val highlightedWords: List<HighlightedWord> = emptyList(), // 👈 NEW
        val isLoading: Boolean = true,
        val readingComplete: Boolean = false
    )

    private val _uiState = MutableStateFlow(ReadingUiState())
    val uiState: StateFlow<ReadingUiState> = _uiState.asStateFlow()

    fun loadLesson(lessonId: String) {
        viewModelScope.launch {
            val lesson = lessonRepository.getLessonById(lessonId)

            /// 🚀 STEP 6 LOGIC: Find the words in the text
            val highlights = mutableListOf<HighlightedWord>()
            lesson?.let { l ->
                // Clean the string (remove brackets and quotes if it's JSON)
                val rawWords = l.highlightedWords
                    .replace("[", "")
                    .replace("]", "")
                    .replace("\"", "")

                val wordsToFind = rawWords.split(",").map { it.trim() }.filter { it.isNotBlank() }

                wordsToFind.forEach { word ->
                    var startIndex = l.passageText.indexOf(word, ignoreCase = true)
                    while (startIndex >= 0) {
                        highlights.add(HighlightedWord(word, startIndex, startIndex + word.length))
                        startIndex = l.passageText.indexOf(word, startIndex + 1, ignoreCase = true)
                    }
                }
            }

            _uiState.value = ReadingUiState(
                lesson = lesson,
                highlightedWords = highlights,
                isLoading = false
            )
        }
    }

    fun onScrolledToBottom() {
        _uiState.update { it.copy(readingComplete = true) }
    }
}