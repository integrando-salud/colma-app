# AGENTS.md

This file contains development guidelines and commands for agentic coding agents working on the ColmaApp Android project.

## Project Overview

ColmaApp is a native Android health tracking application built with Kotlin and Jetpack Compose. The app follows MVVM architecture with clean architecture principles, featuring user authentication, health data tracking (glucose, blood pressure, mood), daily educational sessions, and data synchronization capabilities.

**Technology Stack:**
- **Platform:** Android (native)
- **Language:** Kotlin (100%)
- **UI Framework:** Jetpack Compose with Material3
- **Architecture:** MVVM + Clean Architecture
- **Database:** SQLite with sqlite-asset-helper
- **Media:** ExoPlayer for video playback
- **Build System:** Gradle with Kotlin DSL

## Build and Development Commands

### Essential Commands (run after each implementation)
```bash
./gradlew build              # Full project build - ALWAYS RUN AFTER CHANGES
./gradlew check              # Run all checks (lint + tests)
```

### Build Commands
```bash
./gradlew assembleDebug      # Generate debug APK
./gradlew assembleRelease    # Generate release APK
./gradlew clean              # Clean build artifacts
```

### Testing Commands
```bash
./gradlew test               # Unit tests for all variants
./gradlew testDebugUnitTest  # Debug unit tests only
./gradlew connectedAndroidTest # Instrumentation tests (requires device/emulator)
```

### Single Test Execution
```bash
# Run specific test class
./gradlew test --tests "com.colma.colmaApp.ExampleUnitTest"

# Run specific test method
./gradlew test --tests "com.colma.colmaApp.ExampleUnitTest.addition_isCorrect"
```

### Code Quality Commands
```bash
./gradlew lint               # Lint analysis
./gradlew lintDebug          # Lint debug variant
./gradlew lintRelease        # Lint release variant
./gradlew lintFix            # Auto-fix safe lint suggestions
```

## Code Style Guidelines

### Kotlin Code Style
- **Style:** Official Kotlin style (configured in gradle.properties)
- **Target JVM:** Java 11
- **No semicolons:** Follow Kotlin conventions

### Import Organization
```kotlin
// Android imports first
import android.content.Intent
import android.os.Bundle

// AndroidX imports
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*

// Project imports (grouped by feature)
import com.colma.colmaApp.features.MenuMain.ui.MenuMainUI
import com.colma.colmaApp.ui.theme.ColmaAppTheme

// Third-party libraries
import kotlinx.coroutines.launch
```

### Naming Conventions
- **Activities:** `{Feature}UI` (e.g., `MenuMainUI`, `LoginUI`)
- **ViewModels:** `{Feature}ViewModel` (e.g., `LoginViewModel`)
- **Repositories:** `{Feature}Repository` (interface) / `{Feature}RepositoryImpl` (implementation)
- **Events:** `{Feature}Event` (sealed class)
- **State:** `{Feature}UiState` (data class)
- **Composables:** PascalCase for reusable composables, camelCase for local ones

### File Structure and Package Organization
```
com.colma.colmaApp/
├── features/                    # Feature-based modules
│   ├── {Feature}/
│   │   ├── data/               # Repository implementations
│   │   ├── domain/             # Repository interfaces
│   │   ├── ui/                 # Activities/Composables
│   │   └── viewmodel/          # ViewModels
├── servicios/                   # Services layer
│   ├── database/
│   │   └── dbhelper/
│   └── Modelos/
├── ui/theme/                    # Compose theming
└── utils/                       # Utility classes
```

## Architecture Patterns

### MVVM Implementation
- **ViewModels:** Handle business logic, expose state via StateFlow
- **UI:** Activities/Composables observe state and send events
- **Repository Pattern:** Interface in domain, implementation in data
- **Event-Driven:** Use sealed classes for events and state management

### State Management Pattern
```kotlin
// In ViewModel
private val _uiState = MutableStateFlow(FeatureUiState())
val uiState = _uiState.asStateFlow()

fun onEvent(event: FeatureEvent) {
    when (event) {
        is FeatureEvent.SomeAction -> {
            _uiState.update { it.copy(isLoading = true) }
            // Handle action
        }
    }
}
```

### Navigation Pattern
```kotlin
// In ViewModel
private val _navigationEvent = Channel<NavigationEvent>()
val navigationEvent = _navigationEvent.receiveAsFlow()

// In Activity
LaunchedEffect(Unit) {
    viewModel.navigationEvent.collect { event ->
        when (event) {
            is NavigationEvent.NavigateToSomeScreen -> {
                startActivity(Intent(this, SomeScreenUI::class.java))
            }
        }
    }
}
```

## Compose Guidelines

### Composable Structure
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureScreen(
    uiState: FeatureUiState,
    onEvent: (FeatureEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    // Screen implementation
}
```

### Material3 Usage
- Use Material3 components (`androidx.compose.material3.*`)
- Follow Material3 design patterns
- Custom theme defined in `ui/theme/`

### RTL/LTR Support
- App supports Spanish with RTL considerations
- Use `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)` for RTL screens
- Navigation drawer uses LTR regardless of screen direction

## Error Handling

### Repository Pattern
```kotlin
// Repository returns Result<T>
suspend fun login(dni: String, password: String): Result<Boolean> {
    return try {
        // Implementation
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### ViewModel Error Handling
```kotlin
result.onSuccess {
    // Handle success
    _navigationEvent.send(NavigationEvent.NavigateToHome)
}.onFailure { error ->
    _uiState.update { it.copy(error = error.message) }
}
```

## Database Guidelines

### SQLite Usage
- Database bundled as asset in `app/src/main/assets/databases/colmadb.db`
- Use `DbHelper` class for database operations
- Follow repository pattern for data access

## Testing Guidelines

### Unit Tests
- Located in `app/src/test/java/com/colma/colmaApp/`
- Use JUnit 4
- Test ViewModels and business logic

### Instrumentation Tests
- Located in `app/src/androidTest/java/com/colma/colmaApp/`
- Use AndroidX Test and Espresso
- Test UI components and integration

## Development Workflow

1. **Implement Changes:** Make code changes following established patterns
2. **Build Verification:** Run `./gradlew build` to verify compilation
3. **Run Tests:** Execute `./gradlew test` for unit tests
4. **Lint Check:** Run `./gradlew lint` to check code quality
5. **Final Build:** Run `./gradlew check` for comprehensive verification

## Key Dependencies and Versions

- **Kotlin:** 2.0.21
- **Compose BOM:** 2024.09.00
- **AndroidX Core KTX:** 1.16.0
- **ExoPlayer:** 2.19.0
- **SQLite Asset Helper:** Latest version
- **Min SDK:** 24
- **Target SDK:** 35
- **Compile SDK:** 35

## Important Notes

- **Language:** Spanish localization throughout the app
- **Theme:** Custom theme with principal color defined in colors.xml
- **Icons:** Use drawable resources for custom icons
- **Navigation:** Intent-based navigation between activities
- **Data Sync:** Webhook-based synchronization with remote server
- **Multimedia:** ExoPlayer for video content delivery

## Code Quality Standards

- Follow official Kotlin style guide
- Use meaningful variable and function names
- Keep functions small and focused
- Add comments only when necessary for complex logic
- Use proper error handling with Result types
- Follow Android architecture guidelines