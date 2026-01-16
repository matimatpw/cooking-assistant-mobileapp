package com.example.cookingassistant.ui.theme

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cookingassistant.R
import com.example.cookingassistant.model.MediaType
import com.example.cookingassistant.model.Recipe
import com.example.cookingassistant.timer.TimerServiceBridgeImpl
import com.example.cookingassistant.viewmodel.RecipeViewModel
import com.example.cookingassistant.viewmodel.TtsCallback
import com.example.cookingassistant.voice.TextToSpeechManager
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

    // Text-to-Speech manager
    val ttsManager = remember { TextToSpeechManager(context) }
    val isTtsEnabled by viewModel.isTtsEnabled.collectAsState()
    val isTtsSpeaking by ttsManager.isSpeaking.collectAsState()

    // State
    val currentStepIndex by viewModel.currentStepIndex.collectAsState()
    val isListening by voiceCommandManager.isListening.collectAsState()
    val recognizedText by voiceCommandManager.recognizedText.collectAsState()
    val allRecognizedText by voiceCommandManager.allRecognizedText.collectAsState()
    var isPaused by remember { mutableStateOf(false) }

    // Timer state
    val currentStepTimer by viewModel.currentStepTimer.collectAsState()
    val allTimers by viewModel.timers.collectAsState()

    // Derive active timers from the reactive timers state
    val allActiveTimers = allTimers
        .filter { it.value.status == com.example.cookingassistant.model.TimerStatus.RUNNING ||
                  it.value.status == com.example.cookingassistant.model.TimerStatus.PAUSED }
        .toList()
        .sortedBy { it.first }

    // Timer service bridge
    val timerServiceBridge = remember { TimerServiceBridgeImpl(context) }

    // Flag to prevent circular updates between pager and ViewModel
    var isUpdatingFromViewModel by remember { mutableStateOf(false) }

    // Pager state for swipe navigation
    val pagerState = rememberPagerState(
        initialPage = currentStepIndex,
        pageCount = { recipe.steps.size }
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

    // Initialize components on first composition
    LaunchedEffect(Unit) {
        // Start cooking mode
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        viewModel.startCookingMode(recipe)

        // Initialize timer service bridge
        timerServiceBridge.bindService(viewModel)
        viewModel.setTimerServiceBridge(timerServiceBridge)

        // Set TTS callback
        viewModel.setTtsCallback(object : TtsCallback {
            override fun speakInstruction(instruction: String) {
                ttsManager.speakInstruction(instruction)
            }

            override fun speakIngredients(ingredients: List<com.example.cookingassistant.model.Ingredient>) {
                ttsManager.speakIngredients(ingredients)
            }

            override fun speakDuration(minutes: Int) {
                ttsManager.speakDuration(minutes)
            }

            override fun speakTips(tips: String) {
                ttsManager.speakTips(tips)
            }

            override fun speakStepNumber(stepNumber: Int, totalSteps: Int) {
                ttsManager.speakStepNumber(stepNumber, totalSteps)
            }

            // Timer TTS methods
            override fun speakTimerStarted(minutes: Int) {
                ttsManager.speakTimerStarted(minutes)
            }

            override fun speakTimerPaused(minutes: Int, seconds: Int) {
                ttsManager.speakTimerPaused(minutes, seconds)
            }

            override fun speakTimerResumed() {
                ttsManager.speakTimerResumed()
            }

            override fun speakTimerCancelled() {
                ttsManager.speakTimerCancelled()
            }

            override fun speakTimerFinished(stepNumber: Int) {
                ttsManager.speakTimerFinished(stepNumber)
            }

            override fun speakTimerStatus(minutes: Int, seconds: Int) {
                ttsManager.speakTimerStatus(minutes, seconds)
            }

            override fun speakMessage(message: String) {
                ttsManager.speakMessage(message)
            }
        })

        // Then initialize TTS
        ttsManager.initialize()
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

    // Auto-speak when step changes
    LaunchedEffect(currentStepIndex, isTtsEnabled) {
        if (isTtsEnabled && hasRecordPermission) {
            delay(500) // Small delay to let UI settle
            val step = recipe.steps.getOrNull(currentStepIndex)
            step?.let {
                ttsManager.speakInstruction(it.instruction)
            }
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
            ttsManager.stop()
            ttsManager.destroy()
            timerServiceBridge.unbindService()
            viewModel.setTimerServiceBridge(null)
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
                            text = stringResource(R.string.step_x_of_y, currentStepIndex + 1, recipe.steps.size),
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Timer control button (only if step has duration)
                val currentStep = recipe.steps.getOrNull(currentStepIndex)
                if (currentStep?.durationMinutes != null) {
                    TimerControlButton(
                        timer = currentStepTimer,
                        onStartTimer = { viewModel.startTimer() },
                        onPauseTimer = { viewModel.pauseTimer() },
                        onResumeTimer = { viewModel.resumeTimer() },
                        onStopTimer = { viewModel.stopTimer() }
                    )
                }

                // Voice control toggle
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

                // TTS toggle button
                FloatingActionButton(
                    onClick = {
                        viewModel.toggleTts()
                        if (!isTtsEnabled) {
                            ttsManager.stop()
                        }
                    },
                    containerColor = if (isTtsEnabled)
                        MaterialTheme.colorScheme.secondary
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Icon(
                        imageVector = if (isTtsEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = stringResource(R.string.toggle_tts),
                        tint = if (isTtsEnabled)
                            MaterialTheme.colorScheme.onSecondary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
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

            // TTS speaking feedback
            if (isTtsSpeaking) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.tts_speaking),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Timer status bar (shows countdown for current step)
            currentStepTimer?.let { timer ->
                TimerStatusBar(timer = timer)
            }

            // Active timers overview (shows all timers from other steps)
            if (allActiveTimers.isNotEmpty()) {
                ActiveTimersOverview(
                    timers = allActiveTimers,
                    currentStepIndex = currentStepIndex,
                    onTimerClick = { stepIndex ->
                        scope.launch {
                            viewModel.goToStep(stepIndex)
                        }
                    }
                )
            }

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
                    step = recipe.steps[page],
                    totalSteps = recipe.steps.size
                )
            }

            // Navigation hints
            NavigationHints(
                currentStep = currentStepIndex,
                totalSteps = recipe.steps.size
            )
        }
    }
}

/**
 * Individual cooking step content
 */
@Composable
fun CookingStepContent(
    step: com.example.cookingassistant.model.RecipeStep,
    totalSteps: Int
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Step number badge
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = stringResource(R.string.step_number, step.stepNumber),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Media items (photos/videos)
            if (step.mediaItems.isNotEmpty()) {
                item {
                    StepMediaGallery(mediaItems = step.mediaItems)
                }
            }

            item {
                // Instruction text
                Text(
                    text = step.instruction,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Show duration if available
            step.durationMinutes?.let { duration ->
                item {
                    Text(
                        text = "⏱️ ${duration} min",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Show tips if available
            step.tips?.let { tips ->
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💡 ",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = tips,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Start,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Displays media items (photos/videos) for a cooking step
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepMediaGallery(mediaItems: List<com.example.cookingassistant.model.StepMedia>) {
    if (mediaItems.size == 1) {
        // Single media item - display directly
        StepMediaItem(media = mediaItems[0])
    } else {
        // Multiple media items - use pager
        val mediaPagerState = rememberPagerState(pageCount = { mediaItems.size })

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = mediaPagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                StepMediaItem(media = mediaItems[page])
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Page indicators
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(mediaItems.size) { index ->
                    val color = if (mediaPagerState.currentPage == index) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    }

                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .then(
                                Modifier.then(
                                    Modifier.padding(0.dp)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = color,
                            modifier = Modifier.size(8.dp)
                        ) {}
                    }
                }
            }
        }
    }
}

/**
 * Displays a single media item (photo or video)
 */
@Composable
fun StepMediaItem(media: com.example.cookingassistant.model.StepMedia) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (media.type) {
            MediaType.PHOTO -> {
                AsyncImage(
                    model = media.uri,
                    contentDescription = media.caption,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            MediaType.VIDEO -> {
                // For videos, display thumbnail if available, otherwise show video icon
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (media.thumbnailUri != null) {
                        AsyncImage(
                            model = media.thumbnailUri,
                            contentDescription = media.caption,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Fallback: try to load video directly with Coil
                        AsyncImage(
                            model = media.uri,
                            contentDescription = media.caption,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Video play indicator overlay
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.play_video),
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }

        // Caption if available
        media.caption?.let { caption ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
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
