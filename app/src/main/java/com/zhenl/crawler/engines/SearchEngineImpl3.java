package com.zhenl.crawler.engines;

import android.os.Message;

import com.zhenl.crawler.Constants;
import com.zhenl.crawler.R;
import com.zhenl.crawler.SearchActivity;
import com.zhenl.crawler.models.DramasModel;
import com.zhenl.crawler.models.MovieModel;

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
    @Override
    public void search(int seqNum, String keyword, SearchActivity.SearchHandler handler) throws Exception {
        String url = Constants.API_HOST3 + "/search.php";
        Connection.Response res = Jsoup.connect(url).method(Connection.Method.POST).data("searchword", keyword)
                .followRedirects(false).execute();
        while (res.hasHeader("Location")) {
            String location = res.header("Location");
            if (location != null && location.startsWith("http:/") && location.charAt(6) != '/')
                location = location.substring(6);
            String redir = StringUtil.resolve(url, location);
            Connection connection = Jsoup.connect(redir).method(Connection.Method.POST).data("searchword", keyword);
            for (Map.Entry<String, String> cookie : res.cookies().entrySet())
                connection.cookie(cookie.getKey(), cookie.getValue());
            res = connection.followRedirects(false).execute();
        }
        Document document = res.parse();
        if (handler.recSeqNum > seqNum)
            return;
        handler.recSeqNum = seqNum;
        Elements elements = document.select(".listfl");
        List<MovieModel> list = new ArrayList<>();
        for (Element element : elements) {
            MovieModel model = new MovieModel();
            model.url = element.select("a").attr("href");
            model.setImg(element.select("img").attr("src").replace("\n", ""));
            model.title = element.select(".list-name").text();
            model.date = element.select(".duration").text();
            list.add(model);
        }
        Message msg = handler.obtainMessage(0);
        msg.obj = list;
        msg.sendToTarget();
    }

    @Override
    public void detail(String url, DetailCallback callback) throws Exception {
        Document document = Jsoup.connect(Constants.API_HOST3 + url).get();
        String img = document.select(".detail-pic").select("img").attr("src")
                .replace("\n", "");
        String summary = document.select(".info").text();
        Elements elements = document.select(".dslist-group a");
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