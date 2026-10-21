package com.example.data.local.room

import androidx.room.Entity
import androidx.room.Fts4

@Fts4
@Entity(tableName = "cached_verses_fts")
data class CachedVerseFtsEntity(
    val translationId: String,
    val bookId: String,
    val chapterId: Int,
    val verseNumber: Int,
    val text: String
)
