package com.example.cookingassistant.timer

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log

class AlarmSoundPlayer(private val context: Context) {

    companion object {
        private const val TAG = "AlarmSoundPlayer"
        private const val ALARM_DURATION_MS = 5000L
    }

    private var mediaPlayer: MediaPlayer? = null

    fun getAlarmSoundUri(): Uri {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: Settings.System.DEFAULT_NOTIFICATION_URI
    }

    fun playAlarm() {
        stop()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, getAlarmSoundUri())
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = false
                prepare()
                start()
            }

            Log.d(TAG, "Alarm sound started")

            Handler(Looper.getMainLooper()).postDelayed({
                stop()
            }, ALARM_DURATION_MS)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to play alarm sound", e)
        }
    }

    fun stop() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
                Log.d(TAG, "Alarm sound stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping alarm sound", e)
            } finally {
                mediaPlayer = null
            }
        }
    }
}
