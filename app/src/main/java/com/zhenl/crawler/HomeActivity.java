package com.zhenl.crawler;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.zhenl.crawler.adapter.MovieAdapter;
import com.zhenl.crawler.engines.SearchEngineFactory;
import com.zhenl.crawler.models.MovieModel;
import com.zhenl.violet.base.RecyclerAdapter;
import com.zhenl.violet.core.Dispatcher;
import com.zhenl.violet.widget.SwipeFooterFactory;
import com.zhenl.violet.widget.SwipeRecyclerView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private final String TAG = getClass().getSimpleName();

    private int types[] = {7, 1, 2, 4, 6, 9};

    private SwipeRefreshLayout refreshLayout;
    private SwipeRecyclerView recyclerView;

    private List<MovieModel> list = new ArrayList<>();
    private int page = 1;
    private MovieAdapter adapter;
    private int pos;

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
                SearchEngineFactory.type = 1;
                MovieDetailActivity.start(view.getContext(), model.title, model.url);
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
                    loadData();
                } catch (Exception e) {
                    e.printStackTrace();
                    handler.sendEmptyMessage(0);
                }
            }
        });
    }

    private void loadData() throws Exception {
        int type = types[pos];
        Document document = Jsoup.connect(Constants.API_HOST + "/type/" + type + "/" + page + ".html")
                .userAgent(Constants.USER_AGENT).get();
        if (document.location().startsWith(Constants.API_HOST)) {
            Elements elements = document.select(".p1");
            List<MovieModel> list = new ArrayList<>();
            for (Element element : elements) {
                MovieModel model = new MovieModel();
                model.url = element.select("a").attr("href");
                model.setImg(element.select("img").attr("src"));
                model.title = element.select(".name").text();
                model.date = element.select(".other i").text();
                list.add(model);
            }
            Message msg = Message.obtain();
            msg.what = type;
            msg.obj = list;
            handler.sendMessage(msg);
        } else {
            Uri uri = Uri.parse(document.location());
            Constants.API_HOST = uri.getScheme() + "://" + uri.getHost();
            if (type == types[pos])
                loadData();
        }
    }

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int type = msg.what;
            if (type == 0) {
                recyclerView.loadMoreComplete();
                refreshLayout.setRefreshing(false);
            } else if (type == types[pos]) {
                List<MovieModel> models = (List<MovieModel>) msg.obj;
                if (page++ == 1)
                    list.clear();
                list.addAll(models);
                recyclerView.loadMoreComplete();
                refreshLayout.setRefreshing(false);
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem moreItem = menu.add(Menu.NONE, Menu.FIRST, Menu.FIRST, "SEARCH");
        moreItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == Menu.FIRST) {
            showSearchEngines();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSearchEngines() {
        final String items[] = {"Search Engine 1", "Search Engine 2", "Search Engine 3"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(items, (DialogInterface dialog, int which) -> {
            dialog.dismiss();
            SearchEngineFactory.type = which + 1;
            startActivity(new Intent(HomeActivity.this, SearchActivity.class));
        });
        builder.create().show();
    }

    public void onClick(View view) {
        list.clear();
        recyclerView.loadMoreComplete();
        pos = ++pos % types.length;
        refreshLayout.setRefreshing(true);
        page = 1;
        load();
    }
}
