package com.zhenl.crawler.utils

import com.zhenl.crawler.Constants
import okhttp3.*
import okhttp3.Headers.Companion.toHeaders


object HttpUtil {

    private val singleClient: OkHttpClient
    private val webClient: OkHttpClient

    init {
        val okHttpClientBuilder = OkHttpClient.Builder()
        singleClient = okHttpClientBuilder.build()
        webClient = okHttpClientBuilder.followRedirects(false).followSslRedirects(false).build()
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

    fun loadWebResourceResponse(url: String, headers: MutableMap<String, String>): Response? {
        headers["User-Agent"] = Constants.USER_AGENT
        val request = Request.Builder().url(url).get().headers(headers.toHeaders())
        try {
            return webClient.newCall(request.build()).execute()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}