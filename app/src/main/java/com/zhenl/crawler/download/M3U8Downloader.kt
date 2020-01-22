package com.zhenl.crawler.download

import android.util.Log
import com.liulishuo.okdownload.DownloadContext
import com.liulishuo.okdownload.DownloadContextListener
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.SpeedCalculator
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.liulishuo.okdownload.core.listener.DownloadListener1
import com.liulishuo.okdownload.core.listener.assist.Listener1Assist
import java.io.File

/**
 * Created by lin on 20-1-19.
 */
internal object M3U8Downloader {

    private const val TAG = "M3U8Downloader"

    internal val downloadList = arrayListOf<String>()

    fun bunchDownload(path: File) {
        val config = FileDownloader.getConfigFile(path)
        Log.d(TAG, "config==>${config.readText()}")
        val entity = parseJsonToVideoDownloadEntity(config.readText()) ?: return

        if (entity.status == DELETE) {
            path.deleteRecursively()
            return
        }

        if (downloadList.contains(entity.originalUrl))
            return

        var lastCallback = 0L
        val CURRENT_PROGRESS = entity.originalUrl.hashCode()
        val speedCalculator = SpeedCalculator()
        val listener = object : DownloadListener1() {
            override fun taskStart(
                    task: DownloadTask, model: Listener1Assist.Listener1Model
            ) {
            }

            override fun taskEnd(
                    task: DownloadTask, cause: EndCause, realCause: Exception?,
                    model: Listener1Assist.Listener1Model
            ) {
            }

            override fun progress(
                    task: DownloadTask, currentOffset: Long, totalLength: Long
            ) {
                val preOffset = (task.getTag(CURRENT_PROGRESS) as Long?) ?: 0
                speedCalculator.downloading(currentOffset - preOffset)
                val now = System.currentTimeMillis()
                if (now - lastCallback > 1000) {
                    entity.currentSpeed = speedCalculator.speed() ?: ""
                    entity.status = DOWNLOADING
                    entity.toFile()
                    FileDownloader.downloadCallback.postValue(entity)
                    lastCallback = now
                }
                task.addTag(CURRENT_PROGRESS, currentOffset)
            }

            override fun connected(
                    task: DownloadTask, blockCount: Int, currentOffset: Long, totalLength: Long
            ) {
            }

            override fun retry(task: DownloadTask, cause: ResumeFailedCause) {
            }
        }

        Log.d(TAG, "bunchDownload")
        val m3u8ListFile = File(path, "m3u8.list")
        val urls = m3u8ListFile.readLines()
        val tsDirectory = File(path, ".ts")
        if (!tsDirectory.exists()) {
            tsDirectory.mkdir()
        }
        val builder = DownloadContext.QueueSet()
                .setParentPathFile(tsDirectory)
                .setMinIntervalMillisCallbackProcess(1000)
                .setPassIfAlreadyCompleted(true)
                .commit()
        Log.d(TAG, "ts.size===>${urls.size}")
        if (entity.fileSize > urls.size)
            entity.fileSize = 0
        urls.subList(entity.fileSize, urls.size).forEach { builder.bind(it) }
        val downloadContext = builder.setListener(object : DownloadContextListener {
            override fun taskEnd(
                    context: DownloadContext, task: DownloadTask, cause: EndCause,
                    realCause: Exception?, remainCount: Int
            ) {
                if (context.isStarted && cause == EndCause.COMPLETED) {
                    val progress = 1 - remainCount.toDouble() / urls.size
                    entity.status = DOWNLOADING
                    entity.currentProgress = progress
                    entity.fileSize++
                    entity.currentSize += task.file?.length() ?: 0
                    val now = System.currentTimeMillis()
                    if (now - lastCallback > 1000) {
                        FileDownloader.downloadCallback.postValue(entity)
                        lastCallback = now
                    }
                    entity.toFile()
                }
            }

            override fun queueEnd(context: DownloadContext) {
                Log.d(TAG, "queueEnd")
                downloadList.remove(entity.originalUrl)
                if (entity.status == DELETE)
                    return
                when (entity.currentProgress) {
                    1.0 -> entity.status = COMPLETE
                    0.0 -> entity.status = ERROR
                    else -> entity.status = PAUSE
                }
                entity.toFile()
                FileDownloader.downloadCallback.postValue(entity)
            }
        }).build()
        entity.downloadContext = downloadContext
        downloadContext.startOnSerial(listener)
        downloadList.add(entity.originalUrl)
        entity.status = DOWNLOADING
        FileDownloader.downloadCallback.postValue(entity)
    }
}