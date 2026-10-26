package com.example.data.local.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "verse_partial_highlights",
    indices = [
        Index(value = ["bookId", "chapter", "verseNumber"])
    ]
)
data class PartialHighlightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: String,
    val chapter: Int,
    val verseNumber: Int,
    val startIndex: Int,
    val endIndex: Int,
    val colorHex: String,
    val colorName: String = "yellow",
    val translationId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
