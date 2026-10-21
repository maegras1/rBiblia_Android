package com.example.data.local.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.Translation

@Entity(
    tableName = "cached_translations",
    indices = [Index(value = ["language"])]
)
data class CachedTranslationEntity(
    @PrimaryKey
    val id: String,
    val language: String,
    val name: String,
    val description: String,
    val date: String,
    val updatedTimestamp: Long = System.currentTimeMillis()
) {
    fun toDomain(isFavorite: Boolean = false): Translation = Translation(
        id = id,
        language = language,
        name = name,
        description = description,
        date = date,
        isFavorite = isFavorite
    )

    companion object {
        fun fromDomain(translation: Translation): CachedTranslationEntity = CachedTranslationEntity(
            id = translation.id,
            language = translation.language,
            name = translation.name,
            description = translation.description,
            date = translation.date
        )
    }
}
