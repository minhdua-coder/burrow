package com.burrow.app.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.burrow.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val GITHUB_REPO = "minhdua-coder/burrow"
private const val CHANNEL_ID = "burrow_updates"
private const val NOTIFICATION_ID = 1001

private val Context.updateDataStore by preferencesDataStore(name = "burrow_update")
private val LAST_NOTIFIED_TAG = stringPreferencesKey("last_notified_tag")

data class ReleaseInfo(val tagName: String, val htmlUrl: String, val apkUrl: String?)

object UpdateChecker {

    suspend fun checkForUpdate(context: Context) {
        val release = fetchLatestRelease() ?: return
        val remoteVersion = release.tagName.removePrefix("v")
        if (!isNewer(remoteVersion, BuildConfig.VERSION_NAME)) return

        val lastNotified = context.updateDataStore.data.first()[LAST_NOTIFIED_TAG]
        if (lastNotified == release.tagName) return

        showUpdateNotification(context, release)
        context.updateDataStore.edit { it[LAST_NOTIFIED_TAG] = release.tagName }
    }

    private suspend fun fetchLatestRelease(): ReleaseInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.inputStream.bufferedReader().use { it.readText() }.let { body ->
                val json = JSONObject(body)
                val tagName = json.getString("tag_name")
                val htmlUrl = json.getString("html_url")
                val assets = json.optJSONArray("assets")
                var apkUrl: String? = null
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url")
                            break
                        }
                    }
                }
                ReleaseInfo(tagName, htmlUrl, apkUrl)
            }
        }.getOrNull()
    }

    private fun isNewer(remoteVersion: String, currentVersion: String): Boolean {
        val remote = remoteVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val current = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val len = maxOf(remote.size, current.size)
        for (i in 0 until len) {
            val r = remote.getOrElse(i) { 0 }
            val c = current.getOrElse(i) { 0 }
            if (r != c) return r > c
        }
        return false
    }

    private fun showUpdateNotification(context: Context, release: ReleaseInfo) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "App updates", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }

        val targetUrl = release.apkUrl ?: release.htmlUrl
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Burrow ${release.tagName} available")
            .setContentText("Tap to download the update")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        ) {
            runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
        }
    }
}
