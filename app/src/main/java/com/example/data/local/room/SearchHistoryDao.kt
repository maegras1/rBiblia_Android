package com.example.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 30")
    fun getRecentSearchesFlow(): Flow<List<SearchHistoryEntity>>

    @Query("SELECT query FROM search_history ORDER BY timestamp DESC LIMIT 30")
    fun getRecentQueries(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSearch(item: SearchHistoryEntity): Long

    @Query("DELETE FROM search_history WHERE query = :query")
    fun deleteByQuery(query: String): Int

    @Query("DELETE FROM search_history WHERE id = :id")
    fun deleteById(id: Long): Int

    @Query("DELETE FROM search_history")
    fun clearHistory(): Int

    @Query("SELECT COUNT(*) FROM search_history")
    fun getCount(): Int
}
