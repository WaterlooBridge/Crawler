package com.zhenl.crawler.engines;

import android.annotation.SuppressLint;
import android.content.Context;
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

import com.zhenl.crawler.Constants;
import com.zhenl.crawler.R;
import com.zhenl.crawler.SearchActivity;
import com.zhenl.crawler.models.DramasModel;
import com.zhenl.crawler.models.MovieModel;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lin on 2018/8/22.
 */
public class SearchEngineImpl2 implements SearchEngine {

    private static String js;
    private final String TAG = getClass().getSimpleName();

    private Context context;
    private String url;
    private Callback callback;
    private boolean isDestroy;
    private WebView wv;

    @Override
    public void search(String keyword, SearchActivity.SearchHandler handler) throws Exception {
        Document document = Jsoup.connect(Constants.API_HOST2 + "/index.php?s=vod-search").data("wd", keyword).post();
        if (handler.recSeqNum > handler.seqNum)
            return;
        handler.recSeqNum = handler.seqNum;
        Elements elements = document.select(".mui-table-view-cell");
        List<MovieModel> list = new ArrayList<>();
        for (Element element : elements) {
            MovieModel model = new MovieModel();
            model.url = element.select("a").attr("href");
            model.img = Constants.API_HOST2 + element.select("img").attr("data-original");
            model.title = element.select(".type-title").text();
            model.date = element.select("span").text();
            list.add(model);
        }
        Message msg = handler.obtainMessage(0);
        msg.obj = list;
        msg.sendToTarget();
    }

    @Override
    public void detail(String url, DetailCallback callback) throws Exception{
        Document document = Jsoup.connect(Constants.API_HOST2 + url).get();
        String img = null;
        String summary = document.select(".tab-jq").text();
        Elements elements = document.select(".ptab a");
        List<DramasModel> list = new ArrayList<>();
        for (Element element : elements) {
            DramasModel model = new DramasModel();
            model.text = element.text();
            model.url = element.attr("href");
            list.add(model);
        }
        if (callback != null)
            callback.onSuccess(img, summary, list);
    }

    @Override
    public void load(Context context, String url, Callback callback) {
        this.context = context;
        this.url = url;
        this.callback = callback;
        if (js == null) {
            try {
                InputStream inputStream = context.getResources().openRawResource(R.raw.inject2);
                byte[] bytes = new byte[inputStream.available()];
                inputStream.read(bytes);
                js = new String(bytes);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        load(url);
    }

    private void load(String url) {
        if (isDestroy)
            return;
        if (wv == null) {
            wv = new WebView(context);
            wv.getSettings().setJavaScriptEnabled(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                wv.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            wv.addJavascriptInterface(new JsBridge(), "bridge");
            wv.setWebViewClient(new WebViewClient() {

                @Override
                public void onPageFinished(WebView view, String url) {
                    Log.e(TAG, "[INFO:CONSOLE]" + url);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        view.evaluateJavascript("javascript:" + js, null);
                    }
                }

                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                    if (true) {
                        return super.shouldInterceptRequest(view, url);
                    } else
                        return new WebResourceResponse("text/html", "utf-8", new ByteArrayInputStream("".getBytes()));
                }

                @Override
                public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                    handler.proceed();
                }
            });
        }
        Map<String, String> map = new HashMap<>();
        wv.loadUrl(url, map);
    }

    @Override
    public void destroy() {
        isDestroy = true;
        destroyWebView();
        context = null;
        callback = null;
    }

    private void destroyWebView() {
        if (wv != null) {
            wv.removeAllViews();
            wv.destroy();
            wv = null;
        }
    }

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (isDestroy)
                return;
            switch (msg.what) {
                case 0:
                    load(url);
                    break;
                case 1:
                    String path = (String) msg.obj;
                    if (callback != null)
                        callback.play(path);
                    destroy();
                    break;
                case 2:
                    if (callback != null)
                        callback.finish();
                    break;
                case 3:
                    wv.loadUrl((String) msg.obj);
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
    }
}
