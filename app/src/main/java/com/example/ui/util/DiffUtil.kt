package com.example.ui.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.DiffAddedColor

enum class DiffMode {
    LOOSE, // Ignores word order (set membership)
    STRICT // LCS (Longest Common Subsequence) respects word order
}

object DiffUtil {
    fun highlightDifferences(
        baseText: String,
        targetText: String,
        mode: DiffMode = DiffMode.LOOSE
    ): AnnotatedString {
        if (baseText.isBlank() || targetText.isBlank()) {
            return AnnotatedString(targetText)
        }

        return when (mode) {
            DiffMode.LOOSE -> highlightLooseDifferences(baseText, targetText)
            DiffMode.STRICT -> highlightStrictDifferences(baseText, targetText)
        }
    }

    private fun highlightLooseDifferences(baseText: String, targetText: String): AnnotatedString {
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

    private fun highlightStrictDifferences(baseText: String, targetText: String): AnnotatedString {
        val baseTokens = baseText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val targetTokensWithSpaces = targetText.split(Regex("(?<=\\s)|(?=\\s+)"))

        // Normalized tokens for LCS
        val cleanBase = baseTokens.map { it.lowercase().replace(Regex("[^\\p{L}\\p{Nd}]"), "") }
        val targetTokensOnly = targetTokensWithSpaces.filter { it.isNotBlank() }
        val cleanTarget = targetTokensOnly.map { it.lowercase().replace(Regex("[^\\p{L}\\p{Nd}]"), "") }

        val n = cleanBase.size
        val m = cleanTarget.size

        // LCS dynamic programming table
        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in 0 until n) {
            for (j in 0 until m) {
                dp[i + 1][j + 1] = if (cleanBase[i] == cleanTarget[j] && cleanBase[i].isNotEmpty()) {
                    dp[i][j] + 1
                } else {
                    maxOf(dp[i + 1][j], dp[i][j + 1])
                }
            }
        }

        // Backtrack to find indices in target that are in LCS
        val lcsTargetIndices = mutableSetOf<Int>()
        var i = n
        var j = m
        while (i > 0 && j > 0) {
            if (cleanBase[i - 1] == cleanTarget[j - 1] && cleanBase[i - 1].isNotEmpty()) {
                lcsTargetIndices.add(j - 1)
                i--
                j--
            } else if (dp[i - 1][j] >= dp[i][j - 1]) {
                i--
            } else {
                j--
            }
        }

        return buildAnnotatedString {
            var targetWordIdx = 0
            for (token in targetTokensWithSpaces) {
                val isWord = token.isNotBlank()
                if (isWord) {
                    val isMatched = lcsTargetIndices.contains(targetWordIdx)
                    targetWordIdx++
                    if (!isMatched) {
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
                } else {
                    append(token)
                }
            }
        }
    }
}
