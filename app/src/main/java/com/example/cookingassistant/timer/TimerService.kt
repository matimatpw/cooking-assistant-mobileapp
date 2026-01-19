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

/**
 * Foreground service for managing cooking timers in background
 * Runs even when app is closed/backgrounded
 *
 * Key features:
 * - Runs timer countdowns via coroutines
 * - Shows persistent notification with active timers
 * - Updates ViewModel with remaining time
 * - Survives app backgrounding and configuration changes
 */
class TimerService : Service() {

    companion object {
        private const val TAG = "TimerService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "cooking_timer_channel"
        private const val CHANNEL_NAME = "Cooking Timers"

        // Intent actions
        const val ACTION_START_TIMER = "START_TIMER"
        const val ACTION_PAUSE_TIMER = "PAUSE_TIMER"
        const val ACTION_RESUME_TIMER = "RESUME_TIMER"
        const val ACTION_STOP_TIMER = "STOP_TIMER"

        // Intent extras
        const val EXTRA_TIMER_STATE = "timer_state"
        const val EXTRA_TIMER_ID = "timer_id"
    }

    private val binder = TimerServiceBinder()

    // Active timers managed by this service
    private val activeTimers = mutableMapOf<String, TimerJob>()

    // Coroutine scope for timer jobs
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Notification manager
    private lateinit var notificationManager: NotificationManager

    // Alarm manager for timer completion
    private lateinit var alarmManager: AlarmManager

    // Reference to ViewModel (set via binder)
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
        }

        return START_STICKY // Restart service if killed
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

    /**
     * Start countdown for a timer
     */
    private fun startTimer(timer: TimerState) {
        Log.d(TAG, "Starting timer: ${timer.timerId} for step ${timer.stepIndex} (${timer.durationSeconds}s)")

        // Cancel existing job if any
        activeTimers[timer.timerId]?.job?.cancel()

        // Create countdown job
        val job = serviceScope.launch {
            var remaining = timer.remainingSeconds

            while (remaining > 0 && isActive) {
                delay(1000) // 1 second intervals
                remaining--

                // Update ViewModel via callback
                viewModel?.updateTimerState(timer.timerId, remaining)

                // Update notification
                updateNotification()
            }

            if (remaining == 0) {
                // Timer finished
                onTimerFinished(timer)
            }
        }

        activeTimers[timer.timerId] = TimerJob(timer, job)

        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    /**
     * Pause a running timer
     */
    private fun pauseTimer(timerId: String) {
        Log.d(TAG, "Pausing timer: $timerId")
        activeTimers[timerId]?.job?.cancel()
        updateNotification()
    }

    /**
     * Resume a paused timer
     */
    private fun resumeTimer(timer: TimerState) {
        Log.d(TAG, "Resuming timer: ${timer.timerId}")
        startTimer(timer) // Restart with updated state
    }

    /**
     * Stop and remove a timer
     */
    private fun stopTimer(timerId: String) {
        Log.d(TAG, "Stopping timer: $timerId")
        activeTimers[timerId]?.job?.cancel()
        activeTimers.remove(timerId)

        // Stop foreground service if no timers left
        if (activeTimers.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else {
            updateNotification()
        }
    }

    /**
     * Handle timer completion
     */
    private fun onTimerFinished(timer: TimerState) {
        Log.d(TAG, "Timer finished: ${timer.timerId} for step ${timer.stepIndex}")

        // Trigger alarm system (TTS + vibration + sound)
        alarmManager.triggerTimerAlarm(timer)

        // Update ViewModel
        viewModel?.updateTimerState(timer.timerId, 0)

        // Remove from active timers
        activeTimers.remove(timer.timerId)

        // Stop service if no more timers
        if (activeTimers.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else {
            updateNotification()
        }
    }

    /**
     * Build notification showing active timers
     */
    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification content
        val activeTimersList = activeTimers.values.joinToString("\n") { timerJob ->
            val timer = timerJob.timer
            val minutes = timer.remainingSeconds / 60
            val seconds = timer.remainingSeconds % 60
            "Step ${timer.stepIndex + 1}: ${minutes}:${seconds.toString().padStart(2, '0')}"
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
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use app icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
    }

    /**
     * Update notification with current timer states
     */
    private fun updateNotification() {
        if (activeTimers.isEmpty()) return
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    /**
     * Create notification channel (required for Android O+)
     */
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

    /**
     * Set ViewModel reference (called via binder)
     */
    fun setViewModel(vm: RecipeViewModel) {
        viewModel = vm
    }

    /**
     * Binder for local service binding
     */
    inner class TimerServiceBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    /**
     * Data class holding timer state and coroutine job
     */
    private data class TimerJob(
        val timer: TimerState,
        val job: Job
    )
}
