# COOKING ASSISTANT MOBILE APP

## PROJECT OVERVIEW

**Platform:** Android Mobile Application
**Language:** Kotlin
**UI Framework:** Jetpack Compose (Material 3)
**Architecture:** MVVM (Model-View-ViewModel)
**Build System:** Gradle with Kotlin DSL
**Min SDK:** 31 (Android 12+)
**Target SDK:** 36

## PROJECT PURPOSE

This application is cooking assistant, it is written to be Android application.
We should focus mainly on the presentation/logic layers, data will not be passed from API, we will mock it in the code.
The main logic this application should have is steering everything by voice. When an user is cooking something, they may not be able to click anything on the phone or tablet, so we need to provide the option to steer it by voice.

## HIGH-LEVEL ARCHITECTURE

```
┌─────────────────────────────────────────────────────────────────┐
│                         PRESENTATION LAYER                       │
│  ┌────────────────────┐              ┌─────────────────────┐   │
│  │  RecipeListScreen  │◄────────────►│ RecipeDetailScreen  │   │
│  │  - Display recipes │              │ - Show full recipe  │   │
│  │  - Handle clicks   │              │ - Navigate back     │   │
│  └─────────┬──────────┘              └──────────┬──────────┘   │
│            │                                     │               │
└────────────┼─────────────────────────────────────┼──────────────┘
             │                                     │
             │         ┌──────────────────┐        │
             └────────►│   Navigation     │◄───────┘
                       │   - NavHost      │
                       │   - Routes       │
                       └────────┬─────────┘
                                │
┌───────────────────────────────┼──────────────────────────────────┐
│                        VIEWMODEL LAYER                            │
│                       ┌───────▼────────┐                          │
│                       │ RecipeViewModel │                         │
│                       │ - StateFlow     │                         │
│                       │ - Business logic│                         │
│                       │ - Data management│                        │
│                       └───────┬─────────┘                         │
└───────────────────────────────┼──────────────────────────────────┘
                                │
┌───────────────────────────────┼──────────────────────────────────┐
│                          MODEL LAYER                              │
│                       ┌───────▼─────────┐                         │
│                       │     Recipe      │                         │
│                       │  - id: Int      │                         │
│                       │  - name: String │                         │
│                       │  - ingredients  │                         │
│                       │  - instructions │                         │
│                       │  - cookingTime  │                         │
│                       └─────────────────┘                         │
└───────────────────────────────────────────────────────────────────┘
```

## DIRECTORY STRUCTURE

```
app/src/main/java/com/example/cookingassistant/
├── MainActivity.kt                    # Application entry point
├── model/
│   └── Recipe.kt                      # Recipe data model
├── viewmodel/
│   └── RecipeViewModel.kt             # State management & business logic
├── voice/
│   └── VoiceCommandManager.kt         # Voice recognition & command processing
├── navigation/
│   └── Navigation.kt                  # Navigation graph & routes
└── ui/theme/
    ├── RecipeListScreen.kt            # Recipe list UI
    ├── RecipeDetailScreen.kt          # Recipe detail UI
    ├── CookingStepScreen.kt           # Step-by-step cooking with voice control
    ├── Theme.kt                       # App theme configuration
    ├── Color.kt                       # Color palette
    └── Type.kt                        # Typography
```

## KEY TECHNOLOGIES & LIBRARIES

### Core Dependencies
- **Jetpack Compose BOM 2024.09.00** - UI toolkit
- **Material 3** - Design system
- **Navigation Compose 2.7.7** - Screen navigation
- **Lifecycle ViewModel Compose 2.7.0** - State management
- **Kotlin Coroutines** - Asynchronous programming
- **StateFlow** - Reactive state management

### Build Configuration
- **Gradle 8.13** - Build automation
- **Android Gradle Plugin 8.13.2**
- **Kotlin 2.0.21**
- **Java 11** compatibility

## ARCHITECTURAL PATTERNS

### MVVM Implementation

**Model (Recipe.kt)**
- Immutable data classes
- Represents business entities
- No business logic

**View (Composable Screens)**
- UI rendering with Jetpack Compose
- Observes ViewModel state via StateFlow
- Triggers ViewModel methods on user interaction
- No direct business logic

**ViewModel (RecipeViewModel.kt)**
- Manages UI state with StateFlow
- Contains business logic
- Survives configuration changes
- Single source of truth for data

### Navigation Pattern
- Sealed class for type-safe routes
- NavHost with composable destinations
- Shared ViewModel across navigation graph
- Back stack management

### State Management
- **StateFlow** for reactive UI updates
- Private mutable state, public immutable exposure
- Kotlin Coroutines for async operations

## CURRENT IMPLEMENTATION STATUS

### ✅ Completed Features
- Basic MVVM architecture
- Recipe list display
- Recipe detail view
- Navigation between screens
- Material 3 theming (light/dark mode)
- Type-safe navigation
- Hardcoded recipe data (5 recipes)
- **Voice-controlled cooking mode** with auto-listening
- Step-by-step recipe navigation with swipe gestures
- Automatic voice recognition restart after commands
- Pause/resume voice control functionality

