# Diagrama de Entidad-Relación (DER) de la Base de Datos Local

Este archivo contiene el diagrama Mermaid de la base de datos SQLite local de `colmaApp`. Puedes visualizar este diagrama pegando el código de abajo en el [Editor en Vivo de Mermaid](https://mermaid.live/) o utilizando un visor de Markdown que soporte Mermaid.

```mermaid
erDiagram
    USUARIOS ||--o{ METAS_TABLE : "gestiona metas"
    USUARIOS ||--o{ GLUCOSA_TABLE : "registra glucosa"
    USUARIOS ||--o{ PRESION_TABLE : "registra presión"
    USUARIOS ||--o{ ESTADO_ANIMO_TABLE : "registra estado de ánimo"
    USUARIOS ||--o{ MULTIMEDIA_READ_TABLE : "seguimiento de contenido"
    USUARIOS ||--o{ QUIZ_RESPONSE_TABLE : "envía cuestionarios"
    QUIZ_RESPONSE_TABLE ||--o{ QUESTION_RESPONSE_TABLE : "contiene preguntas"
    USUARIOS }o--|| PAISES : "reside en"
    USUARIOS }o--|| CENTROS_SALUD : "asiste a"

    USUARIOS {
        INTEGER idUsuario PK
        TEXT nombre
        TEXT documento
        TEXT email
        TEXT sexo
        INTEGER id_pais FK
        INTEGER id_centro_salud FK
        TEXT fecha_registro
        TEXT primer_login
    }

    METAS_TABLE {
        INTEGER id_meta PK
        INTEGER id_usuario FK
        INTEGER semana
        INTEGER sesion
        TEXT tipo_actividad
        TEXT actividad
        TEXT lugar
        TEXT acompañante
        TEXT fecha_regisitro
        TEXT fecha_edicion
        TEXT fecha_sincronizacion
        INTEGER meta_cumplida
        TEXT fecha_objetivo
        TEXT motivo_incumplimiento
    }

    GLUCOSA_TABLE {
        INTEGER id_glucosa PK
        INTEGER id_usuario FK
        REAL valor_mgdl
        INTEGER ayuno
        TEXT fecha_registro
        TEXT fecha_hora_edicion
        TEXT fecha_sincronizacion
        TEXT advertencia
    }

    PRESION_TABLE {
        INTEGER id_presion PK
        INTEGER id_usuario FK
        REAL sistolica
        REAL diastolica
        TEXT fecha_registro
        TEXT fecha_hora_edicion
        TEXT fecha_sincronizacion
        TEXT advertencia
    }

    ESTADO_ANIMO_TABLE {
        INTEGER rowid PK "ID de Fila Interno de SQLite"
        INTEGER id_paciente FK
        TEXT fecha_registro
        TEXT fecha_edicion
        INTEGER pregunta_1
        INTEGER pregunta_2
        INTEGER pregunta_3
        INTEGER pregunta_4
        INTEGER pregunta_5
        INTEGER pregunta_6
        INTEGER pregunta_7
        INTEGER pregunta_8
        INTEGER pregunta_9
        TEXT fecha_sincronizacion
    }

    MULTIMEDIA_READ_TABLE {
        INTEGER id_multimedia_read PK
        INTEGER id_usuario FK
        TEXT treatment_id
        INTEGER week_id
        INTEGER session_id
        TEXT multimedia_type
        TEXT multimedia_unique_key
        TEXT fecha_registro
        TEXT fecha_sincronizacion
    }

    QUIZ_RESPONSE_TABLE {
        INTEGER id_quiz_response PK
        INTEGER id_usuario FK
        TEXT quiz_type
        INTEGER week_id
        INTEGER session_id
        TEXT quiz_title
        TEXT fecha_registro
        TEXT fecha_sincronizacion
        INTEGER total_questions
        INTEGER correct_answers
        REAL percentage_correct
        TEXT multimedia_unique_key
        TEXT treatment_type
    }

    QUESTION_RESPONSE_TABLE {
        INTEGER id_question_response PK
        INTEGER id_quiz_response FK
        INTEGER question_index
        TEXT question_text
        INTEGER user_answer_index
        TEXT user_answer_text
        INTEGER correct_answer_index
        TEXT correct_answer_text
        INTEGER is_correct
        TEXT posibles_respuestas
    }

    PAISES {
        INTEGER id PK
        TEXT nombre
    }

    CENTROS_SALUD {
        INTEGER id PK
        TEXT nombre
    }

    SINCRONIZACION_TABLE {
        INTEGER id PK
        TEXT fecha_sincronizacion
        TEXT estado
        TEXT data
    }
```
