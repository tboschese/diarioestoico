package com.diarioestoico.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diarioestoico.app.data.DailyEntry
import com.diarioestoico.app.data.FavoritesRepository
import com.diarioestoico.app.data.SavedPhrase
import com.diarioestoico.app.data.ThemeMode
import com.diarioestoico.app.data.ThemePreferences
import com.diarioestoico.app.ui.theme.LoraFamily
import com.diarioestoico.app.ui.theme.SansFamily
import kotlinx.coroutines.launch

private enum class FavTab { MEDITATIONS, PHRASES }

@Composable
fun FavoritesScreen(
    favoritesRepository: FavoritesRepository,
    allEntries: List<DailyEntry>,
    themePreferences: ThemePreferences,
    currentThemeMode: ThemeMode,
    onOpenEntry: (DailyEntry) -> Unit
) {
    val favoriteIds  by favoritesRepository.favoriteEntryIds.collectAsState(initial = emptySet())
    val savedPhrases by favoritesRepository.savedPhrases.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    // Cycle: SYSTEM → LIGHT → DARK → SYSTEM
    val nextThemeMode = when (currentThemeMode) {
        ThemeMode.SYSTEM -> ThemeMode.LIGHT
        ThemeMode.LIGHT  -> ThemeMode.DARK
        ThemeMode.DARK   -> ThemeMode.SYSTEM
    }
    val themeIcon = when (currentThemeMode) {
        ThemeMode.SYSTEM -> Icons.Outlined.BrightnessAuto
        ThemeMode.LIGHT  -> Icons.Outlined.LightMode
        ThemeMode.DARK   -> Icons.Outlined.DarkMode
    }
    val themeLabel = when (currentThemeMode) {
        ThemeMode.SYSTEM -> "Sistema"
        ThemeMode.LIGHT  -> "Claro"
        ThemeMode.DARK   -> "Escuro"
    }

    val favoriteEntries = remember(favoriteIds, allEntries) {
        allEntries
            .filter { favoritesRepository.isFavoriteEntry(it.day, it.month, favoriteIds) }
            .sortedWith(compareBy({ it.month }, { it.day }))
    }

    var activeTab by remember { mutableStateOf(FavTab.MEDITATIONS) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Header ──────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp)
                .padding(top = 28.dp, bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Favoritos",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${favoriteEntries.size} ${if (favoriteEntries.size == 1) "meditação" else "meditações"} · " +
                               "${savedPhrases.size} ${if (savedPhrases.size == 1) "frase" else "frases"}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp,
                            fontFamily = SansFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    )
                }
                // ── Theme toggle ─────────────────────────────────────
                ThemeToggleButton(
                    icon = themeIcon,
                    label = themeLabel,
                    onClick = { scope.launch { themePreferences.save(nextThemeMode) } }
                )
            }
            Spacer(Modifier.height(16.dp))

            // Segmented control
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
                listOf(FavTab.MEDITATIONS to "Meditações", FavTab.PHRASES to "Frases")
                    .forEach { (tab, label) ->
                        val on = activeTab == tab
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { activeTab = tab },
                            shape = RoundedCornerShape(9.dp),
                            color = if (on) MaterialTheme.colorScheme.surface else Color.Transparent,
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
        }

        // ── List ────────────────────────────────────────────────────
        val listItems = if (activeTab == FavTab.MEDITATIONS) favoriteEntries else emptyList<DailyEntry>()
        val phraseItems = if (activeTab == FavTab.PHRASES) savedPhrases else emptyList<SavedPhrase>()

        if (listItems.isEmpty() && phraseItems.isEmpty()) {
            EmptyFavState(which = activeTab)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (activeTab == FavTab.MEDITATIONS) {
                    items(favoriteEntries, key = { "${it.day}-${it.month}" }) { entry ->
                        MeditationCard(
                            entry = entry,
                            onOpen = { onOpenEntry(entry) },
                            onRemove = {
                                scope.launch {
                                    favoritesRepository.toggleFavoriteEntry(entry.day, entry.month)
                                }
                            }
                        )
                    }
                } else {
                    items(savedPhrases, key = { it.savedAt }) { phrase ->
                        PhraseCard(
                            phrase = phrase,
                            onDelete = {
                                scope.launch { favoritesRepository.deletePhrase(phrase) }
                            }
                        )
                    }
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeditationCard(
    entry: DailyEntry,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onOpen)
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
                .padding(start = 14.dp, top = 14.dp, bottom = 14.dp)
        ) {
            Text(
                text = "${entry.day} de ${entry.monthName}".uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = accent,
                    letterSpacing = 1.8.sp,
                    fontSize = 9.5.sp
                )
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = entry.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.W600
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = entry.widgetQuote,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        // Bookmark remove button
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .padding(4.dp)
                .size(32.dp)
                .align(Alignment.Top)
        ) {
            Icon(
                Icons.Default.Bookmark,
                contentDescription = "Remover dos favoritos",
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun PhraseCard(
    phrase: SavedPhrase,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 18.dp, top = 16.dp, end = 14.dp, bottom = 14.dp)
    ) {
        // Decorative " mark
        Text(
            text = "“",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = LoraFamily,
                fontSize = 38.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
            ),
            modifier = Modifier.height(18.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = phrase.text,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 15.5.sp,
                fontStyle = FontStyle.Italic
            )
        )
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline,
            thickness = 0.5.dp
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${phrase.sourceDay} de ${phrase.sourceMonthName} · ${phrase.sourceTitle}",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp,
                    fontSize = 10.sp
                ),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Apagar frase",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ThemeToggleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Tema: $label",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.primary,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp
            )
        )
    }
}

@Composable
private fun EmptyFavState(which: FavTab) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                Icon(
                    imageVector = if (which == FavTab.PHRASES) Icons.Outlined.FavoriteBorder
                                  else Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = if (which == FavTab.PHRASES) "Nenhuma frase salva"
                       else "Nenhuma meditação salva",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp
                )
            )
            Text(
                text = if (which == FavTab.PHRASES)
                    "Selecione um trecho durante a leitura para guardar uma frase que te marcou."
                else
                    "Toque no marcador na leitura do dia para guardar a meditação aqui.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.5.sp,
                    fontStyle = FontStyle.Italic
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}
