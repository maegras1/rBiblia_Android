package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.BookInfo
import com.example.data.model.Translation
import com.example.data.model.Verse
import com.example.ui.util.Strings

@Composable
fun ErrorReportDialog(
    currentLanguage: String,
    book: BookInfo,
    chapter: Int,
    verse: Verse,
    translation: Translation?,
    isSubmitting: Boolean,
    onSubmit: (name: String, email: String, notes: String, content: String, errorType: String) -> Unit,
    onDismiss: () -> Unit
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    var errorType by remember { mutableStateOf("typo") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var content by remember { mutableStateOf(verse.text) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Strings.get("report_error", currentLanguage),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "${translation?.id?.uppercase()} - ${book.name} $chapter:${verse.number}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Error Type Chips
                Text(
                    text = Strings.get("error_type", currentLanguage),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = errorType == "typo",
                        onClick = { errorType = "typo" },
                        label = { Text(Strings.get("type_typo", currentLanguage)) }
                    )
                    FilterChip(
                        selected = errorType == "punctuation",
                        onClick = { errorType = "punctuation" },
                        label = { Text(Strings.get("type_punct", currentLanguage)) }
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = errorType == "missing_words",
                        onClick = { errorType = "missing_words" },
                        label = { Text(Strings.get("type_missing", currentLanguage)) }
                    )
                    FilterChip(
                        selected = errorType == "inaccuracy",
                        onClick = { errorType = "inaccuracy" },
                        label = { Text(Strings.get("type_inaccuracy", currentLanguage)) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(Strings.get("reporter_name", currentLanguage)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(Strings.get("reporter_email", currentLanguage)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Suggested Correction
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(Strings.get("suggested_text", currentLanguage)) },
                    minLines = 3,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Notes / Comment
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(Strings.get("report_comment", currentLanguage)) },
                    minLines = 2,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons with equal balanced size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val text = "Report: ${book.sigla} $chapter:${verse.number} (${translation?.id})\nOriginal: ${verse.text}\nSuggested: $content\nBy: $name ($email)\nNotes: $notes"
                            clipboardManager.setText(AnnotatedString(text))
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(Strings.get("copy_report", currentLanguage), maxLines = 1)
                    }

                    Button(
                        onClick = {
                            onSubmit(name, email, notes, content, errorType)
                        },
                        enabled = name.isNotBlank() && email.isNotBlank() && content.isNotBlank() && !isSubmitting,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("submit_report_button")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                        } else {
                            Text(Strings.get("submit_report", currentLanguage), maxLines = 1)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
