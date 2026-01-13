package com.example.cookingassistant

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Set up navigation graph with repository
                    CookingAssistantNavigation(
                        navController = navController,
                        repository = repository
                    )
                }
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.applyLanguage(newBase))
    }
}