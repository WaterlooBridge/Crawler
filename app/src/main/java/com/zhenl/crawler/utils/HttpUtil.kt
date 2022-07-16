package com.zhenl.crawler.utils

import android.annotation.SuppressLint
import com.zhenl.crawler.Constants
import okhttp3.*
import okhttp3.Headers.Companion.toHeaders
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager


object HttpUtil {

    private val singleClient: OkHttpClient
    private val webClient: OkHttpClient

    init {
        val okHttpClientBuilder = OkHttpClient.Builder()
        singleClient = okHttpClientBuilder.build()
        webClient = okHttpClientBuilder.followRedirects(false).followSslRedirects(false)
            .sslSocketFactory(ignoreSSLError(), trustAllCerts()).build()
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

    fun ignoreSSLError(): SSLSocketFactory {
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, arrayOf(trustAllCerts()), SecureRandom())
        return sslContext.socketFactory
    }

    @SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager")
    fun trustAllCerts(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
            }
        }
    }
}