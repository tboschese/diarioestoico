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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.diarioestoico.app.data.DailyEntry
import com.diarioestoico.app.data.EntryRepository
import com.diarioestoico.app.data.FavoritesRepository
import com.diarioestoico.app.ui.DailyReadingScreen
import com.diarioestoico.app.ui.FavoritesScreen
import com.diarioestoico.app.ui.theme.DiarioEstoicoTheme
import com.diarioestoico.app.ui.theme.SansFamily

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

        val entryRepository    = EntryRepository(applicationContext)
        val favoritesRepository = FavoritesRepository(applicationContext)
        val allEntries         = entryRepository.getAllEntries()
        val todayEntry         = entryRepository.getTodayEntry()

        setContent {
            DiarioEstoicoTheme {
                AppScaffold(
                    allEntries          = allEntries,
                    todayEntry          = todayEntry,
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
                listOf(
                    Triple(Screen.TODAY,     Icons.Default.AutoStories, "Hoje"),
                    Triple(Screen.FAVORITES, Icons.Default.Bookmark,    "Favoritos")
                ).forEach { (screen, icon, label) ->
                    val selected = currentScreen == screen
                    NavigationBarItem(
                        selected = selected,
                        onClick  = { currentScreen = screen },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(if (selected) 56.dp else 40.dp, 28.dp)
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    modifier = Modifier.matchParentSize()
                                        .padding(horizontal = if (selected) 17.dp else 9.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = label,
                                fontFamily = SansFamily,
                                fontWeight = if (selected) FontWeight.W600 else FontWeight.W500,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = MaterialTheme.colorScheme.primary,
                            selectedTextColor   = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor      = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )
                }
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
                    entry               = currentEntry,
                    favoritesRepository = favoritesRepository,
                    entryIndex          = entryIndex,
                    totalEntries        = allEntries.size,
                    todayIndex          = todayIndex,
                    onPrevious          = { if (entryIndex > 0) entryIndex-- },
                    onNext              = { if (entryIndex < allEntries.size - 1) entryIndex++ },
                    onGoToToday         = { entryIndex = todayIndex }
                )
                Screen.FAVORITES -> FavoritesScreen(
                    favoritesRepository = favoritesRepository,
                    allEntries          = allEntries,
                    onOpenEntry         = { entry ->
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
