package com.zhenl.crawler.utils

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
}