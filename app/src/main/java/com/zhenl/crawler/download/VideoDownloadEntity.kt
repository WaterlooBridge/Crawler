package com.zhenl.crawler.download

import androidx.media3.exoplayer.offline.Download
import com.google.gson.Gson
import com.zhenl.crawler.models.VideoModel

/**
 * Created by lin on 20-1-19.
 */

class VideoDownloadEntity(
        var originalUrl: String,//原始下载链接
        var name: String = "",//视频名称
        var subName: String = "",//视频子名称
        var downloadTask: Download
) : Comparable<VideoDownloadEntity> {

    override fun compareTo(other: VideoDownloadEntity) =
            other.downloadTask.startTimeMs.compareTo(this.downloadTask.startTimeMs)
}

fun VideoModel.toFile(downloadId: String) {
    val config = VideoDownloader.getVideoConfigFile(downloadId)
    config.writeText(Gson().toJson(this))
}

fun Download.toVideoDownloadEntity(): VideoDownloadEntity? {
    val config = VideoDownloader.getVideoConfigFile(request.id)
    if (!config.exists())
        return null
    val text = config.readText()
    if (text.isEmpty())
        return null

    val data = Gson().fromJson(text, VideoModel::class.java)
        ?: return null

    return VideoDownloadEntity(data.url, data.title ?: "", data.subtitle ?: "", this)
}
