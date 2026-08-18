# ColmaApp - Android Application

ColmaApp es una aplicación móvil nativa para Android diseñada para el seguimiento y acompañamiento de pacientes con condiciones crónicas (como hipertensión y diabetes). Facilita el registro de parámetros de salud (presión arterial, glucosa), la visualización de contenidos educativos multimedia (videos, audios) y la resolución de cuestionarios (quizzes) de seguimiento de salud, todo integrado mediante un sistema de sincronización con un servidor remoto.

---

## Tecnologías y Arquitectura

La aplicación se construyó siguiendo las mejores prácticas modernas de desarrollo de Android:

- **Lenguaje**: Kotlin
- **UI Framework**: Jetpack Compose (con Material Design 3)
- **Estructura de Build**: Gradle Kotlin DSL (`build.gradle.kts`)
- **Arquitectura**: Estructurada por características (*Features*), implementando patrones de presentación desacoplados (MVVM/MVI) con separación limpia de capas (UI, ViewModel, Domain y Data).
- **Base de Datos Local**: SQLite nativo (gestionado mediante `DbHelper` y cifrado en el dispositivo).
- **Comunicación de Red**: Retrofit 2 + OkHttp 3 + Gson para la serialización de datos de la API.
- **Notificaciones locales**: Recordatorios y alarmas locales integrados para incentivar el cumplimiento de metas y tareas pendientes.

---

##  Requisitos Previos

Para poder compilar y ejecutar el proyecto localmente, necesitarás tener instalado:

- **Android Studio** (Versión Ladybug o superior recomendada).
- **Java Development Kit (JDK)**: JDK 11 o JDK 17 configurado en el entorno de desarrollo.
- **Android SDK**:
  - `compileSdk` / `targetSdk`: 35
  - `minSdk`: 24 (Compatible con Android 7.0 o superior)

---

##  Configuración del Entorno Local

Para compilar el proyecto correctamente, es fundamental configurar las variables de entorno locales (las cuales están excluidas del control de versiones por seguridad):

1. **Clonar el repositorio** en tu máquina local.
2. Localizar el archivo de plantilla `local.properties.example` en la raíz del proyecto.
3. Crear una copia de este archivo en la misma raíz y nombrarla exactamente como **`local.properties`**.
4. Abrir `local.properties` y configurar la variable `API_HOST` con la URL base del backend del servidor. Por ejemplo:
   ```properties
   sdk.dir=/ruta/a/tu/Android/sdk
   API_HOST=https://api.tu-servidor-backend.com/
   ```
   *(Nota: Si no se define esta propiedad, la compilación usará una dirección de prueba genérica, pero la sincronización de datos con el servidor fallará).*

---

##  Compilación y Ejecución

Una vez que tengas configurado tu archivo `local.properties`:

### Desde Android Studio
1. Abre Android Studio y selecciona **Open**.
2. Selecciona la carpeta raíz del proyecto.
3. Deja que Gradle sincronice las dependencias del proyecto (`Sync Project with Gradle Files`).
4. Selecciona un dispositivo virtual (Emulador) o físico con depuración USB activa.
5. Haz clic en el botón de **Run** (ícono de play verde) o presiona `Shift + F10`.

### Desde la Terminal (CLI)
Para compilar y verificar que todo esté correcto desde la línea de comandos, puedes usar el Gradle Wrapper incluido:

* **Compilar y ejecutar pruebas unitarias**:
  ```bash
  ./gradlew build
  ```
* **Instalar la versión de Debug en un dispositivo conectado**:
  ```bash
  ./gradlew installDebug
  ```

---

##  Estructura Principal del Proyecto

La estructura del código fuente se encuentra organizada dentro del módulo `app/src/main/java/com/colma/colmaApp/` de la siguiente forma:

- **`features/`**: Contiene los distintos módulos funcionales de la app (ej: `Login`, `MenuMain`, `multimedia`, `presion`, `sesiones`). Cada feature encapsula su lógica de UI, ViewModel y persistencia/repositorios asociados.
- **`servicios/`**: Servicios globales de la aplicación, incluyendo comunicación HTTP/Retrofit (`internetCommunication`), persistencia local (`database`) y manejo de sesión (`session`).
- **`ui/theme/`**: Configuración de estilos globales, tipografías y paleta de colores de Material 3.
- **`utils/`**: Clases de utilidad generales (cifrado, utilidades de red, reproductores de video locales y manejadores de notificaciones).
