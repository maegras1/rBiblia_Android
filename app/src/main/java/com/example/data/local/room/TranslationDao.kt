package com.example.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationDao {

    @Query("SELECT translationId FROM favorite_translations")
    fun getFavoriteIdsFlow(): Flow<List<String>>

    @Query("SELECT translationId FROM favorite_translations")
    fun getFavoriteIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertFavorite(favorite: FavoriteTranslationEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertFavorites(favorites: List<FavoriteTranslationEntity>): List<Long>

    @Query("DELETE FROM favorite_translations WHERE translationId = :translationId")
    fun deleteFavorite(translationId: String): Int

    @Query("SELECT COUNT(*) FROM favorite_translations")
    fun getFavoritesCount(): Int

    @Query("SELECT * FROM cached_translations ORDER BY language ASC, name ASC")
    fun getAllCachedTranslations(): List<CachedTranslationEntity>

    @Query("SELECT * FROM cached_translations WHERE language = :language ORDER BY name ASC")
    fun getCachedTranslationsByLanguage(language: String): List<CachedTranslationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCachedTranslations(translations: List<CachedTranslationEntity>): List<Long>
}
