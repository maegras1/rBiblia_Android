package com.example.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class CachedChapterInfo(
    val translationId: String,
    val bookId: String,
    val chapterId: Int,
    val verseCount: Int
)

@Dao
interface CachedVerseDao {

    @Query("""
        SELECT * FROM cached_verses 
        WHERE translationId = :translationId AND bookId = :bookId AND chapterId = :chapterId 
        ORDER BY verseNumber ASC
    """)
    fun getChapterVersesFlow(
        translationId: String,
        bookId: String,
        chapterId: Int
    ): Flow<List<CachedVerseEntity>>

    @Query("""
        SELECT * FROM cached_verses 
        WHERE translationId = :translationId AND bookId = :bookId AND chapterId = :chapterId 
        ORDER BY verseNumber ASC
    """)
    fun getChapterVerses(
        translationId: String,
        bookId: String,
        chapterId: Int
    ): List<CachedVerseEntity>

    @Query("""
        SELECT text FROM cached_verses 
        WHERE translationId = :translationId AND bookId = :bookId AND chapterId = :chapterId AND verseNumber = :verseNumber 
        LIMIT 1
    """)
    fun getSingleVerse(
        translationId: String,
        bookId: String,
        chapterId: Int,
        verseNumber: Int
    ): String?

    @Query("""
        SELECT COUNT(*) FROM cached_verses 
        WHERE translationId = :translationId AND bookId = :bookId AND chapterId = :chapterId
    """)
    fun getVerseCountForChapter(
        translationId: String,
        bookId: String,
        chapterId: Int
    ): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertVerses(verses: List<CachedVerseEntity>): List<Long>

    @Query("""
        DELETE FROM cached_verses 
        WHERE translationId = :translationId AND bookId = :bookId AND chapterId = :chapterId
    """)
    fun deleteChapter(
        translationId: String,
        bookId: String,
        chapterId: Int
    ): Int

    @Query("DELETE FROM cached_verses")
    fun clearAll(): Int

    @Query("SELECT COUNT(*) FROM cached_verses")
    fun getTotalCachedVersesCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cached_verses")
    fun getTotalCachedVersesCount(): Int

    @Query("""
        SELECT translationId, bookId, chapterId, COUNT(verseNumber) as verseCount 
        FROM cached_verses 
        GROUP BY translationId, bookId, chapterId 
        ORDER BY timestamp DESC
    """)
    fun getCachedChaptersFlow(): Flow<List<CachedChapterInfo>>
}
