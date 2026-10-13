# Diccionario de Datos - ColmaApp (SQLite)

Documentación clara y directa de la base de datos local de la aplicación móvil (`colmadb.db`).

---

## Datos Clave de la Base de Datos

* **Archivo:** `colmadb.db` (en `app/src/main/assets/databases/colmadb.db`)
* **Código de gestión:** [DbHelper.kt](../app/src/main/java/com/colma/colmaApp/servicios/database/dbhelper/DbHelper.kt)
* **Versión actual:** `13`
* **Formato de fechas:** `yyyy-MM-dd HH:mm:ss` (hora local)
* **Regla de sincronización:** Si `fecha_sincronizacion` es `NULL`, el dato está pendiente de enviarse al servidor. Si tiene fecha, ya fue sincronizado.

---

## Mapa de Tablas y Relaciones (Vista Rápida)

```text
[paises]  ──┐
            ├──> [usuarios] ──┬──> [credenciales] (login local seguro)
[centros] ──┘        │        ├──> [sesiones_completadas] (avance de sesiones)
                     │        ├──> [presion_table] (tomas de presión arterial)
                     │        ├──> [glucosa_table] (tomas de glucosa)
                     │        ├──> [estado_animo_table] (test de ánimo PHQ-9)
                     │        ├──> [metas_table] (metas semanales de hábitos)
                     │        ├──> [multimedia_read_table] (lecturas y videos vistos)
                     │        └──> [quiz_response_table] (evaluaciones completadas)
                     │                 └──> [question_response_table] (cada pregunta)
                     │
                     └──> [sincronizacion_table] (historial de sincronizaciones)
```

---

## Índice Rápido de Tablas

| Tabla | ¿Qué información guarda? |
| :--- | :--- |
| **`usuarios`** | Datos del perfil del paciente logueado. |
| **`credenciales`** | Documento, contraseña y token encriptados para login sin conexión. |
| **`paises`** | Lista de países disponibles (Argentina, Perú). |
| **`centros_salud`** | Postas o centros médicos de los pacientes. |
| **`sesiones_completadas`** | Semanas y sesiones educativas completadas por el usuario. |
| **`presion_table`** | Registro de presión arterial (sistólica/diastólica) y alertas. |
| **`glucosa_table`** | Registro de glucosa en sangre (mg/dL), ayuno y alertas. |
| **`estado_animo_table`** | Respuestas al cuestionario de ánimo PHQ-9 (9 preguntas). |
| **`metas_table`** | Metas de cambio de hábitos, estado de logro y justificaciones. |
| **`multimedia_read_table`** | Bitácora de lecturas, audios y videos consumidos en la app. |
| **`quiz_response_table`** | Puntuaciones y resultados de los cuestionarios/quizzes. |
| **`question_response_table`** | Detalle de respuestas correctas e incorrectas de cada quiz. |
| **`sincronizacion_table`** | Auditoría y registro de sincronizaciones con el backend. |

---

##  Detalle de Tablas por Módulo

---

### 1. Usuarios y Acceso

#### Tabla: `usuarios`
Guarda el perfil del usuario activo tras hacer login.

| Campo | Tipo | Clave / Null | Descripción |
| :--- | :--- | :--- | :--- |
| `idUsuario` | INTEGER | **PK** | ID asignado por el servidor (ej: `71`). |
| `nombre` | TEXT | Opcional | Nombre del usuario (ej: `"Juan Pérez"`). |
| `documento` | TEXT | Opcional | DNI o cédula de identidad. |
| `email` | TEXT | Opcional | Correo electrónico del usuario. |
| `sexo` | TEXT | Opcional | Género/sexo registrado (`"M"`, `"F"`). |
| `id_pais` | INTEGER | FK -> `paises` | ID del país (`1`: Argentina, `2`: Perú). |
| `id_centro_salud`| INTEGER | FK -> `centros_salud` | ID del centro de salud asignado. |
| `primer_login` | TEXT | Opcional | Fecha del primer inicio de sesión. Se usa para calcular la semana actual del programa. |
| `fecha_registro`| TEXT | Opcional | Fecha de creación del usuario en el sistema. |
| `contrasena` | TEXT | Opcional | Campo heredado (no usado actualmente). |

