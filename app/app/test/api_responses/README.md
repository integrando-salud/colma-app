# Respuestas de la API del Servidor Real (http://158.69.219.243/)

Este directorio contiene las respuestas reales obtenidas de los endpoints del backend para los 5 usuarios de prueba (Perú y Argentina).

## Estructura de Archivos

Cada carpeta contiene:
- `1_login_response.json`: Respuesta de `POST /api/login` (Token, Usuario, País, Centro de Salud).
- `2_tratamiento_response.json`: Respuesta de `GET /api/tratamiento` (Semanas, Sesiones, Contenidos con `tratamiento_multimedia_id` y enlaces de descarga).
- `3_multimedia_completados_response.json`: Respuesta de `GET /api/multimedia-completados` (Lista `multimedia_completados` para recuperación de progreso).

## Resumen de Usuarios de Prueba

| Carpeta | Usuario | País | Condición | DNI | Items Multimedia | Con `tratamiento_multimedia_id` |
| :--- | :--- | :--- | :--- | :--- | :---: | :---: |
| [`guillermo_arribas_HTA_PE`](./guillermo_arribas_HTA_PE) | Guillermo Arribas | Perú | HTA | 15864485 | 134 | 134 (100%) |
| [`erika_becerra_DBT_PE`](./erika_becerra_DBT_PE) | Erika Becerra | Perú | DBT | 81306555 | 139 | 139 (100%) |
| [`denisse_molina_DBT_HTA_PE`](./denisse_molina_DBT_HTA_PE) | Denisse Molina | Perú | DBT + HTA | 12253242 | 155 | 155 (100%) |
| [`maria_gonzalez_HTA_AR`](./maria_gonzalez_HTA_AR) | María González | Argentina | HTA | 88888888 | 133 | 133 (100%) |
| [`pedro_flores_DBT_HTA_AR`](./pedro_flores_DBT_HTA_AR) | Pedro Flores | Argentina | DBT + HTA | 77777777 | 154 | 154 (100%) |

## Script de Regeneración
Para actualizar o volver a consultar las respuestas en cualquier momento:
```bash
python3 scripts/fetch_and_save_api_responses.py
```
