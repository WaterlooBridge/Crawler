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
        int page = handler.pageNum;
        Document document = Jsoup.connect(Constants.API_HOST2 + "/search.php").data("searchword", keyword)
                .data("page", String.valueOf(page)).get();
        if (handler.recSeqNum > seqNum)
            return;
        handler.recSeqNum = seqNum;
        Elements elements = document.select(".v-thumb.stui-vodlist__thumb");
        List<MovieModel> list = new ArrayList<>();
        for (Element element : elements) {
            MovieModel model = new MovieModel();
            model.url = element.attr("href");
            model.setImg(element.attr("data-original"));
            model.title = element.attr("title");
            model.date = element.select(".pic-text").text();
            list.add(model);
        }
        Message msg = handler.obtainMessage(page);
        msg.obj = list;
        msg.sendToTarget();
    }

    @Override
    public void detail(String url, DetailCallback callback) throws Exception {
        Document document = Jsoup.connect(Constants.API_HOST2 + url).get();
        String img = document.select(".stui-vodlist__thumb img").attr("data-original");
        String summary = document.select("meta[name=description]").attr("content");
        Elements elements = document.select(".stui-content__playlist a");
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
