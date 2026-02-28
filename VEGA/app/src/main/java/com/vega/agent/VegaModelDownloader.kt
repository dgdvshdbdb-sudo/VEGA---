package com.vega.agent

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * VegaModelDownloader — Gemma model ko app ke andar se auto-download karta hai
 * User ko kuch nahi karna — bas ek baar WiFi pe download hoga
 */
object VegaModelDownloader {

    private const val TAG = "VegaModelDownloader"

    // Gemma 2B INT4 — HuggingFace se available (Google ka official)
    // ~1.5GB, ek baar download, hamesha offline
    private const val MODEL_URL =
        "https://huggingface.co/google/gemma-2b-it-cpu-int4/resolve/main/gemma-2b-it-cpu-int4.bin"

    const val MODEL_FILENAME = "gemma-2b-it-cpu-int4.bin"

    data class DownloadProgress(
        val downloadedMB: Float,
        val totalMB: Float,
        val percent: Int,
        val speedKBps: Float
    )

    /** Check karo model already downloaded hai ya nahi */
    fun isDownloaded(context: Context): Boolean {
        return getSavePath(context).exists()
    }

    /** Model save hone ki path */
    fun getSavePath(context: Context): File {
        // App private storage — no extra permission needed (Android 10+)
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        return File(dir, MODEL_FILENAME)
    }

    /**
     * Model download karo with progress callback
     * @param onProgress download progress updates
     * @param onComplete download complete
     * @param onError error callback
     */
    suspend fun download(
        context: Context,
        onProgress: (DownloadProgress) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val destFile = getSavePath(context)
        val tempFile = File(destFile.parent, "$MODEL_FILENAME.tmp")

        Log.d(TAG, "Downloading model to: ${destFile.absolutePath}")

        try {
            val url = URL(MODEL_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.connect()

            if (conn.responseCode !in 200..299) {
                onError("Server error: ${conn.responseCode}")
                return@withContext
            }

            val totalBytes = conn.contentLengthLong
            val totalMB = totalBytes / (1024f * 1024f)
            var downloadedBytes = 0L
            val startTime = System.currentTimeMillis()

            conn.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    var lastProgressTime = startTime

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read

                        val now = System.currentTimeMillis()
                        if (now - lastProgressTime > 500) { // Update every 500ms
                            lastProgressTime = now
                            val elapsed = (now - startTime) / 1000f
                            val speedKBps = if (elapsed > 0) (downloadedBytes / 1024f) / elapsed else 0f
                            val dlMB = downloadedBytes / (1024f * 1024f)
                            val pct = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt() else 0

                            withContext(Dispatchers.Main) {
                                onProgress(DownloadProgress(dlMB, totalMB, pct, speedKBps))
                            }
                        }
                    }
                }
            }

            // Temp → final
            if (tempFile.exists()) {
                tempFile.renameTo(destFile)
            }

            Log.d(TAG, "Download complete: ${destFile.absolutePath}")
            withContext(Dispatchers.Main) { onComplete() }

        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}")
            tempFile.delete()
            withContext(Dispatchers.Main) { onError(e.message ?: "Download failed") }
        }
    }

    /** Partial download resume support */
    fun getPartialSize(context: Context): Long {
        val tmp = File(getSavePath(context).parent, "$MODEL_FILENAME.tmp")
        return if (tmp.exists()) tmp.length() else 0L
    }

    /** Model delete karo (for retry) */
    fun deleteModel(context: Context) {
        getSavePath(context).delete()
        File(getSavePath(context).parent, "$MODEL_FILENAME.tmp").delete()
    }

    fun getFileSizeMB(context: Context): Float {
        val f = getSavePath(context)
        return if (f.exists()) f.length() / (1024f * 1024f) else 0f
    }
}
