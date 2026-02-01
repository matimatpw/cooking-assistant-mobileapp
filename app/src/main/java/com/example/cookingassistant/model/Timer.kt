package com.example.cookingassistant.model

import kotlinx.serialization.Serializable
import java.io.Serializable as JavaSerializable
import java.util.UUID

@Serializable
data class TimerState(
    val stepIndex: Int,
    val recipeId: String,
    val durationSeconds: Int,
    val remainingSeconds: Int,
    val status: TimerStatus,
    val startTimestamp: Long,
    val pauseTimestamp: Long? = null,
    val timerId: String = UUID.randomUUID().toString()
) : JavaSerializable

@Serializable
enum class TimerStatus {
    /** Timer has not been started yet */
    IDLE,

    /** Timer is actively counting down */
    RUNNING,

    /** Timer has been paused by the user */
    PAUSED,

    /** Timer has reached zero */
    FINISHED,

    /** Timer was cancelled by the user */
    CANCELLED
}

@Serializable
data class TimerSnapshot(
    val timers: List<TimerState>,
    val activeRecipeId: String?,
    val currentStepIndex: Int,
    val savedAt: Long = System.currentTimeMillis()
)
