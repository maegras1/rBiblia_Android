package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import com.example.BuildConfig
import com.example.service.GitHubUpdateService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class InstalledAppDiagnostics(
    val applicationId: String,
    val versionName: String,
    val versionCode: Long,
    val signatureSha256: String
)

data class ApkCompatibilityReport(
    val isCompatibleForInPlaceUpdate: Boolean,
    val installedPackage: String,
    val apkPackage: String,
    val packageMatches: Boolean,
    val installedVersionCode: Long,
    val apkVersionCode: Long,
    val apkVersionName: String,
    val versionCodeValid: Boolean,
    val installedSignatureSha256: String,
    val apkSignatureSha256: String,
    val signatureMatches: Boolean,
    val summaryMessage: String
)

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
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    val CURRENT_VERSION: String = BuildConfig.VERSION_NAME
    val CURRENT_VERSION_CODE: Int = BuildConfig.VERSION_CODE
    val APPLICATION_ID: String = BuildConfig.APPLICATION_ID
    val GITHUB_REPO: String = GitHubUpdateService.getEffectiveRepo()

    fun getInstalledAppDiagnostics(context: Context): InstalledAppDiagnostics {
        return try {
            val pm = context.packageManager
            val pkgName = context.packageName
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(pkgName, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkgName, flags)
            }
            val vCode = PackageInfoCompat.getLongVersionCode(pkgInfo)
            val sha256 = extractSignatureSha256(pkgInfo)
            InstalledAppDiagnostics(
                applicationId = pkgName,
                versionName = pkgInfo.versionName ?: CURRENT_VERSION,
                versionCode = vCode,
                signatureSha256 = sha256
            )
        } catch (_: Exception) {
            InstalledAppDiagnostics(
                applicationId = APPLICATION_ID,
                versionName = CURRENT_VERSION,
                versionCode = CURRENT_VERSION_CODE.toLong(),
                signatureSha256 = "N/A"
            )
        }
    }

    private fun extractSignatureSha256(pkgInfo: PackageInfo): String {
        return try {
            val certBytes: ByteArray? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = pkgInfo.signingInfo
                val sigs = if (signingInfo != null) {
                    if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners
                    } else {
                        signingInfo.signingCertificateHistory
                    }
                } else null
                sigs?.firstOrNull()?.toByteArray()
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.signatures?.firstOrNull()?.toByteArray()
            }
            if (certBytes == null) return "BRAK"
            val digest = MessageDigest.getInstance("SHA-256").digest(certBytes)
            digest.joinToString(":") { "%02X".format(it) }
        } catch (_: Exception) {
            "BŁĄD"
        }
    }

    suspend fun verifyRemoteApkCompatibility(
        context: Context,
        apkDownloadUrl: String
    ): Result<ApkCompatibilityReport> = withContext(Dispatchers.IO) {
        if (!GitHubUpdateService.isTrustedUpdateUrl(apkDownloadUrl)) {
            return@withContext Result.failure(IllegalArgumentException("Niezaufany adres URL pliku APK."))
        }

        val tempFile = File(context.cacheDir, "verify_github_release.apk")
        try {
            val request = Request.Builder()
                .url(apkDownloadUrl.trim())
                .header("User-Agent", "rBiblia-Android-App")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Nie udało się pobrać pliku APK do weryfikacji (HTTP ${response.code}).")
                    )
                }
                val body = response.body ?: return@withContext Result.failure(
                    IllegalStateException("Pusta odpowiedź podczas pobierania pliku APK.")
                )
                FileOutputStream(tempFile).use { output ->
                    body.byteStream().use { input ->
                        input.copyTo(output)
                    }
                }
            }

            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }

            val archiveInfo: PackageInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(tempFile.absolutePath, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(tempFile.absolutePath, flags)
            }

            if (archiveInfo == null) {
                return@withContext Result.failure(
                    IllegalStateException("Pobrany plik nie jest prawidłowym archiwum APK.")
                )
            }

            val installed = getInstalledAppDiagnostics(context)
            val apkPkg = archiveInfo.packageName ?: ""
            val apkVCode = PackageInfoCompat.getLongVersionCode(archiveInfo)
            val apkVName = archiveInfo.versionName ?: "?"
            val apkSha256 = extractSignatureSha256(archiveInfo)

            val pkgMatches = installed.applicationId == apkPkg
            val vCodeValid = apkVCode >= installed.versionCode
            val sigMatches = installed.signatureSha256 != "BRAK" &&
                apkSha256 != "BRAK" &&
                installed.signatureSha256.equals(apkSha256, ignoreCase = true)

            val isCompatible = pkgMatches && vCodeValid && sigMatches

            val message = when {
                !pkgMatches -> "Niezgodne ID aplikacji (applicationId): zainstalowano '${installed.applicationId}', a w APK jest '$apkPkg'."
                !sigMatches -> "Niezgodny klucz podpisu certyfikatu (SHA-256). Zainstalowana wersja została podpisana innym kluczem niż plik na GitHubie (błąd INSTALL_FAILED_UPDATE_INCOMPATIBLE). Wyeksportuj notatki do XML w Ustawieniach, odinstaluj obecną wersję jednorazowo i zainstaluj nową — kolejne aktualizacje będą już działać automatycznie."
                !vCodeValid -> "Niższy kod wersji w pliku APK (versionCode $apkVCode < zainstalowany ${installed.versionCode}, błąd INSTALL_FAILED_VERSION_DOWNGRADE). Uruchom nowy build na GitHubie."
                else -> "Plik APK jest w 100% zgodny (ID pakietu, certyfikat SHA-256 oraz versionCode ${installed.versionCode} ➔ $apkVCode). Możesz zaktualizować aplikację bez jej odinstalowywania."
            }

            Result.success(
                ApkCompatibilityReport(
                    isCompatibleForInPlaceUpdate = isCompatible,
                    installedPackage = installed.applicationId,
                    apkPackage = apkPkg,
                    packageMatches = pkgMatches,
                    installedVersionCode = installed.versionCode,
                    apkVersionCode = apkVCode,
                    apkVersionName = apkVName,
                    versionCodeValid = vCodeValid,
                    installedSignatureSha256 = installed.signatureSha256,
                    apkSignatureSha256 = apkSha256,
                    signatureMatches = sigMatches,
                    summaryMessage = message
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            runCatching { if (tempFile.exists()) tempFile.delete() }
        }
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

            client.newCall(request).execute().use { response ->
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

                // Find APK asset in release (prefer versioned rBiblia-*.apk)
                var apkUrl: String? = null
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    var fallbackApkUrl: String? = null
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        val downloadUrl = asset.optString("browser_download_url")
                        if (name.endsWith(".apk", ignoreCase = true) && GitHubUpdateService.isTrustedUpdateUrl(downloadUrl)) {
                            if (name.startsWith("rBiblia", ignoreCase = true)) {
                                apkUrl = downloadUrl
                                break
                            } else if (fallbackApkUrl == null) {
                                fallbackApkUrl = downloadUrl
                            }
                        }
                    }
                    if (apkUrl == null) {
                        apkUrl = fallbackApkUrl
                    }
                }

                if (isVersionNewer(cleanTag, cleanCurrent)) {
                    UpdateCheckResult.UpdateAvailable(
                        latestVersion = tagName,
                        releaseName = releaseName,
                        releaseNotes = releaseNotes,
                        apkDownloadUrl = apkUrl,
                        releaseUrl = if (GitHubUpdateService.isTrustedUpdateUrl(htmlUrl)) htmlUrl else "https://github.com/$cleanRepo/releases"
                    )
                } else {
                    UpdateCheckResult.UpToDate
                }
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Nie udało się sprawdzić aktualizacji")
        }
    }

    private fun isVersionNewer(remote: String, current: String): Boolean {
        if (remote.isBlank() || current.isBlank()) return false
        val remoteParts = remote.trim().removePrefix("v").removePrefix("V")
            .split(".", "-", "_")
            .mapNotNull { it.takeWhile { ch -> ch.isDigit() }.toIntOrNull() }
        val currentParts = current.trim().removePrefix("v").removePrefix("V")
            .split(".", "-", "_")
            .mapNotNull { it.takeWhile { ch -> ch.isDigit() }.toIntOrNull() }

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
            if (!GitHubUpdateService.isTrustedUpdateUrl(url)) return
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url.trim())).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
