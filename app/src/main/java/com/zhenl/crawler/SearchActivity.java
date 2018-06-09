package com.zhenl.crawler;

import android.annotation.SuppressLint;
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
import com.zhenl.crawler.models.MovieModel;
import com.zhenl.violet.core.Dispatcher;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lin on 2018/6/9.
 */
public class SearchActivity extends AppCompatActivity {

    private EditText et;
    private RecyclerView rv;

    private MovieAdapter adapter;
    private int seqNum;
    private volatile int recSeqNum;

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
                load(seqNum++);
            }
        });
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
        Document document = Jsoup.connect(Constants.API_HOST + "/search?wd=" + et.getText()).get();
        if (recSeqNum > seqNum)
            return;
        recSeqNum = seqNum;
        Elements elements = document.select(".movie-item");
        List<MovieModel> list = new ArrayList<>();
        for (Element element : elements) {
            MovieModel model = new MovieModel();
            model.url = element.select("a").attr("href");
            model.img = element.select("img").attr("src");
            model.title = element.select(".movie-name").text();
            model.date = element.select(".hdtag").text();
            list.add(model);
        }
        Message msg = handler.obtainMessage(0);
        msg.obj = list;
        msg.sendToTarget();
    }

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (isFinishing())
                return;
            List<MovieModel> list = (List<MovieModel>) msg.obj;
            adapter.refresh(list);
        }
    };
}
