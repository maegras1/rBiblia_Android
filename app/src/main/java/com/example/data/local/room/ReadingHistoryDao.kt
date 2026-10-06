package com.example.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: ReadingHistoryEntity): Long

    @Query("SELECT * FROM reading_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 50): Flow<List<ReadingHistoryEntity>>

    @Query("SELECT * FROM reading_history WHERE bookId = :bookId ORDER BY timestamp DESC")
    fun getHistoryForBook(bookId: String): Flow<List<ReadingHistoryEntity>>

    @Query("SELECT * FROM reading_history ORDER BY timestamp DESC LIMIT 1")
    fun getLatestEntry(): Flow<ReadingHistoryEntity?>

    @Query("SELECT * FROM reading_history ORDER BY timestamp DESC LIMIT 1")
    fun getLatestEntryDirect(): ReadingHistoryEntity?

    @Query("DELETE FROM reading_history WHERE id = :id")
    fun deleteEntry(id: Long): Int

    @Query("DELETE FROM reading_history")
    fun clearHistory(): Int

    @Query("SELECT COUNT(*) FROM reading_history")
    fun getHistoryCountFlow(): Flow<Int>
}
