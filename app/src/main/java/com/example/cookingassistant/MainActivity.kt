package com.example.cookingassistant

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.cookingassistant.navigation.CookingAssistantNavigation
import com.example.cookingassistant.repository.CachedRecipeRepository
import com.example.cookingassistant.repository.FileRecipeRepository
import com.example.cookingassistant.repository.MockRemoteDataSource
import com.example.cookingassistant.ui.theme.CookingAssistantTheme
import com.example.cookingassistant.util.BundledRecipesInstaller
import com.example.cookingassistant.util.LocaleManager
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    // Track current intent for deep link navigation
    private var currentIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set initial intent for deep link handling
        currentIntent = intent

        // Install bundled recipes on app startup (runs only on first launch)
        lifecycleScope.launch {
            val installer = BundledRecipesInstaller(applicationContext)
            installer.installBundledRecipesIfNeeded()
        }

        enableEdgeToEdge()
        setContent {
            CookingAssistantTheme {
                val navController = rememberNavController()

                // Create data source instances and repository
                // In the future, replace MockRemoteDataSource with real API client
                val repository = remember {
                    CachedRecipeRepository(
                        localDataSource = FileRecipeRepository(applicationContext),
                        remoteDataSource = MockRemoteDataSource(networkDelayMs = 1000)
                    )
                }

                // Handle deep links from widget and timer notifications
                LaunchedEffect(currentIntent) {
                    currentIntent?.let { intent ->
                        // Check if this is a navigation from timer notification
                        if (intent.getBooleanExtra("navigate_to_cooking", false)) {
                            val recipeId = intent.getStringExtra("recipe_id") ?: ""
                            val stepIndex = intent.getIntExtra("step_index", 0)
                            if (recipeId.isNotEmpty()) {
                                // Create deep link URI for cooking step
                                val deepLinkUri = Uri.parse("cookingassistant://cooking_step/$recipeId?stepIndex=$stepIndex")
                                navController.handleDeepLink(Intent(Intent.ACTION_VIEW, deepLinkUri))
                            }
                        } else {
                            // Handle regular deep links from widget
                            intent.data?.let { uri ->
                                navController.handleDeepLink(Intent(Intent.ACTION_VIEW, uri))
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Set up navigation graph with repository and context
                    CookingAssistantNavigation(
                        navController = navController,
                        repository = repository,
                        context = applicationContext
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Update both the activity intent and our state to trigger navigation
        setIntent(intent)
        currentIntent = intent
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.applyLanguage(newBase))
    }
}