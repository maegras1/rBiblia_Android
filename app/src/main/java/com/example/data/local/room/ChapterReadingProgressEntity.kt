package com.example.data.local.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapter_reading_progress",
    indices = [
        Index(value = ["bookId", "chapter"], unique = true),
        Index(value = ["lastReadTimestamp"])
    ]
)
data class ChapterReadingProgressEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: String,
    val chapter: Int,
    val isCompleted: Boolean = false,
    val lastVerseRead: Int = 1,
    val readCount: Int = 1,
    val lastTranslationId: String = "",
    val lastReadTimestamp: Long = System.currentTimeMillis()
)
