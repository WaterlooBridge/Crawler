package com.zhenl.crawler.download

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.zhenl.crawler.MyApplication
import com.zhenl.crawler.utils.FileUtil
import com.zhenl.violet.core.Dispatcher
import java.io.File

/**
 * Created by lin on 20-1-19.
 */
object FileDownloader {

    private const val TAG = "FileDownloader"

    val downloadCallback = MutableLiveData<VideoDownloadEntity>()//下载进度回调

    /**
     * 获取最顶层的下载目录
     */
    @JvmStatic
    fun getBaseDownloadPath(): File {
        val file = MyApplication.instance.getExternalFilesDir("m3u8Downloader")
                ?: File(MyApplication.instance.filesDir, "m3u8Downloader")
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

    /**
     * 获取根据链接得到的下载存储路径
     */
    @JvmStatic
    fun getDownloadPath(url: String): File {
        val file = File(getBaseDownloadPath(), FileUtil.md5FileName(url))
        if (!file.exists()) {
            file.mkdir()
        }
        return file
    }

    /**
     * 获取相关配置文件
     */
    @JvmStatic
    fun getConfigFile(url: String): File {
        val path = getDownloadPath(url)
        return File(path, "video.config")
    }

    /**
     * 获取相关配置文件
     */
    @JvmStatic
    fun getConfigFile(path: File): File {
        return File(path, "video.config")
    }

    @JvmStatic
    fun getLocalPlayFile(url: String): File {
        val path = getDownloadPath(url)
        return File(path, "localPlaylist.m3u8")
    }

    /**
     * 下载的入口
     */
    @JvmStatic
    internal fun downloadVideo(entity: VideoDownloadEntity) {
        if (entity.status == DELETE) {
            return
        }
        Toast.makeText(MyApplication.instance, "已加入下载队列", Toast.LENGTH_SHORT).show()
        if (entity.originalUrl.contains(".m3u8")) {
            downloadM3U8File(entity)
        } else {
            downloadSingleVideo(entity)
        }
    }

    @JvmStatic
    fun deleteVideo(entity: VideoDownloadEntity) {
        entity.status = DELETE
        getDownloadPath(entity.originalUrl).deleteRecursively()
    }

    @JvmStatic
    private fun downloadM3U8File(entity: VideoDownloadEntity) {
        if (entity.status == DELETE) {//删除状态的忽略
            Log.d(TAG, "downloadM3U8File---DELETE")
            return
        }
        Log.d(TAG, "--downloadM3U8File--${entity.originalUrl}")
        Dispatcher.getInstance().enqueue {
            val file = M3U8ConfigDownloader.start(entity)//准备下载列表
            if (file != null) {//需要下载
                Log.d(TAG, "file.exists()==>${file.exists()}")
                if (file.exists())
                    M3U8Downloader.bunchDownload(getDownloadPath(entity.originalUrl))
            } else {
                Log.d(TAG, "file===null")
            }
        }
    }

    @JvmStatic
    private fun downloadSingleVideo(entity: VideoDownloadEntity) {
        if (entity.status == DELETE) {//删除状态的忽略
            Log.d(TAG, "downloadSingleVideo---DELETE")
            return
        }
        SingleVideoDownloader.fileDownloader(entity)
    }
}