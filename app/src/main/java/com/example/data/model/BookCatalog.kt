package com.example.data.model

object BookCatalog {
    private val books = listOf(
        // Old Testament
        BookData("gen", "Genesis", "I Mojżeszowa (Rodzaju)", "1. Mose (Genesis)", "Gen", BookGroup.OT, 50),
        BookData("exo", "Exodus", "II Mojżeszowa (Wyjścia)", "2. Mose (Exodus)", "Exod", BookGroup.OT, 40),
        BookData("lev", "Leviticus", "III Mojżeszowa (Kapłańska)", "3. Mose (Levitikus)", "Lev", BookGroup.OT, 27),
        BookData("num", "Numbers", "IV Mojżeszowa (Liczb)", "4. Mose (Numeri)", "Num", BookGroup.OT, 36),
        BookData("deu", "Deuteronomy", "V Mojżeszowa (Powt. Prawa)", "5. Mose (Deuteronomium)", "Deut", BookGroup.OT, 34),
        BookData("jos", "Joshua", "Jozuego", "Josua", "Josh", BookGroup.OT, 24),
        BookData("jdg", "Judges", "Sędziów", "Richter", "Judg", BookGroup.OT, 21),
        BookData("rut", "Ruth", "Rut", "Rut", "Ruth", BookGroup.OT, 4),
        BookData("1sa", "1 Samuel", "1 Samuela", "1. Samuel", "1 Sam", BookGroup.OT, 31),
        BookData("2sa", "2 Samuel", "2 Samuela", "2. Samuel", "2 Sam", BookGroup.OT, 24),
        BookData("1ki", "1 Kings", "1 Królewska", "1. Könige", "1 Kgs", BookGroup.OT, 22),
        BookData("2ki", "2 Kings", "2 Królewska", "2. Könige", "2 Kgs", BookGroup.OT, 25),
        BookData("1ch", "1 Chronicles", "1 Kronik", "1. Chronik", "1 Chr", BookGroup.OT, 29),
        BookData("2ch", "2 Chronicles", "2 Kronik", "2. Chronik", "2 Chr", BookGroup.OT, 36),
        BookData("ezr", "Ezra", "Ezdrasza", "Esra", "Ezra", BookGroup.OT, 10),
        BookData("neh", "Nehemiah", "Nehemiasza", "Nehemia", "Neh", BookGroup.OT, 13),
        BookData("est", "Esther", "Estery", "Ester", "Esth", BookGroup.OT, 10),
        BookData("job", "Job", "Hioba", "Hiob", "Job", BookGroup.OT, 42),
        BookData("psa", "Psalms", "Psalmy", "Psalmen", "Ps", BookGroup.OT, 150),
        BookData("pro", "Proverbs", "Przysłów", "Sprüche", "Prov", BookGroup.OT, 31),
        BookData("ecc", "Ecclesiastes", "Kaznodziei", "Prediger (Kohelet)", "Eccl", BookGroup.OT, 12),
        BookData("sol", "Song of Solomon", "Pieśń nad Pieśniami", "Hoheslied", "Song", BookGroup.OT, 8),
        BookData("isa", "Isaiah", "Izajasza", "Jesaja", "Isa", BookGroup.OT, 66),
        BookData("jer", "Jeremiah", "Jeremiasza", "Jeremia", "Jer", BookGroup.OT, 52),
        BookData("lam", "Lamentations", "Lamentacje", "Klagelieder", "Lam", BookGroup.OT, 5),
        BookData("eze", "Ezekiel", "Ezechiela", "Hesekiel", "Ezek", BookGroup.OT, 48),
        BookData("dan", "Daniel", "Daniela", "Daniel", "Dan", BookGroup.OT, 12),
        BookData("hos", "Hosea", "Ozeasza", "Hosea", "Hos", BookGroup.OT, 14),
        BookData("joe", "Joel", "Joela", "Joel", "Joel", BookGroup.OT, 3),
        BookData("amo", "Amos", "Amosa", "Amos", "Amos", BookGroup.OT, 9),
        BookData("oba", "Obadiah", "Abdiasza", "Obadja", "Obad", BookGroup.OT, 1),
        BookData("jon", "Jonah", "Jonasza", "Jona", "Jonah", BookGroup.OT, 4),
        BookData("mic", "Micah", "Micheasza", "Micha", "Mic", BookGroup.OT, 7),
        BookData("nah", "Nahum", "Nahuma", "Nahum", "Nah", BookGroup.OT, 3),
        BookData("hab", "Habakkuk", "Habakuka", "Habakuk", "Hab", BookGroup.OT, 3),
        BookData("zep", "Zephaniah", "Sofoniasza", "Zefanja", "Zeph", BookGroup.OT, 3),
        BookData("hag", "Haggai", "Aggeusza", "Haggai", "Hag", BookGroup.OT, 2),
        BookData("zec", "Zechariah", "Zachariasza", "Sacharja", "Zech", BookGroup.OT, 14),
        BookData("mal", "Malachi", "Malachiasza", "Maleachi", "Mal", BookGroup.OT, 4),

        // New Testament
        BookData("mat", "Matthew", "Mateusza", "Matthäus", "Matt", BookGroup.NT, 28),
        BookData("mar", "Mark", "Marka", "Markus", "Mark", BookGroup.NT, 16),
        BookData("luk", "Luke", "Łukasza", "Lukas", "Luke", BookGroup.NT, 24),
        BookData("joh", "John", "Jana", "Johannes", "John", BookGroup.NT, 21),
        BookData("act", "Acts", "Dzieje Apostolskie", "Apostelgeschichte", "Acts", BookGroup.NT, 28),
        BookData("rom", "Romans", "Rzymian", "Römer", "Rom", BookGroup.NT, 16),
        BookData("1co", "1 Corinthians", "1 Koryntian", "1. Korinther", "1 Cor", BookGroup.NT, 16),
        BookData("2co", "2 Corinthians", "2 Koryntian", "2. Korinther", "2 Cor", BookGroup.NT, 13),
        BookData("gal", "Galatians", "Galacjan", "Galater", "Gal", BookGroup.NT, 6),
        BookData("eph", "Ephesians", "Efezjan", "Epheser", "Eph", BookGroup.NT, 6),
        BookData("phi", "Philippians", "Filipian", "Philipper", "Phil", BookGroup.NT, 4),
        BookData("col", "Colossians", "Kolosan", "Kolosser", "Col", BookGroup.NT, 4),
        BookData("1th", "1 Thessalonians", "1 Tesaloniczan", "1. Thessalonicher", "1 Thess", BookGroup.NT, 5),
        BookData("2th", "2 Thessalonians", "2 Tesaloniczan", "2. Thessalonicher", "2 Thess", BookGroup.NT, 3),
        BookData("1ti", "1 Timothy", "1 Tymoteusza", "1. Timotheus", "1 Tim", BookGroup.NT, 6),
        BookData("2ti", "2 Timothy", "2 Tymoteusza", "2. Timotheus", "2 Tim", BookGroup.NT, 4),
        BookData("tit", "Titus", "Tytusa", "Titus", "Titus", BookGroup.NT, 3),
        BookData("phm", "Philemon", "Filemona", "Philemon", "Phlm", BookGroup.NT, 1),
        BookData("heb", "Hebrews", "Hebrajczyków", "Hebräer", "Heb", BookGroup.NT, 13),
        BookData("jam", "James", "Jakuba", "Jakobus", "Jas", BookGroup.NT, 5),
        BookData("1pe", "1 Peter", "1 Piotra", "1. Petrus", "1 Pet", BookGroup.NT, 5),
        BookData("2pe", "2 Peter", "2 Piotra", "2. Petrus", "2 Pet", BookGroup.NT, 3),
        BookData("1jo", "1 John", "1 Jana", "1. Johannes", "1 John", BookGroup.NT, 5),
        BookData("2jo", "2 John", "2 Jana", "2. Johannes", "2 John", BookGroup.NT, 1),
        BookData("3jo", "3 John", "3 Jana", "3. Johannes", "3 John", BookGroup.NT, 1),
        BookData("jud", "Jude", "Judy", "Judas", "Jude", BookGroup.NT, 1),
        BookData("rev", "Revelation", "Objawienie Jana", "Offenbarung", "Rev", BookGroup.NT, 22),

        // Deuterocanonical / Other
        BookData("tob", "Tobit", "Tobiasza", "Tobit", "Tob", BookGroup.DC, 14),
        BookData("jdt", "Judith", "Judyty", "Judit", "Jdt", BookGroup.DC, 16),
        BookData("1ma", "1 Maccabees", "1 Machabejska", "1. Makkabäer", "1 Macc", BookGroup.DC, 16),
        BookData("2ma", "2 Maccabees", "2 Machabejska", "2. Makkabäer", "2 Macc", BookGroup.DC, 15),
        BookData("wis", "Wisdom of Solomon", "Mądrości", "Weisheit", "Wis", BookGroup.DC, 19),
        BookData("sir", "Sirach", "Syracha", "Jesus Sirach", "Sir", BookGroup.DC, 51),
        BookData("bar", "Baruch", "Barucha", "Baruch", "Bar", BookGroup.DC, 6)
    )

