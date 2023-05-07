package com.zhenl.crawler.download

import android.app.Notification
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Scheduler
import com.zhenl.crawler.BuildConfig
import com.zhenl.crawler.R


class VideoDownloadService : DownloadService(
    1,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    BuildConfig.APPLICATION_ID,
    R.string.exo_download_notification_channel_name,
    0
) {

    override fun getDownloadManager(): DownloadManager {
        return VideoDownloader.getDownloadManager()
    }

    override fun getScheduler(): Scheduler {
        return PlatformScheduler(this, 1)
    }

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int
    ): Notification {
        return VideoDownloader.getDownloadNotificationHelper().buildProgressNotification(
            this, R.mipmap.bilibili, null, null, downloads, notMetRequirements
        )
    }
}
