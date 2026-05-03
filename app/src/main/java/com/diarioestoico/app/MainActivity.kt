package com.diarioestoico.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.diarioestoico.app.notifications.createNotificationChannel
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.diarioestoico.app.data.DailyEntry
import com.diarioestoico.app.data.EntryRepository
import com.diarioestoico.app.data.FavoritesRepository
import com.diarioestoico.app.ui.DailyReadingScreen
import com.diarioestoico.app.ui.FavoritesScreen
import com.diarioestoico.app.ui.theme.DiarioEstoicoTheme
import com.diarioestoico.app.ui.theme.LibreBaskerville

private enum class Screen { TODAY, FAVORITES }

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        createNotificationChannel(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val entryRepository = EntryRepository(applicationContext)
        val favoritesRepository = FavoritesRepository(applicationContext)
        val allEntries = entryRepository.getAllEntries()
        val todayEntry = entryRepository.getTodayEntry()

        setContent {
            DiarioEstoicoTheme {
                AppScaffold(
                    allEntries = allEntries,
                    todayEntry = todayEntry,
                    favoritesRepository = favoritesRepository
                )
            }
        }
    }
}

@Composable
private fun AppScaffold(
    allEntries: List<DailyEntry>,
    todayEntry: DailyEntry?,
    favoritesRepository: FavoritesRepository
) {
    var currentScreen by remember { mutableStateOf(Screen.TODAY) }

    // Index-based navigation so prev/next work correctly
    val todayIndex = remember(allEntries, todayEntry) {
        if (todayEntry == null) 0
        else allEntries.indexOfFirst { it.day == todayEntry.day && it.month == todayEntry.month }
            .coerceAtLeast(0)
    }
    var entryIndex by remember { mutableStateOf(todayIndex) }
    val currentEntry = allEntries.getOrNull(entryIndex)

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = currentScreen == Screen.TODAY,
                    onClick = { currentScreen = Screen.TODAY },
                    icon = { Icon(Icons.Default.AutoStories, contentDescription = null) },
                    label = {
                        Text("Hoje", fontFamily = LibreBaskerville, fontSize = 11.sp)
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.FAVORITES,
                    onClick = { currentScreen = Screen.FAVORITES },
                    icon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                    label = {
                        Text("Favoritos", fontFamily = LibreBaskerville, fontSize = 11.sp)
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            modifier = Modifier.padding(innerPadding),
            label = "screen_transition"
        ) { screen ->
            when (screen) {
                Screen.TODAY -> DailyReadingScreen(
                    entry = currentEntry,
                    favoritesRepository = favoritesRepository,
                    entryIndex = entryIndex,
                    totalEntries = allEntries.size,
                    todayIndex = todayIndex,
                    onPrevious = { if (entryIndex > 0) entryIndex-- },
                    onNext = { if (entryIndex < allEntries.size - 1) entryIndex++ },
                    onGoToToday = { entryIndex = todayIndex }
                )
                Screen.FAVORITES -> FavoritesScreen(
                    favoritesRepository = favoritesRepository,
                    allEntries = allEntries,
                    onOpenEntry = { entry ->
                        val idx = allEntries.indexOfFirst {
                            it.day == entry.day && it.month == entry.month
                        }
                        if (idx >= 0) entryIndex = idx
                        currentScreen = Screen.TODAY
                    }
                )
            }
        }
    }
}