    private val bookMap = books.associateBy { it.id }

    fun getAllBooks(language: String = "en"): List<BookInfo> {
        return books.map { it.toBookInfo(language) }
    }

    fun getOldTestamentBookIds(): Set<String> {
        return books.filter { it.group == BookGroup.OT }.map { it.id }.toSet()
    }

    fun getNewTestamentBookIds(): Set<String> {
        return books.filter { it.group == BookGroup.NT }.map { it.id }.toSet()
    }

    fun getBooksByGroup(group: BookGroup, language: String = "en"): List<BookInfo> {
        return books.filter { it.group == group }.map { it.toBookInfo(language) }
    }

    fun getBook(id: String, language: String = "en"): BookInfo {
        return bookMap[id]?.toBookInfo(language)
            ?: BookInfo(id, id.uppercase(), id.uppercase(), BookGroup.OT, 1)
    }

    fun getBookName(id: String, language: String = "en"): String {
        return getBook(id, language).name
    }
}

private data class BookData(
    val id: String,
    val nameEn: String,
    val namePl: String,
    val nameDe: String,
    val sigla: String,
    val group: BookGroup,
    val defaultChapters: Int
) {
    fun toBookInfo(language: String): BookInfo {
        val name = when (language.lowercase()) {
            "pl" -> namePl
            "de" -> nameDe
            else -> nameEn
        }
        return BookInfo(id, name, sigla, group, defaultChapters)
    }
}
