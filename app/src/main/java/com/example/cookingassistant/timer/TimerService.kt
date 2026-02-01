package com.example.cookingassistant.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.cookingassistant.MainActivity
import com.example.cookingassistant.R
import com.example.cookingassistant.model.TimerState
import com.example.cookingassistant.viewmodel.RecipeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerService : Service() {

    companion object {
        private const val TAG = "TimerService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "cooking_timer_channel"
        private const val CHANNEL_NAME = "Cooking Timers"

        const val ACTION_START_TIMER = "START_TIMER"
        const val ACTION_PAUSE_TIMER = "PAUSE_TIMER"
        const val ACTION_RESUME_TIMER = "RESUME_TIMER"
        const val ACTION_STOP_TIMER = "STOP_TIMER"
        const val ACTION_STOP_ALL_AND_CLEANUP = "STOP_ALL_AND_CLEANUP"

        const val EXTRA_TIMER_STATE = "timer_state"
        const val EXTRA_TIMER_ID = "timer_id"
        const val EXTRA_STEP_INDICES = "step_indices"
    }

    private val binder = TimerServiceBinder()

    private val activeTimers = mutableMapOf<String, TimerJob>()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var notificationManager: NotificationManager

    private lateinit var alarmManager: AlarmManager

    private var viewModel: RecipeViewModel? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        alarmManager = AlarmManager(this)
        createNotificationChannel()
        Log.d(TAG, "TimerService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TIMER -> {
                val timerState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getSerializableExtra(EXTRA_TIMER_STATE, TimerState::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getSerializableExtra(EXTRA_TIMER_STATE) as? TimerState
                }
                timerState?.let { startTimer(it) }
            }
            ACTION_PAUSE_TIMER -> {
                val timerId = intent.getStringExtra(EXTRA_TIMER_ID)
                timerId?.let { pauseTimer(it) }
            }
            ACTION_RESUME_TIMER -> {
                val timerState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getSerializableExtra(EXTRA_TIMER_STATE, TimerState::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getSerializableExtra(EXTRA_TIMER_STATE) as? TimerState
                }
                timerState?.let { resumeTimer(it) }
            }
            ACTION_STOP_TIMER -> {
                val timerId = intent.getStringExtra(EXTRA_TIMER_ID)
                timerId?.let { stopTimer(it) }
            }
            ACTION_STOP_ALL_AND_CLEANUP -> {
                val stepIndices = intent.getIntArrayExtra(EXTRA_STEP_INDICES)?.toList() ?: emptyList()
                stopAllTimersAndCleanup(stepIndices)
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        activeTimers.clear()
        Log.d(TAG, "TimerService destroyed")
    }

    private fun startTimer(timer: TimerState) {
        Log.d(TAG, "Starting timer: ${timer.timerId} for step ${timer.stepIndex} (${timer.durationSeconds}s)")

        activeTimers[timer.timerId]?.job?.cancel()

        val timerId = timer.timerId

        val job = serviceScope.launch {
            var remaining = timer.remainingSeconds

            while (remaining > 0 && isActive) {
                delay(1000)
                remaining--

                activeTimers[timerId]?.currentRemainingSeconds = remaining

                viewModel?.updateTimerState(timerId, remaining)

                updateNotification()
            }

            if (remaining == 0) {
                onTimerFinished(timer)
            }
        }

        activeTimers[timerId] = TimerJob(timer, job)

        startForeground(NOTIFICATION_ID, buildNotification())
    }

    private fun pauseTimer(timerId: String) {
        Log.d(TAG, "Pausing timer: $timerId")
        activeTimers[timerId]?.job?.cancel()
        updateNotification()
    }

    private fun resumeTimer(timer: TimerState) {
        Log.d(TAG, "Resuming timer: ${timer.timerId}")
        startTimer(timer)
    }

    private fun stopTimer(timerId: String) {
        Log.d(TAG, "Stopping timer: $timerId")
        activeTimers[timerId]?.job?.cancel()
        activeTimers.remove(timerId)

        if (activeTimers.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else {
            updateNotification()
        }
    }

    private fun stopAllTimersAndCleanup(stepIndices: List<Int>) {
        Log.d(TAG, "Stopping all timers and cleaning up, step indices: $stepIndices")

        activeTimers.values.forEach { timerJob ->
            timerJob.job.cancel()
        }
        activeTimers.clear()

        alarmManager.cancelAllAlarmNotifications(stepIndices)

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        Log.d(TAG, "All timers stopped and notifications cleaned up")
    }

    private fun onTimerFinished(timer: TimerState) {
        Log.d(TAG, "Timer finished: ${timer.timerId} for step ${timer.stepIndex}")

        alarmManager.triggerTimerAlarm(timer)

        viewModel?.updateTimerState(timer.timerId, 0)

        activeTimers.remove(timer.timerId)

        if (activeTimers.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else {
            updateNotification()
        }
    }

    private fun buildNotification(): Notification {
        val lowestTimerStep = activeTimers.values
            .filter { it.currentRemainingSeconds > 0 }
            .minByOrNull { it.currentRemainingSeconds }
            ?.timer

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to_cooking", true)
            putExtra("recipe_id", lowestTimerStep?.recipeId ?: "")
            putExtra("step_index", lowestTimerStep?.stepIndex ?: 0)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val activeTimersList = activeTimers.values.joinToString("\n") { timerJob ->
            val minutes = timerJob.currentRemainingSeconds / 60
            val seconds = timerJob.currentRemainingSeconds % 60
            "Step ${timerJob.timer.stepIndex + 1}: ${minutes}:${seconds.toString().padStart(2, '0')}"
        }

        val contentTitle = if (activeTimers.size == 1) {
            "Cooking Timer Active"
        } else {
            "${activeTimers.size} Cooking Timers Active"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(contentTitle)
            .setContentText("Tap to return to cooking mode")
            .setStyle(NotificationCompat.BigTextStyle().bigText(activeTimersList))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    private fun updateNotification() {
        if (activeTimers.isEmpty()) return
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for active cooking timers"
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun setViewModel(vm: RecipeViewModel) {
        viewModel = vm
    }

    fun getActiveTimerStates(): Map<String, Pair<TimerState, Int>> {
        return activeTimers.mapValues { (_, timerJob) ->
            Pair(timerJob.timer, timerJob.currentRemainingSeconds)
        }
    }

    fun hasActiveTimersForRecipe(recipeId: String): Boolean {
        return activeTimers.values.any { it.timer.recipeId == recipeId && it.currentRemainingSeconds > 0 }
    }

    fun hasAnyActiveTimers(): Boolean {
        return activeTimers.values.any { it.currentRemainingSeconds > 0 }
    }

    fun getActiveTimersRecipeId(): String? {
        return activeTimers.values.firstOrNull { it.currentRemainingSeconds > 0 }?.timer?.recipeId
    }

    inner class TimerServiceBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    private class TimerJob(
        val timer: TimerState,
        val job: Job,
        var currentRemainingSeconds: Int = timer.remainingSeconds
    )
}
