package com.example.cookingassistant.ui.theme

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cookingassistant.R
import com.example.cookingassistant.model.Recipe
import com.example.cookingassistant.viewmodel.RecipeViewModel
import com.example.cookingassistant.voice.VoiceCommandManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Cooking step screen with swipe navigation and voice control
 * Displays one cooking step at a time with voice command support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookingStepScreen(
    recipe: Recipe,
    viewModel: RecipeViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Voice command manager
    val voiceCommandManager = remember { VoiceCommandManager(context) }

    // State
    val currentStepIndex by viewModel.currentStepIndex.collectAsState()
    val isListening by voiceCommandManager.isListening.collectAsState()
    val recognizedText by voiceCommandManager.recognizedText.collectAsState()
    val allRecognizedText by voiceCommandManager.allRecognizedText.collectAsState()
    var isPaused by remember { mutableStateOf(false) }

    // Flag to prevent circular updates between pager and ViewModel
    var isUpdatingFromViewModel by remember { mutableStateOf(false) }

    // Pager state for swipe navigation
    val pagerState = rememberPagerState(
        initialPage = currentStepIndex,
        pageCount = { recipe.instructions.size }
    )

    // Permission launcher
    var hasRecordPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRecordPermission = isGranted
        if (isGranted) {
            voiceCommandManager.initialize()
        }
    }

    // Auto-restart function with delay
    fun autoRestartListening() {
        scope.launch {
            delay(1500) // Wait 1.5 seconds before restarting
            if (!isPaused && hasRecordPermission) {
                voiceCommandManager.startListening(
                    onCommand = { command ->
                        scope.launch {
                            viewModel.processVoiceCommand(command)
                        }
                    },
                    enableAutoRestart = true,
                    onAutoRestart = { autoRestartListening() }
                )
            }
        }
    }

    // Initialize voice command manager and start auto-listening
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        viewModel.startCookingMode(recipe)
    }

    // Auto-start listening when permission is granted
    LaunchedEffect(hasRecordPermission) {
        if (hasRecordPermission) {
            voiceCommandManager.initialize()
            delay(500) // Small delay to ensure everything is initialized
            voiceCommandManager.startListening(
                onCommand = { command ->
                    scope.launch {
                        viewModel.processVoiceCommand(command)
                    }
                },
                enableAutoRestart = true,
                onAutoRestart = { autoRestartListening() }
            )
        }
    }

    // Sync pager with ViewModel (user swipes)
    LaunchedEffect(pagerState.currentPage) {
        if (!isUpdatingFromViewModel && pagerState.currentPage != currentStepIndex) {
            viewModel.goToStep(pagerState.currentPage)
        }
    }

    // Sync ViewModel with pager (voice commands or programmatic navigation)
    LaunchedEffect(currentStepIndex) {
        if (pagerState.currentPage != currentStepIndex) {
            isUpdatingFromViewModel = true
            pagerState.scrollToPage(currentStepIndex) // Use scrollToPage instead of animateScrollToPage for instant navigation
            isUpdatingFromViewModel = false
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            voiceCommandManager.stopListening()
            voiceCommandManager.destroy()
            viewModel.exitCookingMode()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(recipe.name)
                        Text(
                            text = stringResource(R.string.step_x_of_y, currentStepIndex + 1, recipe.instructions.size),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (hasRecordPermission) {
                FloatingActionButton(
                    onClick = {
                        isPaused = !isPaused
                        if (isPaused) {
                            voiceCommandManager.stopListening(isPause = true)
                        } else {
                            voiceCommandManager.resume()
                            autoRestartListening()
                        }
                    },
                    containerColor = if (isPaused)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.tertiary
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = stringResource(if (isPaused) R.string.resume_voice_control else R.string.pause_voice_control)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Voice recognition feedback
            VoiceRecognitionFeedback(
                isListening = isListening,
                recognizedText = recognizedText,
                isPaused = isPaused
            )

            // Debug: Show all recognized text
            DebugRecognizedTextDisplay(
                allRecognizedText = allRecognizedText
            )

            // Horizontal pager for step navigation
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) { page ->
                CookingStepContent(
                    stepNumber = page + 1,
                    totalSteps = recipe.instructions.size,
                    instruction = recipe.instructions[page]
                )
            }

            // Navigation hints
            NavigationHints(
                currentStep = currentStepIndex,
                totalSteps = recipe.instructions.size
            )
        }
    }
}

/**
 * Individual cooking step content
 */
@Composable
fun CookingStepContent(
    stepNumber: Int,
    totalSteps: Int,
    instruction: String
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Step number badge
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = stringResource(R.string.step_number, stepNumber),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Instruction text
                Text(
                    text = instruction,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

/**
 * Voice recognition feedback indicator
 */
@Composable
fun VoiceRecognitionFeedback(
    isListening: Boolean,
    recognizedText: String?,
    isPaused: Boolean
) {
    val backgroundColor = when {
        isPaused -> MaterialTheme.colorScheme.surfaceVariant
        isListening -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    val textColor = when {
        isPaused -> MaterialTheme.colorScheme.onSurfaceVariant
        isListening -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    // Debug: Always show recognizedText if available, with status
    val displayText = when {
        isPaused -> stringResource(R.string.voice_control_paused)
        isListening && recognizedText != null -> "LISTENING: $recognizedText"
        isListening -> stringResource(R.string.listening)
        recognizedText != null -> "LAST: $recognizedText"
        else -> stringResource(R.string.auto_listening_enabled)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = when {
                    isPaused -> Icons.Default.Pause
                    isListening -> Icons.Default.Mic
                    else -> Icons.Default.PlayArrow
                },
                contentDescription = null,
                tint = textColor
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
    }
}

/**
 * Navigation hints at the bottom of the screen
 */
@Composable
fun NavigationHints(
    currentStep: Int,
    totalSteps: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { (currentStep + 1).toFloat() / totalSteps },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Voice command hints
            Text(
                text = stringResource(R.string.voice_commands_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.swipe_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Debug display showing all recognized text (for testing voice recognition)
 */
@Composable
fun DebugRecognizedTextDisplay(
    allRecognizedText: List<String>
) {
    if (allRecognizedText.isNotEmpty()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = "DEBUG - All Recognized Speech:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        reverseLayout = true // Show newest at bottom
                    ) {
                        items(allRecognizedText.size) { index ->
                            val reversedIndex = allRecognizedText.size - 1 - index
                            Text(
                                text = "${reversedIndex + 1}. ${allRecognizedText[reversedIndex]}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
