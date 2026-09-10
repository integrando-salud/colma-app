# Guía Integral de Testing y QA - ColmaApp Android

Esta guía establece los estándares de calidad, la estrategia de pruebas automatizadas y los protocolos de QA manual para el desarrollo y mantenimiento de **ColmaApp**.

---

## 1. Estrategia y Pirámide de Testing

La estrategia de calidad de ColmaApp se basa en la pirámide de pruebas:

```
        / \
       /   \     E2E / Instrumentados (Compose UI, Espresso)
      / UI  \    - Verificación visual y flujos críticos de usuario
     /-------\
    /         \   Pruebas de Integración (MockWebServer, DbHelper)
   / Integrac. \  - Contratos de API REST, serialización y base de datos
  /-------------\
 /               \ Pruebas Unitarias (JUnit, MockK, Coroutines Test, Turbine)
/    Unitarias    \ - ViewModels, validaciones clínicas, lógica de negocio y mapeos
-------------------
```

### Tecnologías de Testing Utilizadas
- **Unit Testing**: [JUnit 4](https://junit.org/junit4/), [MockK](https://mockk.io/) (Mocking idiomático en Kotlin), [kotlinx-coroutines-test](https://github.com/Kotlin/kotlinx.coroutines/tree/master/kotlinx-coroutines-test) (`runTest`, `TestDispatcher`), [Turbine](https://github.com/cashapp/turbine) (Pruebas de `StateFlow` y `SharedFlow`).
- **Integración de Red**: [MockWebServer](https://github.com/square/okhttp/tree/master/mockwebserver) de OkHttp/Square.
- **UI & Compose Testing**: `androidx.compose.ui:ui-test-junit4`, `androidx.test.espresso`.

---

## 2. Ejecución de Pruebas Automatizadas

### Desde la Terminal

| Tarea / Comando | Descripción |
| :--- | :--- |
| `./gradlew test` | Ejecuta todas las pruebas unitarias y de integración de la JVM. |
| `./gradlew testDebugUnitTest` | Ejecuta la suite de pruebas unitarias para la variante Debug. |
| `./gradlew connectedAndroidTest` | Ejecuta pruebas instrumentadas en un emulador o dispositivo conectado. |
| `./gradlew lintDebug` | Ejecuta el análisis estático de código y reglas de calidad de Android. |
| `./gradlew check` | Ejecuta lint y todas las pruebas de verificación. |

### Desde Android Studio
1. Haz clic derecho en el paquete `app/src/test/java` y selecciona **Run 'Tests in colmaApp'** (`Ctrl + Shift + F10` o `Control + Shift + R` en macOS).
2. Para ver la cobertura, selecciona **Run 'Tests in colmaApp' with Coverage**.

---

## 3. Estructura de las Pruebas Unitarias

Seguimos el patrón **Given - When - Then** (o *Arrange - Act - Assert*):

```kotlin
@Test
fun `cuando el login es exitoso, se guarda la sesion y se navega al home`() = runTest {
    // Dado (Configuración inicial / Given)
    coEvery { loginRepository.isLoggedIn() } returns false
    coEvery { loginRepository.login("12345678", "secret123") } returns Result.success(true)
    val viewModel = LoginViewModel(loginRepository)

    // Cuando (Acción / When) y Entonces (Verificación / Then)
    viewModel.navigationEvent.test {
        viewModel.onEvent(LoginEvent.LoginClicked)
        val navEvent = awaitItem()
        assertTrue(navEvent is LoginViewModel.NavigationEvent.NavigateToHome)
    }
}
```

---

## 4. Matriz de Casos de Prueba Críticos (QA Funcional & Manual)

### Módulo 1: Autenticación y Sesión
- [ ] **CP-AUTH-01 (Login exitoso)**: Ingresar DNI y contraseña válidos -> Navegación automática al Home y almacenamiento de sesión.
- [ ] **CP-AUTH-02 (Credenciales inválidas)**: Ingresar credenciales erróneas -> Mensaje de error visible en pantalla y habilitación del botón.
- [ ] **CP-AUTH-03 (Auto-login con sesión existente)**: Iniciar la app con sesión activa -> Omitir pantalla de login y dirigir directamente a la pantalla principal.

### Módulo 2: Registro de Presión Arterial (Validaciones Clínicas)
- [ ] **CP-PRES-01 (Validación Rango Válido)**: Presión sistólica entre 40-300 mmHg y diastólica entre 20-200 mmHg.
- [ ] **CP-PRES-02 (Validación Lógica Sistólica/Diastólica)**: Sistólica debe ser estrictamente mayor a diastólica.
- [ ] **CP-PRES-03 (Clasificación de Alerta)**:
  - Sistólica $\ge 140$ o Diastólica $\ge 90 \rightarrow$ Alerta: **Alta**.
  - Sistólica $\le 90 \rightarrow$ Alerta: **Baja**.
  - Rango intermedio $\rightarrow$ Estado: **Normal**.
- [ ] **CP-PRES-04 (Persistencia y Sincronización)**: Registro almacenado en SQLite local y sincronizado exitosamente al servidor.

### Módulo 3: Resiliencia Offline y Sincronización
- [ ] **CP-OFF-01 (Modo Avión / Sin Conectividad)**: La app debe permitir registrar mediciones y consultar historiales locales sin bloquear la UI ni crashear.
- [ ] **CP-OFF-02 (Recuperación de Red)**: Al recuperar conexión a Internet, la sincronización en background debe despachar los registros pendientes.

### Módulo 4: Contenido Multimedia y Quizzes
- [ ] **CP-MED-01 (Reproducción de Video/Audio)**: Contenidos educativos reproducen correctamente con controles accesibles y reporte de lectura generado.

---

## 5. Checklist de Liberación (Release Candidate QA Checklist)

Antes de generar un APK / AAB de producción:
- [ ] Todos los tests unitarios pasan en verde (`./gradlew testDebugUnitTest`).
- [ ] Android Lint no reporta errores fatales (`./gradlew lintDebug`).
- [ ] ProGuard / R8 no ofusca clases críticas requeridas por Retrofit/Gson (`keep` rules validadas).
- [ ] Verificación de compatibilidad en dispositivos mínimos (Android 7.0 / SDK 24) y versiones modernas (Android 14/15 / SDK 35).
- [ ] Verificación de cifrado de credenciales locales en Keystore.
