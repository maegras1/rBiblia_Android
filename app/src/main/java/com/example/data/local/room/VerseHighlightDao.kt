package com.example.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VerseHighlightDao {

    @Query("""
        SELECT * FROM verse_highlights 
        WHERE bookId = :bookId AND chapter = :chapter
        ORDER BY verseNumber ASC
    """)
    fun getHighlightsForChapterFlow(bookId: String, chapter: Int): Flow<List<VerseHighlightEntity>>

    @Query("""
        SELECT * FROM verse_highlights 
        WHERE bookId = :bookId AND chapter = :chapter
        ORDER BY verseNumber ASC
    """)
    fun getHighlightsForChapter(bookId: String, chapter: Int): List<VerseHighlightEntity>

    @Query("SELECT * FROM verse_highlights ORDER BY timestamp DESC")
    fun getAllHighlightsFlow(): Flow<List<VerseHighlightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setHighlight(item: VerseHighlightEntity): Long

    @Query("""
        DELETE FROM verse_highlights 
        WHERE bookId = :bookId AND chapter = :chapter AND verseNumber = :verseNumber
    """)
    fun removeHighlight(bookId: String, chapter: Int, verseNumber: Int): Int

    @Query("DELETE FROM verse_highlights WHERE id = :id")
    fun deleteById(id: Long): Int

    @Query("DELETE FROM verse_highlights")
    fun clearAll(): Int

    @Query("SELECT COUNT(*) FROM verse_highlights")
    fun getCount(): Int
}
