package com.example.data.repository

import com.example.data.local.DatabaseHelper
import com.example.data.local.PreferencesManager
import com.example.data.model.BookCatalog
import com.example.data.model.BookInfo
import com.example.data.model.ErrorReport
import com.example.data.model.SearchResult
import com.example.data.model.Translation
import com.example.data.model.Verse
import com.example.data.model.VerseNote
import com.example.data.remote.RBibliaApiService

class BibleRepository(
    private val dbHelper: DatabaseHelper,
    val preferences: PreferencesManager,
    private val apiService: RBibliaApiService
) {
    suspend fun getTranslations(language: String): List<Translation> {
        val favorites = dbHelper.getFavorites()
        var list = apiService.getTranslations(language)
        if (list.isNotEmpty()) {
            dbHelper.saveCachedTranslations(list)
        } else {
            val cached = dbHelper.getCachedTranslations(language)
            if (cached.isNotEmpty()) {
                list = cached
            }
        }
        return list.map { it.copy(isFavorite = favorites.contains(it.id)) }
    }

    suspend fun toggleFavorite(translationId: String, isFav: Boolean) {
        dbHelper.toggleFavorite(translationId, isFav)
    }

    suspend fun getTranslationStructure(language: String, translationId: String): Map<String, List<Int>> {
        return apiService.getTranslationStructure(language, translationId)
    }

    suspend fun getVerses(
        language: String,
        translationId: String,
        bookId: String,
        chapterId: Int
    ): List<Verse> {
        // 1. Check local SQLite cache first for instant rendering
        val cached = dbHelper.getCachedVerses(translationId, bookId, chapterId)
        val finalMap: Map<Int, String> = if (cached.isNotEmpty()) {
            cached
        } else {
            // 2. Fetch from API
            val remote = apiService.getVerses(language, translationId, bookId, chapterId)
            if (remote.isNotEmpty()) {
                dbHelper.saveCachedVerses(translationId, bookId, chapterId, remote)
                remote
            } else {
                // If API returned empty, try fallback from local database
                dbHelper.getCachedVerses(translationId, bookId, chapterId)
            }
        }

        val notes = dbHelper.getNotesForChapter(bookId, chapterId, translationId)
        val notesVerseIds = notes.map { it.verseId }.toSet()

        return finalMap.entries
            .sortedBy { it.key }
            .map { (num, text) ->
                Verse(
                    number = num,
                    text = text,
                    hasNote = notesVerseIds.contains(num)
                )
            }
    }

    suspend fun getVerseInTranslations(
        language: String,
        translationIds: List<String>,
        bookId: String,
        chapterId: Int,
        verseId: Int
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (tid in translationIds) {
            val verses = getVerses(language, tid, bookId, chapterId)
            val verse = verses.find { it.number == verseId }
            if (verse != null && verse.text.isNotBlank()) {
                result[tid] = verse.text
            }
            kotlinx.coroutines.delay(60)
        }
        return result
    }

    suspend fun search(language: String, translationId: String, query: String): List<SearchResult> {
        dbHelper.addSearchQuery(query)
        val results = apiService.search(language, translationId, query)
        return results.map { sr ->
            sr.copy(bookName = BookCatalog.getBookName(sr.book, language))
        }
    }

    suspend fun getRecentSearches(): List<String> {
        return dbHelper.getRecentSearches()
    }

    suspend fun clearSearchHistory() {
        dbHelper.clearSearchHistory()
    }

    suspend fun saveNote(note: VerseNote): Long {
        return dbHelper.saveNote(note)
    }

    suspend fun deleteNote(noteId: Long) {
        dbHelper.deleteNote(noteId)
    }

    suspend fun getNotesForChapter(bookId: String, chapterId: Int, translationId: String): List<VerseNote> {
        return dbHelper.getNotesForChapter(bookId, chapterId, translationId)
    }

    suspend fun getAllNotes(): List<VerseNote> {
        return dbHelper.getAllNotes()
    }

    suspend fun exportNotesXml(): String {
        return dbHelper.exportNotesToXml()
    }

    suspend fun submitReport(language: String, report: ErrorReport): Boolean {
        return apiService.submitReport(language, report)
    }

    fun getBooks(language: String): List<BookInfo> {
        return BookCatalog.getAllBooks(language)
    }

    fun getBook(id: String, language: String): BookInfo {
        return BookCatalog.getBook(id, language)
    }
}
