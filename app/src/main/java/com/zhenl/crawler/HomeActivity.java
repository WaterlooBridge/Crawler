package com.zhenl.crawler;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.zhenl.crawler.models.MovieModel;
import com.zhenl.violet.base.RecyclerAdapter;
import com.zhenl.violet.core.Dispatcher;
import com.zhenl.violet.widget.SwipeAdapter;
import com.zhenl.violet.widget.SwipeFooterFactory;
import com.zhenl.violet.widget.SwipeRecyclerView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private final String TAG = getClass().getSimpleName();

    private SwipeRefreshLayout refreshLayout;
    private SwipeRecyclerView recyclerView;

    private List<MovieModel> list = new ArrayList<>();
    private int page = 1;
    private MovieAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        refreshLayout = findViewById(R.id.refresh_layout);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        recyclerView.addFootView(SwipeFooterFactory.createSwipeFooter(this, recyclerView));
        adapter = new MovieAdapter(list);
        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(new RecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Object object, View view, int position) {
                MovieModel model = (MovieModel) object;
                MainActivity.start(view.getContext(), model.url);
            }
        });
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                page = 1;
                load();
            }
        });
        recyclerView.setOnLoadMoreListener(new SwipeRecyclerView.OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                load();
            }
        });
        refreshLayout.setRefreshing(true);
        load();
    }

    private void load() {
        Dispatcher.getInstance().enqueue(new Runnable() {
            @Override
            public void run() {
                try {
                    Document document = Jsoup.connect(Constants.API_HOST + "/htm/movielist4/" + page + ".htm").get();
                    Elements elements = document.select("li");
                    if (page++ == 1)
                        list.clear();
                    for (Element element : elements) {
                        MovieModel model = new MovieModel();
                        model.url = element.select("a").attr("href");
                        model.img = element.select("img").attr("src");
                        model.title = element.select("h3").text();
                        model.date = element.select(".movie_date").text();
                        list.add(model);
                    }
                    handler.sendEmptyMessage(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            recyclerView.loadMoreComplete();
            refreshLayout.setRefreshing(false);
        }
    };

    private static class MovieAdapter extends RecyclerAdapter {

        private List<MovieModel> list;

        public MovieAdapter(List<MovieModel> list) {
            this.list = list;
        }

        @Override
        public RecyclerView.ViewHolder onCreateHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movie, parent, false);
            return new Holder(view);
        }

        @Override
        public int getContentItemCount() {
            return list == null ? 0 : list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
            Holder holder = (Holder) viewHolder;
            MovieModel model = list.get(position);
            Glide.with(holder.iv.getContext()).load(model.img).centerCrop().into(holder.iv);
            holder.tvTitle.setText(model.title);
            holder.tvDate.setText(model.date);
        }

        private class Holder extends RecyclerHolder {

            private ImageView iv;
            private TextView tvTitle;
            private TextView tvDate;

            public Holder(View itemView) {
                super(itemView);
                iv = itemView.findViewById(R.id.iv);
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvDate = itemView.findViewById(R.id.tv_date);
            }
        }
    }
}
