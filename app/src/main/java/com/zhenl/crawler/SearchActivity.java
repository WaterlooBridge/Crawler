package com.zhenl.crawler;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.zhenl.crawler.adapter.MovieAdapter;
import com.zhenl.crawler.engines.SearchEngine;
import com.zhenl.crawler.engines.SearchEngineFactory;
import com.zhenl.crawler.models.MovieModel;
import com.zhenl.violet.core.Dispatcher;
import com.zhenl.violet.widget.SwipeFooterFactory;
import com.zhenl.violet.widget.SwipeRecyclerView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lin on 2018/6/9.
 */
public class SearchActivity extends AppCompatActivity {

    private EditText et;
    private SwipeRecyclerView rv;

    private SearchEngine engine;

    private List<MovieModel> list = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        et = findViewById(R.id.et);
        rv = findViewById(R.id.rv);
        rv.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        rv.addFootView(SwipeFooterFactory.createSwipeFooter(this, rv));
        MovieAdapter adapter = new MovieAdapter(list);
        rv.setAdapter(adapter);
        adapter.setOnItemClickListener((Object object, View view, int position) -> {
            MovieModel model = (MovieModel) object;
            MovieDetailActivity.start(view.getContext(), model.title, model.url);
        });
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                handler.pageNum = 1;
                load(++handler.seqNum);
            }
        });
        rv.setOnLoadMoreListener(() -> load(handler.seqNum));
        engine = SearchEngineFactory.create();
    }

    private void load(final int seqNum) {
        Dispatcher.getInstance().enqueue(() -> {
            try {
                loadData(seqNum);
            } catch (Exception e) {
                e.printStackTrace();
                handler.sendEmptyMessage(0);
            }
        });
    }

    private void loadData(int seqNum) throws Exception {
        engine.search(seqNum, et.getText().toString(), handler);
    }

    SearchHandler handler = new SearchHandler(this);

    public static class SearchHandler extends Handler {

        int seqNum;
        public volatile int recSeqNum;
        public volatile int pageNum;

        WeakReference<SearchActivity> reference;

        SearchHandler(SearchActivity activity) {
            reference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            SearchActivity activity = reference.get();
            if (activity == null || activity.isFinishing())
                return;
            List<MovieModel> models = (List<MovieModel>) msg.obj;
            if (msg.what == 1)
                activity.list.clear();
            if (models != null && !models.isEmpty()) {
                pageNum = msg.what + 1;
                activity.list.addAll(models);
            }
            activity.rv.loadMoreComplete();
        }
    }
}
