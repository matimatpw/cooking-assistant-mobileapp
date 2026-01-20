package com.example.cookingassistant.timer

import com.example.cookingassistant.model.TimerState

/**
 * Interface for communication between ViewModel and TimerService
 * Decouples ViewModel from Android Service dependencies
 *
 * This bridge pattern allows the ViewModel to remain testable and
 * independent of Android framework components.
 */
interface TimerServiceBridge {
    /**
     * Start a new timer countdown
     * @param timer The timer state to start
     */
    fun startTimer(timer: TimerState)

    /**
     * Pause an active timer
     * @param timerId Unique identifier of the timer to pause
     */
    fun pauseTimer(timerId: String)

    /**
     * Resume a paused timer
     * @param timerId Unique identifier of the timer to resume
     * @param updatedTimer Updated timer state with new start timestamp
     */
    fun resumeTimer(timerId: String, updatedTimer: TimerState)

    /**
     * Stop and remove a timer
     * @param timerId Unique identifier of the timer to stop
     */
    fun stopTimer(timerId: String)

    /**
     * Stop all timers and cleanup all notifications
     * Called when user exits cooking mode
     * @param stepIndices List of step indices that had timers (for cancelling alarm notifications)
     */
    fun stopAllTimersAndCleanup(stepIndices: List<Int>)

    /**
     * Check if there are active timers for a specific recipe
     * @param recipeId The recipe ID to check
     * @return true if there are running timers for this recipe
     */
    fun hasActiveTimersForRecipe(recipeId: String): Boolean

    /**
     * Check if there are any active timers running
     * @return true if there are any running timers
     */
    fun hasAnyActiveTimers(): Boolean

    /**
     * Get the recipe ID of the currently running timers
     * @return recipe ID if there are active timers, null otherwise
     */
    fun getActiveTimersRecipeId(): String?

    /**
     * Get all active timer states for a recipe
     * @param recipeId The recipe ID to get timers for
     * @return List of TimerRestoreData (timerId, stepIndex, durationSeconds, remainingSeconds)
     */
    fun getActiveTimersForRecipe(recipeId: String): List<TimerRestoreData>
}

/**
 * Data class for restoring timer state
 * Contains all information needed to restore a timer in the ViewModel
 */
data class TimerRestoreData(
    val timerId: String,
    val stepIndex: Int,
    val durationSeconds: Int,
    val remainingSeconds: Int
)

/**
 * Callback interface for timer alarm events
 * Implemented by UI layer to handle timer completion alarms
 */
interface TimerAlarmCallback {
    /**
     * Called when a timer finishes (reaches zero)
     * Should trigger TTS announcement, vibration, and alarm sound
     *
     * @param timer The timer that finished
     */
    fun onTimerFinished(timer: TimerState)
}
