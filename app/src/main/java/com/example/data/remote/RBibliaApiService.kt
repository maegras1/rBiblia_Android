package com.example.data.remote

import com.example.BuildConfig
import com.example.data.model.ErrorReport
import com.example.data.model.SearchResult
import com.example.data.model.Translation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class RBibliaApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
                .header("User-Agent", "rBiblia-Android/1.0")
                .header("Accept", "application/json")

            val apiKey = BuildConfig.RBIBLIA_API_KEY
            if (apiKey.isNotBlank() && apiKey != "none") {
                builder.header("Authorization", if (apiKey.startsWith("Bearer ", ignoreCase = true)) apiKey else "Bearer $apiKey")
                builder.header("X-API-Key", apiKey)
            }

            val requestWithHeaders = builder.build()
            var response = chain.proceed(requestWithHeaders)
            if (response.code == 429) {
                try {
                    Thread.sleep(800)
                } catch (e: InterruptedException) {
                    // Ignore
                }
                response.close()
                response = chain.proceed(requestWithHeaders)
            }
            response
        }
        .build()

    private val baseUrl = if (BuildConfig.RBIBLIA_API_BASE_URL.isNotBlank()) {
        BuildConfig.RBIBLIA_API_BASE_URL.removeSuffix("/")
    } else {
        "https://rbiblia.app/api"
    }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Memory cache for chapters
    private val chapterCache = mutableMapOf<String, Map<Int, String>>()
    private val structureCache = mutableMapOf<String, Map<String, List<Int>>>()

    suspend fun getTranslations(language: String): List<Translation> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/$language/translation"
        val request = Request.Builder().url(url).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext getFallbackTranslations()
                val body = response.body?.string() ?: return@withContext getFallbackTranslations()
                val json = JSONObject(body)
                val data = json.optJSONArray("data") ?: return@withContext getFallbackTranslations()

                val list = mutableListOf<Translation>()
                for (i in 0 until data.length()) {
                    val obj = data.getJSONObject(i)
                    list.add(
                        Translation(
                            id = obj.getString("id"),
                            language = obj.optString("language", language),
                            name = obj.getString("name"),
                            description = obj.optString("description", ""),
                            date = obj.optString("date", "")
                        )
                    )
                }
                if (list.isEmpty()) getFallbackTranslations() else list
            }
        } catch (e: Exception) {
            getFallbackTranslations()
        }
    }

    suspend fun getTranslationStructure(
        language: String,
        translationId: String
    ): Map<String, List<Int>> = withContext(Dispatchers.IO) {
        val cacheKey = "$language:$translationId"
        structureCache[cacheKey]?.let { return@withContext it }

        val url = "$baseUrl/$language/translation/$translationId"
        val request = Request.Builder().url(url).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyMap()
                val body = response.body?.string() ?: return@withContext emptyMap()
                val json = JSONObject(body)
                val data = json.optJSONObject("data") ?: return@withContext emptyMap()

                val result = mutableMapOf<String, List<Int>>()
                val keys = data.keys()
                while (keys.hasNext()) {
                    val bookKey = keys.next()
                    val chapterArray = data.optJSONArray(bookKey) ?: JSONArray()
                    val chapters = mutableListOf<Int>()
                    for (i in 0 until chapterArray.length()) {
                        chapters.add(chapterArray.getInt(i))
                    }
                    result[bookKey] = chapters
                }
                structureCache[cacheKey] = result
                result
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun getVerses(
        language: String,
        translationId: String,
        bookId: String,
        chapterId: Int
    ): Map<Int, String> = withContext(Dispatchers.IO) {
        val cacheKey = "$translationId:$bookId:$chapterId"
        chapterCache[cacheKey]?.let { return@withContext it }

        val url = "$baseUrl/$language/translation/$translationId/book/$bookId/chapter/$chapterId"
        val request = Request.Builder().url(url).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 404) {
                        return@withContext emptyMap()
                    }
                    return@withContext getFallbackVerses(bookId, chapterId)
                }
                val body = response.body?.string() ?: return@withContext emptyMap()
                val json = JSONObject(body)
                val data = json.optJSONObject("data") ?: return@withContext emptyMap()

                val result = mutableMapOf<Int, String>()
                val keys = data.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val verseNum = key.toIntOrNull() ?: continue
                    result[verseNum] = data.getString(key)
                }
                if (result.isNotEmpty()) {
                    chapterCache[cacheKey] = result
                }
                result
            }
        } catch (e: Exception) {
            getFallbackVerses(bookId, chapterId)
        }
    }

    suspend fun search(
        language: String,
        translationId: String,
        query: String
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        if (query.trim().length < 2) return@withContext emptyList()

        val url = "$baseUrl/$language/search"
        val payload = JSONObject().apply {
            put("translation", translationId)
            put("query", query.trim())
        }
        val requestBody = payload.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder().url(url).post(requestBody).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(body)
                val data = json.optJSONObject("data") ?: return@withContext emptyList()
                val resultsArray = data.optJSONArray("results") ?: return@withContext emptyList()

                val list = mutableListOf<SearchResult>()
                for (i in 0 until resultsArray.length()) {
                    val item = resultsArray.getJSONObject(i)
                    list.add(
                        SearchResult(
                            book = item.getString("book"),
                            chapter = item.getInt("chapter"),
                            verse = item.getInt("verse"),
                            content = item.getString("content")
                        )
                    )
                }
                list
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun submitReport(language: String, report: ErrorReport): Boolean = withContext(Dispatchers.IO) {
        val url = "$baseUrl/$language/report"
        val payload = JSONObject().apply {
            put("name", report.name)
            put("email", report.email)
            put("notes", report.notes)
            put("content", report.content)
            put("original_content", report.originalContent)
            put("translation", report.translation)
            put("book", report.book)
            put("chapter", report.chapter)
            put("verse", report.verse)
        }
        val requestBody = payload.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder().url(url).post(requestBody).build()

        try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun getFallbackTranslations(): List<Translation> {
        return listOf(
            Translation("en_kjv", "en", "King James Version", "Classic Authorized Version", "1611"),
            Translation("en_asv", "en", "American Standard Version", "American Standard Version", "1901"),
            Translation("pl_ubg", "pl", "Uwspółcześniona Biblia Gdańska", "Fundacja Wrota Nadziei", "2017"),
            Translation("pl_bg", "pl", "Biblia Gdańska", "Drukarnia Krzysztofa Schenka", "1632"),
            Translation("pl_bw", "pl", "Biblia Warszawska", "Brytyjskie i Zagraniczne Towarzystwo Biblijne", "1975"),
            Translation("de_lb", "de", "Luther Bibel", "Martin Luther Übersetzung", "1912"),
            Translation("de_e", "de", "Einheitsübersetzung", "Einheitsübersetzung der Heiligen Schrift", "1980")
        )
    }

    private fun getFallbackVerses(bookId: String, chapterId: Int): Map<Int, String> {
        if (bookId == "gen" && chapterId == 1) {
            return mapOf(
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
        }
        if (bookId == "mat" && chapterId == 1) {
            return mapOf(
                1 to "Księga rodu Jezusa Chrystusa, syna Dawida, syna Abrahama.",
                2 to "Abraham spłodził Izaaka, a Izaak spłodził Jakuba, a Jakub spłodził Judę i jego braci;",
                18 to "A z narodzeniem Jezusa Chrystusa było tak: Gdy Maria, jego matka, została zaręczona z Józefem, zanim się zeszli, okazało się, że jest brzemienna z Ducha Świętego.",
                20 to "Gdy o tym myślał, oto anioł Pana ukazał mu się we śnie i powiedział: Józefie, synu Dawida, nie bój się przyjąć Marii, twojej żony, gdyż to, co się w niej poczęło, jest z Ducha Świętego.",
                21 to "I urodzi syna, a ty nadasz mu imię Jezus; on bowiem zbawi swój lud od jego grzechów.",
                22 to "A to wszystko się stało, aby się wypełniło, co powiedział Pan przez proroka:",
                23 to "Oto dziewica będzie brzemienna i urodzi syna, i nadadzą mu imię Emmanuel, co się tłumaczy: Bóg z nami."
            )
        }
        if (bookId == "joh" && chapterId == 1) {
            return mapOf(
                1 to "Na początku było Słowo, a Słowo było u Boga i Bogiem było Słowo.",
                2 to "Ono było na początku u Boga.",
                3 to "Wszystko przez nie powstało, a bez niego nic nie powstało, co powstało.",
                4 to "W nim było życie, a życie było światłością ludzi.",
                5 to "A światłość świeci w ciemności, lecz ciemność jej nie ogarnęła.",
                6 to "Był człowiek posłany od Boga, któremu na imię było Jan.",
                7 to "Przyszedł on na świadectwo, aby świadczyć o tej światłości, aby wszyscy przez niego uwierzyli.",
                8 to "Nie był on tą światłością, ale przyszedł, aby świadczyć o tej światłości.",
                9 to "Ten był tą prawdziwą światłością, która oświeca każdego człowieka przychodzącego na świat.",
                10 to "Był na świecie, a świat przez niego powstał, ale świat go nie poznał.",
                11 to "Do swego przyszedł, a swoi go nie przyjęli.",
                12 to "Lecz wszystkim tym, którzy go przyjęli, dał moc, aby się stali synami Bożymi, to jest tym, którzy wierzą w jego imię;",
                13 to "Którzy są narodzeni nie z krwi ani z woli ciała, ani z woli mężczyzny, ale z Boga.",
                14 to "A to Słowo stało się ciałem i mieszkało wśród nas (i widzieliśmy jego chwałę, chwałę jako jednorodzonego od Ojca), pełne łaski i prawdy."
            )
        }
        return emptyMap()
    }
}
