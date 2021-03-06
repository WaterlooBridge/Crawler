package com.zhenl.crawler.download

import android.net.Uri
import android.util.Log
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.core.listener.DownloadListener2
import com.zhenl.crawler.utils.FileUtil
import java.io.File
import java.util.regex.Pattern

/**
 * Created by lin on 20-1-19.
 */
internal object M3U8ConfigDownloader {

    private const val TAG = "M3U8ConfigDownloader"

    internal val downloadList = arrayListOf<String>()

    /**
     * @return 如果返回空则不需要下载，如果返回的文件存在了，则开始下载，否则等待下载完成
     */
    fun start(entity: VideoDownloadEntity): File? {
        if (entity.status == DELETE) {
            return null
        }
        if (downloadList.contains(entity.originalUrl)) {
            return null
        }
        if (entity.createTime == 0L) {
            entity.createTime = System.currentTimeMillis()
        }
        entity.redirectUrl = ""
        val path = FileDownloader.getDownloadPath(entity.originalUrl)
        val config = FileDownloader.getConfigFile(entity.originalUrl)
        val realEntity = if (!config.exists()) {
            entity.toFile()
            entity
        } else {
            parseJsonToVideoDownloadEntity(config.readText()) ?: entity
        }
        if (entity.status == DELETE) {
            path.deleteRecursively()
            return null
        }
        val m3u8ListFile = File(path, "m3u8.list")
        return if (realEntity.status != COMPLETE) {//没有完成的才有必要下载
            if (!m3u8ListFile.exists()) {
                realEntity.status = PREPARE
                FileDownloader.downloadCallback.postValue(realEntity)
                entity.toFile()

                downloadM3U8File(path, realEntity)
            }
            m3u8ListFile
        } else {
            null
        }
    }


    /**
     * 下载单个文件
     */
    private fun downloadM3U8File(path: File, entity: VideoDownloadEntity) {
        if (entity.status == DELETE) {
            return
        }
        val fileName: String
        val url = if (entity.redirectUrl.isNotEmpty()) {//如果有了重定向的url
            fileName = "real.m3u8"
            entity.redirectUrl
        } else {//否则就用初始的url
            fileName = "original.m3u8"
            entity.originalUrl
        }
        Log.d(TAG, "downloadM3U8File-url=$url,fileName=$fileName")
        DownloadTask.Builder(url, path)
                .setFilename(fileName)
                .setAutoCallbackToUIThread(false)
                .build()
                .execute(object : DownloadListener2() {
                    override fun taskStart(task: DownloadTask) {
                        Log.d(TAG, "taskStart-->")
                        downloadList.add(entity.originalUrl)
                    }

                    override fun taskEnd(task: DownloadTask, cause: EndCause, realCause: java.lang.Exception?) {
                        Log.d(TAG, "taskEnd-->${cause.name},${realCause?.message}")
                        downloadList.remove(entity.originalUrl)
                        if (cause == EndCause.COMPLETED) {
                            getFileContent(path, entity)
                        } else {
                            entity.status = ERROR
                            entity.toFile()
                            FileDownloader.downloadCallback.postValue(entity)
                        }
                    }
                })
    }

    /**
     * 分析文件内容
     */
    private fun getFileContent(path: File, entity: VideoDownloadEntity) {
        if (entity.status == DELETE) {
            return
        }
        Log.d(TAG, "getFileContent---$entity")
        val url = if (entity.redirectUrl.isNotEmpty()) {//如果有了重定向的url
            entity.redirectUrl
        } else {//否则就用初始的url
            entity.originalUrl
        }
        val uri = Uri.parse(url)
        val realM3U8File = File(path, "real.m3u8")
        var file = realM3U8File
        if (!file.exists()) {//直接判断真实的m3u8文件是否存在，存在则读取
            file = File(path, "original.m3u8")
        }
        Log.d(TAG, "getFileContent---${file.name}")
        val prefix = "#EXT-X-KEY:"
        val p = Pattern.compile("(?<=URI=\").*?(?=\")")
        val list = file.readLines().filter { !it.startsWith("#") || it.startsWith(prefix) }//读取m3u8文件
        if (list.size > 1) {//直接的m3u8的ts链接
            entity.tsSize = list.size
            entity.toFile()
            if (file != realM3U8File)
                file.copyTo(realM3U8File)

            val m3u8ListFile = File(path, "m3u8.list")
            val localPlaylist = File(path, "localPlaylist.m3u8")
            val localDir = path.absolutePath
            file.readLines().forEach {
                var key: String? = null
                if (it.startsWith(prefix)) {
                    val m = p.matcher(it)
                    if (m.find())
                        key = m.group()
                }
                if (key == null && it.startsWith("#")) {
                    localPlaylist.appendText("$it\n")
                    return@forEach
                }
                var hls = key ?: it
                val ts = if (hls.startsWith("http")) {
                    hls
                } else if (!hls.startsWith("/")) {
                    url.substring(0, url.lastIndexOf("/") + 1) + hls
                } else {
                    "${uri.scheme}://${uri.host}${if (uri.port != -1) ":${uri.port}" else ""}$hls"
                }
                m3u8ListFile.appendText("$ts\n")
                hls = "$localDir/.ts/${FileUtil.md5(ts)}"
                key?.apply {
                    hls = it.replace(this, hls)
                }
                localPlaylist.appendText("$hls\n")
            }
            Log.d(TAG, "start--->$entity")
        } else if (list.size == 1) {//重定向
            var newUrl = list[0]
            newUrl = if (newUrl.startsWith("/")) {
                "${uri.scheme}://${uri.host}${if (uri.port != -1) ":${uri.port}" else ""}$newUrl"
            } else {
                url.substring(0, url.lastIndexOf("/") + 1) + newUrl
            }
            if (newUrl == entity.redirectUrl)
                return
            entity.redirectUrl = newUrl
            entity.toFile()
            downloadM3U8File(path, entity)
        }
    }

}