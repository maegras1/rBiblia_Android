package com.example.data.repository

import com.example.data.local.DatabaseHelper
import com.example.data.local.PreferencesManager
import com.example.data.local.room.BibleDatabase
import com.example.data.local.room.CachedVerseEntity
import com.example.data.local.room.SearchHistoryEntity
import com.example.data.model.BookCatalog
import com.example.data.model.BookInfo
import com.example.data.model.ErrorReport
import com.example.data.model.SearchResult
import com.example.data.model.Translation
import com.example.data.model.Verse
import com.example.data.model.VerseNote
import com.example.data.remote.RBibliaApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class BibleRepository(
    private val dbHelper: DatabaseHelper,
    val preferences: PreferencesManager,
    private val apiService: RBibliaApiService,
    val roomDb: BibleDatabase
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

    suspend fun syncBooksFromApi(language: String) {
        val books = apiService.getBooksFromApi(language)
        if (books.isNotEmpty()) {
            BookCatalog.updateBooksFromApi(language, books)
        }
    }

    suspend fun getVerses(
        language: String,
        translationId: String,
        bookId: String,
        chapterId: Int
    ): List<Verse> = withContext(Dispatchers.IO) {
        // 1. Check local Room database cache first for instant rendering & offline mode
        val roomCached = try {
            roomDb.cachedVerseDao().getChapterVerses(translationId, bookId, chapterId)
        } catch (e: Exception) {
            emptyList()
        }

        val finalMap: Map<Int, String> = if (roomCached.isNotEmpty()) {
            roomCached.associate { it.verseNumber to it.text }
        } else {
            // Check legacy SQLite cache as secondary fallback
            val legacyCached = dbHelper.getCachedVerses(translationId, bookId, chapterId)
            if (legacyCached.isNotEmpty()) {
                // Populate into Room database for future instant access
                val entities = legacyCached.map { (num, text) ->
                    CachedVerseEntity(translationId, bookId, chapterId, num, text)
                }
                try {
                    roomDb.cachedVerseDao().insertVerses(entities)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                legacyCached
            } else {
                // 2. Fetch from API
                val remote = apiService.getVerses(language, translationId, bookId, chapterId)
                if (remote.isNotEmpty()) {
                    // Cache in Room Database
                    val entities = remote.map { (num, text) ->
                        CachedVerseEntity(translationId, bookId, chapterId, num, text)
                    }
                    try {
                        roomDb.cachedVerseDao().insertVerses(entities)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    dbHelper.saveCachedVerses(translationId, bookId, chapterId, remote)
                    remote
                } else {
                    // Fallback
                    dbHelper.getCachedVerses(translationId, bookId, chapterId)
                }
            }
        }

        val notes = dbHelper.getNotesForChapter(bookId, chapterId, translationId)
        val notesVerseIds = notes.map { it.verseId }.toSet()

        finalMap.entries
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
            delay(60)
        }
        return result
    }

    suspend fun search(language: String, translationId: String, query: String): List<SearchResult> {
        recordSearchQuery(query)
        val results = apiService.search(language, translationId, query)
        return results.map { sr ->
            sr.copy(bookName = BookCatalog.getBookName(sr.book, language))
        }
    }

    fun getRecentSearchesFlow(): Flow<List<String>> {
        return roomDb.searchHistoryDao().getRecentSearchesFlow().map { list ->
            list.map { it.query }
        }
    }

    suspend fun getRecentSearches(): List<String> = withContext(Dispatchers.IO) {
        val roomList = roomDb.searchHistoryDao().getRecentQueries()
        if (roomList.isNotEmpty()) {
            roomList
        } else {
            // Seed from legacy SQLite helper if present
            val legacy = dbHelper.getRecentSearches()
            for (q in legacy) {
                recordSearchQuery(q)
            }
            legacy
        }
    }

    suspend fun recordSearchQuery(query: String) = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            roomDb.searchHistoryDao().deleteByQuery(trimmed)
            roomDb.searchHistoryDao().insertSearch(
                SearchHistoryEntity(query = trimmed, timestamp = System.currentTimeMillis())
            )
            try { dbHelper.addSearchQuery(trimmed) } catch (_: Exception) {}
        }
    }

    suspend fun deleteRecentSearch(query: String) = withContext(Dispatchers.IO) {
        roomDb.searchHistoryDao().deleteByQuery(query.trim())
    }

    suspend fun clearSearchHistory() = withContext(Dispatchers.IO) {
        roomDb.searchHistoryDao().clearHistory()
        try { dbHelper.clearSearchHistory() } catch (_: Exception) {}
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

    // --- Room Cache Specific Operations ---
    fun getCachedVersesCountFlow(): Flow<Int> {
        return roomDb.cachedVerseDao().getTotalCachedVersesCountFlow()
    }

    suspend fun clearRoomCache() = withContext(Dispatchers.IO) {
        roomDb.cachedVerseDao().clearAll()
    }

    suspend fun isChapterCached(translationId: String, bookId: String, chapterId: Int): Boolean = withContext(Dispatchers.IO) {
        roomDb.cachedVerseDao().getVerseCountForChapter(translationId, bookId, chapterId) > 0
    }

    suspend fun downloadChapterToCache(
        language: String,
        translationId: String,
        bookId: String,
        chapterId: Int
    ): Boolean = withContext(Dispatchers.IO) {
        val remote = apiService.getVerses(language, translationId, bookId, chapterId)
        if (remote.isNotEmpty()) {
            val entities = remote.map { (num, text) ->
                CachedVerseEntity(translationId, bookId, chapterId, num, text)
            }
            roomDb.cachedVerseDao().insertVerses(entities)
            dbHelper.saveCachedVerses(translationId, bookId, chapterId, remote)
            true
        } else {
            false
        }
    }

    suspend fun downloadBookToCache(
        language: String,
        translationId: String,
        bookId: String,
        totalChapters: Int,
        onProgress: (current: Int, total: Int) -> Unit
    ) {
        for (chap in 1..totalChapters) {
            downloadChapterToCache(language, translationId, bookId, chap)
            onProgress(chap, totalChapters)
            delay(100)
        }
    }

    suspend fun downloadEntireTranslationToCache(
        language: String,
        translationId: String,
        onProgress: (currentBook: String, currentChapter: Int, totalBooks: Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val structure = getTranslationStructure(language, translationId)
        val books = if (structure.isNotEmpty()) {
            structure.keys.toList()
        } else {
            BookCatalog.getAllBooks(language).map { it.id }
        }
        val totalBooks = books.size
        for (bookId in books) {
            val chapters = structure[bookId] ?: (1..BookCatalog.getBook(bookId, language).chapterCount).toList()
            for (chap in chapters) {
                downloadChapterToCache(language, translationId, bookId, chap)
                onProgress(bookId, chap, totalBooks)
                delay(40)
            }
        }
    }
}
