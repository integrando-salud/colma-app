package com.colma.colmaApp.servicios.Modelos

data class QuizResponseRecord(
    val id_quiz_response: Long = 0,
    val id_usuario: Int?,
    val quiz_type: String?,
    val week_id: Int?,
    val session_id: Int?,
    val quiz_title: String?,
    val fecha_registro: String?,
    val fecha_sincronizacion: String?,
    val total_questions: Int?,
    val correct_answers: Int?,
    val percentage_correct: Double?,
    val multimedia_unique_key: String?
)

data class QuestionResponseRecord(
    val id_question_response: Long = 0,
    val id_quiz_response: Long?,
    val question_index: Int?,
    val question_text: String?,
    val user_answer_index: Int?,
    val user_answer_text: String?,
    val correct_answer_index: Int?,
    val correct_answer_text: String?,
    val is_correct: Int?
)
