package com.example.cookingassistant.timer

import com.example.cookingassistant.model.TimerState

interface TimerServiceBridge {
    fun startTimer(timer: TimerState)

    fun pauseTimer(timerId: String)

    fun resumeTimer(timerId: String, updatedTimer: TimerState)

    fun stopTimer(timerId: String)

    fun stopAllTimersAndCleanup(stepIndices: List<Int>)

    fun hasActiveTimersForRecipe(recipeId: String): Boolean

    fun hasAnyActiveTimers(): Boolean

    fun getActiveTimersRecipeId(): String?

    fun getActiveTimersForRecipe(recipeId: String): List<TimerRestoreData>
}
data class TimerRestoreData(
    val timerId: String,
    val stepIndex: Int,
    val durationSeconds: Int,
    val remainingSeconds: Int
)

interface TimerAlarmCallback {
    fun onTimerFinished(timer: TimerState)
}
