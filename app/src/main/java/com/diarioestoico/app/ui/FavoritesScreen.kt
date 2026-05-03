package com.diarioestoico.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diarioestoico.app.data.DailyEntry
import com.diarioestoico.app.data.FavoritesRepository
import com.diarioestoico.app.data.SavedPhrase
import kotlinx.coroutines.launch

@Composable
fun FavoritesScreen(
    favoritesRepository: FavoritesRepository,
    allEntries: List<DailyEntry>,
    onOpenEntry: (DailyEntry) -> Unit
) {
    val favoriteIds by favoritesRepository.favoriteEntryIds.collectAsState(initial = emptySet())
    val savedPhrases by favoritesRepository.savedPhrases.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val favoriteEntries = remember(favoriteIds, allEntries) {
        allEntries.filter { favoritesRepository.isFavoriteEntry(it.day, it.month, favoriteIds) }
            .sortedWith(compareBy({ it.month }, { it.day }))
    }

    val hasContent = favoriteEntries.isNotEmpty() || savedPhrases.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(top = 64.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "FAVORITOS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 3.sp,
                    fontSize = 10.sp
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
        }

        if (!hasContent) {
            EmptyFavorites()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Saved entries ────────────────────────────────────────────
                if (favoriteEntries.isNotEmpty()) {
                    item {
                        SectionLabel(text = "Meditações salvas")
                    }
                    items(favoriteEntries, key = { "${it.day}-${it.month}" }) { entry ->
                        FavoriteEntryCard(
                            entry = entry,
                            onOpen = { onOpenEntry(entry) },
                            onRemove = {
                                scope.launch {
                                    favoritesRepository.toggleFavoriteEntry(entry.day, entry.month)
                                }
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }

                // ── Saved phrases ─────────────────────────────────────────────
                if (savedPhrases.isNotEmpty()) {
                    item {
                        SectionLabel(text = "Frases salvas")
                    }
                    items(savedPhrases, key = { it.savedAt }) { phrase ->
                        SavedPhraseCard(
                            phrase = phrase,
                            onDelete = {
                                scope.launch {
                                    favoritesRepository.deletePhrase(phrase)
                                }
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 2.sp,
            fontSize = 9.sp
        ),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoriteEntryCard(
    entry: DailyEntry,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${entry.day} de ${entry.monthName}".uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 9.sp,
                        letterSpacing = 2.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = entry.widgetQuote,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remover dos favoritos",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SavedPhraseCard(
    phrase: SavedPhrase,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Opening quote mark
                Text(
                    text = "\u201C",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 28.sp,
                        lineHeight = 10.sp
                    ),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .offset(y = (-4).dp)
                )
                Text(
                    text = phrase.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Apagar frase",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (phrase.sourceTitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${phrase.sourceDay} de ${phrase.sourceMonthName} · ${phrase.sourceTitle}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun EmptyFavorites() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            Text(
                text = "✦",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 32.sp
                )
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Nenhum favorito ainda",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Toque no marcador na leitura do dia para salvar uma meditação, ou selecione um trecho do texto para salvar uma frase.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.secondary
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}
