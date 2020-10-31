package com.zhenl.crawler.download

import android.util.Log
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.SpeedCalculator
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.liulishuo.okdownload.core.listener.DownloadListener1
import com.liulishuo.okdownload.core.listener.assist.Listener1Assist

/**
 * Created by lin on 20-10-30.
 */
internal object SingleVideoDownloader {
    private val downloadList = arrayListOf<String>()
    private const val TAG = "SingleVideoDownloader"

    fun fileDownloader(entity: VideoDownloadEntity) {
        val path = FileDownloader.getDownloadPath(entity.originalUrl)
        if (entity.status == DELETE) {//如果是删除状态的则忽略
            path.deleteRecursively()
            return
        }
        if (downloadList.contains(entity.originalUrl)) {//避免重复下载
            Log.d(TAG, "contains---${entity.originalUrl},${entity.name}")
            return
        }
        entity.status = PREPARE
        entity.fileSize = 0
        entity.currentSize = 0
        FileDownloader.downloadCallback.postValue(entity)
        var lastCallback = 0L
        val CURRENT_PROGRESS = entity.originalUrl.hashCode()
        val speedCalculator = SpeedCalculator()

        Log.d(TAG, "downloadFile===>${path.absolutePath}")
        val task = DownloadTask.Builder(entity.originalUrl, path)
                .setFilename("localPlaylist.m3u8")
                .setPassIfAlreadyCompleted(true)
                .setMinIntervalMillisCallbackProcess(1000)
                .setConnectionCount(3)
                .build()
        task.enqueue(object : DownloadListener1() {
            override fun taskStart(task: DownloadTask, model: Listener1Assist.Listener1Model) {
                Log.d(TAG, "taskStart-->")
                entity.status = PREPARE
                entity.fileSize = 0
                entity.currentSize = 0
                entity.toFile()
                FileDownloader.downloadCallback.postValue(entity)
            }

            override fun taskEnd(
                    task: DownloadTask, cause: EndCause, realCause: Exception?,
                    model: Listener1Assist.Listener1Model
            ) {
                Log.d(TAG, "taskEnd-->${cause.name},${realCause?.message}")
                when (cause) {
                    EndCause.COMPLETED -> entity.status = COMPLETE
                    EndCause.CANCELED -> entity.status = PAUSE
                    else -> entity.status = ERROR
                }
                entity.toFile()
                FileDownloader.downloadCallback.postValue(entity)
                downloadList.remove(entity.originalUrl)
            }

            override fun progress(task: DownloadTask, currentOffset: Long, totalLength: Long) {
                val preOffset = (task.getTag(CURRENT_PROGRESS) as Long?) ?: 0
                speedCalculator.downloading(currentOffset - preOffset)
                entity.currentSize = currentOffset
                val now = System.currentTimeMillis()
                if (now - lastCallback > 1000) {
                    entity.currentProgress = if (totalLength > 0) currentOffset.toDouble() / totalLength else 0.0
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
                entity.currentSize += currentOffset
                entity.fileSize += totalLength.toInt()
                entity.toFile()
                FileDownloader.downloadCallback.postValue(entity)
            }

            override fun retry(task: DownloadTask, cause: ResumeFailedCause) {
            }
        })
        entity.downloadTask = task
        downloadList.add(entity.originalUrl)
    }
}