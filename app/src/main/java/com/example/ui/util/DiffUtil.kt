package com.example.ui.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.DiffAddedColor

object DiffUtil {
    fun highlightDifferences(baseText: String, targetText: String): AnnotatedString {
        if (baseText.isBlank() || targetText.isBlank()) {
            return AnnotatedString(targetText)
        }

        val baseWords = baseText.trim().split(Regex("\\s+"))
            .map { it.lowercase().replace(Regex("[^\\p{L}\\p{Nd}]"), "") }
            .filter { it.isNotEmpty() }
            .toSet()

        return buildAnnotatedString {
            val wordsWithSpaces = targetText.split(Regex("(?<=\\s)|(?=\\s+)"))
            for (token in wordsWithSpaces) {
                val cleanWord = token.trim().lowercase().replace(Regex("[^\\p{L}\\p{Nd}]"), "")
                if (cleanWord.isNotEmpty() && !baseWords.contains(cleanWord)) {
                    val start = length
                    append(token)
                    addStyle(
                        SpanStyle(
                            background = DiffAddedColor,
                            fontWeight = FontWeight.Bold
                        ),
                        start,
                        length
                    )
                } else {
                    append(token)
                }
            }
        }
    }
}
