package com.zhenl.crawler.engines

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import android.util.Log
import android.webkit.*
import com.zhenl.crawler.Constants
import com.zhenl.crawler.MyApplication
import com.zhenl.crawler.models.DramasModel
import com.zhenl.crawler.models.MovieModel
import java.util.*
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

    abstract fun loadJs(): String

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    it.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                it.addJavascriptInterface(JsBridge(), "bridge")
                it.webViewClient = this
            }
        }
        val map: MutableMap<String, String?> = HashMap()
        map["referer"] = referer
        wv!!.loadUrl(url, map)
        referer = url
    }

    fun destroy() {
        isDestroy = true
        destroyWebView()
        callback = null
    }

    private fun destroyWebView() {
        if (wv != null) {
            wv!!.removeAllViews()
            wv!!.destroy()
            wv = null
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        handler.removeMessages(5)
        handler.sendEmptyMessageDelayed(5, 5000)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        Log.e(TAG, "[INFO:CONSOLE]$url")
        handler.removeMessages(5)
        view?.evaluateJavascript("javascript:" + loadJs(), null)
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        super.onReceivedError(view, request, error)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && request?.isForMainFrame == true)
            callback?.finish()
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        handler?.proceed()
    }

    protected fun loadJs(name: String?): String? {
        val context: Context = MyApplication.instance
        var js = context.getSharedPreferences("search_engine", Context.MODE_PRIVATE)
                .getString(name, null)
        if (!Constants.DEBUG && !TextUtils.isEmpty(js)) return js
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
                    callback!!.play(path)
                    destroy()
                }
                2 -> callback!!.finish()
                3 -> {
                    val url = msg.obj as String
                    val map: MutableMap<String, String?> = HashMap()
                    map["referer"] = referer
                    wv!!.loadUrl(url, map)
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
            msg.obj = url
            msg.sendToTarget()
        }

        @JavascriptInterface
        fun destroy() {
            handler.obtainMessage(2).sendToTarget()
        }

        @JavascriptInterface
        fun loadUrl(url: String?) {
            val msg = handler.obtainMessage(3)
            msg.obj = url
            msg.sendToTarget()
        }

        @JavascriptInterface
        fun reload(url: String?) {
            val msg = handler.obtainMessage(4)
            msg.obj = url
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