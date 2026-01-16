package com.example.cookingassistant.model

import kotlinx.serialization.Serializable
import java.io.Serializable as JavaSerializable
import java.util.UUID

/**
 * Represents the state of a cooking timer for a specific recipe step
 *
 * @param stepIndex The index of the recipe step this timer belongs to
 * @param recipeId The ID of the recipe this timer belongs to
 * @param durationSeconds Total duration of the timer in seconds
 * @param remainingSeconds Time remaining in seconds
 * @param status Current status of the timer
 * @param startTimestamp When the timer was started (System.currentTimeMillis())
 * @param pauseTimestamp When the timer was paused (null if not paused)
 * @param timerId Unique identifier for this timer
 */
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

/**
 * Status of a cooking timer
 */
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

/**
 * Snapshot of all timer states for persistence
 * Used to save and restore timer state when app is backgrounded or restarted
 *
 * @param timers List of all active timer states
 * @param activeRecipeId ID of the currently active recipe
 * @param currentStepIndex Current step index in cooking mode
 * @param savedAt Timestamp when this snapshot was saved
 */
@Serializable
data class TimerSnapshot(
    val timers: List<TimerState>,
    val activeRecipeId: String?,
    val currentStepIndex: Int,
    val savedAt: Long = System.currentTimeMillis()
)
