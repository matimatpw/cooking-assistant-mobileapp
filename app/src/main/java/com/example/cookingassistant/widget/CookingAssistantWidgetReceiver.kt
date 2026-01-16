package com.example.cookingassistant.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Widget receiver for Cooking Assistant
 * Handles widget lifecycle events (onUpdate, onEnabled, onDisabled)
 */
class CookingAssistantWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CookingAssistantWidget()
}
