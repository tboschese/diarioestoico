package com.diarioestoico.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diarioestoico.app.data.DailyEntry
import com.diarioestoico.app.data.FavoritesRepository
import com.diarioestoico.app.data.NotificationPreferences
import com.diarioestoico.app.data.NotificationSettings
import com.diarioestoico.app.data.SavedPhrase
import com.diarioestoico.app.data.ThemeMode
import com.diarioestoico.app.data.ThemePreferences
import com.diarioestoico.app.notifications.cancelNotification
import com.diarioestoico.app.notifications.scheduleNotification
import com.diarioestoico.app.ui.components.NotificationDialog
import com.diarioestoico.app.ui.components.ShareCardGenerator
import com.diarioestoico.app.ui.components.StoicTextToolbar
import com.diarioestoico.app.ui.theme.LoraFamily
import com.diarioestoico.app.ui.theme.SansFamily
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DailyReadingScreen(
    entry: DailyEntry?,
    favoritesRepository: FavoritesRepository,
    themePreferences: ThemePreferences,
    currentThemeMode: ThemeMode,
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
    LaunchedEffect(entryIndex) { scrollState.scrollTo(0) }

    val isToday = entryIndex == todayIndex

    val favoriteIds by favoritesRepository.favoriteEntryIds.collectAsState(initial = emptySet())
    val isFavorite = entry != null &&
            favoritesRepository.isFavoriteEntry(entry.day, entry.month, favoriteIds)

    val progress by remember {
        derivedStateOf {
            if (scrollState.maxValue > 0)
                scrollState.value.toFloat() / scrollState.maxValue.toFloat()
            else 0f
        }
    }
    val progressAnim by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 80),
        label = "readingProgress"
    )

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

    val context = LocalContext.current
    val notifPrefs = remember { NotificationPreferences(context) }
    val notifSettings by notifPrefs.settings.collectAsState(initial = NotificationSettings())
    var showNotifDialog by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(99.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (entry == null) {
                EmptyState()
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // ── Sticky top bar ─────────────────────────────────────────
                    ReadingTopBar(
                        progressAnim = progressAnim,
                        isFavorite = isFavorite,
                        notifEnabled = notifSettings.enabled,
                        themeMode = currentThemeMode,
                        onShare = { showShareSheet = true },
                        onBell = { showNotifDialog = true },
                        onBookmark = {
                            scope.launch {
                                favoritesRepository.toggleFavoriteEntry(entry.day, entry.month)
                                snackbarHostState.showSnackbar(
                                    message = if (isFavorite) "Removido dos favoritos"
                                              else "Meditação salva nos favoritos",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        },
                        onToggleTheme = {
                            scope.launch {
                                val next = when (currentThemeMode) {
                                    ThemeMode.SYSTEM -> ThemeMode.LIGHT
                                    ThemeMode.LIGHT  -> ThemeMode.DARK
                                    ThemeMode.DARK   -> ThemeMode.SYSTEM
                                }
                                themePreferences.save(next)
                            }
                        }
                    )

                    // ── Scrollable content ─────────────────────────────────────
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
                                        .padding(horizontal = 26.dp)
                                ) {
                                    Spacer(Modifier.height(20.dp))
                                    DateRow(entry = entry, isToday = isToday)
                                    Spacer(Modifier.height(14.dp))
                                    TitleBlock(title = entry.title)
                                    Spacer(Modifier.height(28.dp))
                                    QuoteBlock(entry = entry)
                                    Spacer(Modifier.height(36.dp))
                                    ReflexaoLabel()
                                    Spacer(Modifier.height(20.dp))
                                    CommentaryBlock(text = entry.commentary)
                                    Spacer(Modifier.height(44.dp))
                                    FooterCredit()
                                    Spacer(Modifier.height(96.dp))
                                }
                            }
                        }
                    }
                }

                // ── Floating day-nav pill ──────────────────────────────────────
                FloatingNavPill(
                    entryIndex = entryIndex,
                    totalEntries = totalEntries,
                    isToday = isToday,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onGoToToday = onGoToToday,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                )

                // ── Share sheet overlay ────────────────────────────────────────
                if (showShareSheet) {
                    ShareSheetOverlay(
                        entry = entry,
                        onClose = { showShareSheet = false },
                        onShare = { mode ->
                            showShareSheet = false
                            scope.launch {
                                ShareCardGenerator.shareEntry(context, entry, mode)
                            }
                        }
                    )
                }
            }

            // Notification dialog (outside the null-check so it shows over everything)
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
    }
}

