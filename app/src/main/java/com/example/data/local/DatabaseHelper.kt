package com.example.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.data.model.VerseNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "rbiblia.db"
        private const val DATABASE_VERSION = 2

        // Table Notes
        const val TABLE_NOTES = "notes"
        const val COL_NOTE_ID = "id"
        const val COL_NOTE_TRANSLATION_ID = "translation_id"
        const val COL_NOTE_BOOK_ID = "book_id"
        const val COL_NOTE_CHAPTER_ID = "chapter_id"
        const val COL_NOTE_VERSE_ID = "verse_id"
        const val COL_NOTE_CONTENT = "content"
        const val COL_NOTE_TIMESTAMP = "timestamp"
        const val COL_NOTE_IS_GLOBAL = "is_global"

        // Table Favorites
        const val TABLE_FAVORITES = "favorites"
        const val COL_FAV_TRANSLATION_ID = "translation_id"

        // Table Recent Searches
        const val TABLE_SEARCH_HISTORY = "search_history"
        const val COL_SEARCH_QUERY = "query"
        const val COL_SEARCH_TIMESTAMP = "timestamp"

        // Table Reading Progress
        const val TABLE_READING_HISTORY = "reading_history"
        const val COL_HIST_ID = "id"
        const val COL_HIST_TRANSLATION = "translation_id"
        const val COL_HIST_BOOK = "book_id"
        const val COL_HIST_CHAPTER = "chapter_id"
        const val COL_HIST_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_NOTES (
                $COL_NOTE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOTE_TRANSLATION_ID TEXT,
                $COL_NOTE_BOOK_ID TEXT NOT NULL,
                $COL_NOTE_CHAPTER_ID INTEGER NOT NULL,
                $COL_NOTE_VERSE_ID INTEGER NOT NULL,
                $COL_NOTE_CONTENT TEXT NOT NULL,
                $COL_NOTE_TIMESTAMP INTEGER NOT NULL,
                $COL_NOTE_IS_GLOBAL INTEGER NOT NULL DEFAULT 1
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_FAVORITES (
                $COL_FAV_TRANSLATION_ID TEXT PRIMARY KEY
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_SEARCH_HISTORY (
                $COL_SEARCH_QUERY TEXT PRIMARY KEY,
                $COL_SEARCH_TIMESTAMP INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_READING_HISTORY (
                $COL_HIST_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_HIST_TRANSLATION TEXT NOT NULL,
                $COL_HIST_BOOK TEXT NOT NULL,
                $COL_HIST_CHAPTER INTEGER NOT NULL,
                $COL_HIST_TIMESTAMP INTEGER NOT NULL
            )
        """.trimIndent())

        // Default favorite translation
        db.execSQL("INSERT OR IGNORE INTO $TABLE_FAVORITES VALUES ('en_kjv')")
        db.execSQL("INSERT OR IGNORE INTO $TABLE_FAVORITES VALUES ('pl_ubg')")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_READING_HISTORY ($COL_HIST_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_HIST_TRANSLATION TEXT, $COL_HIST_BOOK TEXT, $COL_HIST_CHAPTER INTEGER, $COL_HIST_TIMESTAMP INTEGER)")
        }
    }

    // --- Notes CRUD ---
    suspend fun saveNote(note: VerseNote): Long = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_NOTE_TRANSLATION_ID, if (note.isGlobal) null else note.translationId)
            put(COL_NOTE_BOOK_ID, note.bookId)
            put(COL_NOTE_CHAPTER_ID, note.chapterId)
            put(COL_NOTE_VERSE_ID, note.verseId)
            put(COL_NOTE_CONTENT, note.content)
            put(COL_NOTE_TIMESTAMP, System.currentTimeMillis())
            put(COL_NOTE_IS_GLOBAL, if (note.isGlobal) 1 else 0)
        }

        if (note.id > 0) {
            db.update(TABLE_NOTES, cv, "$COL_NOTE_ID = ?", arrayOf(note.id.toString()))
            note.id
        } else {
            db.insert(TABLE_NOTES, null, cv)
        }
    }

    suspend fun deleteNote(noteId: Long) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete(TABLE_NOTES, "$COL_NOTE_ID = ?", arrayOf(noteId.toString()))
    }

    suspend fun getNotesForChapter(bookId: String, chapterId: Int, translationId: String): List<VerseNote> =
        withContext(Dispatchers.IO) {
            val db = readableDatabase
            val notes = mutableListOf<VerseNote>()
            val cursor = db.rawQuery(
                """
                SELECT * FROM $TABLE_NOTES 
                WHERE $COL_NOTE_BOOK_ID = ? AND $COL_NOTE_CHAPTER_ID = ? 
                AND ($COL_NOTE_IS_GLOBAL = 1 OR $COL_NOTE_TRANSLATION_ID = ?)
                ORDER BY $COL_NOTE_VERSE_ID ASC
                """.trimIndent(),
                arrayOf(bookId, chapterId.toString(), translationId)
            )

            cursor.use {
                while (it.moveToNext()) {
                    notes.add(
                        VerseNote(
                            id = it.getLong(it.getColumnIndexOrThrow(COL_NOTE_ID)),
                            translationId = it.getString(it.getColumnIndexOrThrow(COL_NOTE_TRANSLATION_ID)),
                            bookId = it.getString(it.getColumnIndexOrThrow(COL_NOTE_BOOK_ID)),
                            chapterId = it.getInt(it.getColumnIndexOrThrow(COL_NOTE_CHAPTER_ID)),
                            verseId = it.getInt(it.getColumnIndexOrThrow(COL_NOTE_VERSE_ID)),
                            content = it.getString(it.getColumnIndexOrThrow(COL_NOTE_CONTENT)),
                            timestamp = it.getLong(it.getColumnIndexOrThrow(COL_NOTE_TIMESTAMP)),
                            isGlobal = it.getInt(it.getColumnIndexOrThrow(COL_NOTE_IS_GLOBAL)) == 1
                        )
                    )
                }
            }
            notes
        }

    suspend fun getAllNotes(): List<VerseNote> = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val notes = mutableListOf<VerseNote>()
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NOTES ORDER BY $COL_NOTE_TIMESTAMP DESC", null)
        cursor.use {
            while (it.moveToNext()) {
                notes.add(
                    VerseNote(
                        id = it.getLong(it.getColumnIndexOrThrow(COL_NOTE_ID)),
                        translationId = it.getString(it.getColumnIndexOrThrow(COL_NOTE_TRANSLATION_ID)),
                        bookId = it.getString(it.getColumnIndexOrThrow(COL_NOTE_BOOK_ID)),
                        chapterId = it.getInt(it.getColumnIndexOrThrow(COL_NOTE_CHAPTER_ID)),
                        verseId = it.getInt(it.getColumnIndexOrThrow(COL_NOTE_VERSE_ID)),
                        content = it.getString(it.getColumnIndexOrThrow(COL_NOTE_CONTENT)),
                        timestamp = it.getLong(it.getColumnIndexOrThrow(COL_NOTE_TIMESTAMP)),
                        isGlobal = it.getInt(it.getColumnIndexOrThrow(COL_NOTE_IS_GLOBAL)) == 1
                    )
                )
            }
        }
        notes
    }

    // --- Favorites ---
    suspend fun getFavorites(): Set<String> = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val favs = mutableSetOf<String>()
        val cursor = db.rawQuery("SELECT $COL_FAV_TRANSLATION_ID FROM $TABLE_FAVORITES", null)
        cursor.use {
            while (it.moveToNext()) {
                favs.add(it.getString(0))
            }
        }
        favs
    }

    suspend fun toggleFavorite(translationId: String, isFav: Boolean) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        if (isFav) {
            val cv = ContentValues().apply { put(COL_FAV_TRANSLATION_ID, translationId) }
            db.insertWithOnConflict(TABLE_FAVORITES, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        } else {
            db.delete(TABLE_FAVORITES, "$COL_FAV_TRANSLATION_ID = ?", arrayOf(translationId))
        }
    }

    // --- Recent Searches ---
    suspend fun getRecentSearches(): List<String> = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val list = mutableListOf<String>()
        val cursor = db.rawQuery(
            "SELECT $COL_SEARCH_QUERY FROM $TABLE_SEARCH_HISTORY ORDER BY $COL_SEARCH_TIMESTAMP DESC LIMIT 10",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(it.getString(0))
            }
        }
        list
    }

    suspend fun addSearchQuery(query: String) = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_SEARCH_QUERY, query.trim())
            put(COL_SEARCH_TIMESTAMP, System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE_SEARCH_HISTORY, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    suspend fun clearSearchHistory() = withContext(Dispatchers.IO) {
        writableDatabase.delete(TABLE_SEARCH_HISTORY, null, null)
    }

    // --- XML / JSON Import & Export for Notes ---
    suspend fun exportNotesToXml(): String = withContext(Dispatchers.IO) {
        val notes = getAllNotes()
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<rbiblia_notes version=\"1.0\">\n")
        for (n in notes) {
            sb.append("  <note>\n")
            sb.append("    <book>${escapeXml(n.bookId)}</book>\n")
            sb.append("    <chapter>${n.chapterId}</chapter>\n")
            sb.append("    <verse>${n.verseId}</verse>\n")
            sb.append("    <translation>${escapeXml(n.translationId ?: "")}</translation>\n")
            sb.append("    <is_global>${n.isGlobal}</is_global>\n")
            sb.append("    <timestamp>${n.timestamp}</timestamp>\n")
            sb.append("    <content>${escapeXml(n.content)}</content>\n")
            sb.append("  </note>\n")
        }
        sb.append("</rbiblia_notes>")
        sb.toString()
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
