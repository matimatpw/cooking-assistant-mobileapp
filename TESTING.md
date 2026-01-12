# Testing Guide

This document explains how to test the voice command functionality without running the app on a device.

## Running Unit Tests

### Using Android Studio
1. Open the project in Android Studio
2. Navigate to the test file you want to run:
   - `app/src/test/java/com/example/cookingassistant/voice/VoiceCommandTranslatorTest.kt`
   - `app/src/test/java/com/example/cookingassistant/viewmodel/RecipeViewModelTest.kt`
3. Right-click on the test class or method
4. Select "Run 'TestClassName'"

### Using Command Line
```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.example.cookingassistant.voice.VoiceCommandTranslatorTest"

# Run with detailed output
./gradlew test --info
```

## What Can Be Tested Without Running the App?

### ✅ Voice Command Parsing
**File:** `VoiceCommandTranslatorTest.kt`

Tests that verify:
- English commands are recognized ("next", "forward", "continue")
- Polish commands are recognized ("następny", "dalej", "kontynuuj")
- Commands work in sentences ("go to the next step")
- Case insensitivity ("NEXT" = "next")
- Invalid commands return null

**Example:**
```kotlin
@Test
fun translate_recognizes_next_command() {
    val result = translator.translate("next")
    assertThat(result).isEqualTo(VoiceCommand.NEXT)
}

@Test
fun translate_recognizes_polish_next_command() {
    val result = translator.translate("następny")
    assertThat(result).isEqualTo(VoiceCommand.NEXT)
}
```

### ✅ Step Navigation Logic
**File:** `RecipeViewModelTest.kt`

Tests that verify:
- Moving to next/previous step changes state correctly
- Can't go beyond first/last step
- Voice commands trigger correct navigation
- Complete cooking flow works end-to-end

**Example:**
```kotlin
@Test
fun process_voice_command_next_advances_step() {
    val recipe = viewModel.getRecipeById(1)!!
    viewModel.startCookingMode(recipe)

    viewModel.processVoiceCommand(VoiceCommand.NEXT)

    assertThat(viewModel.currentStepIndex.value).isEqualTo(1)
}
```

### ❌ Cannot Be Tested Without Device
- Actual microphone speech recognition
- Speech-to-text accuracy
- Audio quality/noise handling
- Real-time voice command performance

These require running on an actual device or emulator with microphone access.

## Test Structure

### VoiceCommandTranslatorTest
- **English commands**: Tests all English voice patterns
- **Polish commands**: Tests all Polish voice patterns
- **Edge cases**: Whitespace, case sensitivity, unrecognized commands

### RecipeViewModelTest
- **Navigation**: Next, previous, go to step
- **Boundaries**: First/last step handling
- **Voice commands**: All command types (NEXT, PREVIOUS, REPEAT, START)
- **Complete flow**: Full cooking session simulation

## Adding Tests for New Languages

To add support for a new language (e.g., German):

1. **Add string resources** (`values-de/strings.xml`):
```xml
<string name="voice_command_next">weiter|nächste|fortsetzen</string>
<string name="voice_command_previous">zurück|vorherige</string>
```

2. **Add test class**:
```kotlin
class VoiceCommandTranslatorGermanTest {
    @Before
    fun setup() {
        // Mock German patterns
        `when`(context.getString(R.string.voice_command_next))
            .thenReturn("weiter|nächste|fortsetzen")
    }

    @Test
    fun translate_recognizes_german_next_command() {
        val result = translator.translate("weiter")
        assertThat(result).isEqualTo(VoiceCommand.NEXT)
    }
}
```

3. **Run tests** to verify the language works correctly

## Continuous Integration

These tests can be integrated into CI/CD pipelines:

```yaml
# Example GitHub Actions
- name: Run Unit Tests
  run: ./gradlew test

- name: Publish Test Results
  uses: EnricoMi/publish-unit-test-result-action@v2
  with:
    files: app/build/test-results/**/*.xml
```

## Test Coverage

To generate test coverage report:

```bash
./gradlew testDebugUnitTestCoverage
```

View the report at:
`app/build/reports/coverage/test/debug/index.html`

## Debugging Tests

### View test output:
```bash
cat app/build/reports/tests/testDebugUnitTest/index.html
```

### Enable verbose logging:
Add to test class:
```kotlin
@get:Rule
val logRule = LoggingRule()
```

## Best Practices

1. **Test behavior, not implementation** - Test what happens, not how it happens
2. **Use descriptive test names** - `translate_recognizes_next_command_with_exact_match`
3. **One assertion per test** - Makes failures easier to diagnose
4. **Test edge cases** - Empty strings, nulls, boundaries
5. **Keep tests fast** - Mock expensive operations (network, database)

## Common Issues

### Tests not found
- Ensure test files are in `app/src/test/java/` directory
- Check package names match source files

### Mockito errors
- Verify Mockito dependencies are in `build.gradle.kts`
- Use `mock()` from `org.mockito.kotlin`

### Coroutine errors
- Use `StandardTestDispatcher` for testing coroutines
- Set `Dispatchers.setMain(testDispatcher)` in `@Before`
- Reset with `Dispatchers.resetMain()` in `@After`