#### Tabla: `credenciales`
Almacena credenciales locales encriptadas con AES para permitir el login offline.

| Campo | Tipo | Clave / Null | Descripción |
| :--- | :--- | :--- | :--- |
| `id` | INTEGER | **PK Auto** | ID interno del registro. |
| `documento_encriptado` | TEXT | Obligatorio | DNI encriptado con AES. |
| `password_encriptada` | TEXT | Obligatorio | Contraseña encriptada con AES. |
| `token_encriptado` | TEXT | Obligatorio | Token JWT encriptado. |
| `id_usuario` | INTEGER | FK -> `usuarios`| ID del usuario dueño de las credenciales. |

#### Tabla: `paises`
Catálogo maestro de países.

| Campo | Tipo | Clave / Null | Descripción |
| :--- | :--- | :--- | :--- |
| `id` | INTEGER | **PK** | Identificador del país (`1`, `2`). |
| `nombre` | TEXT | Obligatorio | Nombre (`"Argentina"`, `"Perú"`). |

#### Tabla: `centros_salud`
Catálogo de centros de salud y postas médicas.

| Campo | Tipo | Clave / Null | Descripción |
| :--- | :--- | :--- | :--- |
| `id` | INTEGER | **PK** | Identificador del centro. |
| `nombre` | TEXT | Obligatorio | Nombre de la posta o centro de salud. |

---

### 2. Registros Clínicos (Salud)

#### Tabla: `presion_table`
Tomas de presión arterial registradas por el paciente.

| Campo | Tipo | Clave / Null | Descripción |
| :--- | :--- | :--- | :--- |
| `id_presion` | INTEGER | **PK Auto** | ID de la medición. |
| `id_usuario` | INTEGER | FK -> `usuarios` | ID del paciente. |
| `sistolica` | REAL | Obligatorio | Presión máxima en mmHg (ej: `120.0`). |
| `diastolica` | REAL | Obligatorio | Presión mínima en mmHg (ej: `80.0`). |
| `advertencia` | TEXT | Opcional | Clasificación clínica (ej: `"Presión arterial elevada"`). |
| `fecha_registro` | TEXT | Obligatorio | Fecha y hora en que se ingresó la medición. |
| `fecha_hora_edicion` | INTEGER/TEXT | Opcional | Timestamp de la toma real o edición. |
| `fecha_sincronizacion`| TEXT | Opcional | Fecha de sincronización (`NULL` = pendiente). |

#### Tabla: `glucosa_table`
Tomas de glucosa capilar registradas por el paciente.

| Campo | Tipo | Clave / Null | Descripción |
| :--- | :--- | :--- | :--- |
| `id_glucosa` | INTEGER | **PK Auto** | ID de la medición. |
| `id_usuario` | INTEGER | FK -> `usuarios` | ID del paciente. |
| `valor_mgdl` | REAL | Obligatorio | Nivel de glucosa en mg/dL (ej: `95.0`, `140.0`). |
| `ayuno` | INTEGER | Obligatorio | `1` = En ayunas, `0` = Después de comer (postprandial). |
| `advertencia` | TEXT | Opcional | Alerta clínica (ej: `"Nivel normal"`, `"Hipoglucemia"`). |
| `fecha_registro` | TEXT | Obligatorio | Fecha y hora de guardado. |
| `fecha_hora_edicion` | TEXT | Opcional | Fecha y hora efectiva de la medición. |
| `fecha_sincronizacion`| TEXT | Opcional | Fecha de sincronización (`NULL` = pendiente). |

#### Tabla: `estado_animo_table`
Cuestionario de tamizaje de salud emocional PHQ-9 (9 preguntas con escala 0 a 3).

