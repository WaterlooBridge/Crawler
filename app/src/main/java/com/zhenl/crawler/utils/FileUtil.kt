package com.zhenl.crawler.utils

import tv.danmaku.ijk.media.cache.ProxyCacheUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by lin on 20-1-21.
 */
object FileUtil {

    fun getFileNameFromUrl(url: String): String {
        return if (url.contains("?")) {
            url.substring(url.lastIndexOf("/") + 1, url.indexOf("?"))
        } else {
            url.substring(url.lastIndexOf("/") + 1)
        }
    }

    fun md5(url: String): String {
        return ProxyCacheUtils.computeMD5(url)
    }

    private val units = arrayOf("B", "KB", "MB", "GB", "TB")
    private val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun getFormatSize(size: Double): String {
        var sizeUnit = size
        var index = 0
        while (sizeUnit > 1024 && index < 4) {
            sizeUnit /= 1024.0
            index++
        }
        return String.format(Locale.getDefault(), "%.2f %s", sizeUnit, units[index])
    }
}