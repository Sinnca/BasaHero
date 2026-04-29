package com.basahero.elearning.ui.student.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basahero.elearning.data.model.QuizQuestion
import com.basahero.elearning.data.model.QuizResult
import com.basahero.elearning.data.repository.QuizRepository // 👇 USING YOUR REPO
import com.basahero.elearning.domain.QuizScoringUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class QuizViewModel(
    private val quizRepository: QuizRepository, // 👇 CHANGED HERE
    private val scoringUseCase: QuizScoringUseCase
) : ViewModel() {

    data class QuizUiState(
        val questions: List<QuizQuestion> = emptyList(),
        val currentIndex: Int = 0,
        val answers: MutableMap<String, QuizScoringUseCase.StudentAnswer> = mutableMapOf(),
        val isLoading: Boolean = true,
        val isSubmitted: Boolean = false,
        val result: QuizResult? = null
    ) {
        val currentQuestion get() = questions.getOrNull(currentIndex)
        val isLastQuestion get() = currentIndex == questions.size - 1
        val progressFraction get() = if (questions.isEmpty()) 0f else (currentIndex + 1).toFloat() / questions.size
    }

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var lessonTitle = ""

    fun loadQuiz(lessonId: String, title: String) {
        lessonTitle = title
        viewModelScope.launch {
            // 👇 USING YOUR EXACT REPOSITORY FUNCTION
            val questions = quizRepository.getQuizForLesson(lessonId)
            _uiState.update { it.copy(questions = questions, isLoading = false) }
        }
    }

    fun answerQuestion(questionId: String, answer: QuizScoringUseCase.StudentAnswer) {
        _uiState.update {
            val newAnswers = it.answers.toMutableMap()
            newAnswers[questionId] = answer
            it.copy(answers = newAnswers)
        }
    }

    fun nextQuestion() {
        _uiState.update {
            it.copy(currentIndex = (it.currentIndex + 1).coerceAtMost(it.questions.size - 1))
        }
    }

    fun previousQuestion() {
        _uiState.update {
            it.copy(currentIndex = (it.currentIndex - 1).coerceAtLeast(0))
        }
    }

    fun submitQuiz(lessonId: String) {
        val state = _uiState.value
        val result = scoringUseCase.calculate(
            lessonId = lessonId,
            lessonTitle = lessonTitle,
            questions = state.questions,
            studentAnswers = state.answers
        )
        _uiState.update { it.copy(isSubmitted = true, result = result) }
    }
}