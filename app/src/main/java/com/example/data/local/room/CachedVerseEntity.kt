package com.example.data.local.room

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "cached_verses",
    primaryKeys = ["translationId", "bookId", "chapterId", "verseNumber"],
    indices = [
        Index(value = ["translationId", "bookId", "chapterId"]),
        Index(value = ["timestamp"])
    ]
)
data class CachedVerseEntity(
    val translationId: String,
    val bookId: String,
    val chapterId: Int,
    val verseNumber: Int,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
