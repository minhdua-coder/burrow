package com.burrow.app.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * Downloads the release APK via the system DownloadManager (so it survives
 * the app being backgrounded, and shows the OS's own progress notification),
 * then hands the finished file to the package installer. Android still
 * requires one user tap on its own "Install" confirmation - that step can't
 * be skipped for a non-system app - but this removes every step before it
 * (no browser, no hunting for the download in the notification shade).
 */
object UpdateInstaller {

    fun downloadAndInstall(context: Context, apkUrl: String, versionTag: String) {
        val appContext = context.applicationContext
        val fileName = "warren-$versionTag.apk"
        val destFile = File(appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (destFile.exists()) destFile.delete()

        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Warren $versionTag")
            .setDescription("Downloading update")
            .setDestinationUri(Uri.fromFile(destFile))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val manager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)

        Toast.makeText(appContext, "Downloading Warren $versionTag…", Toast.LENGTH_SHORT).show()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id != downloadId) return
                runCatching { appContext.unregisterReceiver(this) }
                if (destFile.exists()) {
                    installApk(appContext, destFile)
                } else {
                    Toast.makeText(appContext, "Download failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            appContext.registerReceiver(receiver, filter)
        }
    }

    private fun installApk(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
