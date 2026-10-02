package com.example.ui.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DarkVariant
import com.example.data.model.TextFontFamily
import com.example.data.model.TextSize
import com.example.data.model.ThemeMode
import com.example.ui.util.DiffMode
import com.example.ui.util.Strings
import com.example.util.AppUpdateManager

@Composable
fun SettingsDialog(
    currentLanguage: String,
    textSize: TextSize,
    fontFamily: TextFontFamily,
    themeMode: ThemeMode,
    darkVariant: DarkVariant,
    zenMode: Boolean,
    continuousText: Boolean,
    hideVerseNumbers: Boolean,
    diffMode: DiffMode = DiffMode.LOOSE,
    comparisonLimit: Int = 4,
    cachedVersesCount: Int = 0,
    isDownloadingOffline: Boolean = false,
    offlineDownloadProgress: Pair<Int, Int>? = null,
    onTextSizeChanged: (TextSize) -> Unit,
    onFontFamilyChanged: (TextFontFamily) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onDarkVariantChanged: (DarkVariant) -> Unit,
    onZenModeChanged: (Boolean) -> Unit,
    onContinuousTextChanged: (Boolean) -> Unit,
    onHideVerseNumbersChanged: (Boolean) -> Unit,
    onLanguageChanged: (String) -> Unit,
    onDiffModeChanged: (DiffMode) -> Unit = {},
    onComparisonLimitChanged: (Int) -> Unit = {},
    onClearCache: () -> Unit = {},
    onDownloadBookOffline: () -> Unit = {},
    onDismiss: () -> Unit
) {
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
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Strings.get("settings", currentLanguage),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = Strings.get("cancel", currentLanguage))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // --- Text Size ---
                    Text(
                        text = Strings.get("text_size", currentLanguage),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextSize.values().forEach { size ->
                            FilterChip(
                                selected = textSize == size,
                                onClick = { onTextSizeChanged(size) },
                                label = { Text(size.getLabel(currentLanguage)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Font Family ---
                    Text(
                        text = Strings.get("font_family", currentLanguage),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextFontFamily.values().forEach { ff ->
                            FilterChip(
                                selected = fontFamily == ff,
                                onClick = { onFontFamilyChanged(ff) },
                                label = { Text(ff.getLabel(currentLanguage)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Layout Mode ---
                    Text(
                        text = Strings.get("verse_layout", currentLanguage),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = !continuousText,
                            onClick = { onContinuousTextChanged(false) },
                            label = { Text(Strings.get("layout_split", currentLanguage)) }
                        )
                        FilterChip(
                            selected = continuousText,
                            onClick = { onContinuousTextChanged(true) },
                            label = { Text(Strings.get("layout_continuous", currentLanguage)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Verse Numbers Switch ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = Strings.get("verse_numbers", currentLanguage),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (hideVerseNumbers) Strings.get("hide", currentLanguage) else Strings.get("show", currentLanguage),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = !hideVerseNumbers,
                            onCheckedChange = { onHideVerseNumbersChanged(!it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Zen Mode Switch ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = Strings.get("zen_mode", currentLanguage),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = Strings.get("zen_mode_hint", currentLanguage),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = zenMode,
                            onCheckedChange = onZenModeChanged
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Theme ---
                    Text(
                        text = Strings.get("theme", currentLanguage),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ThemeMode.values().forEach { mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { onThemeModeChanged(mode) },
                                label = { Text(mode.getLabel(currentLanguage)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // --- Dark Variant ---
                    Text(
                        text = Strings.get("dark_variant", currentLanguage),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DarkVariant.values().forEach { variant ->
                            FilterChip(
                                selected = darkVariant == variant,
                                onClick = { onDarkVariantChanged(variant) },
                                label = { Text(variant.getLabel(currentLanguage)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Comparison Settings (Limit & Diff Mode) ---
                    Text(
                        text = Strings.get("chapter_comparison", currentLanguage),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Strings.get("comparison_count", currentLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(2, 3, 4, 5, 6).forEach { limit ->
                            FilterChip(
                                selected = comparisonLimit == limit,
                                onClick = { onComparisonLimitChanged(limit) },
                                label = { Text(limit.toString()) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = Strings.get("diff_mode", currentLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = diffMode == DiffMode.LOOSE,
                            onClick = { onDiffModeChanged(DiffMode.LOOSE) },
                            label = { Text(Strings.get("diff_mode_loose", currentLanguage)) }
                        )
                        FilterChip(
                            selected = diffMode == DiffMode.STRICT,
                            onClick = { onDiffModeChanged(DiffMode.STRICT) },
                            label = { Text(Strings.get("diff_mode_strict", currentLanguage)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- App Language ---
                    Text(
                        text = Strings.get("language", currentLanguage),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = currentLanguage == "en",
                            onClick = { onLanguageChanged("en") },
                            label = { Text("English (EN)") }
                        )
                        FilterChip(
                            selected = currentLanguage == "pl",
                            onClick = { onLanguageChanged("pl") },
                            label = { Text("Polski (PL)") }
                        )
                        FilterChip(
                            selected = currentLanguage == "de",
                            onClick = { onLanguageChanged("de") },
                            label = { Text("Deutsch (DE)") }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Offline Cache (Room Database) ---
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Strings.get("offline_cache", currentLanguage),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${Strings.get("cached_verses_count", currentLanguage)}: $cachedVersesCount",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isDownloadingOffline && offlineDownloadProgress != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${Strings.get("downloading_offline", currentLanguage)} (${offlineDownloadProgress.first}/${offlineDownloadProgress.second})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val progress = if (offlineDownloadProgress.second > 0) {
                            offlineDownloadProgress.first.toFloat() / offlineDownloadProgress.second.toFloat()
                        } else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onDownloadBookOffline,
                            enabled = !isDownloadingOffline,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = Strings.get("download_book_offline", currentLanguage),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        OutlinedButton(
                            onClick = onClearCache,
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = Strings.get("clear_cache", currentLanguage),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
