package com.example.cookingassistant.timer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.cookingassistant.model.TimerState
import com.example.cookingassistant.viewmodel.RecipeViewModel

/**
 * Implementation of TimerServiceBridge
 * Manages communication between ViewModel and TimerService
 *
 * This bridge pattern allows the ViewModel to remain testable and
 * independent of Android framework components.
 */
class TimerServiceBridgeImpl(
    private val context: Context
) : TimerServiceBridge {

    companion object {
        private const val TAG = "TimerServiceBridge"
    }

    private var serviceConnection: ServiceConnection? = null
    private var boundService: TimerService? = null

    /**
     * Bind to TimerService and set ViewModel reference
     * @param viewModel The ViewModel to connect to the service
     */
    fun bindService(viewModel: RecipeViewModel) {
        val intent = Intent(context, TimerService::class.java)

        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val serviceBinder = binder as? TimerService.TimerServiceBinder
                boundService = serviceBinder?.getService()
                boundService?.setViewModel(viewModel)
                Log.d(TAG, "Service connected and ViewModel set")
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                boundService = null
                Log.d(TAG, "Service disconnected")
            }
        }

        context.bindService(intent, serviceConnection!!, Context.BIND_AUTO_CREATE)
        Log.d(TAG, "Binding to TimerService")
    }

    /**
     * Unbind from TimerService
     */
    fun unbindService() {
        serviceConnection?.let {
            context.unbindService(it)
            serviceConnection = null
            boundService = null
            Log.d(TAG, "Unbound from TimerService")
        }
    }

    override fun startTimer(timer: TimerState) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_START_TIMER
            putExtra(TimerService.EXTRA_TIMER_STATE, timer)
        }
        ContextCompat.startForegroundService(context, intent)
        Log.d(TAG, "Started timer via service: ${timer.timerId}")
    }

    override fun pauseTimer(timerId: String) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_PAUSE_TIMER
            putExtra(TimerService.EXTRA_TIMER_ID, timerId)
        }
        context.startService(intent)
        Log.d(TAG, "Paused timer via service: $timerId")
    }

    override fun resumeTimer(timerId: String, updatedTimer: TimerState) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_RESUME_TIMER
            putExtra(TimerService.EXTRA_TIMER_STATE, updatedTimer)
        }
        context.startService(intent)
        Log.d(TAG, "Resumed timer via service: $timerId")
    }

    override fun stopTimer(timerId: String) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP_TIMER
            putExtra(TimerService.EXTRA_TIMER_ID, timerId)
        }
        context.startService(intent)
        Log.d(TAG, "Stopped timer via service: $timerId")
    }
}
