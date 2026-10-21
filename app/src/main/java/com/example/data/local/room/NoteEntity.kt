package com.example.data.local.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.VerseNote

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["bookId", "chapterId"]),
        Index(value = ["timestamp"])
    ]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val translationId: String? = null,
    val bookId: String,
    val chapterId: Int,
    val verseId: Int,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isGlobal: Boolean = true
) {
    fun toDomain(): VerseNote = VerseNote(
        id = id,
        translationId = translationId,
        bookId = bookId,
        chapterId = chapterId,
        verseId = verseId,
        content = content,
        timestamp = timestamp,
        isGlobal = isGlobal
    )

    companion object {
        fun fromDomain(note: VerseNote): NoteEntity = NoteEntity(
            id = if (note.id > 0) note.id else 0,
            translationId = if (note.isGlobal) null else note.translationId,
            bookId = note.bookId,
            chapterId = note.chapterId,
            verseId = note.verseId,
            content = note.content,
            timestamp = note.timestamp,
            isGlobal = note.isGlobal
        )
    }
}
