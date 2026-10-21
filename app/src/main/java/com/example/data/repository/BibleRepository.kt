package com.example.data.repository

import com.example.data.local.DatabaseHelper
import com.example.data.local.PreferencesManager
import com.example.data.local.room.BibleDatabase
import com.example.data.local.room.BookReadingProgressEntity
import com.example.data.local.room.CachedTranslationEntity
import com.example.data.local.room.CachedVerseEntity
import com.example.data.local.room.ChapterReadingProgressEntity
import com.example.data.local.room.FavoriteTranslationEntity
import com.example.data.local.room.NoteEntity
import com.example.data.local.room.ReadingHistoryEntity
import com.example.data.local.room.SearchHistoryEntity
import com.example.data.local.room.VerseHighlightEntity
import com.example.data.model.BookCatalog
import com.example.data.model.BookInfo
import com.example.data.model.ErrorReport
import com.example.data.model.SearchResult
import com.example.data.model.Translation
import com.example.data.model.Verse
import com.example.data.model.VerseNote
import com.example.data.remote.RBibliaApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class BibleRepository(
    private val dbHelper: DatabaseHelper? = null,
    val preferences: PreferencesManager,
    private val apiService: RBibliaApiService,
    val roomDb: BibleDatabase
) {
    private val migrationMutex = Mutex()

    /**
     * One-time idempotent migration of user data (notes, favorites, cached translations,
     * recent searches) from legacy SQLiteOpenHelper (DatabaseHelper) into unified Room database.
     */
    suspend fun ensureLegacyMigrated() = withContext(Dispatchers.IO) {
        if (preferences.legacyDbMigrated) return@withContext
        migrationMutex.withLock {
            if (preferences.legacyDbMigrated) return@withLock
            try {
                // Ensure default favorites exist in Room
                val translationDao = roomDb.translationDao()
                if (translationDao.getFavoritesCount() == 0) {
                    val now = System.currentTimeMillis()
                    translationDao.insertFavorites(
                        listOf(
                            FavoriteTranslationEntity("pl_ubg", now),
                            FavoriteTranslationEntity("pl_bt5", now),
                            FavoriteTranslationEntity("pl_bw", now),
                            FavoriteTranslationEntity("en_kjv", now)
                        )
                    )
                }

                if (dbHelper != null) {
                    // 1. Migrate legacy favorites
                    val legacyFavs = try { dbHelper.getFavorites() } catch (_: Exception) { emptySet() }
                    if (legacyFavs.isNotEmpty()) {
                        val now = System.currentTimeMillis()
                        translationDao.insertFavorites(
                            legacyFavs.map { FavoriteTranslationEntity(it, now) }
                        )
                    }

                    // 2. Migrate legacy cached translations
                    val legacyTrans = try { dbHelper.getCachedTranslations(null) } catch (_: Exception) { emptyList() }
                    if (legacyTrans.isNotEmpty()) {
                        translationDao.insertCachedTranslations(
                            legacyTrans.map { CachedTranslationEntity.fromDomain(it) }
                        )
                    }

                    // 3. Migrate legacy notes if Room notes table is empty
                    val noteDao = roomDb.noteDao()
                    if (noteDao.getCount() == 0) {
                        val legacyNotes = try { dbHelper.getAllNotes() } catch (_: Exception) { emptyList() }
                        if (legacyNotes.isNotEmpty()) {
                            noteDao.insertNotes(
                                legacyNotes.map { NoteEntity.fromDomain(it).copy(id = 0) }
                            )
                        }
                    }

                    // 4. Migrate legacy recent searches if Room search history is empty
                    val searchDao = roomDb.searchHistoryDao()
                    if (searchDao.getCount() == 0) {
                        val legacySearches = try { dbHelper.getRecentSearches() } catch (_: Exception) { emptyList() }
                        for (q in legacySearches) {
                            if (q.isNotBlank()) {
                                searchDao.insertSearch(
                                    SearchHistoryEntity(query = q.trim(), timestamp = System.currentTimeMillis())
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                preferences.legacyDbMigrated = true
            }
        }
    }

    suspend fun getTranslations(language: String): List<Translation> = withContext(Dispatchers.IO) {
        ensureLegacyMigrated()
        val translationDao = roomDb.translationDao()
        var favIds = translationDao.getFavoriteIds().toSet()
        if (favIds.isEmpty()) {
            val defaults = listOf("pl_ubg", "pl_bt5", "pl_bw", "en_kjv")
            val now = System.currentTimeMillis()
            translationDao.insertFavorites(defaults.map { FavoriteTranslationEntity(it, now) })
            favIds = defaults.toSet()
        }

        var list = apiService.getTranslations(language)
        if (list.isNotEmpty()) {
            translationDao.insertCachedTranslations(
                list.map { CachedTranslationEntity.fromDomain(it) }
            )
        } else {
            val cached = translationDao.getCachedTranslationsByLanguage(language)
            if (cached.isNotEmpty()) {
                list = cached.map { it.toDomain() }
            }
        }
        list.map { it.copy(isFavorite = favIds.contains(it.id)) }
    }

    suspend fun toggleFavorite(translationId: String, isFav: Boolean) = withContext(Dispatchers.IO) {
        ensureLegacyMigrated()
        val translationDao = roomDb.translationDao()
        if (isFav) {
            translationDao.insertFavorite(FavoriteTranslationEntity(translationId))
        } else {
            translationDao.deleteFavorite(translationId)
        }
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
        ensureLegacyMigrated()
        // 1. Check unified Room database cache first for instant rendering & offline mode
        val roomCached = try {
            roomDb.cachedVerseDao().getChapterVerses(translationId, bookId, chapterId)
        } catch (e: Exception) {
            emptyList()
        }

        val finalMap: Map<Int, String> = if (roomCached.isNotEmpty()) {
            roomCached.associate { it.verseNumber to it.text }
        } else {
            // 2. Fetch from API and persist into Room (triggers automatically update FTS4 index)
            val remote = apiService.getVerses(language, translationId, bookId, chapterId)
            if (remote.isNotEmpty()) {
                val entities = remote.map { (num, text) ->
                    CachedVerseEntity(translationId, bookId, chapterId, num, text)
                }
                try {
                    roomDb.cachedVerseDao().insertVerses(entities)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                remote
            } else {
                // One-time legacy fallback if a chapter was only in old SQLite helper
                val legacyCached = try {
                    dbHelper?.getCachedVerses(translationId, bookId, chapterId) ?: emptyMap()
                } catch (_: Exception) {
                    emptyMap()
                }
                if (legacyCached.isNotEmpty()) {
                    val entities = legacyCached.map { (num, text) ->
                        CachedVerseEntity(translationId, bookId, chapterId, num, text)
                    }
                    try {
                        roomDb.cachedVerseDao().insertVerses(entities)
                    } catch (_: Exception) {}
                }
                legacyCached
            }
        }

        val notes = getNotesForChapter(bookId, chapterId, translationId)
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
    ): Map<String, String> = coroutineScope {
        val deferreds = translationIds.map { tid ->
            async(Dispatchers.IO) {
                val verses = getVerses(language, tid, bookId, chapterId)
                val verse = verses.find { it.number == verseId }
                if (verse != null && verse.text.isNotBlank()) {
                    tid to verse.text
                } else {
                    null
                }
            }
        }
        deferreds.mapNotNull { it.await() }.toMap()
    }

    /**
     * Converts a raw user search query into a safe SQLite FTS4 MATCH expression with prefix tokens.
     */
    private fun buildFtsQuery(rawQuery: String): String {
        val tokens = rawQuery
            .trim()
            .split(Regex("\\s+"))
            .map { token ->
                // Keep letters and digits (including Polish/German diacritics)
                token.replace(Regex("[^\\p{L}\\p{N}]"), "")
            }
            .filter { it.length >= 2 }

        if (tokens.isEmpty()) return ""
        return tokens.joinToString(" ") { "$it*" }
    }

    /**
     * Hybrid Full-Text Search (FTS4 local offline search + remote API search).
     * Searches local Room FTS4 index in milliseconds (with LIKE fallback for mid-word substrings)
     * and merges with remote API results when online.
     */
    suspend fun search(language: String, translationId: String, query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()
        recordSearchQuery(trimmed)

        coroutineScope {
            val localDeferred = async(Dispatchers.IO) {
                searchLocalFts(language, translationId, trimmed)
            }
            val remoteDeferred = async(Dispatchers.IO) {
                try {
                    apiService.search(language, translationId, trimmed)
                } catch (_: Exception) {
                    emptyList()
                }
            }

            val localResults = localDeferred.await()
            val remoteResults = remoteDeferred.await()

            val bookOrderMap = BookCatalog.getAllBooks(language)
                .mapIndexed { index, bookInfo -> bookInfo.id to index }
                .toMap()

            val merged = LinkedHashMap<String, SearchResult>()
            for (r in remoteResults) {
                val key = "${r.book}_${r.chapter}_${r.verse}"
                merged[key] = r.copy(bookName = BookCatalog.getBookName(r.book, language))
            }
            for (l in localResults) {
                val key = "${l.book}_${l.chapter}_${l.verse}"
                if (!merged.containsKey(key)) {
                    merged[key] = l.copy(bookName = BookCatalog.getBookName(l.book, language))
                }
            }

            merged.values.sortedWith(
                compareBy<SearchResult>(
                    { bookOrderMap[it.book] ?: Int.MAX_VALUE },
                    { it.chapter },
                    { it.verse }
                )
            )
        }
    }

    suspend fun searchLocalFts(
        language: String,
        translationId: String,
        query: String
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val dao = roomDb.cachedVerseDao()
        val ftsQuery = buildFtsQuery(query)
        val ftsMatches = if (ftsQuery.isNotEmpty()) {
            try {
                dao.searchVersesFts(translationId, ftsQuery, limit = 250)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        val finalEntities = if (ftsMatches.isNotEmpty()) {
            ftsMatches
        } else {
            try {
                dao.searchVersesLike(translationId, query.trim(), limit = 250)
            } catch (_: Exception) {
                emptyList()
            }
        }

        finalEntities.map { entity ->
            SearchResult(
                book = entity.bookId,
                chapter = entity.chapterId,
                verse = entity.verseNumber,
                content = entity.text,
                bookName = BookCatalog.getBookName(entity.bookId, language)
            )
        }
    }

    fun getRecentSearchesFlow(): Flow<List<String>> {
        return roomDb.searchHistoryDao().getRecentSearchesFlow().map { list ->
            list.map { it.query }
        }
    }

    suspend fun getRecentSearches(): List<String> = withContext(Dispatchers.IO) {
        ensureLegacyMigrated()
        roomDb.searchHistoryDao().getRecentQueries()
    }

    suspend fun recordSearchQuery(query: String) = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            roomDb.searchHistoryDao().deleteByQuery(trimmed)
            roomDb.searchHistoryDao().insertSearch(
                SearchHistoryEntity(query = trimmed, timestamp = System.currentTimeMillis())
            )
        }
    }

    suspend fun deleteRecentSearch(query: String) = withContext(Dispatchers.IO) {
        roomDb.searchHistoryDao().deleteByQuery(query.trim())
    }

    suspend fun clearSearchHistory() = withContext(Dispatchers.IO) {
        roomDb.searchHistoryDao().clearHistory()
    }

    // --- Notes (Unified Room Database) ---

    fun getAllNotesFlow(): Flow<List<VerseNote>> {
        return roomDb.noteDao().getAllNotesFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun saveNote(note: VerseNote): Long = withContext(Dispatchers.IO) {
        ensureLegacyMigrated()
        val entity = NoteEntity.fromDomain(note.copy(timestamp = System.currentTimeMillis()))
        roomDb.noteDao().insertNote(entity)
    }

    suspend fun deleteNote(noteId: Long) = withContext(Dispatchers.IO) {
        ensureLegacyMigrated()
        roomDb.noteDao().deleteNoteById(noteId)
    }

    suspend fun getNotesForChapter(bookId: String, chapterId: Int, translationId: String): List<VerseNote> =
        withContext(Dispatchers.IO) {
            ensureLegacyMigrated()
            roomDb.noteDao().getNotesForChapter(bookId, chapterId, translationId).map { it.toDomain() }
        }

    suspend fun getAllNotes(): List<VerseNote> = withContext(Dispatchers.IO) {
        ensureLegacyMigrated()
        roomDb.noteDao().getAllNotes().map { it.toDomain() }
    }

    suspend fun exportNotesXml(): String = withContext(Dispatchers.IO) {
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

    suspend fun submitReport(language: String, report: ErrorReport): Boolean {
        return apiService.submitReport(language, report)
    }

    fun getBooks(language: String): List<BookInfo> {
        return BookCatalog.getAllBooks(language)
    }

    fun getBook(id: String, language: String): BookInfo {
        return BookCatalog.getBook(id, language)
    }

    // --- Room Cache & FTS4 Operations ---
    fun getCachedVersesCountFlow(): Flow<Int> {
        return roomDb.cachedVerseDao().getTotalCachedVersesCountFlow()
    }

    suspend fun clearRoomCache() = withContext(Dispatchers.IO) {
        roomDb.cachedVerseDao().clearAll()
        try {
            roomDb.cachedVerseDao().clearAllFts()
        } catch (_: Exception) {}
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

    // --- Verse Highlights (Room Database) ---

    fun getVerseHighlightsFlow(bookId: String, chapter: Int): Flow<List<VerseHighlightEntity>> {
        return roomDb.verseHighlightDao().getHighlightsForChapterFlow(bookId, chapter)
    }

    suspend fun getVerseHighlights(bookId: String, chapter: Int): List<VerseHighlightEntity> = withContext(Dispatchers.IO) {
        roomDb.verseHighlightDao().getHighlightsForChapter(bookId, chapter)
    }

    suspend fun saveVerseHighlight(
        bookId: String,
        chapter: Int,
        verseNumber: Int,
        colorHex: String,
        colorName: String = "yellow",
        translationId: String? = null
    ) = withContext(Dispatchers.IO) {
        val entity = VerseHighlightEntity(
            bookId = bookId,
            chapter = chapter,
            verseNumber = verseNumber,
            colorHex = colorHex,
            colorName = colorName,
            translationId = translationId,
            timestamp = System.currentTimeMillis()
        )
        roomDb.verseHighlightDao().setHighlight(entity)
    }

    suspend fun removeVerseHighlight(
        bookId: String,
        chapter: Int,
        verseNumber: Int
    ) = withContext(Dispatchers.IO) {
        roomDb.verseHighlightDao().removeHighlight(bookId, chapter, verseNumber)
    }

    suspend fun clearAllHighlights() = withContext(Dispatchers.IO) {
        roomDb.verseHighlightDao().clearAll()
    }

    // --- Reading History (Room Database) ---

    fun getReadingHistoryFlow(limit: Int = 50): Flow<List<ReadingHistoryEntity>> {
        return roomDb.readingHistoryDao().getRecentHistory(limit)
    }

    fun getLatestReadingSessionFlow(): Flow<ReadingHistoryEntity?> {
        return roomDb.readingHistoryDao().getLatestEntry()
    }

    suspend fun recordReadingSession(
        bookId: String,
        bookName: String,
        chapter: Int,
        verseNumber: Int = 1,
        translationId: String
    ) = withContext(Dispatchers.IO) {
        val entry = ReadingHistoryEntity(
            bookId = bookId,
            bookName = bookName,
            chapter = chapter,
            verseNumber = verseNumber,
            translationId = translationId,
            timestamp = System.currentTimeMillis()
        )
        roomDb.readingHistoryDao().insert(entry)

        // Also update chapter progress and book progress
        updateProgressOnRead(bookId, chapter, verseNumber, translationId)
    }

    suspend fun clearReadingHistory() = withContext(Dispatchers.IO) {
        roomDb.readingHistoryDao().clearHistory()
    }

    suspend fun deleteReadingHistoryEntry(id: Long) = withContext(Dispatchers.IO) {
        roomDb.readingHistoryDao().deleteEntry(id)
    }

    // --- Reading Progress per Book & Chapter (Room Database) ---

    fun getAllBookProgressFlow(): Flow<List<BookReadingProgressEntity>> {
        return roomDb.readingProgressDao().getAllBookProgressFlow()
    }

    fun getBookProgressFlow(bookId: String): Flow<BookReadingProgressEntity?> {
        return roomDb.readingProgressDao().getBookProgressFlow(bookId)
    }

    fun getChapterProgressFlow(bookId: String, chapter: Int): Flow<ChapterReadingProgressEntity?> {
        return roomDb.readingProgressDao().getChapterProgressFlow(bookId, chapter)
    }

    fun getChaptersProgressForBookFlow(bookId: String): Flow<List<ChapterReadingProgressEntity>> {
        return roomDb.readingProgressDao().getChaptersProgressForBookFlow(bookId)
    }

    private suspend fun updateProgressOnRead(
        bookId: String,
        chapter: Int,
        verseNumber: Int,
        translationId: String
    ) {
        val now = System.currentTimeMillis()
        val existingChapter = roomDb.readingProgressDao().getChapterProgressDirect(bookId, chapter)
        val readCount = (existingChapter?.readCount ?: 0) + 1
        val chapterEntity = ChapterReadingProgressEntity(
            id = existingChapter?.id ?: 0,
            bookId = bookId,
            chapter = chapter,
            isCompleted = existingChapter?.isCompleted ?: false,
            lastVerseRead = verseNumber,
            readCount = readCount,
            lastTranslationId = translationId,
            lastReadTimestamp = now
        )
        roomDb.readingProgressDao().upsertChapterProgress(chapterEntity)

        // Update book progress
        val totalChapters = BookCatalog.getBook(bookId, "pl").chapterCount
        val chaptersProgress = roomDb.readingProgressDao().getChaptersProgressForBookDirect(bookId)
        val completedSet = chaptersProgress.filter { it.isCompleted }.map { it.chapter }.toSet()
        val completedCsv = completedSet.sorted().joinToString(",")
        val percent = if (totalChapters > 0) (completedSet.size.toFloat() / totalChapters.toFloat()) * 100f else 0f

        val bookEntity = BookReadingProgressEntity(
            bookId = bookId,
            totalChapters = totalChapters,
            lastReadChapter = chapter,
            lastReadVerse = verseNumber,
            completedChaptersCsv = completedCsv,
            completedChaptersCount = completedSet.size,
            percentCompleted = percent,
            lastTranslationId = translationId,
            lastReadTimestamp = now
        )
        roomDb.readingProgressDao().upsertBookProgress(bookEntity)
    }

    suspend fun setChapterCompleted(
        bookId: String,
        chapter: Int,
        isCompleted: Boolean,
        translationId: String = ""
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val existingChapter = roomDb.readingProgressDao().getChapterProgressDirect(bookId, chapter)
        val chapterEntity = ChapterReadingProgressEntity(
            id = existingChapter?.id ?: 0,
            bookId = bookId,
            chapter = chapter,
            isCompleted = isCompleted,
            lastVerseRead = existingChapter?.lastVerseRead ?: 1,
            readCount = maxOf(1, existingChapter?.readCount ?: 1),
            lastTranslationId = translationId.ifBlank { existingChapter?.lastTranslationId ?: "" },
            lastReadTimestamp = now
        )
        roomDb.readingProgressDao().upsertChapterProgress(chapterEntity)

        // Update book progress
        val totalChapters = BookCatalog.getBook(bookId, "pl").chapterCount
        val chaptersProgress = roomDb.readingProgressDao().getChaptersProgressForBookDirect(bookId)
        val completedSet = chaptersProgress.filter { it.isCompleted }.map { it.chapter }.toSet()
        val completedCsv = completedSet.sorted().joinToString(",")
        val percent = if (totalChapters > 0) (completedSet.size.toFloat() / totalChapters.toFloat()) * 100f else 0f

        val existingBook = roomDb.readingProgressDao().getBookProgressDirect(bookId)
        val bookEntity = BookReadingProgressEntity(
            bookId = bookId,
            totalChapters = totalChapters,
            lastReadChapter = existingBook?.lastReadChapter ?: chapter,
            lastReadVerse = existingBook?.lastReadVerse ?: 1,
            completedChaptersCsv = completedCsv,
            completedChaptersCount = completedSet.size,
            percentCompleted = percent,
            lastTranslationId = translationId.ifBlank { existingBook?.lastTranslationId ?: "" },
            lastReadTimestamp = now
        )
        roomDb.readingProgressDao().upsertBookProgress(bookEntity)
    }

    suspend fun clearReadingProgress() = withContext(Dispatchers.IO) {
        roomDb.readingProgressDao().clearBookProgress()
        roomDb.readingProgressDao().clearChapterProgress()
    }
}
