package com.example.cookingassistant.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WidgetUpdater {
    private const val TAG = "WidgetUpdater"

    fun updateWidgets(context: Context) {
        Log.d(TAG, "=== Triggering widget update ===")
        Log.d(TAG, "Context: ${context.javaClass.simpleName}")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val glanceManager = GlanceAppWidgetManager(context)
                val widgetIds = glanceManager.getGlanceIds(CookingAssistantWidget::class.java)
                Log.d(TAG, "Found ${widgetIds.size} widget instances")

                CookingAssistantWidget().updateAll(context)
                Log.d(TAG, "Widget update completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update widget", e)
            }
        }
    }
}
