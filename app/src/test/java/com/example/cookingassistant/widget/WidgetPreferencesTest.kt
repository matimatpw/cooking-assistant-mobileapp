package com.example.cookingassistant.widget

import android.content.Context
import android.content.SharedPreferences
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq

@RunWith(MockitoJUnitRunner::class)
class WidgetPreferencesTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var sharedPreferences: SharedPreferences

    @Mock
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var widgetPreferences: WidgetPreferences

    @Before
    fun setup() {
        `when`(context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE))
            .thenReturn(sharedPreferences)
        `when`(sharedPreferences.edit()).thenReturn(editor)
        `when`(editor.putString(any(), any())).thenReturn(editor)
        `when`(editor.putInt(any(), any())).thenReturn(editor)
        `when`(editor.putLong(any(), any())).thenReturn(editor)
        `when`(editor.remove(any())).thenReturn(editor)

        widgetPreferences = WidgetPreferences(context)
    }

    @Test
    fun set_last_viewed_recipe_stores_recipe_id() {
        // When
        widgetPreferences.setLastViewedRecipe("recipe123")

        // Then
        verify(editor).putString("last_viewed_recipe_id", "recipe123")
        verify(editor).putLong(eq("last_viewed_timestamp"), any())
        verify(editor).apply()
    }

    @Test
    fun get_last_viewed_recipe_returns_stored_recipe_id() {
        // Given
        `when`(sharedPreferences.getString("last_viewed_recipe_id", null))
            .thenReturn("recipe123")

        // When
        val result = widgetPreferences.getLastViewedRecipe()

        // Then
        assertThat(result).isEqualTo("recipe123")
    }

    @Test
    fun get_last_viewed_recipe_returns_null_when_not_set() {
        // Given
        `when`(sharedPreferences.getString("last_viewed_recipe_id", null))
            .thenReturn(null)

        // When
        val result = widgetPreferences.getLastViewedRecipe()

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun set_active_cooking_session_stores_recipe_id_and_step_index() {
        // When
        widgetPreferences.setActiveCookingSession("recipe456", 3)

        // Then
        verify(editor).putString("active_cooking_recipe_id", "recipe456")
        verify(editor).putInt("active_cooking_step_index", 3)
        verify(editor).putLong(eq("active_cooking_timestamp"), any())
        verify(editor).apply()
    }

    @Test
    fun get_active_cooking_session_returns_session_when_valid() {
        // Given - session less than 24 hours old
        val currentTime = System.currentTimeMillis()
        `when`(sharedPreferences.getString("active_cooking_recipe_id", null))
            .thenReturn("recipe789")
        `when`(sharedPreferences.getInt("active_cooking_step_index", 0))
            .thenReturn(5)
        `when`(sharedPreferences.getLong("active_cooking_timestamp", 0))
            .thenReturn(currentTime - 1000) // 1 second ago

        // When
        val result = widgetPreferences.getActiveCookingSession()

        // Then
        assertThat(result).isNotNull
        assertThat(result?.recipeId).isEqualTo("recipe789")
        assertThat(result?.stepIndex).isEqualTo(5)
    }

    @Test
    fun get_active_cooking_session_returns_null_when_stale() {
        // Given - session more than 24 hours old
        val staleTime = System.currentTimeMillis() - (25 * 60 * 60 * 1000) // 25 hours ago
        `when`(sharedPreferences.getString("active_cooking_recipe_id", null))
            .thenReturn("recipe789")
        `when`(sharedPreferences.getInt("active_cooking_step_index", 0))
            .thenReturn(5)
        `when`(sharedPreferences.getLong("active_cooking_timestamp", 0))
            .thenReturn(staleTime)

        // When
        val result = widgetPreferences.getActiveCookingSession()

        // Then
        assertThat(result).isNull()
        // Verify that stale session was cleared
        verify(editor).remove("active_cooking_recipe_id")
        verify(editor).remove("active_cooking_step_index")
        verify(editor).remove("active_cooking_timestamp")
    }

    @Test
    fun get_active_cooking_session_returns_null_when_not_set() {
        // Given
        `when`(sharedPreferences.getString("active_cooking_recipe_id", null))
            .thenReturn(null)

        // When
        val result = widgetPreferences.getActiveCookingSession()

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun clear_cooking_session_removes_all_session_data() {
        // When
        widgetPreferences.clearCookingSession()

        // Then
        verify(editor).remove("active_cooking_recipe_id")
        verify(editor).remove("active_cooking_step_index")
        verify(editor).remove("active_cooking_timestamp")
        verify(editor).apply()
    }

    @Test
    fun has_active_cooking_session_returns_true_when_session_exists() {
        // Given
        val currentTime = System.currentTimeMillis()
        `when`(sharedPreferences.getString("active_cooking_recipe_id", null))
            .thenReturn("recipe123")
        `when`(sharedPreferences.getInt("active_cooking_step_index", 0))
            .thenReturn(2)
        `when`(sharedPreferences.getLong("active_cooking_timestamp", 0))
            .thenReturn(currentTime - 1000)

        // When
        val result = widgetPreferences.hasActiveCookingSession()

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun has_active_cooking_session_returns_false_when_no_session() {
        // Given
        `when`(sharedPreferences.getString("active_cooking_recipe_id", null))
            .thenReturn(null)

        // When
        val result = widgetPreferences.hasActiveCookingSession()

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun set_cached_random_recipe_stores_recipe_id() {
        // When
        widgetPreferences.setCachedRandomRecipe("randomRecipe")

        // Then
        verify(editor).putString("cached_random_recipe_id", "randomRecipe")
        verify(editor).putLong(eq("cached_random_timestamp"), any())
        verify(editor).apply()
    }

    @Test
    fun get_cached_random_recipe_returns_recipe_when_fresh() {
        // Given - cache less than 5 minutes old
        val currentTime = System.currentTimeMillis()
        `when`(sharedPreferences.getString("cached_random_recipe_id", null))
            .thenReturn("randomRecipe")
        `when`(sharedPreferences.getLong("cached_random_timestamp", 0))
            .thenReturn(currentTime - 1000) // 1 second ago

        // When
        val result = widgetPreferences.getCachedRandomRecipe()

        // Then
        assertThat(result).isEqualTo("randomRecipe")
    }

    @Test
    fun get_cached_random_recipe_returns_null_when_expired() {
        // Given - cache more than 5 minutes old
        val expiredTime = System.currentTimeMillis() - (6 * 60 * 1000) // 6 minutes ago
        `when`(sharedPreferences.getString("cached_random_recipe_id", null))
            .thenReturn("randomRecipe")
        `when`(sharedPreferences.getLong("cached_random_timestamp", 0))
            .thenReturn(expiredTime)

        // When
        val result = widgetPreferences.getCachedRandomRecipe()

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun get_cached_random_recipe_returns_null_when_not_set() {
        // Given
        `when`(sharedPreferences.getString("cached_random_recipe_id", null))
            .thenReturn(null)

        // When
        val result = widgetPreferences.getCachedRandomRecipe()

        // Then
        assertThat(result).isNull()
    }
}
