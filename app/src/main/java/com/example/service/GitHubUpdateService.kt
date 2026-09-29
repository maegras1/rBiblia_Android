package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ReleaseInfo(
    val tagName: String,
    val releaseName: String,
    val releaseNotes: String,
    val apkDownloadUrl: String?,
    val releasePageUrl: String
)

sealed class UpdateStatus {
    data object Idle : UpdateStatus()
    data object Checking : UpdateStatus()
    data class UpdateAvailable(val release: ReleaseInfo) : UpdateStatus()
    data object UpToDate : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

class GitHubUpdateService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val _status = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val status: StateFlow<UpdateStatus> = _status.asStateFlow()

    companion object {
        const val CHANNEL_ID = "channel_app_updates"
        const val NOTIFICATION_ID = 2001

        fun getEffectiveRepo(): String {
            val repo = BuildConfig.GITHUB_UPDATE_REPO.trim()
            return if (repo.isNotBlank() && repo != "none" && repo != "owner/repo" && !repo.contains("example.com")) {
                repo.removePrefix("https://github.com/")
                    .removePrefix("http://github.com/")
                    .removeSuffix("/")
            } else {
                "maegras1/rBiblia_Android"
            }
        }
    }

    init {
        createNotificationChannel()
    }

    fun checkForUpdatesAsync(notifySystemNotification: Boolean = true, onComplete: ((UpdateStatus) -> Unit)? = null) {
        _status.value = UpdateStatus.Checking
        CoroutineScope(Dispatchers.IO).launch {
            val result = checkLatestRelease()
            _status.value = result
            if (result is UpdateStatus.UpdateAvailable && notifySystemNotification) {
                showUpdateNotification(result.release)
            }
            withContext(Dispatchers.Main) {
                onComplete?.invoke(result)
            }
        }
    }

    suspend fun checkLatestRelease(): UpdateStatus = withContext(Dispatchers.IO) {
        try {
            val repo = getEffectiveRepo()
            val url = "https://api.github.com/repos/$repo/releases/latest"

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "rBiblia-Android-App")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                if (response.code == 404) {
                    return@withContext UpdateStatus.Error(
                        "Brak wydań (Releases) w repozytorium $repo lub repozytorium jest prywatne (HTTP 404)."
                    )
                }
                return@withContext UpdateStatus.Error("Błąd serwera GitHub: HTTP ${response.code}")
            }

            val body = response.body?.string() ?: return@withContext UpdateStatus.Error("Pusta odpowiedź z GitHub")
            val json = JSONObject(body)

            val tagName = json.optString("tag_name", "").trim()
            val releaseName = json.optString("name", tagName)
            val releaseNotes = json.optString("body", "")
            val htmlUrl = json.optString("html_url", "https://github.com/$repo/releases")

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

            val remoteClean = tagName.removePrefix("v").removePrefix("V")
            val currentClean = BuildConfig.VERSION_NAME.removePrefix("v").removePrefix("V")

            val release = ReleaseInfo(
                tagName = tagName,
                releaseName = releaseName,
                releaseNotes = releaseNotes,
                apkDownloadUrl = apkUrl,
                releasePageUrl = htmlUrl
            )

            if (isVersionNewer(remoteClean, currentClean)) {
                UpdateStatus.UpdateAvailable(release)
            } else {
                UpdateStatus.UpToDate
            }
        } catch (e: Exception) {
            UpdateStatus.Error(e.message ?: "Błąd sprawdzania aktualizacji")
        }
    }

    fun isVersionNewer(remote: String, current: String): Boolean {
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Aktualizacje rBiblia",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Powiadomienia o nowych wersjach aplikacji na GitHubie"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun showUpdateNotification(release: ReleaseInfo) {
        val targetUrl = release.apkDownloadUrl ?: release.releasePageUrl
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Dostępna nowa wersja rBiblia: ${release.tagName}")
            .setContentText("Kliknij, aby pobrać i zainstalować aktualizację")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Zainstalowana wersja: v${BuildConfig.VERSION_NAME}\nNowa wersja: ${release.tagName}\n\n${release.releaseNotes.take(150)}")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        try {
            manager?.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
