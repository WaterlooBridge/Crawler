package com.zhenl.crawler.utils

import com.zhenl.crawler.Constants
import okhttp3.*


object HttpUtil {

    private val singleClient: OkHttpClient

    init {
        val okHttpClientBuilder = OkHttpClient.Builder()
        singleClient = okHttpClientBuilder.build()
    }

    fun getAsync(url: String, callback: Callback): Call {
        val request = Request.Builder().url(url).get()
        val call = singleClient.newCall(request.build())
        call.enqueue(callback)
        return call
    }

    fun getSync(url: String): String? {
        val request = Request.Builder().url(url).get()
        try {
            val response = singleClient.newCall(request.build()).execute()
            if (response.code == 200)
                return response.body?.string()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun loadWebResourceResponse(url: String): Response? {
        val request = Request.Builder().url(url).get().addHeader("user-agent", Constants.USER_AGENT)
        try {
            return singleClient.newCall(request.build()).execute()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}