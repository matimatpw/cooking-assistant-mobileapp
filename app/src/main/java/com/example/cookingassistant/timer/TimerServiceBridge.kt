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
}

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
