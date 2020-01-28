package com.zhenl.crawler.engines;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.zhenl.crawler.Constants;
import com.zhenl.crawler.MyApplication;
import com.zhenl.crawler.SearchActivity;
import com.zhenl.crawler.models.DramasModel;
import com.zhenl.crawler.models.MovieModel;
import com.zhenl.violet.core.Dispatcher;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by lin on 2018/8/25.
 */
public class SearchEngineImpl3 extends SearchEngine {

    private static final String TAG = "SearchEngineImpl3";

    private static final int MAX_REQUEST_TIME = 3;

    private static volatile boolean isPinging;
    private static volatile int requestTime;
    static volatile String baseUrl;

    private static Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            isPinging = false;
            loadLink();
        }
    };

    private static void loadLink() {
        if (isPinging || !TextUtils.isEmpty(baseUrl))
            return;
        WebView wv = new WebView(MyApplication.getInstance());
        wv.getSettings().setJavaScriptEnabled(true);
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                isPinging = true;
                handler.removeMessages(0);
                handler.sendEmptyMessageDelayed(0, 30000);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                wv.evaluateJavascript("document.getElementsByClassName('link')[0].innerHTML", SearchEngineImpl3::ping);
            }
        });
        wv.loadUrl(Constants.API_HOST3);
    }

    private static void ping(String html) {
        String decodeHtml = html.replace("\\u003C", "<");
        Log.e("SearchEngineImpl3", decodeHtml);
        Dispatcher.getInstance().enqueue(() -> {
            Elements elements = Jsoup.parse(decodeHtml).select("a");
            for (Element e : elements) {
                String url = e.attr("href").replace("\\\"", "");
                try {
                    Connection.Response res = Jsoup.connect(url).timeout(5000).execute();
                    if (res.statusCode() == 200) {
                        baseUrl = url;
                        break;
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "", t);
                }
            }
            isPinging = false;
            handler.removeMessages(0);
            if (TextUtils.isEmpty(baseUrl) && ++requestTime < MAX_REQUEST_TIME)
                handler.post(SearchEngineImpl3::loadLink);
        });
    }

    SearchEngineImpl3() {
        requestTime = 0;
        loadLink();
    }

    @Override
    public void search(int seqNum, String keyword, SearchActivity.SearchHandler handler) throws Exception {
        if (baseUrl == null)
            return;
        int page = handler.pageNum;
        String url = baseUrl + "/search";
        Connection.Response res = Jsoup.connect(url).method(Connection.Method.GET).data("wd", keyword)
                .data("page", String.valueOf(page))
                .userAgent(Constants.USER_AGENT)
                .followRedirects(false).execute();
        while (res.hasHeader("Location")) {
            String location = res.header("Location");
            if (location != null && location.startsWith("http:/") && location.charAt(6) != '/')
                location = location.substring(6);
            String redir = StringUtil.resolve(url, location);
            Connection connection = Jsoup.connect(redir).method(Connection.Method.GET).data("wd", keyword)
                    .data("page", String.valueOf(page))
                    .userAgent(Constants.USER_AGENT);
            for (Map.Entry<String, String> cookie : res.cookies().entrySet())
                connection.cookie(cookie.getKey(), cookie.getValue());
            res = connection.followRedirects(false).execute();
        }
        Document document = res.parse();
        if (handler.recSeqNum > seqNum)
            return;
        handler.recSeqNum = seqNum;
        Elements elements = document.select(".p1.m1");
        List<MovieModel> list = new ArrayList<>();
        for (Element element : elements) {
            MovieModel model = new MovieModel();
            model.url = element.select("a").attr("href");
            model.setImg(element.select("img").attr("data-original").replace("\n", ""));
            model.title = element.select(".name").text();
            model.date = element.select(".actor").first().text();
            if (!"VIP".equals(model.date))
                list.add(model);
        }
        Message msg = handler.obtainMessage(page);
        msg.obj = list;
        msg.sendToTarget();
    }

    @Override
    public void detail(String url, DetailCallback callback) throws Exception {
        Document document = Jsoup.connect(baseUrl + url).userAgent(Constants.USER_AGENT).get();
        String img = document.select(".ct-l").select("img").attr("data-original")
                .replace("\n", "");
        String summary = document.select(".tab-jq.ctc").text();
        Elements elements = document.select(".show_player_gogo a");
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
    public void load(String url, Callback callback) {
        this.url = url;
        this.callback = callback;
        load(url);
    }

    @Override
    public String loadJs() {
        return loadJs("inject3");
    }
}