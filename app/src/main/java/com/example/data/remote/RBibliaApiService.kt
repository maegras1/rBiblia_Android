package com.example.data.remote

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
        .build()

    private val baseUrl = "https://rbiblia.app/api"
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
        if (bookId == "joh" && chapterId == 1) {
            return mapOf(
                1 to "In the beginning was the Word, and the Word was with God, and the Word was God.",
                2 to "The same was in the beginning with God.",
                3 to "All things were made by him; and without him was not any thing made that was made.",
                4 to "In him was life; and the life was the light of men.",
                5 to "And the light shineth in darkness; and the darkness comprehended it not.",
                6 to "There was a man sent from God, whose name was John.",
                7 to "The same came for a witness, to bear witness of the Light, that all men through him might believe.",
                8 to "He was not that Light, but was sent to bear witness of that Light.",
                9 to "That was the true Light, which lighteth every man that cometh into the world.",
                10 to "He was in the world, and the world was made by him, and the world knew him not.",
                11 to "He came unto his own, and his own received him not.",
                12 to "But as many as received him, to them gave he power to become the sons of God, even to them that believe on his name:",
                13 to "Which were born, not of blood, nor of the will of the flesh, nor of the will of man, but of God.",
                14 to "And the Word was made flesh, and dwelt among us, (and we beheld his glory, the glory as of the only begotten of the Father,) full of grace and truth."
            )
        }
        return emptyMap()
    }
}
