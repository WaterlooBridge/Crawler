package com.zhenl.crawler.utils

import android.net.Uri
import java.net.URL

/**
 * Created by lin on 21-5-29.
 */
object UrlHelper {

    fun makeAbsoluteUrl(basePath: String, relativePath: String): String {
        return try {
            val baseUrl = URL(basePath)
            val absoluteUrl = URL(baseUrl, relativePath)
            absoluteUrl.toString()
        } catch (e: Exception) {
            basePath + relativePath
        }
    }

    fun String.encode(): String {
        return Uri.encode(this, ",/?:@&=+$#%")
    }

    fun String.toRefererHeader(): Map<String, String>? {
        if (this.contains("chaxun.truechat365.com"))
            return mapOf("Referer" to this)
        return null
    }
}