| Campo | Tipo | Clave / Null | Descripción |
| :--- | :--- | :--- | :--- |
| `rowid` | INTEGER | **PK Auto** | ID interno de fila SQLite. |
| `id_paciente` | INTEGER | FK -> `usuarios` | ID del paciente evaluado. |
| `pregunta_1` ... `pregunta_9` | INTEGER | Obligatorio | Puntos de cada ítem (0: Nunca, 1: Varios días, 2: > mitad, 3: Casi diario). |
| `fecha_registro` | TEXT | Obligatorio | Fecha en que se completó el cuestionario. |
| `fecha_edicion` | TEXT | Opcional | Fecha de modificación. |
| `fecha_sincronizacion` | TEXT | Opcional | Fecha de sincronización (`NULL` = pendiente). |

---

### 3. Metas y Hábitos

#### Tabla: `metas_table`
Metas semanales de cambio de conducta (ej: caminata, alimentación, toma de medicación).

| Campo | Tipo | Clave / Null | Descripción |
| :--- | :--- | :--- | :--- |
| `id_meta` | INTEGER | **PK Auto** | ID de la meta. |
| `id_usuario` | INTEGER | FK -> `usuarios` | ID del paciente. |
| `semana` | INTEGER | Obligatorio | Semana del tratamiento (ej: `1`, `2`). |
| `sesion` | INTEGER | Obligatorio | Sesión asociada. |
| `tipo_actividad` | TEXT | Opcional | Categoría (ej: `"Actividad Física"`, `"Alimentación"`). |
| `actividad` | TEXT | Opcional | Descripción de la meta (ej: `"Caminar 20 min"`). |
| `lugar` | TEXT | Opcional | Dónde lo hará (ej: `"Parque"`). |
| `acompañante` | TEXT | Opcional | Con quién lo hará (ej: `"Hijo"`, `"Solo"`). |
| `fecha_objetivo` | TEXT | Opcional | Fecha pactada para cumplirla (`yyyy-MM-dd`). |
| `meta_cumplida` | INTEGER | Opcional | `1` = Cumplida, `0` = No cumplida, `NULL` = Pendiente. |
| `motivo_incumplimiento`| TEXT | Opcional | Explicación del paciente si no la cumplió. |
| `tratamiento_elemento_id`| INTEGER | Opcional | ID del elemento asociado en el backend. |
| `fecha_regisitro` | TEXT | Obligatorio | Fecha de creación de la meta. *(Nombre en BD)*. |
| `fecha_edicion` | TEXT | Opcional | Fecha de última edición. |
| `fecha_sincronizacion` | TEXT | Opcional | Fecha de sincronización (`NULL` = pendiente). |

---

### 4. Educación y Quizzes (Contenidos)

#### Tabla: `sesiones_completadas`
Registra qué sesiones del programa formativo ya finalizó el paciente.

| Campo | Tipo | Clave / Null | Descripción |
| :--- | :--- | :--- | :--- |
| `id_usuario` | INTEGER | **PK Compuesta (1/4)** | ID del paciente. |
| `treatment_id` | TEXT | **PK Compuesta (2/4)** | Código del tratamiento (`"HTA"`, `"DBT"`, `"DBT_HTA"`). |
| `week_id` | INTEGER | **PK Compuesta (3/4)** | Número de la semana (ej: `1`, `2`, `3`). |
| `session_id` | INTEGER | **PK Compuesta (4/4)** | Número de la sesión dentro de la semana. |
| `fecha_completado`| TEXT | Por defecto `now` | Fecha y hora en que completó la sesión. |
| `fecha_sincronizacion`| TEXT | Opcional | Fecha de sincronización (`NULL` = pendiente). |

#### Tabla: `multimedia_read_table`
Bitácora de lecturas, audios, infografías y videos consumidos en la app.

