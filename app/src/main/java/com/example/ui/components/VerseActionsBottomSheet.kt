package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onAddNote: () -> Unit,
    onCompare: () -> Unit,
    onReportError: () -> Unit,
    onDismiss: () -> Unit
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("verse_actions_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // Action: Copy
            VerseActionItem(
                icon = Icons.Default.ContentCopy,
                title = Strings.get("copied", currentLanguage).replace(" to clipboard", ""),
                testTag = "action_copy_verse",
                onClick = {
                    val full = "${book.name} $chapter:${verse.number} - \"${verse.text}\""
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

            Spacer(modifier = Modifier.height(24.dp))
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
            .padding(vertical = 12.dp),
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
