package com.zhenl.crawler;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.zhenl.crawler.models.DramasModel;
import com.zhenl.violet.core.Dispatcher;
import com.zhenl.violet.utils.VHUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
    private GridView gv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        url = getIntent().getStringExtra("url");
        setContentView(R.layout.activity_movie_detail);
        setTitle(getIntent().getStringExtra("title"));
        iv = (ImageView) findViewById(R.id.iv);
        tvSummary = (TextView) findViewById(R.id.tv_summary);
        gv = (GridView) findViewById(R.id.gv);
        gv.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            DramasModel model = dsList.get(position);
            MainActivity.start(view.getContext(), model.text, model.url);
        });
        load();
    }

    private void load() {
        Dispatcher.getInstance().enqueue(new Runnable() {
            @Override
            public void run() {
                try {
                    loadData();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    handler.sendEmptyMessage(0);
                }
            }
        });
    }

    private void loadData() throws Exception {
        Document document = Jsoup.connect(Constants.API_HOST + url).get();
        img = document.select(".img-thumbnail").attr("src");
        summary = document.select(".summary").text();
        Elements elements = document.select(".dslist-group a");
        for (Element element : elements) {
            DramasModel model = new DramasModel();
            model.text = element.text();
            model.url = element.attr("href");
            dsList.add(model);
        }
    }

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (isFinishing())
                return;
            Glide.with(MovieDetailActivity.this).load(img).into(iv);
            tvSummary.setText(summary);
            gv.setAdapter(new DramasAdapter(dsList));
        }
    };

    private static class DramasAdapter extends BaseAdapter {

        private List<DramasModel> list;

        public DramasAdapter(List<DramasModel> list) {
            this.list = list;
        }

        @Override
        public int getCount() {
            return list == null ? 0 : list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movie_dramas, parent, false);
            TextView tv = VHUtil.get(convertView, R.id.tv);
            DramasModel model = list.get(position);
            tv.setText(model.text);
            return convertView;
        }
    }
}
