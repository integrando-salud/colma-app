# Guía de Correcciones de Contenido para Backend (Endpoint `/api/tratamiento`)

Este documento y los archivos JSON en esta carpeta contienen la versión corregida y normalizada de los tratamientos para **Argentina** y **Perú** en sus tres variantes (Diabetes, Hipertensión, Diabetes + Hipertensión), resolviendo las observaciones reportadas en el QA (`comentariosTest.pdf`).

---

## 📁 Archivos JSON Corregidos Generados

1. `tratamiento_AR_DBT.json`: Tratamiento Diabetes - Argentina
2. `tratamiento_AR_HTA.json`: Tratamiento Hipertensión - Argentina
3. `tratamiento_AR_DBT_HTA.json`: Tratamiento Diabetes + Hipertensión - Argentina
4. `tratamiento_PE_DBT.json`: Tratamiento Diabetes - Perú
5. `tratamiento_PE_HTA.json`: Tratamiento Hipertensión - Perú
6. `tratamiento_PE_DBT_HTA.json`: Tratamiento Diabetes + Hipertensión - Perú

---

## 🛠️ Resumen Detallado de Correcciones Realizadas

### 1. Reglas Globales y de Estructura
* **Metas en Semanas 4, 13 y 16 (Sesión 3)**: Se eliminaron los bloques de contenido de tipo `METAS` en estas semanas, ya que clínicamente no corresponden metas en estos cierres.
* **Títulos en Perfil Combinado (HTA + DBT)**: Se eliminaron las menciones de condición entre paréntesis `(Diabetes)`, `(Hipertensión)`, `(DM)` y `(HTA)` de los títulos de lecturas, videos y metas (por ejemplo: `Lectura 1 (Diabetes)` -> `Lectura 1`, `Lectura 3 (Hipertensión)` -> `Lectura 3`, `Metas (Diabetes)` -> `Metas`). Aplica especialmente a Semanas 4, 7, 10, 11 y 13.

### 2. Correcciones Puntuales por Semana y Sesión

| Semana / Sesión | Ubicación | Corrección Realizada |
| :--- | :--- | :--- |
| **Semana 3 - Sesión 1** | Bienvenida | Se eliminó el texto residual `": La activación"` al final del párrafo. |
| **Semana 3 - Sesión 3** | Fin de la sesión | Se corrigió el cierre truncado `"...sueño y ."` por `"...sueño y manejo del estrés. ¡Seguí adelante, vas muy bien!"`. |
| **Semana 7 - Sesión 1 (HTA)** | Fin de la sesión | Se corrigió la errata de tipeo `"hoario"` por `"horario"`. |
| **Semana 8 - Sesión 1 (HTA AR)** | Título / Bienvenida | Se ajustó el título a `"Disfrutá el sabor de cuidarte."` (se removió `.saludable en el día a día`). |
| **Semana 10 - Sesión 1** | Título y Texto Fin | Título corregido a `"La importancia de los controles de salud"` (en vez de *"consultas médicas"*). En el texto se corrigió `"vos sos el protagonista"` por `"vos sos el/la protagonista"`. |
| **Semana 10 - Sesión 2** | Títulos de Lecturas | Se removieron los sufijos `(Diabetes)` e `(Hipertensión)` de las lecturas. |
| **Semana 11 - Sesión 2** | Bienvenida (DM+HTA) | Se eliminó la duplicación del mensaje de bienvenida concatenado dos veces. |
| **Semana 12 - Sesión 2 (HTA)** | Lectura 2 | Se corrigió el espaciado en `“ ritual relajante ”` -> `“ritual relajante”`. |
| **Semana 13 - Sesión 3** | Bienvenida (DM+HTA) | Se separó la concatenación de dos bienvenidas distintas en un único texto coherente. |
| **Semana 13 - Sesión 3** | Lectura 4 | Se formateó `"Considera sumarte a un grupo"` como encabezado con negritas `**`. |
| **Semana 14 - Sesión 2** | Contenido Lecturas | Se removió la `Lectura 5` sobrante que duplicaba el audio de la Lectura 1. |
| **Semana 15 - Sesión 2** | Lectura 1 | Se formateó `"¡El camino no es una línea recta!"` como título con negritas `**`. |
| **Semana 16 - Sesión 2 (PE)** | Quiz Pregunta del día | En la opción C se corrigió el texto corrupto `"Retomar mi plan, ás con un paso..."` por `"Retomar mi plan, quizás con un paso más pequeño, sin juzgarme por la pausa."`. |
| **Semana 16 - Sesión 3** | Fin de la sesión | Se eliminó la lista de links residuales pegados al final (`"Información adicional Información adicional sobre la diabetes..."`). |

---

## 📌 Recomendación de Implementación en Backend
El desarrollador de backend puede utilizar directamente estos archivos JSON para actualizar los seeds, fixtures o scripts de importación de la base de datos de contenidos (`tratamiento` y `tratamiento_elementos`).
