package com.zhenl.crawler.engines;

import android.os.Message;

import com.zhenl.crawler.Constants;
import com.zhenl.crawler.R;
import com.zhenl.crawler.SearchActivity;
import com.zhenl.crawler.models.DramasModel;
import com.zhenl.crawler.models.MovieModel;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lin on 2018/8/22.
 */
public class SearchEngineImpl2 extends SearchEngine {

    private static String js;

    @Override
    public void search(int seqNum, String keyword, SearchActivity.SearchHandler handler) throws Exception {
        Document document = Jsoup.connect(Constants.API_HOST2 + "/index.php?s=vod-search").data("wd", keyword).post();
        if (handler.recSeqNum > seqNum)
            return;
        handler.recSeqNum = seqNum;
        Elements elements = document.select(".mui-table-view-cell");
        List<MovieModel> list = new ArrayList<>();
        for (Element element : elements) {
            MovieModel model = new MovieModel();
            model.url = element.select("a").attr("href");
            model.setImg(Constants.API_HOST2 + element.select("img").attr("data-original"));
            model.title = element.select(".type-title").text();
            model.date = element.select("span").text();
            list.add(model);
        }
        Message msg = handler.obtainMessage(0);
        msg.obj = list;
        msg.sendToTarget();
    }

    @Override
    public void detail(String url, DetailCallback callback) throws Exception {
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
    public void load(String url, Callback callback) {
        this.url = url;
        this.callback = callback;
        load(url);
    }

    @Override
    public String loadJs() {
        if (js == null) {
            js = loadJs("inject2");
        }
        return js;
    }
}
