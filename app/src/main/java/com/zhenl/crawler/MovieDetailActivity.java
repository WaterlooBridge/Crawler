package com.zhenl.crawler;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.zhenl.crawler.engines.SearchEngineFactory;
import com.zhenl.crawler.models.DramasModel;
import com.zhenl.crawler.models.MovieModel;
import com.zhenl.violet.base.RecyclerAdapter;
import com.zhenl.violet.core.Dispatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lin on 2018/6/8.
 */
public class MovieDetailActivity extends AppCompatActivity {

    public static void start(Context context, String title, String url) {
        Intent intent = new Intent(context, MovieDetailActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("url", url);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    private String url;

    private String img;
    private String summary;
    private List<DramasModel> dsList = new ArrayList<>();

    private ImageView iv;
    private TextView tvSummary;
    private RecyclerView gv;
    private DramasAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        url = getIntent().getStringExtra("url");
        setContentView(R.layout.activity_movie_detail);
        setTitle(getIntent().getStringExtra("title"));
        iv = findViewById(R.id.iv);
        tvSummary = findViewById(R.id.tv_summary);
        gv = findViewById(R.id.gv);
        gv.setLayoutManager(new GridLayoutManager(this, 4));
        gv.setNestedScrollingEnabled(false);
        adapter = new DramasAdapter(dsList);
        gv.setAdapter(adapter);
        adapter.setOnItemClickListener((Object object, View view, int position) -> {
            DramasModel model = dsList.get(position);
            MainActivity.start(view.getContext(), model.text, model.url);
        });
        load();
    }

    private void load() {
        Dispatcher.getInstance().enqueue(() -> {
                    try {
                        loadData();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        handler.sendEmptyMessage(0);
                    }
                }
        );
    }

    private void loadData() throws Exception {
        SearchEngineFactory.create().detail(url, (String img, String summary, List<DramasModel> list) -> {
            this.img = MovieModel.handleImg(img);
            this.summary = summary;
            if (list != null)
                dsList.addAll(list);
        });
    }

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (isFinishing() || Build.VERSION.SDK_INT >= 17 && isDestroyed())
                return;
            Glide.with(MovieDetailActivity.this).load(img).into(iv);
            tvSummary.setText(summary);
            adapter.notifyDataSetChanged();
        }
    };

    private static class DramasAdapter extends RecyclerAdapter {

        private List<DramasModel> list;

        public DramasAdapter(List<DramasModel> list) {
            this.list = list;
        }

        @Override
        public RecyclerView.ViewHolder onCreateHolder(ViewGroup parent, int viewType) {
            return new DramasHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movie_dramas, parent, false));
        }

        @Override
        public int getContentItemCount() {
            return list == null ? 0 : list.size();
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
            DramasHolder holder = (DramasHolder) viewHolder;
            DramasModel model = list.get(position);
            holder.tv.setText(model.text);
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        class DramasHolder extends RecyclerHolder {

            TextView tv;

            public DramasHolder(View itemView) {
                super(itemView);
                tv = itemView.findViewById(R.id.tv);
            }
        }
    }
}
