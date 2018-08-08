package com.zhenl.crawler;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.zhenl.crawler.core.RecordAgent;
import com.zhenl.violet.core.Dispatcher;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.widget.AndroidMediaController;
import tv.danmaku.ijk.media.widget.VideoView;

public class MainActivity extends AppCompatActivity implements IMediaPlayer.OnInfoListener {

    public static void start(Context context, String title, String url) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("url", url);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    private final String TAG = getClass().getSimpleName();

    private String url;
    private Uri uri;
    private VideoView mVideoView;
    private AndroidMediaController controller;
    private ProgressBar pb;
    private WebView wv;

    private String js;
    private boolean intercept;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        mOrientationListener = new OrientationListener(this);

        setContentView(R.layout.activity_main);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0x99000000));
        mVideoView = (VideoView) findViewById(R.id.buffer);
        controller = new AndroidMediaController(this, false);
        controller.setSupportActionBar(getSupportActionBar());
        controller.setOnFullscreenClickListener((View v) -> {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mOrientationListener.disable();
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        });
        pb = (ProgressBar) findViewById(R.id.probar);

        setTitle(getIntent().getStringExtra("title"));
        url = Constants.API_HOST + getIntent().getSerializableExtra("url");
        Log.e(TAG, "[INFO:CONSOLE]" + url);
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

    private String referer;

    private void loadData() throws Exception {
        if (js == null) {
            InputStream inputStream = getResources().openRawResource(R.raw.inject);
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            js = new String(bytes);
        }
        referer = url;
        Document document = Jsoup.connect(url).userAgent(Constants.USER_AGENT).get();
        url = document.select("iframe").attr("src");
        document = Jsoup.connect(url).referrer(referer).get();
        Elements elements = document.select("iframe");
        if (elements != null && elements.size() > 0)
            url = document.select("iframe").attr("src");
    }

    private void load(String url) {
        if (isFinishing())
            return;
        if (wv == null) {
            wv = new WebView(this);
            wv.getSettings().setJavaScriptEnabled(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                wv.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            wv.addJavascriptInterface(new JsBridge(), "bridge");
            wv.setWebViewClient(new WebViewClient() {

                @Override
                public void onPageFinished(WebView view, String url) {
                    Log.e(TAG, "[INFO:CONSOLE]" + url);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        view.evaluateJavascript("javascript:" + js, null);
                    }
                }

                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                    if (true) {
                        if (intercept && url.contains("m3u8?")) {
                            Message msg = handler.obtainMessage(1);
                            msg.obj = url;
                            msg.sendToTarget();
                        }
                        String suffix = "package=" + BuildConfig.APPLICATION_ID;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && url.endsWith(suffix)) {
                            try {
                                URL uri = new URL(url.substring(0, url.length() - suffix.length() - 1));
                                HttpURLConnection connection = (HttpURLConnection) uri.openConnection();
                                connection.setRequestMethod("GET");
                                connection.setConnectTimeout(5000);
                                connection.setReadTimeout(5000);

                                if (connection.getResponseCode() == 200) {
                                    InputStream is = connection.getInputStream();
                                    Map<String, String> map = new HashMap<>();
                                    map.put("Access-Control-Allow-Origin", "*");
                                    WebResourceResponse response = new WebResourceResponse("application/json", "utf-8", is);
                                    response.setResponseHeaders(map);
                                    return response;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        return super.shouldInterceptRequest(view, url);
                    } else
                        return new WebResourceResponse("text/html", "utf-8", new ByteArrayInputStream("".getBytes()));
                }

                @Override
                public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                    handler.proceed();
                }
            });
        }
        Map<String, String> map = new HashMap<>();
        map.put("referer", referer);
        wv.loadUrl(url, map);
    }

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    load(url);
                    break;
                case 1:
                    String path = (String) msg.obj;
                    play(path);
                    destroyWebView();
                    break;
                case 2:
                    finish();
                    break;
                case 3:
                    wv.loadUrl((String) msg.obj);
                    break;
                case 4:
                    url = (String) msg.obj;
                    load();
                    break;
            }
        }
    };

    /**
     * TODO: Set the path variable to a streaming video URL or a local media file
     * path.
     */
    private void play(String path) {
        uri = Uri.parse(path);
        mVideoView.setVideoURI(uri);
        mVideoView.setMediaController(controller);
        mVideoView.requestFocus();
        mVideoView.setOnInfoListener(this);
        mVideoView.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer mp) {
                mp.start();
                pb.setVisibility(View.GONE);
            }
        });

        int pos = RecordAgent.getInstance().getRecord(url);
        if (pos > 0)
            mVideoView.seekTo(pos);
    }

    @Override
    public boolean onInfo(IMediaPlayer mp, int what, int extra) {
        switch (what) {
            case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                if (mVideoView.isPlaying()) {
                    mVideoView.pause();
                    pb.setVisibility(View.VISIBLE);
                }
                break;
            case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                mVideoView.start();
                pb.setVisibility(View.GONE);
                break;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mOrientationListener.disable();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else
            super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem moreItem = menu.add(Menu.NONE, Menu.FIRST, Menu.FIRST, "PIP");
        moreItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == Menu.FIRST) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                enterPictureInPictureMode();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        controller.onConfigurationChanged(newConfig);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mOrientationListener.enable();
        } else {
            mOrientationListener.disable();
        }
    }

    @Override
    public void finish() {
        destroyWebView();
        RecordAgent.getInstance().record(url, mVideoView.getCurrentPosition());
        mVideoView.release(true);
        super.finish();
    }

    private void destroyWebView() {
        if (wv != null) {
            wv.removeAllViews();
            wv.destroy();
            wv = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mOrientationListener.enable();
        }
    }

    @Override
    protected void onPause() {
        mOrientationListener.disable();
        super.onPause();
    }

    @Override
    protected void onStop() {
        mVideoView.pause();
        super.onStop();
    }

    OrientationEventListener mOrientationListener;

    class OrientationListener extends OrientationEventListener {
        public OrientationListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            Log.e(TAG, "Orientation changed to " + orientation);
            if (orientation > 80 && orientation < 100) { //90度
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            } else if (orientation > 260 && orientation < 280) { //270度
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }
    }

    public class JsBridge {

        @JavascriptInterface
        public void loadVideo(String url) {
            Message msg = handler.obtainMessage(1);
            msg.obj = url;
            msg.sendToTarget();
        }

        @JavascriptInterface
        public void destroy() {
            handler.obtainMessage(2).sendToTarget();
        }

        @JavascriptInterface
        public void intercept() {
            intercept = true;
        }

        @JavascriptInterface
        public void loadUrl(String url) {
            Message msg = handler.obtainMessage(3);
            msg.obj = url;
            msg.sendToTarget();
        }

        @JavascriptInterface
        public void reload(String url) {
            Message msg = handler.obtainMessage(4);
            msg.obj = url;
            msg.sendToTarget();
        }
    }
}
