package com.example.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotesFlow(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): List<NoteEntity>

    @Query("""
        SELECT * FROM notes 
        WHERE bookId = :bookId AND chapterId = :chapterId 
        AND (isGlobal = 1 OR translationId = :translationId)
        ORDER BY verseId ASC
    """)
    fun getNotesForChapterFlow(
        bookId: String,
        chapterId: Int,
        translationId: String
    ): Flow<List<NoteEntity>>

    @Query("""
        SELECT * FROM notes 
        WHERE bookId = :bookId AND chapterId = :chapterId 
        AND (isGlobal = 1 OR translationId = :translationId)
        ORDER BY verseId ASC
    """)
    fun getNotesForChapter(
        bookId: String,
        chapterId: Int,
        translationId: String
    ): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNote(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNotes(notes: List<NoteEntity>): List<Long>

    @Update
    fun updateNote(note: NoteEntity): Int

    @Query("DELETE FROM notes WHERE id = :id")
    fun deleteNoteById(id: Long): Int

    @Query("SELECT COUNT(*) FROM notes")
    fun getCount(): Int
}
