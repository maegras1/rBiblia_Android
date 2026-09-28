package com.example.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.data.model.Translation
import com.example.data.model.VerseNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "rbiblia.db"
        private const val DATABASE_VERSION = 4

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

        // Table Reading History
        const val TABLE_READING_HISTORY = "reading_history"
        const val COL_HIST_ID = "id"
        const val COL_HIST_TRANSLATION = "translation_id"
        const val COL_HIST_BOOK = "book_id"
        const val COL_HIST_CHAPTER = "chapter_id"
        const val COL_HIST_TIMESTAMP = "timestamp"

        // Table Cached Verses (Offline & High-Speed Cache)
        const val TABLE_CACHED_VERSES = "cached_verses"
        const val COL_CV_TRANS = "translation_id"
        const val COL_CV_BOOK = "book_id"
        const val COL_CV_CHAPTER = "chapter_id"
        const val COL_CV_VERSE = "verse_id"
        const val COL_CV_TEXT = "text"

        // Table Cached Translations
        const val TABLE_CACHED_TRANSLATIONS = "cached_translations"
        const val COL_CT_ID = "id"
        const val COL_CT_LANG = "language"
        const val COL_CT_NAME = "name"
        const val COL_CT_DESC = "description"
        const val COL_CT_DATE = "date"
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

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_CACHED_VERSES (
                $COL_CV_TRANS TEXT NOT NULL,
                $COL_CV_BOOK TEXT NOT NULL,
                $COL_CV_CHAPTER INTEGER NOT NULL,
                $COL_CV_VERSE INTEGER NOT NULL,
                $COL_CV_TEXT TEXT NOT NULL,
                PRIMARY KEY ($COL_CV_TRANS, $COL_CV_BOOK, $COL_CV_CHAPTER, $COL_CV_VERSE)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_CACHED_TRANSLATIONS (
                $COL_CT_ID TEXT PRIMARY KEY,
                $COL_CT_LANG TEXT NOT NULL,
                $COL_CT_NAME TEXT NOT NULL,
                $COL_CT_DESC TEXT,
                $COL_CT_DATE TEXT
            )
        """.trimIndent())

        // Default favorites
        db.execSQL("INSERT OR IGNORE INTO $TABLE_FAVORITES VALUES ('pl_ubg')")
        db.execSQL("INSERT OR IGNORE INTO $TABLE_FAVORITES VALUES ('pl_bt5')")
        db.execSQL("INSERT OR IGNORE INTO $TABLE_FAVORITES VALUES ('pl_bw')")
        db.execSQL("INSERT OR IGNORE INTO $TABLE_FAVORITES VALUES ('en_kjv')")

        seedInitialPolishVerses(db)
    }

    private fun seedInitialPolishVerses(db: SQLiteDatabase) {
        val ubgGen1 = listOf(
            1 to "Na początku Bóg stworzył niebo i ziemię.",
            2 to "A ziemia była bezkształtna i pusta i ciemność była nad głębią, a Duch Boży unosił się nad wodami.",
            3 to "I Bóg powiedział: Niech stanie się światłość. I stała się światłość.",
            4 to "I Bóg widział, że światłość była dobra. I oddzielił Bóg światłość od ciemności.",
            5 to "I nazwał Bóg światłość dniem, a ciemność nazwał nocą. I nastał wieczór, i nastał poranek, dzień pierwszy.",
            6 to "Potem Bóg powiedział: Niech stanie się przestworze pośród wód i niech oddziela wody od wód.",
            7 to "I Bóg uczynił przestworze, i oddzielił wody, które były pod przestworzem, od wód, które były nad przestworzem. I tak się stało.",
            8 to "I nazwał Bóg przestworze niebem. I nastał wieczór, i nastał poranek, dzień drugi.",
            9 to "Potem Bóg powiedział: Niech się zbiorą wody spod nieba w jedno miejsce i niech się ukaże sucha powierzchnia. I tak się stało.",
            10 to "I nazwał Bóg suchą powierzchnię ziemią, a zbiorowisko wód nazwał morzami. I Bóg widział, że to było dobre.",
            11 to "Potem Bóg powiedział: Niech ziemia wyda trawę, rośliny wydające nasienie i drzewa owocowe przynoszące owoc według swego rodzaju, którego nasienie będzie w nim na ziemi. I tak się stało.",
            12 to "I ziemia wydała trawę, rośliny wydające nasienie według swego rodzaju i drzewa przynoszące owoc, w którym było nasienie według swego rodzaju. I Bóg widział, że to było dobre.",
            13 to "I nastał wieczór, i nastał poranek, dzień trzeci.",
            14 to "Potem Bóg powiedział: Niech powstaną światła na przestworzu nieba, aby oddzielały dzień od nocy i były znakami określającymi pory i dni, i lata;",
            15 to "I niech będą światłami na przestworzu nieba, aby świeciły nad ziemią. I tak się stało.",
            16 to "I uczynił Bóg dwa wielkie światła: światło większe, aby rządziło dniem, i światło mniejsze, aby rządziło nocą, oraz gwiazdy.",
            17 to "I umieścił je Bóg na przestworzu nieba, aby świeciły nad ziemią;",
            18 to "I aby rządziły dniem i nocą, i oddzielały światłość od ciemności. I Bóg widział, że to było dobre.",
            19 to "I nastał wieczór, i nastał poranek, dzień czwarty.",
            20 to "Potem Bóg powiedział: Niech wody obficie wydadzą żywe istoty i niech ptactwo lata nad ziemią pod przestworzem nieba.",
            21 to "I stworzył Bóg wielkie wieloryby i wszelkie żywe istoty poruszające się, które obficie wydały wody według ich rodzaju, i wszelkie ptactwo skrzydlate według jego rodzaju. I Bóg widział, że to było dobre.",
            22 to "I Bóg błogosławił im, mówiąc: Rozradzajcie się i rozmnażajcie się, i napełniajcie wody w morzach, a ptactwo niech się rozmnaża na ziemi.",
            23 to "I nastał wieczór, i nastał poranek, dzień piąty.",
            24 to "Potem Bóg powiedział: Niech ziemia wyda żywe istoty według swego rodzaju: bydło, zwierzęta pełzające i dzikie zwierzęta ziemi według swego rodzaju. I tak się stało.",
            25 to "I uczynił Bóg dzikie zwierzęta ziemi według ich rodzaju, bydło według swego rodzaju i wszelkie zwierzęta pełzające po ziemi według swego rodzaju. I Bóg widział, że to było dobre.",
            26 to "Potem Bóg powiedział: Uczyńmy człowieka na nasz obraz, według naszego podobieństwa; niech panuje nad rybami morskimi i nad ptactwem niebieskim, i nad bydłem, i nad całą ziemią, i nad wszelkimi zwierzętami pełzającymi po ziemi.",
            27 to "Stworzył więc Bóg człowieka na swój obraz, na obraz Boga go stworzył: stworzył ich mężczyzną i kobietą.",
            28 to "I Bóg błogosławił im, i powiedział do nich Bóg: Rozradzajcie się i rozmnażajcie się, napełniajcie ziemię i czyńcie ją sobie poddaną; panujcie nad rybami morskimi i nad ptactwem niebieskim, i nad wszelkimi żywymi istotami, które poruszają się po ziemi.",
            29 to "I Bóg powiedział: Oto dałem wam wszelkie rośliny wydające nasienie, które są na powierzchni całej ziemi, i wszelkie drzewo, na którym jest owoc drzewa wydający nasienie – będą wam służyły za pokarm.",
            30 to "A wszelkim zwierzętom ziemi i wszelkiemu ptactwu niebieskiemu, i wszelkim poruszającym się po ziemi, w których jest życie, dałem na pokarm wszelką zieloną trawę. I tak się stało.",
            31 to "I Bóg widział wszystko, co uczynił, a było to bardzo dobre. I nastał wieczór, i nastał poranek, dzień szósty."
        )

        for (v in ubgGen1) {
            val cv = ContentValues().apply {
                put(COL_CV_TRANS, "pl_ubg")
                put(COL_CV_BOOK, "gen")
                put(COL_CV_CHAPTER, 1)
                put(COL_CV_VERSE, v.first)
                put(COL_CV_TEXT, v.second)
            }
            db.insertWithOnConflict(TABLE_CACHED_VERSES, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        }

        // BT5 Genesis 1
        for (v in ubgGen1) {
            val cv = ContentValues().apply {
                put(COL_CV_TRANS, "pl_bt5")
                put(COL_CV_BOOK, "gen")
                put(COL_CV_CHAPTER, 1)
                put(COL_CV_VERSE, v.first)
                put(COL_CV_TEXT, v.second)
            }
            db.insertWithOnConflict(TABLE_CACHED_VERSES, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        }

        // John 1
        val joh1 = listOf(
            1 to "Na początku było Słowo, a Słowo było u Boga i Bogiem było Słowo.",
            2 to "Ono było na początku u Boga.",
            3 to "Wszystko przez nie powstało, a bez niego nic nie powstało, co powstało.",
            4 to "W nim było życie, a życie było światłością ludzi.",
            5 to "A światłość świeci w ciemności, lecz ciemność jej nie ogarnęła."
        )
        for (v in joh1) {
            val cv = ContentValues().apply {
                put(COL_CV_TRANS, "pl_ubg")
                put(COL_CV_BOOK, "joh")
                put(COL_CV_CHAPTER, 1)
                put(COL_CV_VERSE, v.first)
                put(COL_CV_TEXT, v.second)
            }
            db.insertWithOnConflict(TABLE_CACHED_VERSES, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_READING_HISTORY ($COL_HIST_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_HIST_TRANSLATION TEXT, $COL_HIST_BOOK TEXT, $COL_HIST_CHAPTER INTEGER, $COL_HIST_TIMESTAMP INTEGER)")
        }
        if (oldVersion < 3) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_CACHED_VERSES (
                    $COL_CV_TRANS TEXT NOT NULL,
                    $COL_CV_BOOK TEXT NOT NULL,
                    $COL_CV_CHAPTER INTEGER NOT NULL,
                    $COL_CV_VERSE INTEGER NOT NULL,
                    $COL_CV_TEXT TEXT NOT NULL,
                    PRIMARY KEY ($COL_CV_TRANS, $COL_CV_BOOK, $COL_CV_CHAPTER, $COL_CV_VERSE)
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_CACHED_TRANSLATIONS (
                    $COL_CT_ID TEXT PRIMARY KEY,
                    $COL_CT_LANG TEXT NOT NULL,
                    $COL_CT_NAME TEXT NOT NULL,
                    $COL_CT_DESC TEXT,
                    $COL_CT_DATE TEXT
                )
            """.trimIndent())
        }
        if (oldVersion < 4) {
            seedInitialPolishVerses(db)
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

    // --- Offline Cache Methods for Verses & Translations ---
    suspend fun getCachedVerses(
        translationId: String,
        bookId: String,
        chapterId: Int
    ): Map<Int, String> = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val map = mutableMapOf<Int, String>()
        val cursor = db.rawQuery(
            """
            SELECT $COL_CV_VERSE, $COL_CV_TEXT FROM $TABLE_CACHED_VERSES
            WHERE $COL_CV_TRANS = ? AND $COL_CV_BOOK = ? AND $COL_CV_CHAPTER = ?
            ORDER BY $COL_CV_VERSE ASC
            """.trimIndent(),
            arrayOf(translationId, bookId, chapterId.toString())
        )
        cursor.use {
            while (it.moveToNext()) {
                val verseNum = it.getInt(0)
                val text = it.getString(1)
                map[verseNum] = text
            }
        }
        map
    }

    suspend fun saveCachedVerses(
        translationId: String,
        bookId: String,
        chapterId: Int,
        verses: Map<Int, String>
    ) = withContext(Dispatchers.IO) {
        if (verses.isEmpty()) return@withContext
        val db = writableDatabase
        db.beginTransaction()
        try {
            for ((num, text) in verses) {
                val cv = ContentValues().apply {
                    put(COL_CV_TRANS, translationId)
                    put(COL_CV_BOOK, bookId)
                    put(COL_CV_CHAPTER, chapterId)
                    put(COL_CV_VERSE, num)
                    put(COL_CV_TEXT, text)
                }
                db.insertWithOnConflict(TABLE_CACHED_VERSES, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    suspend fun getCachedTranslations(language: String? = null): List<Translation> =
        withContext(Dispatchers.IO) {
            val db = readableDatabase
            val list = mutableListOf<Translation>()
            val sql = if (language.isNullOrEmpty()) {
                "SELECT $COL_CT_ID, $COL_CT_LANG, $COL_CT_NAME, $COL_CT_DESC, $COL_CT_DATE FROM $TABLE_CACHED_TRANSLATIONS ORDER BY $COL_CT_LANG ASC, $COL_CT_NAME ASC"
            } else {
                "SELECT $COL_CT_ID, $COL_CT_LANG, $COL_CT_NAME, $COL_CT_DESC, $COL_CT_DATE FROM $TABLE_CACHED_TRANSLATIONS WHERE $COL_CT_LANG = ? ORDER BY $COL_CT_NAME ASC"
            }
            val args = if (language.isNullOrEmpty()) null else arrayOf(language)
            val cursor = db.rawQuery(sql, args)
            cursor.use {
                while (it.moveToNext()) {
                    list.add(
                        Translation(
                            id = it.getString(0),
                            language = it.getString(1),
                            name = it.getString(2),
                            description = it.getString(3) ?: "",
                            date = it.getString(4) ?: ""
                        )
                    )
                }
            }
            list
        }

    suspend fun saveCachedTranslations(translations: List<Translation>) =
        withContext(Dispatchers.IO) {
            if (translations.isEmpty()) return@withContext
            val db = writableDatabase
            db.beginTransaction()
            try {
                for (t in translations) {
                    val cv = ContentValues().apply {
                        put(COL_CT_ID, t.id)
                        put(COL_CT_LANG, t.language)
                        put(COL_CT_NAME, t.name)
                        put(COL_CT_DESC, t.description)
                        put(COL_CT_DATE, t.date)
                    }
                    db.insertWithOnConflict(TABLE_CACHED_TRANSLATIONS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
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
