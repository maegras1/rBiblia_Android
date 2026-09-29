package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class UpdateCheckResult {
    data class UpdateAvailable(
        val latestVersion: String,
        val releaseName: String,
        val releaseNotes: String,
        val apkDownloadUrl: String?,
        val releaseUrl: String
    ) : UpdateCheckResult()

    data object UpToDate : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

object AppUpdateManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val CURRENT_VERSION: String = BuildConfig.VERSION_NAME
    val GITHUB_REPO: String = if (
        BuildConfig.GITHUB_UPDATE_REPO.isNotBlank() &&
        BuildConfig.GITHUB_UPDATE_REPO != "none" &&
        BuildConfig.GITHUB_UPDATE_REPO != "owner/repo" &&
        !BuildConfig.GITHUB_UPDATE_REPO.contains("example.com")
    ) {
        BuildConfig.GITHUB_UPDATE_REPO
    } else {
        "maegras1/rBiblia_Android"
    }

    suspend fun checkForUpdates(
        currentVersion: String = CURRENT_VERSION,
        repoOwnerAndName: String = GITHUB_REPO
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val cleanRepo = repoOwnerAndName.trim()
                .removePrefix("https://github.com/")
                .removePrefix("http://github.com/")
                .removeSuffix("/")

            val url = "https://api.github.com/repos/$cleanRepo/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "rBiblia-Android-App")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                if (response.code == 404) {
                    return@withContext UpdateCheckResult.Error("Brak publicznych wydań lub repozytorium jest prywatne (HTTP 404). Aby aktualizacje działały, repozytorium musi być publiczne lub posiadać opublikowany Release.")
                }
                return@withContext UpdateCheckResult.Error("Błąd serwera GitHub: HTTP ${response.code}")
            }

            val body = response.body?.string() ?: return@withContext UpdateCheckResult.Error("Pusta odpowiedź z GitHub")
            val json = JSONObject(body)

            val tagName = json.optString("tag_name", "").trim()
            val releaseName = json.optString("name", tagName)
            val releaseNotes = json.optString("body", "")
            val htmlUrl = json.optString("html_url", "https://github.com/$cleanRepo/releases")

            val cleanTag = tagName.removePrefix("v").removePrefix("V")
            val cleanCurrent = currentVersion.removePrefix("v").removePrefix("V")

            // Find APK asset in release if any
            var apkUrl: String? = null
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        break
                    }
                }
            }

            if (isVersionNewer(cleanTag, cleanCurrent)) {
                UpdateCheckResult.UpdateAvailable(
                    latestVersion = tagName,
                    releaseName = releaseName,
                    releaseNotes = releaseNotes,
                    apkDownloadUrl = apkUrl,
                    releaseUrl = htmlUrl
                )
            } else {
                UpdateCheckResult.UpToDate
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Nie udało się sprawdzić aktualizacji")
        }
    }

    private fun isVersionNewer(remote: String, current: String): Boolean {
        if (remote.isBlank() || current.isBlank()) return false
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    fun openUpdateUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
