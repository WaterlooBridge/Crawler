package com.zhenl.crawler;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import com.zhenl.crawler.adapter.MovieAdapter;
import com.zhenl.crawler.engines.SearchEngine;
import com.zhenl.crawler.engines.SearchEngineFactory;
import com.zhenl.crawler.models.MovieModel;
import com.zhenl.violet.core.Dispatcher;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by lin on 2018/6/9.
 */
public class SearchActivity extends AppCompatActivity {

    private EditText et;
    private RecyclerView rv;

    private MovieAdapter adapter;
    private SearchEngine engine;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        et = findViewById(R.id.et);
        rv = findViewById(R.id.rv);
        rv.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        adapter = new MovieAdapter(null);
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
                load(handler.seqNum++);
            }
        });
        engine = SearchEngineFactory.create();
    }

    private void load(final int seqNum) {
        Dispatcher.getInstance().enqueue(() -> {
            try {
                loadData(seqNum);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void loadData(int seqNum) throws Exception {
        engine.search(seqNum, et.getText().toString(), handler);
    }

    SearchHandler handler = new SearchHandler(this);

    public static class SearchHandler extends Handler {

        public int seqNum;
        public volatile int recSeqNum;

        WeakReference<SearchActivity> reference;

        public SearchHandler(SearchActivity activity) {
            reference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            SearchActivity activity = reference.get();
            if (activity == null || activity.isFinishing())
                return;
            List<MovieModel> list = (List<MovieModel>) msg.obj;
            activity.adapter.refresh(list);
        }
    }
}
