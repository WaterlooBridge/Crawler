package com.zhenl.crawler.utils

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
}