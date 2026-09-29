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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BookCatalog
import com.example.data.model.VerseNote
import com.example.ui.util.Strings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun NotesListDialog(
    currentLanguage: String,
    notes: List<VerseNote>,
    onNoteClick: (VerseNote) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onExportXml: () -> String,
    onImportXml: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    var showExportSuccess by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

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
                        text = Strings.get("notes", currentLanguage),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Action Buttons (Export & Import XML)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val xml = onExportXml()
                            clipboardManager.setText(AnnotatedString(xml))
                            showExportSuccess = true
                        }
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Strings.get("export_notes_xml", currentLanguage))
                    }

                    OutlinedButton(
                        onClick = {
                            val clip = clipboardManager.getText()?.text ?: ""
                            importText = if (clip.contains("<notes>")) clip else ""
                            showImportDialog = true
                        }
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Strings.get("import_notes_xml", currentLanguage))
                    }
                }

                if (showExportSuccess) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "✓ " + Strings.get("notes_exported", currentLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.End)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (notes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No notes yet. Tap any verse to add a note.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(notes, key = { it.id }) { note ->
                            val bookName = BookCatalog.getBookName(note.bookId, currentLanguage)
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("note_card_${note.id}")
                                    .clickable { onNoteClick(note) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "$bookName ${note.chapterId}:${note.verseId}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (!note.isGlobal && note.translationId != null) {
                                                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                                                Text(
                                                    text = "(${note.translationId.uppercase()})",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = note.content,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = dateFormat.format(Date(note.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteNote(note.id) },
                                        modifier = Modifier.testTag("delete_note_btn_${note.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.outline
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

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(Strings.get("import_notes_xml", currentLanguage)) },
            text = {
                Column {
                    Text(
                        text = "Wklej zawartość pliku XML z notatkami:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        placeholder = { Text("<notes>\n  <translation id=\"_global\">...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importText.isNotBlank()) {
                            onImportXml(importText)
                            showImportDialog = false
                        }
                    },
                    enabled = importText.isNotBlank()
                ) {
                    Text("Importuj")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(Strings.get("cancel", currentLanguage))
                }
            }
        )
    }
}
