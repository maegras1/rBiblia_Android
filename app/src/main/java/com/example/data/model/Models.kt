package com.example.data.model

data class Translation(
    val id: String,
    val language: String,
    val name: String,
    val description: String,
    val date: String,
    val isFavorite: Boolean = false
)

data class BookInfo(
    val id: String,
    val name: String,
    val sigla: String,
    val group: BookGroup, // OT, NT, DC
    val chapterCount: Int = 1
)

enum class BookGroup(val displayNameResKey: String) {
    OT("Old Testament"),
    NT("New Testament"),
    DC("Deuterocanonical / Other")
}

data class Verse(
    val number: Int,
    val text: String,
    val hasNote: Boolean = false
)

data class SearchResult(
    val book: String,
    val chapter: Int,
    val verse: Int,
    val content: String,
    val bookName: String = ""
)

enum class SearchScope(val label: String) {
    ALL("All"),
    OT("Old T."),
    NT("New T.");

    fun getLabel(lang: String): String = when (lang.lowercase()) {
        "pl" -> when (this) {
            ALL -> "Cała Biblia"
            OT -> "Stary Testament"
            NT -> "Nowy Testament"
        }
        "de" -> when (this) {
            ALL -> "Ganze Bibel"
            OT -> "Altes Testament"
            NT -> "Neues Testament"
        }
        else -> label
    }
}

data class VerseNote(
    val id: Long = 0,
    val translationId: String? = null, // null = global note
    val bookId: String,
    val chapterId: Int,
    val verseId: Int,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isGlobal: Boolean = true
)

data class ErrorReport(
    val name: String,
    val email: String,
    val notes: String,
    val content: String,
    val originalContent: String,
    val translation: String,
    val book: String,
    val chapter: Int,
    val verse: Int,
    val errorType: String = "typo"
)

enum class TextSize(val label: String, val scale: Float) {
    SMALL("Small", 0.85f),
    MEDIUM("Medium", 1.0f),
    LARGE("Large", 1.2f),
    XLARGE("X-Large", 1.45f);

    fun getLabel(lang: String): String = when (lang.lowercase()) {
        "pl" -> when (this) {
            SMALL -> "Mały"
            MEDIUM -> "Średni"
            LARGE -> "Duży"
            XLARGE -> "Bardzo duży"
        }
        "de" -> when (this) {
            SMALL -> "Klein"
            MEDIUM -> "Mittel"
            LARGE -> "Groß"
            XLARGE -> "Sehr groß"
        }
        else -> label
    }
}

enum class TextFontFamily(val label: String) {
    SERIF("Serif"),
    SANS("Sans-Serif"),
    MONO("Monospace");

    fun getLabel(lang: String): String = when (lang.lowercase()) {
        "pl" -> when (this) {
            SERIF -> "Szeryfowa"
            SANS -> "Bezszeryfowa"
            MONO -> "Stała szerokość (Mono)"
        }
        "de" -> when (this) {
            SERIF -> "Serifenschrift"
            SANS -> "Serifenlos"
            MONO -> "Monospace"
        }
        else -> label
    }
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark");

    fun getLabel(lang: String): String = when (lang.lowercase()) {
        "pl" -> when (this) {
            SYSTEM -> "Systemowy"
            LIGHT -> "Jasny"
            DARK -> "Ciemny"
        }
        "de" -> when (this) {
            SYSTEM -> "System"
            LIGHT -> "Hell"
            DARK -> "Dunkel"
        }
        else -> label
    }
}

enum class DarkVariant(val label: String) {
    GOLD("Golden Dark"),
    BLUE("Night Blue");

    fun getLabel(lang: String): String = when (lang.lowercase()) {
        "pl" -> when (this) {
            GOLD -> "Ciepły bursztyn"
            BLUE -> "Nocny granat"
        }
        "de" -> when (this) {
            GOLD -> "Goldener Bernstein"
            BLUE -> "Nachtblau"
        }
        else -> label
    }
}
