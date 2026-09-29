package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BookInfo
import com.example.data.model.Translation
import com.example.ui.util.DiffMode
import com.example.ui.util.DiffUtil
import com.example.ui.util.Strings

@Composable
fun VerseComparisonDialog(
    currentLanguage: String,
    book: BookInfo,
    chapter: Int,
    verseNumber: Int,
    baseTranslation: Translation?,
    comparedVerses: Map<String, String>,
    allTranslations: List<Translation>,
    targetTranslations: List<String> = emptyList(),
    showDifferences: Boolean = true,
    diffMode: DiffMode = DiffMode.LOOSE,
    isLoading: Boolean = false,
    onToggleDifferences: () -> Unit = {},
    onToggleDiffMode: () -> Unit = {},
    onPrevVerse: () -> Unit = {},
    onNextVerse: () -> Unit = {},
    onAddTranslation: (String) -> Unit = {},
    onRemoveTranslation: (String) -> Unit = {},
    onSelectTranslation: (Translation) -> Unit = {},
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val transMap = remember(allTranslations) { allTranslations.associateBy { it.id } }

    var showAddTranslationDialog by remember { mutableStateOf(false) }
    var selectedLangFilter by remember { mutableStateOf(currentLanguage) }

    val baseId = baseTranslation?.id ?: ""
    val baseText = comparedVerses[baseId] ?: ""

    // Ordered comparison slots
    val slots = remember(targetTranslations, comparedVerses, baseId) {
        val list = mutableListOf<String>()
        // Add target translations
        for (t in targetTranslations) {
            if (t != baseId && !list.contains(t)) {
                list.add(t)
            }
        }
        // Add any remaining keys in comparedVerses
        for (k in comparedVerses.keys) {
            if (k != baseId && !list.contains(k)) {
                list.add(k)
            }
        }
        list
    }

    val filteredSlots = remember(slots, selectedLangFilter, transMap) {
        if (selectedLangFilter.isBlank()) {
            slots
        } else {
            slots.filter { tid ->
                val tr = transMap[tid]
                tr?.language.equals(selectedLangFilter, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with Verse Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onPrevVerse,
                            enabled = verseNumber > 1,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = Strings.get("previous_verse", currentLanguage),
                                tint = if (verseNumber > 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        }

                        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                            Text(
                                text = Strings.get("compare_verse", currentLanguage),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${book.name} $chapter:$verseNumber",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = onNextVerse,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = Strings.get("next_verse", currentLanguage),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (isLoading) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Options & Filter Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = showDifferences,
                        onClick = onToggleDifferences,
                        label = { Text(Strings.get("show_differences", currentLanguage)) },
                        modifier = Modifier.testTag("toggle_diff_chip")
                    )

                    if (showDifferences) {
                        FilterChip(
                            selected = diffMode == DiffMode.STRICT,
                            onClick = onToggleDiffMode,
                            label = {
                                Text(
                                    if (diffMode == DiffMode.STRICT)
                                        Strings.get("diff_mode_strict", currentLanguage)
                                    else
                                        Strings.get("diff_mode_loose", currentLanguage)
                                )
                            },
                            modifier = Modifier.testTag("toggle_diff_mode_chip")
                        )
                    }

                    FilterChip(
                        selected = selectedLangFilter == "pl",
                        onClick = { selectedLangFilter = if (selectedLangFilter == "pl") "" else "pl" },
                        label = { Text(Strings.get("lang_pl", currentLanguage)) }
                    )

                    FilterChip(
                        selected = selectedLangFilter == "en",
                        onClick = { selectedLangFilter = if (selectedLangFilter == "en") "" else "en" },
                        label = { Text(Strings.get("lang_en", currentLanguage)) }
                    )

                    FilterChip(
                        selected = selectedLangFilter == "de",
                        onClick = { selectedLangFilter = if (selectedLangFilter == "de") "" else "de" },
                        label = { Text(Strings.get("lang_de", currentLanguage)) }
                    )

                    FilterChip(
                        selected = selectedLangFilter.isBlank(),
                        onClick = { selectedLangFilter = "" },
                        label = { Text(Strings.get("lang_all", currentLanguage)) }
                    )

                    // Add translation chip
                    FilterChip(
                        selected = false,
                        onClick = { showAddTranslationDialog = true },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(Strings.get("add_comparison", currentLanguage)) },
                        modifier = Modifier.testTag("add_comparison_chip")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Verses List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Pinned Base Translation Card (Always displays immediately!)
                    item(key = "base_card_$baseId") {
                        val trans = transMap[baseId] ?: baseTranslation
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("base_compared_verse")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = baseId.uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = trans?.name ?: baseId,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "(${Strings.get("current_translation", currentLanguage)})",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            val copyText = "${book.name} $chapter:$verseNumber (${baseId.uppercase()})\n\"$baseText\""
                                            clipboardManager.setText(AnnotatedString(copyText))
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy",
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = if (baseText.isNotBlank()) baseText else if (isLoading) "..." else "—",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }

                    // Comparison Divider
                    item(key = "divider_compare_with") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f))
                            Text(
                                text = Strings.get("compare_with", currentLanguage),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f))
                        }
                    }

                    // Compared translation slots
                    items(filteredSlots, key = { it }) { transId ->
                        val trans = transMap[transId]
                        val text = comparedVerses[transId]
                        val isNotFound = text == "[NOT_FOUND]"
                        val isSlotLoading = text == null && isLoading

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("compared_verse_$transId")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = transId.uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = trans?.name ?: transId,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (!isNotFound && text != null) {
                                            IconButton(
                                                onClick = {
                                                    val copyText = "${book.name} $chapter:$verseNumber (${transId.uppercase()})\n\"$text\""
                                                    clipboardManager.setText(AnnotatedString(copyText))
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "Copy",
                                                    tint = MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            if (trans != null) {
                                                IconButton(
                                                    onClick = { onSelectTranslation(trans) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.MenuBook,
                                                        contentDescription = "Switch to this translation",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        IconButton(
                                            onClick = { onRemoveTranslation(transId) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = Strings.get("remove_from_comparison", currentLanguage),
                                                tint = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                when {
                                    isSlotLoading -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Text(
                                                text = "Wczytywanie...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                    isNotFound -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = Strings.get("verse_not_found_in_translation", currentLanguage),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontStyle = FontStyle.Italic,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                    text != null -> {
                                        val displayedText = if (showDifferences && baseText.isNotBlank()) {
                                            DiffUtil.highlightDifferences(baseText, text, diffMode)
                                        } else {
                                            AnnotatedString(text)
                                        }

                                        Text(
                                            text = displayedText,
                                            style = MaterialTheme.typography.bodyLarge,
                                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = "—",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (filteredSlots.isEmpty() && !isLoading) {
                        item(key = "empty_hint") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Kliknij '+ ${Strings.get("add_comparison", currentLanguage)}' aby wybrać przekłady do porównania",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddTranslationDialog) {
        TranslationSelectorDialog(
            currentLanguage = currentLanguage,
            translations = allTranslations.filter { it.id != baseId && !slots.contains(it.id) },
            selectedTranslation = null,
            onSelectTranslation = { trans ->
                showAddTranslationDialog = false
                onAddTranslation(trans.id)
            },
            onToggleFavorite = {},
            onDismiss = { showAddTranslationDialog = false }
        )
    }
}
