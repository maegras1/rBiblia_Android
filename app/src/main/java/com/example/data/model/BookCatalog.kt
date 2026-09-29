package com.example.data.model

object BookCatalog {

    // Overrides fetched dynamically from /api/{language}/book
    private val dynamicBookOverrides = mutableMapOf<String, MutableMap<String, Pair<String, String>>>()

    fun updateBooksFromApi(language: String, apiBooks: Map<String, Pair<String, String>>) {
        val langMap = dynamicBookOverrides.getOrPut(language.lowercase()) { mutableMapOf() }
        langMap.putAll(apiBooks)
    }

    private val books = listOf(
        // Old Testament (Stary Testament)
        BookData("gen", "Genesis", "Rodzaju (1 Mojż.)", "1. Mose (Genesis)", "Gen", "Rdz", "1Mo", BookGroup.OT, 50),
        BookData("exo", "Exodus", "Wyjścia (2 Mojż.)", "2. Mose (Exodus)", "Exod", "Wj", "2Mo", BookGroup.OT, 40),
        BookData("lev", "Leviticus", "Kapłańska (3 Mojż.)", "3. Mose (Levitikus)", "Lev", "Kpł", "3Mo", BookGroup.OT, 27),
        BookData("num", "Numbers", "Liczb (4 Mojż.)", "4. Mose (Numeri)", "Num", "Lb", "4Mo", BookGroup.OT, 36),
        BookData("deu", "Deuteronomy", "Powtórzonego Prawa (5 Mojż.)", "5. Mose (Deuteronomium)", "Deut", "Pwt", "5Mo", BookGroup.OT, 34),
        BookData("jos", "Joshua", "Jozuego", "Josua", "Josh", "Joz", "Jos", BookGroup.OT, 24),
        BookData("jdg", "Judges", "Sędziów", "Richter", "Judg", "Sdz", "Ri", BookGroup.OT, 21),
        BookData("rut", "Ruth", "Rut", "Rut", "Ruth", "Rt", "Rut", BookGroup.OT, 4),
        BookData("1sa", "1 Samuel", "1 Samuela", "1. Samuel", "1 Sam", "1 Sm", "1Sam", BookGroup.OT, 31),
        BookData("2sa", "2 Samuel", "2 Samuela", "2. Samuel", "2 Sam", "2 Sm", "2Sam", BookGroup.OT, 24),
        BookData("1ki", "1 Kings", "1 Królewska", "1. Könige", "1 Kgs", "1 Krl", "1Kön", BookGroup.OT, 22),
        BookData("2ki", "2 Kings", "2 Królewska", "2. Könige", "2 Kgs", "2 Krl", "2Kön", BookGroup.OT, 25),
        BookData("1ch", "1 Chronicles", "1 Kronik", "1. Chronik", "1 Chr", "1 Krn", "1Chr", BookGroup.OT, 29),
        BookData("2ch", "2 Chronicles", "2 Kronik", "2. Chronik", "2 Chr", "2 Krn", "2Chr", BookGroup.OT, 36),
        BookData("ezr", "Ezra", "Ezdrasza", "Esra", "Ezra", "Ezd", "Esr", BookGroup.OT, 10),
        BookData("neh", "Nehemiah", "Nehemiasza", "Nehemia", "Neh", "Ne", "Neh", BookGroup.OT, 13),
        BookData("est", "Esther", "Estery", "Ester", "Esth", "Est", "Est", BookGroup.OT, 10),
        BookData("job", "Job", "Hioba", "Hiob", "Job", "Hi", "Ijob", BookGroup.OT, 42),
        BookData("psa", "Psalms", "Psalmy", "Psalmen", "Ps", "Ps", "Ps", BookGroup.OT, 150),
        BookData("pro", "Proverbs", "Przysłów", "Sprüche", "Prov", "Prz", "Spr", BookGroup.OT, 31),
        BookData("ecc", "Ecclesiastes", "Koheleta (Kaznodziei)", "Prediger (Kohelet)", "Eccl", "Koh", "Pred", BookGroup.OT, 12),
        BookData("sol", "Song of Solomon", "Pieśń nad Pieśniami", "Hoheslied", "Song", "Pnp", "Hld", BookGroup.OT, 8),
        BookData("isa", "Isaiah", "Izajasza", "Jesaja", "Isa", "Iz", "Jes", BookGroup.OT, 66),
        BookData("jer", "Jeremiah", "Jeremiasza", "Jeremia", "Jer", "Jr", "Jer", BookGroup.OT, 52),
        BookData("lam", "Lamentations", "Treny (Lamentacje)", "Klagelieder", "Lam", "Lm", "Klgl", BookGroup.OT, 5),
        BookData("eze", "Ezekiel", "Ezechiela", "Hesekiel", "Ezek", "Ez", "Hes", BookGroup.OT, 48),
        BookData("dan", "Daniel", "Daniela", "Daniel", "Dan", "Dn", "Dan", BookGroup.OT, 12),
        BookData("hos", "Hosea", "Ozeasza", "Hosea", "Hos", "Oz", "Hos", BookGroup.OT, 14),
        BookData("joe", "Joel", "Joela", "Joel", "Joel", "Jl", "Joel", BookGroup.OT, 3),
        BookData("amo", "Amos", "Amosa", "Amos", "Amos", "Am", "Am", BookGroup.OT, 9),
        BookData("oba", "Obadiah", "Abdiasza", "Obadja", "Obad", "Ab", "Obd", BookGroup.OT, 1),
        BookData("jon", "Jonah", "Jonasza", "Jona", "Jonah", "Jon", "Jona", BookGroup.OT, 4),
        BookData("mic", "Micah", "Micheasza", "Micha", "Mic", "Mi", "Mi", BookGroup.OT, 7),
        BookData("nah", "Nahum", "Nahuma", "Nahum", "Nah", "Na", "Nah", BookGroup.OT, 3),
        BookData("hab", "Habakkuk", "Habakuka", "Habakuk", "Hab", "Ha", "Hab", BookGroup.OT, 3),
        BookData("zep", "Zephaniah", "Sofoniasza", "Zefanja", "Zeph", "So", "Zef", BookGroup.OT, 3),
        BookData("hag", "Haggai", "Aggeusza", "Haggai", "Hag", "Ag", "Hag", BookGroup.OT, 2),
        BookData("zec", "Zechariah", "Zachariasza", "Sacharja", "Zech", "Za", "Sach", BookGroup.OT, 14),
        BookData("mal", "Malachi", "Malachiasza", "Maleachi", "Mal", "Ml", "Mal", BookGroup.OT, 4),

        // New Testament (Nowy Testament)
        BookData("mat", "Matthew", "Ewangelia Mateusza", "Matthäus", "Matt", "Mt", "Mt", BookGroup.NT, 28),
        BookData("mar", "Mark", "Ewangelia Marka", "Markus", "Mark", "Mk", "Mk", BookGroup.NT, 16),
        BookData("luk", "Luke", "Ewangelia Łukasza", "Lukas", "Luke", "Łk", "Lk", BookGroup.NT, 24),
        BookData("joh", "John", "Ewangelia Jana", "Johannes", "John", "J", "Joh", BookGroup.NT, 21),
        BookData("act", "Acts", "Dzieje Apostolskie", "Apostelgeschichte", "Acts", "Dz", "Apg", BookGroup.NT, 28),
        BookData("rom", "Romans", "List do Rzymian", "Römer", "Rom", "Rz", "Röm", BookGroup.NT, 16),
        BookData("1co", "1 Corinthians", "1 List do Koryntian", "1. Korinther", "1 Cor", "1 Kor", "1Kor", BookGroup.NT, 16),
        BookData("2co", "2 Corinthians", "2 List do Koryntian", "2. Korinther", "2 Cor", "2 Kor", "2Kor", BookGroup.NT, 13),
        BookData("gal", "Galatians", "List do Galatów", "Galater", "Gal", "Ga", "Gal", BookGroup.NT, 6),
        BookData("eph", "Ephesians", "List do Efezjan", "Epheser", "Eph", "Ef", "Eph", BookGroup.NT, 6),
        BookData("phi", "Philippians", "List do Filipian", "Philipper", "Phil", "Flp", "Phil", BookGroup.NT, 4),
        BookData("col", "Colossians", "List do Kolosan", "Kolosser", "Col", "Kol", "Kol", BookGroup.NT, 4),
        BookData("1th", "1 Thessalonians", "1 List do Tesaloniczan", "1. Thessalonicher", "1 Thess", "1 Tes", "1Thess", BookGroup.NT, 5),
        BookData("2th", "2 Thessalonians", "2 List do Tesaloniczan", "2. Thessalonicher", "2 Thess", "2 Tes", "2Thess", BookGroup.NT, 3),
        BookData("1ti", "1 Timothy", "1 List do Tymoteusza", "1. Timotheus", "1 Tim", "1 Tm", "1Tim", BookGroup.NT, 6),
        BookData("2ti", "2 Timothy", "2 List do Tymoteusza", "2. Timotheus", "2 Tim", "2 Tm", "2Tim", BookGroup.NT, 4),
        BookData("tit", "Titus", "List do Tytusa", "Titus", "Tit", "Tt", "Tit", BookGroup.NT, 3),
        BookData("phm", "Philemon", "List do Filemona", "Philemon", "Phm", "Flm", "Phlm", BookGroup.NT, 1),
        BookData("heb", "Hebrews", "List do Hebrajczyków", "Hebräer", "Heb", "Hbr", "Hebr", BookGroup.NT, 13),
        BookData("jam", "James", "List św. Jakuba", "Jakobus", "Jas", "Jk", "Jak", BookGroup.NT, 5),
        BookData("1pe", "1 Peter", "1 List św. Piotra", "1. Petrus", "1 Pet", "1 P", "1Petr", BookGroup.NT, 5),
        BookData("2pe", "2 Peter", "2 List św. Piotra", "2. Petrus", "2 Pet", "2 P", "2Petr", BookGroup.NT, 3),
        BookData("1jo", "1 John", "1 List św. Jana", "1. Johannes", "1 John", "1 J", "1Joh", BookGroup.NT, 5),
        BookData("2jo", "2 John", "2 List św. Jana", "2. Johannes", "2 John", "2 J", "2Joh", BookGroup.NT, 1),
        BookData("3jo", "3 John", "3 List św. Jana", "3. Johannes", "3 John", "3 J", "3Joh", BookGroup.NT, 1),
        BookData("jud", "Jude", "List św. Judy", "Judas", "Jude", "Jud", "Jud", BookGroup.NT, 1),
        BookData("rev", "Revelation", "Apokalipsa św. Jana", "Offenbarung", "Rev", "Ap", "Offb", BookGroup.NT, 22),

        // Deuterocanonical / Other (Księgi deuterokanoniczne)
        BookData("tob", "Tobit", "Tobiasza", "Tobit", "Tob", "Tb", "Tob", BookGroup.DC, 14),
        BookData("jdt", "Judith", "Judyty", "Judit", "Jdt", "Jdt", "Jdt", BookGroup.DC, 16),
        BookData("1ma", "1 Maccabees", "1 Machabejska", "1. Makkabäer", "1 Macc", "1 Mch", "1Makk", BookGroup.DC, 16),
        BookData("2ma", "2 Maccabees", "2 Machabejska", "2. Makkabäer", "2 Macc", "2 Mch", "2Makk", BookGroup.DC, 15),
        BookData("wis", "Wisdom of Solomon", "Mądrości", "Weisheit", "Wis", "Mdr", "Weish", BookGroup.DC, 19),
        BookData("sir", "Sirach", "Mądrość Syracha", "Jesus Sirach", "Sir", "Syr", "Sir", BookGroup.DC, 51),
        BookData("bar", "Baruch", "Barucha", "Baruch", "Bar", "Ba", "Bar", BookGroup.DC, 6)
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

    private data class BookData(
        val id: String,
        val nameEn: String,
        val namePl: String,
        val nameDe: String,
        val siglaEn: String,
        val siglaPl: String,
        val siglaDe: String,
        val group: BookGroup,
        val defaultChapters: Int
    ) {
        fun toBookInfo(language: String): BookInfo {
            val dynamic = dynamicBookOverrides[language.lowercase()]?.get(id)
            val name = dynamic?.first ?: when (language.lowercase()) {
                "pl" -> namePl
                "de" -> nameDe
                else -> nameEn
            }
            val sigla = dynamic?.second ?: when (language.lowercase()) {
                "pl" -> siglaPl
                "de" -> siglaDe
                else -> siglaEn
            }
            return BookInfo(id, name, sigla, group, defaultChapters)
        }
    }
}
