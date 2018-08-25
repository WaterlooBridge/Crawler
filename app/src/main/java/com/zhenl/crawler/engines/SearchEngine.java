package com.zhenl.crawler.engines;

import android.annotation.SuppressLint;
import android.net.http.SslError;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.zhenl.crawler.MyApplication;
import com.zhenl.crawler.SearchActivity;
import com.zhenl.crawler.models.DramasModel;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lin on 2018/8/22.
 */
public abstract class SearchEngine extends WebViewClient {

    private static final String TAG = "SearchEngine";

    protected String url;
    protected String referer;
    protected boolean isDestroy;
    protected Callback callback;

    private WebView wv;

    public abstract void search(int seqNum, String keyword, SearchActivity.SearchHandler handler) throws Exception;

    public abstract void detail(String url, DetailCallback callback) throws Exception;

    public abstract void load(String url, Callback callback);

    public abstract String loadJs();

    public interface Callback {
        void play(String path);

        void finish();
    }

    public interface DetailCallback {
        void onSuccess(String img, String summary, List<DramasModel> list);
    }

    protected void load(String url) {
        if (isDestroy)
            return;
        if (wv == null) {
            wv = new WebView(MyApplication.getInstance());
            wv.getSettings().setJavaScriptEnabled(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                wv.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            wv.addJavascriptInterface(new JsBridge(), "bridge");
            wv.setWebViewClient(this);
        }
        Map<String, String> map = new HashMap<>();
        map.put("referer", referer);
        wv.loadUrl(url, map);
        referer = url;
    }

    public void destroy() {
        isDestroy = true;
        destroyWebView();
        callback = null;
    }

    private void destroyWebView() {
        if (wv != null) {
            wv.removeAllViews();
            wv.destroy();
            wv = null;
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        Log.e(TAG, "[INFO:CONSOLE]" + url);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            view.evaluateJavascript("javascript:" + loadJs(), null);
        }
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        if (url.contains(".jpg") || url.contains(".png") || url.contains(".gif"))
            return new WebResourceResponse("text/html", "utf-8", new ByteArrayInputStream("".getBytes()));
        else
            return super.shouldInterceptRequest(view, url);
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        handler.proceed();
    }

    protected String loadJs(int resId) {
        String js = null;
        try {
            InputStream inputStream = MyApplication.getInstance().getResources().openRawResource(resId);
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            js = new String(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return js;
    }

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (isDestroy || callback == null)
                return;
            switch (msg.what) {
                case 0:
                    load(url);
                    break;
                case 1:
                    String path = (String) msg.obj;
                    callback.play(path);
                    destroy();
                    break;
                case 2:
                    callback.finish();
                    break;
                case 3:
                    String url = (String) msg.obj;
                    Map<String, String> map = new HashMap<>();
                    map.put("referer", referer);
                    wv.loadUrl(url, map);
                    referer = url;
                    break;
                case 4:
                    url = (String) msg.obj;
                    load(url, callback);
                    break;
            }
        }
    };

    public class JsBridge {

        @JavascriptInterface
        public void loadVideo(String url) {
            Message msg = handler.obtainMessage(1);
            msg.obj = url;
            msg.sendToTarget();
        }

        @JavascriptInterface
        public void destroy() {
            handler.obtainMessage(2).sendToTarget();
        }

        @JavascriptInterface
        public void loadUrl(String url) {
            Message msg = handler.obtainMessage(3);
            msg.obj = url;
            msg.sendToTarget();
        }

        @JavascriptInterface
        public void reload(String url) {
            Message msg = handler.obtainMessage(4);
            msg.obj = url;
            msg.sendToTarget();
        }
    }
}
