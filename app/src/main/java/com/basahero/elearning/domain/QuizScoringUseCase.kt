package com.basahero.elearning.domain

import com.basahero.elearning.data.model.*
import com.basahero.elearning.data.repository.LessonRepository
import com.basahero.elearning.data.repository.ProgressRepository

// ─────────────────────────────────────────────────────────────────────────────
// QuizScoringUseCase
// Calculates score from student answers using points_value per question.
// Returns a full QuizResult with per-question breakdown.
// ─────────────────────────────────────────────────────────────────────────────
class QuizScoringUseCase {

    data class StudentAnswer(
        val questionId: String,
        val answer: String,                     // For MCQ: choice ID. For FILL_IN: text.
        val selectedChoiceIds: List<String> = emptyList() // For SEQUENCING/MATCHING
    )

    fun calculate(
        lessonId: String,
        lessonTitle: String,
        questions: List<QuizQuestion>,
        studentAnswers: Map<String, StudentAnswer>
    ): QuizResult {
        var totalScore = 0
        var totalPossible = 0
        val answeredQuestions = mutableListOf<AnsweredQuestion>()

        questions.forEach { question ->
            totalPossible += question.pointsValue
            val studentAnswer = studentAnswers[question.id]

            val (isCorrect, pointsEarned, correctAnswerText, studentAnswerText) = when (question.questionType) {
                QuestionType.MCQ -> scoreMcq(question, studentAnswer)
                QuestionType.FILL_IN -> scoreFillIn(question, studentAnswer)
                QuestionType.SEQUENCING -> scoreSequencing(question, studentAnswer)
                QuestionType.MATCHING -> scoreMatching(question, studentAnswer)
                else -> ScoreResult(false, 0, "", "")
            }

            if (isCorrect) totalScore += pointsEarned

            answeredQuestions.add(
                AnsweredQuestion(
                    questionId = question.id,
                    questionText = question.questionText,
                    studentAnswer = studentAnswerText,
                    correctAnswer = correctAnswerText,
                    isCorrect = isCorrect,
                    pointsEarned = pointsEarned,
                    pointsValue = question.pointsValue
                )
            )
        }

        return QuizResult(
            lessonId = lessonId,
            lessonTitle = lessonTitle,
            score = totalScore,
            total = totalPossible,
            passed = totalPossible > 0 && (totalScore.toFloat() / totalPossible) >= 0.6f,
            answeredQuestions = answeredQuestions
        )
    }

    private data class ScoreResult(
        val isCorrect: Boolean,
        val pointsEarned: Int,
        val correctAnswerText: String,
        val studentAnswerText: String
    )

    private fun scoreMcq(question: QuizQuestion, answer: StudentAnswer?): ScoreResult {
        val correctChoice = question.choices.firstOrNull { it.isCorrect }
        val selectedChoice = question.choices.firstOrNull { it.id == answer?.answer }
        val isCorrect = selectedChoice?.isCorrect == true
        return ScoreResult(
            isCorrect = isCorrect,
            pointsEarned = if (isCorrect) question.pointsValue else 0,
            correctAnswerText = correctChoice?.choiceText ?: "",
            studentAnswerText = selectedChoice?.choiceText ?: "No answer"
        )
    }

    private fun scoreFillIn(question: QuizQuestion, answer: StudentAnswer?): ScoreResult {
        val correctAnswers = question.choices.filter { it.isCorrect }.map {
            it.choiceText.lowercase().trim()
        }
        val studentText = answer?.answer?.lowercase()?.trim() ?: ""

        // Accept if student answer matches any accepted correct answer
        val isCorrect = correctAnswers.any { correct ->
            studentText == correct || studentText.contains(correct) || correct.contains(studentText)
        }

        return ScoreResult(
            isCorrect = isCorrect,
            pointsEarned = if (isCorrect) question.pointsValue else 0,
            correctAnswerText = question.choices.firstOrNull { it.isCorrect }?.choiceText ?: "",
            studentAnswerText = answer?.answer ?: "No answer"
        )
    }

    private fun scoreSequencing(question: QuizQuestion, answer: StudentAnswer?): ScoreResult {
        // Correct order is defined by order_index on choices
        val correctOrder = question.choices
            .filter { it.isCorrect }
            .sortedBy { it.orderIndex }
            .map { it.id }

        val studentOrder = answer?.selectedChoiceIds ?: emptyList()
        val isCorrect = studentOrder == correctOrder

        return ScoreResult(
            isCorrect = isCorrect,
            pointsEarned = if (isCorrect) question.pointsValue else 0,
            correctAnswerText = correctOrder.joinToString(" → ") { id ->
                question.choices.firstOrNull { it.id == id }?.choiceText ?: id
            },
            studentAnswerText = if (studentOrder.isEmpty()) "No answer" else
                studentOrder.joinToString(" → ") { id ->
                    question.choices.firstOrNull { it.id == id }?.choiceText ?: id
                }
        )
    }

    private fun scoreMatching(question: QuizQuestion, answer: StudentAnswer?): ScoreResult {
        // For matching: all selected choice IDs must match all correct choice IDs
        val correctIds = question.choices.filter { it.isCorrect }.map { it.id }.toSet()
        val selectedIds = (answer?.selectedChoiceIds ?: emptyList()).toSet()
        val isCorrect = selectedIds == correctIds

        return ScoreResult(
            isCorrect = isCorrect,
            pointsEarned = if (isCorrect) question.pointsValue else 0,
            correctAnswerText = question.choices.filter { it.isCorrect }
                .joinToString(", ") { it.choiceText },
            studentAnswerText = if (selectedIds.isEmpty()) "No answer" else
                question.choices.filter { it.id in selectedIds }
                    .joinToString(", ") { it.choiceText }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UnlockNextLessonUseCase
// ─────────────────────────────────────────────────────────────────────────────
class UnlockNextLessonUseCase(
    private val lessonRepository: LessonRepository,
    private val progressRepository: ProgressRepository
) {
    suspend fun execute(studentId: String, completedLessonId: String, quarterId: String) {
        // The next lesson unlock happens via STUDENT_PROGRESS status read in LessonRepository
        // When getLessonsWithStatus is called, it checks each lesson:
        // - Lesson 1: always IN_PROGRESS or DONE
        // - Lesson N: IN_PROGRESS if lesson N-1 is DONE, else LOCKED
        // So just saving DONE status is enough — no explicit unlock needed per lesson!
    }
}