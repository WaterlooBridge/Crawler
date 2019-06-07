package com.zhenl.crawler.engines;

import android.os.Build;
import android.os.Message;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import com.zhenl.crawler.BuildConfig;
import com.zhenl.crawler.Constants;
import com.zhenl.crawler.R;
import com.zhenl.crawler.SearchActivity;
import com.zhenl.crawler.models.DramasModel;
import com.zhenl.crawler.models.MovieModel;
import com.zhenl.violet.core.Dispatcher;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lin on 2018/8/22.
 */
public class SearchEngineImpl1 extends SearchEngine {

    private static String js;

    @Override
    public void search(int seqNum, String keyword, SearchActivity.SearchHandler handler) throws Exception {
        Document document = Jsoup.connect(Constants.API_HOST + "/search?wd=" + keyword).userAgent(Constants.USER_AGENT).get();
        if (handler.recSeqNum > seqNum)
            return;
        handler.recSeqNum = seqNum;
        Elements elements = document.select(".p1");
        List<MovieModel> list = new ArrayList<>();
        for (Element element : elements) {
            MovieModel model = new MovieModel();
            model.url = element.select("a").attr("href");
            model.setImg(element.select("img").attr("src"));
            model.title = element.select(".name").text();
            model.date = element.select(".other i").text();
            list.add(model);
        }
        Message msg = handler.obtainMessage(0);
        msg.obj = list;
        msg.sendToTarget();
    }

    @Override
    public void detail(String url, DetailCallback callback) throws Exception {
        Document document = Jsoup.connect(Constants.API_HOST + url).userAgent(Constants.USER_AGENT).get();
        String img = document.select(".lazy").attr("src");
        String summary = document.select(".tab-jq").text();
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
        Dispatcher.getInstance().enqueue(() -> {
            try {
                loadData();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                handler.sendEmptyMessage(0);
            }
        });
    }

    @Override
    public String loadJs() {
        if (js == null) {
            js = loadJs("inject");
        }
        return js;
    }

    private void loadData() throws Exception {
        referer = url;
        Document document = Jsoup.connect(url).userAgent(Constants.USER_AGENT).get();
        url = document.select("iframe").attr("src");
        document = Jsoup.connect(url).referrer(referer).get();
        Elements elements = document.select("iframe");
        if (elements != null && elements.size() > 0)
            url = document.select("iframe").attr("src");
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        String suffix = "package=" + BuildConfig.APPLICATION_ID;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && url.endsWith(suffix)) {
            try {
                URL uri = new URL(url.substring(0, url.length() - suffix.length() - 1));
                HttpURLConnection connection = (HttpURLConnection) uri.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                if (connection.getResponseCode() == 200) {
                    InputStream is = connection.getInputStream();
                    Map<String, String> map = new HashMap<>();
                    map.put("Access-Control-Allow-Origin", "*");
                    WebResourceResponse response = new WebResourceResponse("application/json", "utf-8", is);
                    response.setResponseHeaders(map);
                    return response;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return super.shouldInterceptRequest(view, url);
    }
}