## DEVELOPMENT GUIDELINES

### Code Style
- Follow Kotlin coding conventions
- Use KDoc comments for public APIs
- Prefer immutable data structures
- Use meaningful variable names

### Jetpack Compose Best Practices
- Keep Composables small and focused
- Use `remember` and `rememberSaveable` appropriately
- Hoist state when needed
- Prefer stateless Composables
- Use Material 3 components

### State Management Rules
- ViewModel holds all UI state
- Use StateFlow for observable state
- Never pass mutable state to Composables
- Handle lifecycle correctly

### Navigation Guidelines
- Use sealed classes for routes
- Pass minimal data through navigation
- Keep ViewModel shared when needed
- Handle back navigation properly

### Testing Strategy
- Unit tests for ViewModels
- Instrumentation tests for UI
- Test business logic thoroughly
- Mock external dependencies

## FILE NAMING CONVENTIONS

- **Screens:** `*Screen.kt` (e.g., `RecipeListScreen.kt`)
- **ViewModels:** `*ViewModel.kt` (e.g., `RecipeViewModel.kt`)
- **Models:** Entity name (e.g., `Recipe.kt`)
- **Navigation:** `Navigation.kt`
- **Theme components:** `Theme.kt`, `Color.kt`, `Type.kt`

## MATERIAL 3 DESIGN

### Color Scheme
- **Primary:** Purple (#6200EE)
- **Secondary:** Pink
- **Dynamic Color:** Enabled on Android 12+
- **Dark Theme:** Supported

### Typography
- Material 3 default typography scale
- Custom font families can be added in `Type.kt`

## GRADLE CONFIGURATION

### Build Variants
- Debug: Development builds with debugging enabled
- Release: Production builds (ProGuard not yet configured)

### SDK Requirements
- **Minimum SDK:** 31 (Android 12)
- **Target SDK:** 36
- **Compile SDK:** 36

## VOICE STEERING IMPLEMENTATION

### Overview
The voice steering system allows hands-free navigation through cooking steps, designed for users with dirty/wet hands while cooking.

### Key Components

#### VoiceCommandManager (`voice/VoiceCommandManager.kt`)
- Manages Android SpeechRecognizer lifecycle
- Implements auto-restart functionality to minimize screen interaction
- Supports pause/resume for manual control
- Recognizes commands: "next", "previous", "forward", "back", "repeat", "start"

**Auto-Restart Behavior:**
- Automatically starts listening when entering cooking mode
- After recognizing a command, waits 1.5 seconds and restarts listening
- Stops auto-restart when user manually pauses
- Handles timeouts gracefully by restarting

#### CookingStepScreen (`ui/theme/CookingStepScreen.kt`)
- Displays one cooking step per page
- HorizontalPager for swipe navigation between steps
- Floating action button: Play/Pause for voice control
- Visual feedback: Shows listening status and recognized text
- Progress indicator at bottom

#### RecipeViewModel Updates
- `startCookingMode()`: Initialize cooking session
- `nextStep()`, `previousStep()`: Navigate between steps
- `goToStep()`: Jump to specific step
- `processVoiceCommand()`: Handle voice commands
- `exitCookingMode()`: Clean up cooking session

### User Experience Flow

1. User taps "Start Cooking" on recipe detail screen
2. App requests RECORD_AUDIO permission (if not granted)
3. Voice recognition automatically starts (no tap needed)
4. User says commands like "next" or "previous"
5. App recognizes command, executes action, and auto-restarts listening
6. User can tap pause button if they need to stop voice control
7. User can swipe left/right as alternative to voice commands

### Permissions
- `RECORD_AUDIO`: Required for voice recognition, requested at runtime

### Voice Commands
- **"next"** / **"forward"** / **"continue"**: Go to next step
- **"previous"** / **"back"** / **"go back"**: Go to previous step
- **"repeat"** / **"again"**: Stay on current step
- **"start"** / **"begin"**: Return to first step

## NOTES FOR AI ASSISTANTS

### When Working on This Project

1. **Respect the MVVM Architecture**
   - Keep business logic in ViewModel
   - Keep UI logic in Composables
   - Models are data classes only

2. **Follow Jetpack Compose Patterns**
   - Use Material 3 components
   - Proper state management
   - Lifecycle-aware code

3. **Maintain Type Safety**
   - Use sealed classes for navigation
   - Leverage Kotlin's type system
   - Avoid magic strings/numbers

4. **Consider Android Lifecycle**
   - Configuration changes
   - Process death scenarios
   - Proper resource cleanup

5. **Testing Requirements**
   - Write tests for new features
   - Unit test ViewModels
   - UI test critical user flows

6. **Code Quality**
   - Document public APIs
   - Follow Kotlin conventions
   - Keep functions focused
   - Avoid over-engineering

---

