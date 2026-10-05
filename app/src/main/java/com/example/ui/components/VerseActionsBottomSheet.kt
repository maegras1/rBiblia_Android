package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BookInfo
import com.example.data.model.Verse
import com.example.ui.util.Strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerseActionsBottomSheet(
    currentLanguage: String,
    book: BookInfo,
    chapter: Int,
    verse: Verse,
    currentHighlightColorHex: String? = null,
    onSetHighlight: (colorHex: String, colorName: String) -> Unit = { _, _ -> },
    onRemoveHighlight: () -> Unit = {},
    onAddNote: () -> Unit,
    onCompare: () -> Unit,
    onReportError: () -> Unit,
    onDismiss: () -> Unit
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    val highlightColors = listOf(
        Triple("#FFF59D", "yellow", Color(0xFFFFF59D)),
        Triple("#A5D6A7", "green", Color(0xFFA5D6A7)),
        Triple("#90CAF9", "blue", Color(0xFF90CAF9)),
        Triple("#F48FB1", "pink", Color(0xFFF48FB1)),
        Triple("#FFE082", "orange", Color(0xFFFFE082))
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("verse_actions_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Verse Header
            Text(
                text = "${book.name} $chapter:${verse.number}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = verse.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))

            // --- Highlighter Section ---
            Text(
                text = Strings.get("highlight_color", currentLanguage),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    highlightColors.forEach { (hex, name, color) ->
                        val isSelected = currentHighlightColorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.25f),
                                    shape = CircleShape
                                )
                                .testTag("highlight_color_$name")
                                .clickable { onSetHighlight(hex, name) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.DarkGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                if (currentHighlightColorHex != null) {
                    TextButton(
                        onClick = onRemoveHighlight,
                        modifier = Modifier.testTag("action_remove_highlight")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = Strings.get("remove_highlight", currentLanguage),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(4.dp))

            // Action: Copy text only
            VerseActionItem(
                icon = Icons.Default.ContentCopy,
                title = Strings.get("copied", currentLanguage).replace(" to clipboard", "").replace(" do schowka", ""),
                testTag = "action_copy_verse",
                onClick = {
                    val full = "${book.name} $chapter:${verse.number} - \"${verse.text}\""
                    clipboardManager.setText(AnnotatedString(full))
                    onDismiss()
                }
            )

            // Action: Copy with Quote citation
            VerseActionItem(
                icon = Icons.Default.FormatQuote,
                title = "${book.sigla.uppercase()} $chapter:${verse.number}",
                testTag = "action_copy_citation",
                onClick = {
                    val full = "„${verse.text}” (${book.sigla.uppercase()} $chapter:${verse.number})"
                    clipboardManager.setText(AnnotatedString(full))
                    onDismiss()
                }
            )

            // Action: Add / Edit Note
            VerseActionItem(
                icon = Icons.Default.EditNote,
                title = if (verse.hasNote) Strings.get("edit_note", currentLanguage) else Strings.get("add_note", currentLanguage),
                testTag = "action_add_note",
                onClick = onAddNote
            )

            // Action: Compare Verse
            VerseActionItem(
                icon = Icons.Default.Compare,
                title = Strings.get("compare_verse", currentLanguage),
                testTag = "action_compare_verse",
                onClick = onCompare
            )

            // Action: Report Error
            VerseActionItem(
                icon = Icons.Default.Report,
                title = Strings.get("report_error", currentLanguage),
                testTag = "action_report_error",
                onClick = onReportError
            )

            // Safe bottom buffer
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun VerseActionItem(
    icon: ImageVector,
    title: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}