// ── Sticky top bar ───────────────────────────────────────────────────────

@Composable
private fun ReadingTopBar(
    progressAnim: Float,
    isFavorite: Boolean,
    notifEnabled: Boolean,
    themeMode: ThemeMode,
    onShare: () -> Unit,
    onBell: () -> Unit,
    onBookmark: () -> Unit,
    onToggleTheme: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val themeIcon = when (themeMode) {
        ThemeMode.SYSTEM -> Icons.Outlined.BrightnessAuto
        ThemeMode.LIGHT  -> Icons.Outlined.LightMode
        ThemeMode.DARK   -> Icons.Outlined.DarkMode
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .drawBehind {
                drawLine(
                    color = accent,
                    start = Offset(0f, size.height),
                    end = Offset(size.width * progressAnim, size.height),
                    strokeWidth = 2.dp.toPx()
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 22.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Diário Estoico",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.W500
                )
            )
            Row {
                TopBarIcon(
                    imageVector = themeIcon,
                    contentDescription = "Alternar tema",
                    onClick = onToggleTheme
                )
                TopBarIcon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Compartilhar",
                    onClick = onShare
                )
                TopBarIcon(
                    imageVector = if (notifEnabled) Icons.Default.NotificationsActive
                                  else Icons.Default.NotificationsNone,
                    contentDescription = "Lembrete diário",
                    tint = if (notifEnabled) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onBell
                )
                TopBarIcon(
                    imageVector = if (isFavorite) Icons.Filled.Bookmark
                                  else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (isFavorite) "Remover favorito" else "Salvar",
                    tint = if (isFavorite) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onBookmark
                )
            }
        }
        // hairline divider only visible after scrolling starts
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.outline,
            thickness = if (progressAnim > 0.01f) 0.5.dp else 0.dp
        )
    }
}

@Composable
private fun TopBarIcon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(21.dp)
        )
    }
}

// ── Content blocks ───────────────────────────────────────────────────────

