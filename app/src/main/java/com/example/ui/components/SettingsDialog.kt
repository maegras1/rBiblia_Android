package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.util.Strings

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
    onTextSizeChanged: (TextSize) -> Unit,
    onFontFamilyChanged: (TextFontFamily) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onDarkVariantChanged: (DarkVariant) -> Unit,
    onZenModeChanged: (Boolean) -> Unit,
    onContinuousTextChanged: (Boolean) -> Unit,
    onHideVerseNumbersChanged: (Boolean) -> Unit,
    onLanguageChanged: (String) -> Unit,
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
                        Icon(Icons.Default.Close, contentDescription = "Close")
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
                                label = { Text(size.label) }
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
                                label = { Text(ff.label) }
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
                                label = { Text(mode.label) }
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
                                label = { Text(variant.label) }
                            )
                        }
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
                }
            }
        }
    }
}
