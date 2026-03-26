package com.colma.colmaApp.features.mood.viewmodel

data class MoodQuizUiState(
    val questions: List<String> = emptyList(),
    val optionLabels: List<String> = emptyList(),
    val answers: List<Int> = emptyList(),
    val showRestOfQuestions: Boolean = false,
    val firstTwoQuestionsLocked: Boolean = false,
    val showConfirmDialog: Boolean = false,
    val showMoreQuestionsDialog: Boolean = false,
    val showWarningDialog: Boolean = false,
    val warningDialogMessage: String = "",
    val showCompletionDialog: Boolean = false,
    val completionDialogMessage: String = "",
    val shouldNavigateAfterCompletion: Boolean = false,
    val error: String? = null,
    val isLoading: Boolean = false,
)

sealed class MoodQuizEvent {
    data class AnswerQuestion(val index: Int, val answer: Int) : MoodQuizEvent()
    object SaveClicked : MoodQuizEvent()
    object ConfirmSubmit : MoodQuizEvent()
    object DismissConfirmDialog : MoodQuizEvent()
    object ConfirmMoreQuestions : MoodQuizEvent()
    object DismissMoreQuestionsDialog : MoodQuizEvent()
    object DismissWarningDialog : MoodQuizEvent()
    object AcceptCompletionDialog : MoodQuizEvent()
    object DismissCompletionDialog : MoodQuizEvent()
    object BackPressed : MoodQuizEvent()
}