@Composable
private fun DateRow(entry: DailyEntry, isToday: Boolean) {
    val weekdayAbbr = remember(entry.day, entry.month) {
        try {
            LocalDate.of(LocalDate.now().year, entry.month, entry.day)
                .dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("pt", "BR"))
                .replaceFirstChar { it.uppercase() }
        } catch (e: Exception) { null }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val dateLabel = buildString {
            if (weekdayAbbr != null) append("$weekdayAbbr · ")
            append("${entry.day} de ${entry.monthName}")
        }
        Text(
            text = dateLabel.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp
            )
        )
        if (isToday) {
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(99.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "HOJE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 9.sp,
                        letterSpacing = 1.5.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun TitleBlock(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium.copy(
            color = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Composable
private fun QuoteBlock(entry: DailyEntry) {
    val accent = MaterialTheme.colorScheme.primary
    val tint = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(18.dp))
            .background(tint)
    ) {
        // Left accent stripe
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(accent)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 18.dp, top = 20.dp, end = 20.dp, bottom = 20.dp)
        ) {
            // Decorative opening quote mark
            Text(
                text = "“",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = LoraFamily,
                    fontSize = 56.sp,
                    lineHeight = 28.sp,
                    color = accent.copy(alpha = 0.32f)
                ),
                modifier = Modifier.height(26.dp).offset(y = (-2).dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = entry.quote,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            if (entry.author.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "— ${entry.author}".uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = accent,
                        letterSpacing = 1.8.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun ReflexaoLabel() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Reflexão".uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.5.sp
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
private fun CommentaryBlock(text: String) {
    val accent = MaterialTheme.colorScheme.primary
    val paragraphs = text.split("\n\n")
    paragraphs.forEachIndexed { index, para ->
        if (index > 0) Spacer(Modifier.height(16.dp))
        if (index == 0 && para.isNotEmpty()) {
            DropCapParagraph(text = para, accentColor = accent)
        } else {
            Text(
                text = para,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    }
}

@Composable
private fun DropCapParagraph(text: String, accentColor: Color) {
    if (text.isEmpty()) return
    val firstChar = text[0].toString()
    val rest = if (text.length > 1) text.substring(1) else ""

    // Find where the first line break or word naturally ends to flow the drop cap
    val splitAt = rest.indexOf(' ', 50).takeIf { it > 0 } ?: rest.length

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = firstChar,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = LoraFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 56.sp,
                lineHeight = 44.sp,
                color = accentColor
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
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun FooterCredit() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline)
        )
        Text(
            text = "O Diário Estoico · Ryan Holiday & Stephen Hanselman",
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp,
                fontFamily = SansFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 10.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

// ── Floating nav pill ────────────────────────────────────────────────────

@Composable
private fun FloatingNavPill(
    entryIndex: Int,
    totalEntries: Int,
    isToday: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onGoToToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(99.dp),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.93f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 12.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Prev
            PillNavButton(
                onClick = onPrevious,
                enabled = entryIndex > 0
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Dia anterior",
                    modifier = Modifier.size(18.dp)
                )
            }

            // Center: day counter or "Hoje"
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .then(
                        if (!isToday) Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onGoToToday
                        ) else Modifier
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isToday) {
                    Text(
                        text = "${entryIndex + 1} / $totalEntries",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp,
                            fontSize = 12.sp
                        )
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Today,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Hoje",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.sp,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            // Next
            PillNavButton(
                onClick = onNext,
                enabled = entryIndex < totalEntries - 1
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Próximo dia",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun PillNavButton(
    onClick: () -> Unit,
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(99.dp))
            .then(
                if (enabled) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (enabled)
                MaterialTheme.colorScheme.onBackground
            else
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        ) {
            content()
        }
    }
}

// ── Share sheet overlay ──────────────────────────────────────────────────

private val SHARE_MODES = listOf(
    Triple(ShareCardGenerator.Mode.QUOTE_ONLY, "Citação", "Só a citação"),
    Triple(ShareCardGenerator.Mode.REFLECTION_ONLY, "Reflexão", "Só o texto"),
    Triple(ShareCardGenerator.Mode.FULL, "Completo", "Citação + reflexão")
)

@Composable
private fun ShareSheetOverlay(
    entry: DailyEntry,
    onClose: () -> Unit,
    onShare: (ShareCardGenerator.Mode) -> Unit
) {
    var selectedMode by remember { mutableStateOf(ShareCardGenerator.Mode.QUOTE_ONLY) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose
            )
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}  // consume clicks, don't propagate to scrim
                ),
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 24.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Handle
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(99.dp)
                        )
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(18.dp))

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Compartilhar",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 19.sp
                            )
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Escolha o que incluir no card e compartilhe onde quiser.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontStyle = FontStyle.Italic
                    )
                )

                Spacer(Modifier.height(20.dp))

                // Mode segmented control
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SHARE_MODES.forEach { (mode, label, _) ->
                        val on = selectedMode == mode
                        Surface(
                            modifier = Modifier.weight(1f),
                            onClick = { selectedMode = mode },
                            shape = RoundedCornerShape(9.dp),
                            color = if (on) MaterialTheme.colorScheme.surface
                                    else Color.Transparent,
                            shadowElevation = if (on) 2.dp else 0.dp
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = if (on) MaterialTheme.colorScheme.onBackground
                                            else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (on) FontWeight.W600 else FontWeight.W500,
                                    fontSize = 13.sp
                                ),
                                modifier = Modifier.padding(vertical = 9.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Description of selected mode
                val modeDesc = SHARE_MODES.find { it.first == selectedMode }?.third ?: ""
                Spacer(Modifier.height(16.dp))
                Text(
                    text = modeDesc,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(99.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            "Cancelar",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                    Button(
                        onClick = { onShare(selectedMode) },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(99.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            "Compartilhar",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ── Empty state ──────────────────────────────────────────────────────────

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✦",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 24.sp
                    )
                )
            }
            Text(
                text = "Sem leitura para hoje",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp
                )
            )
        }
    }
}
