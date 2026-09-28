package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BookInfo
import com.example.data.model.Translation
import com.example.data.model.Verse
import com.example.ui.util.Strings

@Composable
fun ChapterComparisonDialog(
    currentLanguage: String,
    book: BookInfo,
    chapter: Int,
    allTranslations: List<Translation>,
    selectedTranslations: List<String>,
    chapterVerses: Map<String, List<Verse>>,
    isLoading: Boolean,
    onLoadComparison: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var trans1 by remember { mutableStateOf(selectedTranslations.getOrNull(0) ?: "en_kjv") }
    var trans2 by remember { mutableStateOf(selectedTranslations.getOrNull(1) ?: "pl_ubg") }

    var expanded1 by remember { mutableStateOf(false) }
    var expanded2 by remember { mutableStateOf(false) }

    LaunchedEffect(trans1, trans2) {
        onLoadComparison(listOf(trans1, trans2))
    }

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
                            text = Strings.get("chapter_comparison", currentLanguage),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${book.name} $chapter",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Translation Selectors Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Translation 1 Box
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded1 = true }
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = trans1.uppercase(),
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        DropdownMenu(expanded = expanded1, onDismissRequest = { expanded1 = false }) {
                            allTranslations.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text("${t.id.uppercase()} - ${t.name}") },
                                    onClick = {
                                        trans1 = t.id
                                        expanded1 = false
                                    }
                                )
                            }
                        }
                    }

                    // Translation 2 Box
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded2 = true }
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = trans2.uppercase(),
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        DropdownMenu(expanded = expanded2, onDismissRequest = { expanded2 = false }) {
                            allTranslations.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text("${t.id.uppercase()} - ${t.name}") },
                                    onClick = {
                                        trans2 = t.id
                                        expanded2 = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    val list1 = chapterVerses[trans1] ?: emptyList()
                    val list2 = chapterVerses[trans2] ?: emptyList()
                    val maxVerses = maxOf(list1.size, list2.size)

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items((1..maxVerses).toList()) { vNum ->
                            val text1 = list1.firstOrNull { it.number == vNum }?.text ?: ""
                            val text2 = list2.firstOrNull { it.number == vNum }?.text ?: ""

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("comp_row_$vNum")
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "$chapter:$vNum",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = text1.ifEmpty { "—" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = text2.ifEmpty { "—" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
