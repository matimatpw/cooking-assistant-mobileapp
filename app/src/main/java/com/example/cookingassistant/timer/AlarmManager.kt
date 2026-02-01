package com.example.cookingassistant.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.cookingassistant.MainActivity
import com.example.cookingassistant.R
import com.example.cookingassistant.model.TimerState
import com.example.cookingassistant.voice.TextToSpeechManager

class AlarmManager(private val context: Context) {

    companion object {
        private const val TAG = "AlarmManager"
        private const val ALARM_NOTIFICATION_ID_BASE = 2000
        private const val ALARM_CHANNEL_ID = "timer_alarm_channel"
        private const val ALARM_CHANNEL_NAME = "Timer Alarms"

        private val VIBRATION_PATTERN = longArrayOf(0, 500, 200, 500, 200, 500)
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val ttsManager = TextToSpeechManager(context)
    private val alarmPlayer = AlarmSoundPlayer(context)

    init {
        createAlarmNotificationChannel()
        ttsManager.initialize()
    }

    fun triggerTimerAlarm(timer: TimerState) {
        Log.d(TAG, "Triggering alarm for timer: ${timer.timerId} (step ${timer.stepIndex})")

        vibrateDevice()

        alarmPlayer.playAlarm()

        showAlarmNotification(timer)
    }

    private fun vibrateDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(VIBRATION_PATTERN, -1) // -1 = don't repeat
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(VIBRATION_PATTERN, -1)
        }
        Log.d(TAG, "Device vibrated")
    }

    private fun showAlarmNotification(timer: TimerState) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to_cooking", true)
            putExtra("recipe_id", timer.recipeId)
            putExtra("step_index", timer.stepIndex)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            ALARM_NOTIFICATION_ID_BASE + timer.stepIndex,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, AlarmDismissReceiver::class.java).apply {
            putExtra("timer_id", timer.timerId)
            putExtra("notification_id", ALARM_NOTIFICATION_ID_BASE + timer.stepIndex)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_NOTIFICATION_ID_BASE + timer.stepIndex,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setContentTitle("Timer Finished!")
            .setContentText("Step ${timer.stepIndex + 1} is complete")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            .setVibrate(VIBRATION_PATTERN)
            .setSound(alarmPlayer.getAlarmSoundUri())
            .setFullScreenIntent(pendingIntent, true) // Show on lock screen
            .build()

        notificationManager.notify(ALARM_NOTIFICATION_ID_BASE + timer.stepIndex, notification)
        Log.d(TAG, "Alarm notification shown for step ${timer.stepIndex}")
    }

    private fun createAlarmNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALARM_CHANNEL_ID,
                ALARM_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm notifications when cooking timers finish"
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                vibrationPattern = VIBRATION_PATTERN
                setSound(
                    alarmPlayer.getAlarmSoundUri(),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun cancelAllAlarmNotifications(stepIndices: List<Int>) {
        stepIndices.forEach { stepIndex ->
            notificationManager.cancel(ALARM_NOTIFICATION_ID_BASE + stepIndex)
            Log.d(TAG, "Cancelled alarm notification for step $stepIndex")
        }
        alarmPlayer.stop()
        Log.d(TAG, "Cancelled all alarm notifications and stopped alarm sound")
    }

    fun stopAlarmSound() {
        alarmPlayer.stop()
    }

    fun cleanup() {
        ttsManager.destroy()
        alarmPlayer.stop()
    }
}
