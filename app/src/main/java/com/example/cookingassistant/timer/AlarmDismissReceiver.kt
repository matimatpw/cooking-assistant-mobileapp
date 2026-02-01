package com.example.cookingassistant.timer

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmDismissReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmDismissReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra("notification_id", -1)
        val timerId = intent.getStringExtra("timer_id")

        Log.d(TAG, "Dismissing alarm notification: notificationId=$notificationId, timerId=$timerId")

        if (notificationId != -1) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)

            val alarmPlayer = AlarmSoundPlayer(context)
            alarmPlayer.stop()

            Log.d(TAG, "Alarm dismissed for notification ID: $notificationId")
        }
    }
}
