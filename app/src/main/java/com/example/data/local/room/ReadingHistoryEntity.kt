package com.example.data.local.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reading_history",
    indices = [
        Index(value = ["bookId", "chapter"]),
        Index(value = ["timestamp"])
    ]
)
data class ReadingHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: String,
    val bookName: String,
    val chapter: Int,
    val verseNumber: Int = 1,
    val translationId: String,
    val timestamp: Long = System.currentTimeMillis()
)
