package com.example.cookingassistant.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cookingassistant.model.TimerState
import com.example.cookingassistant.model.TimerStatus

@Composable
fun TimerStatusBar(timer: TimerState) {
    val minutes = timer.remainingSeconds / 60
    val seconds = timer.remainingSeconds % 60

    val backgroundColor = when (timer.status) {
        TimerStatus.RUNNING -> MaterialTheme.colorScheme.primaryContainer
        TimerStatus.PAUSED -> MaterialTheme.colorScheme.secondaryContainer
        TimerStatus.FINISHED -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when (timer.status) {
        TimerStatus.RUNNING -> MaterialTheme.colorScheme.onPrimaryContainer
        TimerStatus.PAUSED -> MaterialTheme.colorScheme.onSecondaryContainer
        TimerStatus.FINISHED -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (timer.status) {
                    TimerStatus.RUNNING -> Icons.Default.Timer
                    TimerStatus.PAUSED -> Icons.Default.Pause
                    TimerStatus.FINISHED -> Icons.Default.Done
                    else -> Icons.Default.Timer
                },
                contentDescription = null,
                tint = textColor
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = when (timer.status) {
                    TimerStatus.RUNNING -> "Running"
                    TimerStatus.PAUSED -> "Paused"
                    TimerStatus.FINISHED -> "Finished!"
                    else -> ""
                },
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
    }
}

@Composable
fun TimerControlButton(
    timer: TimerState?,
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onStopTimer: () -> Unit
) {
    when {
        timer == null || timer.status == TimerStatus.CANCELLED -> {
            FloatingActionButton(
                onClick = onStartTimer,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Start Timer"
                )
            }
        }
        timer.status == TimerStatus.RUNNING -> {
            FloatingActionButton(
                onClick = onPauseTimer,
                containerColor = MaterialTheme.colorScheme.tertiary
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause Timer"
                )
            }
        }
        timer.status == TimerStatus.PAUSED -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = onResumeTimer,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume Timer"
                    )
                }

                FloatingActionButton(
                    onClick = onStopTimer,
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Stop Timer"
                    )
                }
            }
        }
        timer.status == TimerStatus.FINISHED -> {
            FloatingActionButton(
                onClick = onStopTimer,
                containerColor = MaterialTheme.colorScheme.tertiary
            ) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = "Timer Finished"
                )
            }
        }
    }
}

@Composable
fun ActiveTimersOverview(
    timers: List<Pair<Int, TimerState>>,
    currentStepIndex: Int,
    onTimerClick: (Int) -> Unit
) {
    if (timers.isEmpty()) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = "Active Timers",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(timers.size) { index ->
                    val (stepIndex, timer) = timers[index]
                    TimerChip(
                        stepIndex = stepIndex,
                        timer = timer,
                        isCurrent = stepIndex == currentStepIndex,
                        onClick = { onTimerClick(stepIndex) }
                    )
                }
            }
        }
    }
}

@Composable
fun TimerChip(
    stepIndex: Int,
    timer: TimerState,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val minutes = timer.remainingSeconds / 60
    val seconds = timer.remainingSeconds % 60

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isCurrent)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.secondaryContainer,
        border = if (isCurrent)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Step ${stepIndex + 1}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
            )

            Icon(
                imageVector = if (timer.status == TimerStatus.RUNNING)
                    Icons.Default.Timer
                else
                    Icons.Default.Pause,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
