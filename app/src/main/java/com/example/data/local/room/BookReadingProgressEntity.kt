package com.example.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "book_reading_progress")
data class BookReadingProgressEntity(
    @PrimaryKey
    val bookId: String,
    val totalChapters: Int,
    val lastReadChapter: Int,
    val lastReadVerse: Int = 1,
    val completedChaptersCsv: String = "",
    val completedChaptersCount: Int = 0,
    val percentCompleted: Float = 0f,
    val lastTranslationId: String = "",
    val lastReadTimestamp: Long = System.currentTimeMillis()
)
