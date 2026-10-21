package com.example.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_translations")
data class FavoriteTranslationEntity(
    @PrimaryKey
    val translationId: String,
    val addedTimestamp: Long = System.currentTimeMillis()
)
