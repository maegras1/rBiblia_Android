package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.BookCatalog
import com.example.data.model.VerseNote
import com.example.ui.util.Strings

@Composable
fun NoteEditorDialog(
    currentLanguage: String,
    note: VerseNote,
    onSave: (content: String, isGlobal: Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var content by remember { mutableStateOf(note.content) }
    var isGlobal by remember { mutableStateOf(note.isGlobal) }

    val bookName = BookCatalog.getBookName(note.bookId, currentLanguage)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (note.id > 0) Strings.get("edit_note", currentLanguage) else Strings.get("add_note", currentLanguage),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (note.id > 0) {
                        IconButton(
                            onClick = { onDelete(note.id) },
                            modifier = Modifier.testTag("delete_note_button")
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Text(
                    text = "$bookName ${note.chapterId}:${note.verseId}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Content TextField
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text(Strings.get("note_placeholder", currentLanguage)) },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_content_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Global Note checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isGlobal,
                        onCheckedChange = { isGlobal = it },
                        modifier = Modifier.testTag("note_is_global_checkbox")
                    )
                    Text(
                        text = Strings.get("global_note", currentLanguage),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancel_note_button")
                    ) {
                        Text(Strings.get("cancel", currentLanguage))
                    }
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Button(
                        onClick = { onSave(content.trim(), isGlobal) },
                        enabled = content.isNotBlank(),
                        modifier = Modifier.testTag("save_note_button")
                    ) {
                        Text(Strings.get("save_note", currentLanguage))
                    }
                }
            }
        }
    }
}
