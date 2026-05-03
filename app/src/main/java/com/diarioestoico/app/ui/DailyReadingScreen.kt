package com.diarioestoico.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diarioestoico.app.data.DailyEntry
import com.diarioestoico.app.data.FavoritesRepository
import com.diarioestoico.app.data.NotificationPreferences
import com.diarioestoico.app.data.NotificationSettings
import com.diarioestoico.app.data.SavedPhrase
import com.diarioestoico.app.notifications.cancelNotification
import com.diarioestoico.app.notifications.scheduleNotification
import com.diarioestoico.app.ui.components.NotificationDialog
import com.diarioestoico.app.ui.components.StoicTextToolbar
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DailyReadingScreen(
    entry: DailyEntry?,
    favoritesRepository: FavoritesRepository,
    entryIndex: Int = 0,
    totalEntries: Int = 366,
    todayIndex: Int = 0,
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    onGoToToday: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // Scroll back to top whenever the entry changes
    LaunchedEffect(entryIndex) { scrollState.scrollTo(0) }

    val isToday = entryIndex == todayIndex

    val favoriteIds by favoritesRepository.favoriteEntryIds.collectAsState(initial = emptySet())
    val isFavorite = entry != null &&
            favoritesRepository.isFavoriteEntry(entry.day, entry.month, favoriteIds)

    // Custom toolbar: triggers copy then reads clipboard to save phrase
    val view = LocalView.current
    val clipboardManager = LocalClipboardManager.current
    val stoicToolbar = remember(view) {
        StoicTextToolbar(view) {
            val text = clipboardManager.getText()?.text?.trim() ?: return@StoicTextToolbar
            if (text.isBlank() || entry == null) return@StoicTextToolbar
            scope.launch {
                favoritesRepository.savePhrase(
                    SavedPhrase(
                        text = text,
                        sourceTitle = entry.title,
                        sourceDay = entry.day,
                        sourceMonth = entry.month,
                        sourceMonthName = entry.monthName
                    )
                )
                snackbarHostState.showSnackbar(
                    message = "Frase salva nos favoritos",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.small
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .drawBehind {
                    val lineColor = Color(0x06000000)
                    var y = 0f
                    while (y < size.height) {
                        drawLine(lineColor, Offset(0f, y), Offset(size.width, y + 40f), 1f)
                        y += 18f
                    }
                }
        ) {
            if (entry == null) {
                EmptyState()
            } else {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 4 }
                ) {
                    CompositionLocalProvider(LocalTextToolbar provides stoicToolbar) {
                        SelectionContainer {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                                    .padding(horizontal = 28.dp)
                            ) {
                                Spacer(modifier = Modifier.height(64.dp))

                                ChapterHeader(
                                    entry = entry,
                                    isFavorite = isFavorite,
                                    onToggleFavorite = {
                                        scope.launch {
                                            favoritesRepository.toggleFavoriteEntry(
                                                entry.day, entry.month
                                            )
                                            val msg = if (isFavorite)
                                                "Removido dos favoritos"
                                            else
                                                "Meditação salva nos favoritos"
                                            snackbarHostState.showSnackbar(
                                                message = msg,
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(32.dp))
                                OrnamentalDivider()
                                Spacer(modifier = Modifier.height(32.dp))

                                QuoteBlock(entry = entry)

                                Spacer(modifier = Modifier.height(40.dp))
                                OrnamentalDivider()
                                Spacer(modifier = Modifier.height(36.dp))

                                CommentaryBlock(text = entry.commentary)

                                Spacer(modifier = Modifier.height(48.dp))
                                FooterCredit()
                                // extra padding so content clears the nav bar
                                Spacer(modifier = Modifier.height(96.dp))
                            }
                        }
                    }
                }
            }

            // Day navigation bar — fixed at bottom inside the reading area
            DayNavBar(
                entryIndex = entryIndex,
                totalEntries = totalEntries,
                isToday = isToday,
                onPrevious = onPrevious,
                onNext = onNext,
                onGoToToday = onGoToToday,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun DayNavBar(
    entryIndex: Int,
    totalEntries: Int,
    isToday: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onGoToToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 8.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Previous day
            IconButton(
                onClick = onPrevious,
                enabled = entryIndex > 0
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Dia anterior",
                    tint = if (entryIndex > 0) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.outline
                )
            }

            // Center: day counter + "Hoje" button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${entryIndex + 1} / $totalEntries",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                )
                if (!isToday) {
                    TextButton(
                        onClick = onGoToToday,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Today,
                            contentDescription = null,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(end = 2.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Hoje",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            // Next day
            IconButton(
                onClick = onNext,
                enabled = entryIndex < totalEntries - 1
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Próximo dia",
                    tint = if (entryIndex < totalEntries - 1) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun ChapterHeader(
    entry: DailyEntry,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notifPrefs = remember { NotificationPreferences(context) }
    val notifSettings by notifPrefs.settings.collectAsState(initial = NotificationSettings())
    var showNotifDialog by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val weekday = today.dayOfWeek
        .getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
        .replaceFirstChar { it.uppercase() }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$weekday, ${entry.day} de ${entry.monthName}".uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 3.sp,
                    fontSize = 10.sp
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = entry.title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                ),
                textAlign = TextAlign.Center
            )
        }

        // Top-right action icons
        Row(modifier = Modifier.align(Alignment.TopEnd)) {
            // Notification bell
            IconButton(onClick = { showNotifDialog = true }) {
                Icon(
                    imageVector = Icons.Default.NotificationsNone,
                    contentDescription = "Lembrete diário",
                    tint = if (notifSettings.enabled) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(22.dp)
                )
            }
            // Bookmark
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Bookmark
                                  else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (isFavorite) "Remover favorito" else "Salvar nos favoritos",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }

    if (showNotifDialog) {
        NotificationDialog(
            current = notifSettings,
            onDismiss = { showNotifDialog = false },
            onSave = { enabled, hour, minute ->
                scope.launch {
                    notifPrefs.save(enabled, hour, minute)
                    if (enabled) scheduleNotification(context, hour, minute)
                    else cancelNotification(context)
                }
                showNotifDialog = false
            }
        )
    }
}

@Composable
private fun OrnamentalDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outline,
            thickness = 0.5.dp
        )
        Text(
            text = "  ✦  ",
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp
            )
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outline,
            thickness = 0.5.dp
        )
    }
}

@Composable
private fun QuoteBlock(entry: DailyEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Text(
            text = "“",
            style = MaterialTheme.typography.headlineLarge.copy(
                color = MaterialTheme.colorScheme.primary,
                fontSize = 48.sp,
                lineHeight = 20.sp
            ),
            modifier = Modifier.offset(x = (-4).dp, y = (-8).dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = entry.quote,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (entry.author.isNotBlank()) {
            Text(
                text = "— ${entry.author}",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun CommentaryBlock(text: String) {
    val firstChar = text.firstOrNull()?.toString() ?: ""
    val rest = if (text.length > 1) text.substring(1) else ""

    if (firstChar.isNotBlank()) {
        val splitAt = rest.indexOf(' ', 60).takeIf { it > 0 } ?: rest.length
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = firstChar,
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 52.sp,
                    lineHeight = 44.sp
                ),
                modifier = Modifier.padding(end = 8.dp, top = 2.dp)
            )
            Text(
                text = rest.substring(0, minOf(splitAt + 1, rest.length)),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        if (splitAt < rest.length) {
            Text(
                text = rest.substring(splitAt + 1),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    } else {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onBackground
            )
        )
    }
}

@Composable
private fun FooterCredit() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            thickness = 0.5.dp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Desenvolvido por Thiago Boschese para uso pessoal",
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                fontSize = 10.sp,
                letterSpacing = 0.3.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "✦",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 36.sp
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Sem leitura para hoje",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    }
}
