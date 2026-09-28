package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BookInfo
import com.example.data.model.Translation
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
    showDifferences: Boolean,
    onToggleDifferences: () -> Unit,
    onDismiss: () -> Unit
) {
    val baseText = baseTranslation?.let { comparedVerses[it.id] } ?: ""
    val transMap = allTranslations.associateBy { it.id }

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
                    Column {
                        Text(
                            text = Strings.get("compare_verse", currentLanguage),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${book.name} $chapter:$verseNumber",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Toggle Differences Chip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilterChip(
                        selected = showDifferences,
                        onClick = onToggleDifferences,
                        label = { Text(Strings.get("show_differences", currentLanguage)) },
                        modifier = Modifier.testTag("toggle_diff_chip")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Verses List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val entries = comparedVerses.entries.toList()
                    items(entries) { (transId, text) ->
                        val trans = transMap[transId]
                        val isBase = transId == baseTranslation?.id

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isBase) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("compared_verse_$transId")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
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
                                    if (isBase) {
                                        Text(
                                            text = "(Base)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                val displayedText = if (showDifferences && !isBase && baseText.isNotBlank()) {
                                    DiffUtil.highlightDifferences(baseText, text)
                                } else {
                                    androidx.compose.ui.text.AnnotatedString(text)
                                }

                                Text(
                                    text = displayedText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
