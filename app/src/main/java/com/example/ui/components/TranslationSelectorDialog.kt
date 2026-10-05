package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Translation
import com.example.ui.util.Strings

@Composable
fun TranslationSelectorDialog(
    currentLanguage: String,
    translations: List<Translation>,
    selectedTranslation: Translation?,
    onSelectTranslation: (Translation) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedLangFilter by remember { mutableStateOf(currentLanguage) }
    var searchQuery by remember { mutableStateOf("") }
    var showOnlyFavorites by remember { mutableStateOf(false) }

    val filtered = remember(translations, searchQuery, showOnlyFavorites, selectedLangFilter, currentLanguage) {
        val query = searchQuery.trim().lowercase()
        translations
            .filter { t ->
                val matchesLang = selectedLangFilter.isEmpty() || t.language.equals(selectedLangFilter, ignoreCase = true)
                val matchesFav = !showOnlyFavorites || t.isFavorite
                val matchesQuery = query.isEmpty() ||
                    t.name.lowercase().contains(query) ||
                    t.id.lowercase().contains(query) ||
                    t.description.lowercase().contains(query)
                matchesLang && matchesFav && matchesQuery
            }
            .sortedWith(
                compareByDescending<Translation> { it.isFavorite }
                    .thenByDescending { it.language.equals(currentLanguage, ignoreCase = true) }
                    .thenBy { it.name }
            )
    }

    val favoritesList = remember(filtered) { filtered.filter { it.isFavorite } }
    val othersList = remember(filtered) { filtered.filter { !it.isFavorite } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Strings.get("translations", currentLanguage),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(Strings.get("search_hint", currentLanguage)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("translation_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Language & Favorite Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Polski
                    FilterChip(
                        selected = selectedLangFilter == "pl",
                        onClick = { selectedLangFilter = if (selectedLangFilter == "pl") "" else "pl" },
                        label = { Text(Strings.get("lang_pl", currentLanguage)) }
                    )
                    // English
                    FilterChip(
                        selected = selectedLangFilter == "en",
                        onClick = { selectedLangFilter = if (selectedLangFilter == "en") "" else "en" },
                        label = { Text(Strings.get("lang_en", currentLanguage)) }
                    )
                    // Deutsch
                    FilterChip(
                        selected = selectedLangFilter == "de",
                        onClick = { selectedLangFilter = if (selectedLangFilter == "de") "" else "de" },
                        label = { Text(Strings.get("lang_de", currentLanguage)) }
                    )
                    // Wszystkie języki
                    FilterChip(
                        selected = selectedLangFilter.isEmpty(),
                        onClick = { selectedLangFilter = "" },
                        label = { Text(Strings.get("lang_all", currentLanguage)) }
                    )
                    // Ulubione
                    FilterChip(
                        selected = showOnlyFavorites,
                        onClick = { showOnlyFavorites = !showOnlyFavorites },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = if (showOnlyFavorites) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(Strings.get("favorites", currentLanguage)) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (favoritesList.isNotEmpty() && !showOnlyFavorites && searchQuery.isEmpty()) {
                        item(key = "header_favorites") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = Strings.get("favorites", currentLanguage),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        items(favoritesList, key = { "fav_${it.id}" }) { translation ->
                            TranslationItemCard(
                                translation = translation,
                                isSelected = translation.id == selectedTranslation?.id,
                                onSelectTranslation = onSelectTranslation,
                                onToggleFavorite = onToggleFavorite
                            )
                        }

                        if (othersList.isNotEmpty()) {
                            item(key = "header_all") {
                                Text(
                                    text = Strings.get("all_translations", currentLanguage),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                )
                            }

                            items(othersList, key = { it.id }) { translation ->
                                TranslationItemCard(
                                    translation = translation,
                                    isSelected = translation.id == selectedTranslation?.id,
                                    onSelectTranslation = onSelectTranslation,
                                    onToggleFavorite = onToggleFavorite
                                )
                            }
                        }
                    } else {
                        items(filtered, key = { it.id }) { translation ->
                            TranslationItemCard(
                                translation = translation,
                                isSelected = translation.id == selectedTranslation?.id,
                                onSelectTranslation = onSelectTranslation,
                                onToggleFavorite = onToggleFavorite
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TranslationItemCard(
    translation: Translation,
    isSelected: Boolean,
    onSelectTranslation: (Translation) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("translation_item_${translation.id}")
            .clickable {
                onSelectTranslation(translation)
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = translation.language.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = translation.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (translation.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = translation.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (translation.date.isNotBlank()) {
                    Text(
                        text = translation.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            IconButton(
                onClick = { onToggleFavorite(translation.id) },
                modifier = Modifier.testTag("fav_button_${translation.id}")
            ) {
                Icon(
                    imageVector = if (translation.isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Favorite",
                    tint = if (translation.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
