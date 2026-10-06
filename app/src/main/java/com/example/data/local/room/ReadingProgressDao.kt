package com.example.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingProgressDao {

    // --- Book Reading Progress ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertBookProgress(item: BookReadingProgressEntity): Long

    @Query("SELECT * FROM book_reading_progress WHERE bookId = :bookId LIMIT 1")
    fun getBookProgressFlow(bookId: String): Flow<BookReadingProgressEntity?>

    @Query("SELECT * FROM book_reading_progress WHERE bookId = :bookId LIMIT 1")
    fun getBookProgressDirect(bookId: String): BookReadingProgressEntity?

    @Query("SELECT * FROM book_reading_progress ORDER BY lastReadTimestamp DESC")
    fun getAllBookProgressFlow(): Flow<List<BookReadingProgressEntity>>

    @Query("SELECT * FROM book_reading_progress ORDER BY lastReadTimestamp DESC")
    fun getAllBookProgressDirect(): List<BookReadingProgressEntity>

    // --- Chapter Reading Progress ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertChapterProgress(item: ChapterReadingProgressEntity): Long

    @Query("SELECT * FROM chapter_reading_progress WHERE bookId = :bookId AND chapter = :chapter LIMIT 1")
    fun getChapterProgressDirect(bookId: String, chapter: Int): ChapterReadingProgressEntity?

    @Query("SELECT * FROM chapter_reading_progress WHERE bookId = :bookId AND chapter = :chapter LIMIT 1")
    fun getChapterProgressFlow(bookId: String, chapter: Int): Flow<ChapterReadingProgressEntity?>

    @Query("SELECT * FROM chapter_reading_progress WHERE bookId = :bookId ORDER BY chapter ASC")
    fun getChaptersProgressForBookFlow(bookId: String): Flow<List<ChapterReadingProgressEntity>>

    @Query("SELECT * FROM chapter_reading_progress WHERE bookId = :bookId ORDER BY chapter ASC")
    fun getChaptersProgressForBookDirect(bookId: String): List<ChapterReadingProgressEntity>

    @Query("UPDATE chapter_reading_progress SET isCompleted = :isCompleted, lastReadTimestamp = :timestamp WHERE bookId = :bookId AND chapter = :chapter")
    fun updateChapterCompletion(bookId: String, chapter: Int, isCompleted: Boolean, timestamp: Long): Int

    @Query("DELETE FROM book_reading_progress")
    fun clearBookProgress(): Int

    @Query("DELETE FROM chapter_reading_progress")
    fun clearChapterProgress(): Int
}
