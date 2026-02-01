package com.example.cookingassistant.ui.theme

import android.Manifest
import android.os.Build
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookingStepScreen(
    recipe: Recipe,
    viewModel: RecipeViewModel,
    initialStepIndex: Int = 0,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val voiceCommandManager = remember { VoiceCommandManager(context) }

    val ttsManager = remember { TextToSpeechManager(context) }
    val isTtsEnabled by viewModel.isTtsEnabled.collectAsState()
    val isTtsSpeaking by ttsManager.isSpeaking.collectAsState()

    val currentStepIndex by viewModel.currentStepIndex.collectAsState()
    val isListening by voiceCommandManager.isListening.collectAsState()
    val recognizedText by voiceCommandManager.recognizedText.collectAsState()
    val allRecognizedText by voiceCommandManager.allRecognizedText.collectAsState()
    var isPaused by remember { mutableStateOf(false) }

    var showExitConfirmationDialog by remember { mutableStateOf(false) }

    var showResumeDialog by remember { mutableStateOf(false) }
    var showDifferentRecipeDialog by remember { mutableStateOf(false) }
    var hasCheckedForExistingTimers by remember { mutableStateOf(false) }
    var cookingInitialized by remember { mutableStateOf(false) }

    var targetStepIndex by remember { mutableStateOf<Int?>(null) }

    val currentStepTimer by viewModel.currentStepTimer.collectAsState()
    val allTimers by viewModel.timers.collectAsState()

    val allActiveTimers = allTimers
        .filter { it.value.status == com.example.cookingassistant.model.TimerStatus.RUNNING ||
                  it.value.status == com.example.cookingassistant.model.TimerStatus.PAUSED }
        .toList()
        .sortedBy { it.first }

    val timerServiceBridge = remember { TimerServiceBridgeImpl(context) }

    var isUpdatingFromViewModel by remember { mutableStateOf(false) }

    var hasRecordPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRecordPermission = isGranted
        if (isGranted) {
            voiceCommandManager.initialize()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
    }

    fun autoRestartListening() {
        scope.launch {
            delay(1500)
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

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        timerServiceBridge.bindService(viewModel)
        viewModel.setTimerServiceBridge(timerServiceBridge)

        delay(100)

        val hasTimersForThisRecipe = timerServiceBridge.hasActiveTimersForRecipe(recipe.id)
        val hasAnyTimers = timerServiceBridge.hasAnyActiveTimers()
        val otherRecipeId = timerServiceBridge.getActiveTimersRecipeId()
        hasCheckedForExistingTimers = true

        when {
            hasAnyTimers && otherRecipeId != null && otherRecipeId != recipe.id -> {
                showDifferentRecipeDialog = true
            }
            hasTimersForThisRecipe && viewModel.activeRecipe.value?.id != recipe.id -> {
                showResumeDialog = true
            }
            viewModel.activeRecipe.value?.id == recipe.id -> {
                viewModel.restoreTimerStateFromService(recipe.id)
                val activeTimersList = timerServiceBridge.getActiveTimersForRecipe(recipe.id)
                val computedTargetStep = if (activeTimersList.isNotEmpty()) {
                    activeTimersList.minByOrNull { it.remainingSeconds }?.stepIndex ?: initialStepIndex
                } else {
                    initialStepIndex
                }
                targetStepIndex = computedTargetStep
                viewModel.startOrResumeCookingMode(recipe, computedTargetStep)
                cookingInitialized = true
            }
            else -> {
                targetStepIndex = initialStepIndex
                viewModel.startCookingMode(recipe, initialStepIndex)
                cookingInitialized = true
            }
        }

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

        ttsManager.initialize()
    }

    LaunchedEffect(hasRecordPermission) {
        if (hasRecordPermission) {
            voiceCommandManager.initialize()
            delay(500)
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

    LaunchedEffect(currentStepIndex, isTtsEnabled) {
        if (isTtsEnabled && hasRecordPermission) {
            delay(500)
            val step = recipe.steps.getOrNull(currentStepIndex)
            step?.let {
                ttsManager.speakInstruction(it.instruction)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceCommandManager.stopListening()
            voiceCommandManager.destroy()
            ttsManager.stop()
            ttsManager.destroy()
            timerServiceBridge.unbindService()
            viewModel.setTimerServiceBridge(null)
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
                    IconButton(onClick = {
                        if (allActiveTimers.isNotEmpty()) {
                            showExitConfirmationDialog = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
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
            VoiceRecognitionFeedback(
                isListening = isListening,
                recognizedText = recognizedText,
                isPaused = isPaused
            )

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

            currentStepTimer?.let { timer ->
                TimerStatusBar(timer = timer)
            }

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

            DebugRecognizedTextDisplay(
                allRecognizedText = allRecognizedText
            )

            if (cookingInitialized && targetStepIndex != null) {
                val pagerState = rememberPagerState(
                    initialPage = targetStepIndex!!.coerceIn(0, recipe.steps.size - 1),
                    pageCount = { recipe.steps.size }
                )

                LaunchedEffect(pagerState.currentPage) {
                    if (!isUpdatingFromViewModel && pagerState.currentPage != currentStepIndex) {
                        viewModel.goToStep(pagerState.currentPage)
                    }
                }

                LaunchedEffect(currentStepIndex) {
                    if (pagerState.currentPage != currentStepIndex) {
                        isUpdatingFromViewModel = true
                        pagerState.scrollToPage(currentStepIndex)
                        isUpdatingFromViewModel = false
                    }
                }

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
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }

            NavigationHints(
                currentStep = currentStepIndex,
                totalSteps = recipe.steps.size
            )
        }
    }

    if (showExitConfirmationDialog) {
        ExitCookingConfirmationDialog(
            activeTimers = allActiveTimers,
            onKeepCooking = { showExitConfirmationDialog = false },
            onExitAndStopTimers = {
                showExitConfirmationDialog = false
                viewModel.exitCookingMode()
                onNavigateBack()
            }
        )
    }
    fun findStepWithLowestTimer(): Int {
        val activeTimersList = timerServiceBridge.getActiveTimersForRecipe(recipe.id)
        return if (activeTimersList.isNotEmpty()) {
            activeTimersList.minByOrNull { it.remainingSeconds }?.stepIndex ?: initialStepIndex
        } else {
            initialStepIndex
        }
    }

    if (showResumeDialog) {
        ResumeCookingDialog(
            onContinue = {
                showResumeDialog = false
                viewModel.restoreTimerStateFromService(recipe.id)
                val computedTargetStep = findStepWithLowestTimer()
                targetStepIndex = computedTargetStep
                viewModel.startOrResumeCookingMode(recipe, computedTargetStep)
                cookingInitialized = true
            },
            onStartFresh = {
                showResumeDialog = false
                targetStepIndex = initialStepIndex
                viewModel.startCookingFresh(recipe, initialStepIndex)
                cookingInitialized = true
            }
        )
    }

    if (showDifferentRecipeDialog) {
        DifferentRecipeWarningDialog(
            onStopAndStart = {
                showDifferentRecipeDialog = false
                viewModel.exitCookingMode()
                targetStepIndex = initialStepIndex
                viewModel.startCookingMode(recipe, initialStepIndex)
                cookingInitialized = true
            },
            onGoBack = {
                showDifferentRecipeDialog = false
                onNavigateBack()
            }
        )
    }
}

@Composable
fun ExitCookingConfirmationDialog(
    activeTimers: List<Pair<Int, com.example.cookingassistant.model.TimerState>>,
    onKeepCooking: () -> Unit,
    onExitAndStopTimers: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onKeepCooking,
        title = {
            Text(text = stringResource(R.string.exit_cooking_title))
        },
        text = {
            Column {
                Text(text = stringResource(R.string.exit_cooking_message))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.exit_cooking_active_timers),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                activeTimers.forEach { (stepIndex, timer) ->
                    val minutes = timer.remainingSeconds / 60
                    val seconds = timer.remainingSeconds % 60
                    Text(
                        text = stringResource(
                            R.string.exit_cooking_timer_item,
                            stepIndex + 1,
                            minutes,
                            seconds.toString().padStart(2, '0')
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onExitAndStopTimers) {
                Text(
                    text = stringResource(R.string.exit_stop_timers),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onKeepCooking) {
                Text(text = stringResource(R.string.keep_cooking))
            }
        }
    )
}

@Composable
fun ResumeCookingDialog(
    onContinue: () -> Unit,
    onStartFresh: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onContinue,
        title = {
            Text(text = stringResource(R.string.resume_cooking_title))
        },
        text = {
            Text(text = stringResource(R.string.resume_cooking_message))
        },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text(text = stringResource(R.string.continue_cooking))
            }
        },
        dismissButton = {
            TextButton(onClick = onStartFresh) {
                Text(
                    text = stringResource(R.string.start_fresh),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}

@Composable
fun DifferentRecipeWarningDialog(
    onStopAndStart: () -> Unit,
    onGoBack: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onGoBack,
        title = {
            Text(text = stringResource(R.string.different_recipe_title))
        },
        text = {
            Text(text = stringResource(R.string.different_recipe_message))
        },
        confirmButton = {
            TextButton(onClick = onStopAndStart) {
                Text(
                    text = stringResource(R.string.stop_and_start),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onGoBack) {
                Text(text = stringResource(R.string.go_back))
            }
        }
    )
}

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

            if (step.mediaItems.isNotEmpty()) {
                item {
                    StepMediaGallery(mediaItems = step.mediaItems)
                }
            }

            item {
                Text(
                    text = step.instruction,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            step.durationMinutes?.let { duration ->
                item {
                    Text(
                        text = "⏱️ ${duration} min",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepMediaGallery(mediaItems: List<com.example.cookingassistant.model.StepMedia>) {
    if (mediaItems.size == 1) {
        StepMediaItem(media = mediaItems[0])
    } else {
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
                        AsyncImage(
                            model = media.uri,
                            contentDescription = media.caption,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

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
            LinearProgressIndicator(
                progress = { (currentStep + 1).toFloat() / totalSteps },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                        reverseLayout = true
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
