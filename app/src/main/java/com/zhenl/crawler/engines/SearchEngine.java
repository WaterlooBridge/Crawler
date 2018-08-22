package com.zhenl.crawler.engines;

import android.content.Context;

import com.zhenl.crawler.SearchActivity;
import com.zhenl.crawler.models.DramasModel;

import java.util.List;

/**
 * Created by lin on 2018/8/22.
 */
public interface SearchEngine {

    void search(String keyword, SearchActivity.SearchHandler handler) throws Exception;
    void detail(String url, DetailCallback callback) throws Exception;
    void load(Context context, String url, Callback callback);
    void destroy();

    interface Callback {
        void play(String path);
        void finish();
    }

    interface DetailCallback {
        void onSuccess(String img, String summary, List<DramasModel> list);
    }
}
