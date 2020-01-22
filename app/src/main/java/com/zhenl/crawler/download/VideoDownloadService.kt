package com.zhenl.crawler.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Message
import androidx.core.app.NotificationCompat
import com.zhenl.crawler.BuildConfig
import com.zhenl.crawler.IVideoDownloader
import com.zhenl.crawler.MyApplication
import com.zhenl.crawler.utils.FileUtil
import java.lang.ref.WeakReference

class VideoDownloadService : Service() {

    companion object {

        private const val CHANNEL_ID = BuildConfig.APPLICATION_ID
        private const val NOTIFICATION_ID = 10086
        private const val INTERVAL = 10000L

        private val map = HashMap<String, VideoDownloadEntity>()

        fun downloadVideo(url: String) {
            val context = MyApplication.getInstance()
            val intent = Intent(context, VideoDownloadService::class.java)
            context.bindService(intent, object : ServiceConnection {
                override fun onServiceDisconnected(name: ComponentName?) {
                }

                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    IVideoDownloader.Stub.asInterface(service).downloadVideo(url)
                }
            }, Context.BIND_AUTO_CREATE)
        }

        fun downloadVideo(entity: VideoDownloadEntity) {
            val url = entity.originalUrl
            map[url] = entity
            downloadVideo(url)
        }
    }

    private val binder = ViewDownloaderStub()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        ForegroundHandler(this).sendEmptyMessageDelayed(0, INTERVAL)
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private inner class ViewDownloaderStub : IVideoDownloader.Stub() {

        override fun downloadVideo(url: String?) {
            url?.let {
                val entity = map.remove(it)
                        ?: VideoDownloadEntity(it, FileUtil.getFileNameFromUrl(it)).apply { toFile() }
                FileDownloader.downloadVideo(entity)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return
        val channel = NotificationChannel(CHANNEL_ID, "视频下载", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
    }

    private class ForegroundHandler(service: Service) : Handler() {

        private val wr = WeakReference(service)
        private val builder = NotificationCompat.Builder(MyApplication.getInstance(), CHANNEL_ID)

        private var foreground = false

        override fun handleMessage(msg: Message?) {
            val service = wr.get() ?: return
            if (M3U8ConfigDownloader.downloadList.isEmpty()
                    && M3U8Downloader.downloadList.isEmpty()) {
                if (foreground) {
                    service.stopForeground(true)
                    foreground = false
                }
            } else if (!foreground) {
                service.startForeground(NOTIFICATION_ID, builder.build())
                foreground = true
            }
            sendEmptyMessageDelayed(0, INTERVAL)
        }
    }
}
