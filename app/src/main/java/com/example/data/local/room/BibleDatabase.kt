package com.example.data.local.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CachedVerseEntity::class,
        CachedVerseFtsEntity::class,
        SearchHistoryEntity::class,
        VerseHighlightEntity::class,
        ReadingHistoryEntity::class,
        BookReadingProgressEntity::class,
        ChapterReadingProgressEntity::class,
        NoteEntity::class,
        FavoriteTranslationEntity::class,
        CachedTranslationEntity::class,
        PartialHighlightEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class BibleDatabase : RoomDatabase() {

    abstract fun cachedVerseDao(): CachedVerseDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun verseHighlightDao(): VerseHighlightDao
    abstract fun readingHistoryDao(): ReadingHistoryDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun noteDao(): NoteDao
    abstract fun translationDao(): TranslationDao

    companion object {
        @Volatile
        private var INSTANCE: BibleDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `search_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `query` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_search_history_query` ON `search_history` (`query`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `verse_highlights` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bookId` TEXT NOT NULL,
                        `chapter` INTEGER NOT NULL,
                        `verseNumber` INTEGER NOT NULL,
                        `colorHex` TEXT NOT NULL,
                        `colorName` TEXT NOT NULL,
                        `translationId` TEXT,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_verse_highlights_bookId_chapter_verseNumber` ON `verse_highlights` (`bookId`, `chapter`, `verseNumber`)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reading_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bookId` TEXT NOT NULL,
                        `bookName` TEXT NOT NULL,
                        `chapter` INTEGER NOT NULL,
                        `verseNumber` INTEGER NOT NULL,
                        `translationId` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_history_bookId_chapter` ON `reading_history` (`bookId`, `chapter`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_history_timestamp` ON `reading_history` (`timestamp`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `book_reading_progress` (
                        `bookId` TEXT NOT NULL PRIMARY KEY,
                        `totalChapters` INTEGER NOT NULL,
                        `lastReadChapter` INTEGER NOT NULL,
                        `lastReadVerse` INTEGER NOT NULL,
                        `completedChaptersCsv` TEXT NOT NULL,
                        `completedChaptersCount` INTEGER NOT NULL,
                        `percentCompleted` REAL NOT NULL,
                        `lastTranslationId` TEXT NOT NULL,
                        `lastReadTimestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `chapter_reading_progress` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bookId` TEXT NOT NULL,
                        `chapter` INTEGER NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `lastVerseRead` INTEGER NOT NULL,
                        `readCount` INTEGER NOT NULL,
                        `lastTranslationId` TEXT NOT NULL,
                        `lastReadTimestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_chapter_reading_progress_bookId_chapter` ON `chapter_reading_progress` (`bookId`, `chapter`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chapter_reading_progress_lastReadTimestamp` ON `chapter_reading_progress` (`lastReadTimestamp`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Notes table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `notes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `translationId` TEXT,
                        `bookId` TEXT NOT NULL,
                        `chapterId` INTEGER NOT NULL,
                        `verseId` INTEGER NOT NULL,
                        `content` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `isGlobal` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_bookId_chapterId` ON `notes` (`bookId`, `chapterId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_timestamp` ON `notes` (`timestamp`)")

                // 2. Favorite translations table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `favorite_translations` (
                        `translationId` TEXT NOT NULL PRIMARY KEY,
                        `addedTimestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                // 3. Cached translations table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cached_translations` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `language` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `updatedTimestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_translations_language` ON `cached_translations` (`language`)")

                // 4. Full-Text Search (FTS4) virtual table for cached verses
                db.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS `cached_verses_fts` USING FTS4(`translationId` TEXT NOT NULL, `bookId` TEXT NOT NULL, `chapterId` INTEGER NOT NULL, `verseNumber` INTEGER NOT NULL, `text` TEXT NOT NULL)"
                )

                // Populate FTS4 index from existing cached_verses
                db.execSQL(
                    """
                    INSERT INTO `cached_verses_fts`(`translationId`, `bookId`, `chapterId`, `verseNumber`, `text`)
                    SELECT `translationId`, `bookId`, `chapterId`, `verseNumber`, `text` FROM `cached_verses`
                    """.trimIndent()
                )

                installFtsTriggers(db)
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `verse_partial_highlights` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bookId` TEXT NOT NULL,
                        `chapter` INTEGER NOT NULL,
                        `verseNumber` INTEGER NOT NULL,
                        `startIndex` INTEGER NOT NULL,
                        `endIndex` INTEGER NOT NULL,
                        `colorHex` TEXT NOT NULL,
                        `colorName` TEXT NOT NULL,
                        `translationId` TEXT,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_verse_partial_highlights_bookId_chapter_verseNumber` ON `verse_partial_highlights` (`bookId`, `chapter`, `verseNumber`)"
                )
            }
        }

        private fun installFtsTriggers(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS `trg_cached_verses_ai`
                AFTER INSERT ON `cached_verses`
                BEGIN
                    INSERT INTO `cached_verses_fts`(`translationId`, `bookId`, `chapterId`, `verseNumber`, `text`)
                    VALUES (new.`translationId`, new.`bookId`, new.`chapterId`, new.`verseNumber`, new.`text`);
                END
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS `trg_cached_verses_ad`
                AFTER DELETE ON `cached_verses`
                BEGIN
                    DELETE FROM `cached_verses_fts`
                    WHERE `translationId` = old.`translationId`
                      AND `bookId` = old.`bookId`
                      AND `chapterId` = old.`chapterId`
                      AND `verseNumber` = old.`verseNumber`;
                END
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TRIGGER IF NOT EXISTS `trg_cached_verses_au`
                AFTER UPDATE ON `cached_verses`
                BEGIN
                    DELETE FROM `cached_verses_fts`
                    WHERE `translationId` = old.`translationId`
                      AND `bookId` = old.`bookId`
                      AND `chapterId` = old.`chapterId`
                      AND `verseNumber` = old.`verseNumber`;
                    INSERT INTO `cached_verses_fts`(`translationId`, `bookId`, `chapterId`, `verseNumber`, `text`)
                    VALUES (new.`translationId`, new.`bookId`, new.`chapterId`, new.`verseNumber`, new.`text`);
                END
                """.trimIndent()
            )
        }

        fun getInstance(context: Context): BibleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BibleDatabase::class.java,
                    "rbiblia_room_cache.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            installFtsTriggers(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { database ->
                                    seedDefaultFavorites(database.translationDao())
                                    seedDefaultVerses(database.cachedVerseDao())
                                }
                            }
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            installFtsTriggers(db)
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private fun seedDefaultFavorites(dao: TranslationDao) {
            try {
                if (dao.getFavoritesCount() == 0) {
                    val now = System.currentTimeMillis()
                    dao.insertFavorites(
                        listOf(
                            FavoriteTranslationEntity("pl_ubg", now),
                            FavoriteTranslationEntity("pl_bt5", now),
                            FavoriteTranslationEntity("pl_bw", now),
                            FavoriteTranslationEntity("en_kjv", now)
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun seedDefaultVerses(dao: CachedVerseDao) {
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
            val entities = mutableListOf<CachedVerseEntity>()
            for (t in listOf("pl_ubg", "pl_bt5", "pl_bw")) {
                for (v in ubgGen1) {
                    entities.add(CachedVerseEntity(t, "gen", 1, v.first, v.second))
                }
            }
            val joh1 = listOf(
                1 to "Na początku było Słowo, a Słowo było u Boga i Bogiem było Słowo.",
                2 to "Ono było na początku u Boga.",
                3 to "Wszystko przez nie powstało, a bez niego nic nie powstało, co powstało.",
                4 to "W nim było życie, a życie było światłością ludzi.",
                5 to "A światłość świeci w ciemności, lecz ciemność jej nie ogarnęła."
            )
            for (v in joh1) {
                entities.add(CachedVerseEntity("pl_ubg", "joh", 1, v.first, v.second))
            }
            try {
                dao.insertVerses(entities)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
