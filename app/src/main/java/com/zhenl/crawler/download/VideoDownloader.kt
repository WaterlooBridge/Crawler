package com.zhenl.crawler.download

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.*
import com.zhenl.crawler.BuildConfig
import com.zhenl.crawler.Constants
import com.zhenl.crawler.MyApplication
import com.zhenl.crawler.models.VideoModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.connection.RealCall
import tv.zhenl.media.DataSourceWrapper
import tv.zhenl.media.IPCPlayerControl
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors

/**
 * Created by lin on 2023/5/7.
 */
object VideoDownloader : DownloadManager.Listener {

    private const val TAG = "VideoDownloadHelper"
    private const val DOWNLOAD_CONTENT_DIRECTORY = "downloads"

    private lateinit var downloadManager: DownloadManager
    private lateinit var downloadNotificationHelper: DownloadNotificationHelper

    private val context = MyApplication.instance

    val downloadCallback = MutableLiveData<Download>()

    fun getDownloadManager(): DownloadManager {
        if (::downloadManager.isInitialized) return downloadManager
        ensureDownloadManagerInitialized()
        return downloadManager
    }

    @Synchronized
    private fun ensureDownloadManagerInitialized() {
        if (::downloadManager.isInitialized) return
        val databaseProvider = StandaloneDatabaseProvider(context)
        val cache = SimpleCache(getDownloadDirectory(), NoOpCacheEvictor(), databaseProvider)
        IPCPlayerControl.downloadCache = cache
        downloadManager = DownloadManager(
            context,
            databaseProvider,
            cache,
            getHttpDataSourceFactory(),
            Executors.newCachedThreadPool()
        )
        downloadManager.addListener(this)
    }

    private fun getDownloadDirectory(): File {
        var downloadDirectory = context.getExternalFilesDir(null)
        if (downloadDirectory == null) {
            downloadDirectory = context.filesDir
        }
        return File(downloadDirectory, DOWNLOAD_CONTENT_DIRECTORY)
    }

    private fun getHttpDataSourceFactory(): DataSource.Factory {
        val okhttpDataSourceFactory = OkHttpDataSource.Factory { request: Request ->
            RealCall(
                OkHttpClient.Builder().build(),
                request, false
            )
        }
        okhttpDataSourceFactory.setUserAgent(Constants.USER_AGENT)
        return DataSourceWrapper.Factory(okhttpDataSourceFactory)
    }

    fun getDownloadNotificationHelper(): DownloadNotificationHelper {
        if (::downloadNotificationHelper.isInitialized) return downloadNotificationHelper
        downloadNotificationHelper = DownloadNotificationHelper(context, BuildConfig.APPLICATION_ID)
        return downloadNotificationHelper
    }

    fun getVideoConfigFile(downloadId: String): File {
        var downloadDirectory = context.getExternalFilesDir(null)
        if (downloadDirectory == null) {
            downloadDirectory = context.filesDir
        }
        val dir = File(downloadDirectory, "videoConfig")
        if (!dir.exists()) dir.mkdir()
        return File(dir, downloadId)
    }

    fun loadDownloads(): List<Download> {
        val downloads = arrayListOf<Download>()
        try {
            getDownloadManager().downloadIndex.getDownloads().use { loadedDownloads ->
                while (loadedDownloads.moveToNext()) {
                    downloads.add(loadedDownloads.download)
                }
            }
            getDownloadManager().currentDownloads.forEach { current ->
                val index = downloads.indexOfFirst { it.request.id == current.request.id }
                downloads[index] = current
            }
        } catch (e: IOException) {
            Log.w(TAG, "Failed to query downloads", e)
        }
        return downloads
    }

    fun downloadVideo(model: VideoModel) {
        val videoModel = VideoModel(model.title, model.subtitle, model.videoPath ?: return)
        val id = System.currentTimeMillis().toString()
        videoModel.toFile(id)
        val downloadRequest = DownloadRequest.Builder(id, Uri.parse(videoModel.url)).build()
        DownloadService.sendAddDownload(
            context,
            VideoDownloadService::class.java,
            downloadRequest,
            true
        )
        Toast.makeText(context, "已加入下载队列", Toast.LENGTH_SHORT).show()
    }

    fun startDownload(download: Download) {
        DownloadService.sendSetStopReason(
            context,
            VideoDownloadService::class.java,
            download.request.id,
            Download.STOP_REASON_NONE,
            true
        )
    }

    fun stopDownload(download: Download) {
        DownloadService.sendSetStopReason(
            context,
            VideoDownloadService::class.java,
            download.request.id,
            1,
            true
        )
    }

    fun removeDownload(download: Download) {
        val downloadId = download.request.id
        DownloadService.sendRemoveDownload(
            context,
            VideoDownloadService::class.java,
            downloadId,
            true
        )
        GlobalScope.launch { getVideoConfigFile(downloadId).delete() }
    }

    override fun onDownloadChanged(
        downloadManager: DownloadManager,
        download: Download,
        finalException: Exception?
    ) {
        downloadCallback.postValue(download)
    }
}