| Campo | Tipo | Clave / Null | Descripción |
| :--- | :--- | :--- | :--- |
| `id_multimedia_read`| INTEGER | **PK Auto** | ID único del evento de lectura. |
| `id_usuario` | INTEGER | FK -> `usuarios` | ID del paciente. |
| `treatment_id` | TEXT | Obligatorio | Tratamiento (`"HTA"`, `"DBT"`, etc.). |
| `week_id` | INTEGER | Obligatorio | Semana correspondiente. |
| `session_id` | INTEGER | Obligatorio | Sesión correspondiente. |
| `multimedia_type` | TEXT | Obligatorio | Tipo de elemento (`"lectura"`, `"video"`, `"audio"`). |
| `multimedia_unique_key` | TEXT | Obligatorio | Clave del recurso en el catálogo JSON (ej: `"hta_w1_s1_lec1"`). |
| `tratamiento_elemento_id` | INTEGER | Opcional | ID del recurso en el contrato de API del backend. |
| `tratamiento_multimedia_id` | INTEGER | Opcional | ID de recurso heredado. |
| `fecha_registro` | TEXT | Obligatorio | Fecha y hora de consumo. |
| `fecha_sincronizacion` | TEXT | Opcional | Fecha de sincronización (`NULL` = pendiente). |

#### Tabla: `quiz_response_table`
Resumen y puntaje general de las evaluaciones completadas.

| Campo | Tipo | Clave / Null | Descripción |
| :--- | :--- | :--- | :--- |
| `id_quiz_response` | INTEGER | **PK Auto** | ID del quiz rendido. |
| `id_usuario` | INTEGER | FK -> `usuarios` | ID del paciente. |
| `quiz_title` | TEXT | Opcional | Título del quiz (ej: `"Cuestionario de Hipertensión"`). |
| `quiz_type` | TEXT | Opcional | Tipo de patología (`"hta"`, `"diabetes"`). |
| `treatment_type` | TEXT | Opcional | Tratamiento (`"HTA"`, `"DBT"`). |
| `week_id` | INTEGER | Obligatorio | Semana del curso. |
| `session_id` | INTEGER | Obligatorio | Sesión del curso. |
| `total_questions` | INTEGER | Obligatorio | Cantidad total de preguntas. |
| `correct_answers` | INTEGER | Obligatorio | Cantidad de respuestas correctas. |
| `percentage_correct`| REAL | Obligatorio | Porcentaje de acierto (ej: `75.0`, `100.0`). |
| `multimedia_unique_key`| TEXT | Opcional | Clave del quiz en el catálogo. |
| `tratamiento_elemento_id`| INTEGER| Opcional | ID del elemento para sincronización individual. |
| `fecha_registro` | TEXT | Obligatorio | Fecha de resolución. |
| `fecha_sincronizacion`| TEXT | Opcional | Fecha de sincronización (`NULL` = pendiente). |

#### Tabla: `question_response_table`
Detalle de cada pregunta y respuesta de un quiz.

| Campo | Tipo | Clave / Null | Descripción |
| :--- | :--- | :--- | :--- |
| `id_question_response` | INTEGER | **PK Auto** | ID de la respuesta unitaria. |
| `id_quiz_response` | INTEGER | FK -> `quiz_response` | Quiz al que pertenece (se borra en cascada). |
| `question_index` | INTEGER | Obligatorio | Número de la pregunta (inicia en `0`). |
| `question_text` | TEXT | Obligatorio | Enunciado de la pregunta. |
| `user_answer_index` | INTEGER | Opcional | Índice de la opción elegida por el paciente. |
| `user_answer_text` | TEXT | Opcional | Texto de la respuesta elegida. |
| `correct_answer_index`| INTEGER | Opcional | Índice de la opción correcta. |
| `correct_answer_text` | TEXT | Opcional | Texto de la opción correcta. |
| `is_correct` | INTEGER | Obligatorio | `1` = Acertó, `0` = Falló. |
| `posibles_respuestas` | TEXT | Opcional | JSON con todas las opciones que se le mostraron. |

---

### 5. Auditoría de Conexión

#### Tabla: `sincronizacion_table`
Registro histórico de intentos de sincronización con el servidor.

| Campo | Tipo | Clave / Null | Descripción |
| :--- | :--- | :--- | :--- |
| `fecha_sincronizacion`| TEXT | Obligatorio | Momento de la sincronización. |
| `estado` | TEXT | Obligatorio | Resultado (`"OK"`, `"ERROR"`, `"PENDING"`). |
| `data` | TEXT | Opcional | Datos JSON enviados o recibidos. |
