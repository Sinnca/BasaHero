package com.basahero.elearning.ui.student.lessons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basahero.elearning.data.model.Lesson
import com.basahero.elearning.data.repository.LessonRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.basahero.elearning.data.local.SessionManager
import com.basahero.elearning.data.repository.PronunciationRepository
import com.basahero.elearning.data.repository.ProgressRepository

// 🚀 NEW DATA CLASS for Step 6
data class HighlightedWord(
    val word: String,
    val start: Int,
    val end: Int
)

class ReadingViewModel(
    private val lessonRepository: LessonRepository,
    private val pronunciationRepository: PronunciationRepository,
    private val progressRepository: ProgressRepository,
    private val sessionManager: SessionManager
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
                    val startIndex = l.passageText.indexOf(word, ignoreCase = true)
                    if (startIndex >= 0) {
                        highlights.add(HighlightedWord(word, startIndex, startIndex + word.length))
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

    fun savePronunciationAttempt(word: String, heard: String, isCorrect: Boolean, score: Int) {
        val lessonId = _uiState.value.lesson?.id ?: return
        
        viewModelScope.launch {
            val studentId = sessionManager.studentSession.first()?.studentId ?: return@launch
            
            pronunciationRepository.savePronunciationAttempt(
                studentId = studentId,
                lessonId = lessonId,
                word = word,
                heard = heard,
                isCorrect = isCorrect,
                score = score
            )
        }
    }

    fun saveLessonProgress(context: android.content.Context, totalCorrect: Int, totalQuestions: Int) {
        val lessonId = _uiState.value.lesson?.id ?: return
        
        viewModelScope.launch {
            val studentId = sessionManager.studentSession.first()?.studentId ?: return@launch
            
            // To ensure the lesson is marked as DONE even if totalQuestions is 0,
            // we'll pass totalCorrect = 1, totalQuestions = 1 if there were no questions.
            val safeScore = if (totalQuestions == 0) 1 else totalCorrect
            val safeTotal = if (totalQuestions == 0) 1 else totalQuestions

            progressRepository.saveQuizResult(
                studentId = studentId,
                lessonId = lessonId,
                score = safeScore,
                total = safeTotal
            )
            
            // ── Trigger WorkManager sync to push progress to Supabase ────────
            com.basahero.elearning.domain.SyncProgressUseCase().execute(context)
        }
    }
}