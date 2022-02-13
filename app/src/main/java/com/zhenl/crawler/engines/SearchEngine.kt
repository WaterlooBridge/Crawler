package com.zhenl.crawler.engines

import android.annotation.SuppressLint
import android.content.Context
import android.net.http.SslError
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import android.webkit.*
import com.zhenl.crawler.Constants
import com.zhenl.crawler.MyApplication
import com.zhenl.crawler.R
import com.zhenl.crawler.models.DramasModel
import com.zhenl.crawler.models.MovieModel
import com.zhenl.crawler.utils.HttpUtil
import java.io.ByteArrayInputStream
import java.util.regex.Pattern

/**
 * Created by lin on 2018/8/22.
 */
abstract class SearchEngine : WebViewClient() {

    protected var url: String? = null
    protected var referer: String? = null
    protected var isDestroy = false
    protected var callback: Callback? = null

    private var wv: WebView? = null

    @Throws(Exception::class)
    abstract suspend fun search(page: Int, keyword: String?): List<MovieModel>

    @Throws(Exception::class)
    abstract fun detail(url: String?, callback: DetailCallback?)

    abstract fun load(url: String?, callback: Callback?)

    interface Callback {
        fun play(path: String)
        fun finish()
    }

    fun interface DetailCallback {
        fun onSuccess(img: String?, summary: String?, list: List<DramasModel>?)
    }

    protected fun load(url: String?) {
        if (isDestroy) return
        if (wv == null) {
            wv = WebView(MyApplication.instance).also {
                it.settings.javaScriptEnabled = true
                it.settings.blockNetworkImage = true
                it.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                it.addJavascriptInterface(JsBridge(), "bridge")
                it.webViewClient = this
            }
        }
        val map: MutableMap<String, String?> = HashMap()
        map["referer"] = referer
        wv?.loadUrl(url, map)
        referer = url
    }

    fun destroy() {
        isDestroy = true
        destroyWebView()
        callback = null
    }

    private fun destroyWebView() {
        wv?.let {
            it.removeAllViews()
            it.destroy()
            wv = null
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        view?.evaluateJavascript("javascript:" + loadJs(), null)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val scheme = request?.url?.scheme
        if (scheme == "http" || scheme == "https")
            return super.shouldOverrideUrlLoading(view, request)
        return true
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        val url = request.url.toString()
        val accept = request.requestHeaders["Accept"]
        if (url.contains("DPlayer.min.js") || url.contains("aliplayer-min.js")) {
            return loadPlayerJs(R.raw.dplayer)
        } else if (accept?.startsWith("text/html") == true) {
            try {
                val response = HttpUtil.loadWebResourceResponse(url, request.requestHeaders)
                if (response?.code != 200)
                    return super.shouldInterceptRequest(view, request)
                val data = response.body?.string() + loadJs("scan")
                return WebResourceResponse(
                    "text/html", "utf-8",
                    ByteArrayInputStream(data.toByteArray())
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return super.shouldInterceptRequest(view, request)
    }

    private fun loadPlayerJs(resId: Int): WebResourceResponse {
        return WebResourceResponse(
            "application/javascript", "utf-8",
            MyApplication.instance.resources.openRawResource(resId)
        )
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true)
            callback?.finish()
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        handler?.proceed()
    }

    protected fun loadJs(): String {
        return loadJs("inject") ?: ""
    }

    protected fun loadJs(name: String?): String? {
        val context: Context = MyApplication.instance
        val sp = context.getSharedPreferences("search_engine", Context.MODE_PRIVATE)
        val versionCode = sp.getInt("versionCode", 0)
        var js = sp.getString(name, null)
        if (!Constants.DEBUG && !TextUtils.isEmpty(js) && versionCode >= Constants.API_VERSION) return js
        val resId = context.resources.getIdentifier(name, "raw", context.packageName)
        try {
            val inputStream = context.resources.openRawResource(resId)
            val bytes = ByteArray(inputStream.available())
            inputStream.read(bytes)
            js = String(bytes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return js
    }

    @JvmField
    @SuppressLint("HandlerLeak")
    var handler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (isDestroy || callback == null) return
            when (msg.what) {
                0 -> load(url)
                1 -> {
                    val path = msg.obj as String
                    callback?.play(path)
                    destroy()
                }
                2 -> callback?.finish()
                3 -> {
                    val url = msg.obj as String
                    val map: MutableMap<String, String?> = HashMap()
                    map["referer"] = referer
                    wv?.loadUrl(url, map)
                    referer = url
                }
                4 -> {
                    url = msg.obj as String
                    load(url, callback)
                }
                5 -> wv?.evaluateJavascript("javascript:" + loadJs(), null)
            }
        }
    }

    inner class JsBridge {
        @JavascriptInterface
        fun loadVideo(url: String?) {
            val msg = handler.obtainMessage(1)
            msg.obj = url ?: return
            msg.sendToTarget()
        }

        @JavascriptInterface
        fun destroy() {
            handler.obtainMessage(2).sendToTarget()
        }

        @JavascriptInterface
        fun loadUrl(url: String?) {
            val msg = handler.obtainMessage(3)
            msg.obj = url ?: return
            msg.sendToTarget()
        }

        @JavascriptInterface
        fun reload(url: String?) {
            val msg = handler.obtainMessage(4)
            msg.obj = url ?: return
            msg.sendToTarget()
        }
    }

    companion object {
        private const val TAG = "SearchEngine"

        private val backgroundImagePattern = Pattern.compile("(?<=url\\().*?(?=\\))")

        fun String.findBackgroundImage(): String {
            val matcher = backgroundImagePattern.matcher(this)
            return if (matcher.find()) matcher.group() else ""
        }
    }
}