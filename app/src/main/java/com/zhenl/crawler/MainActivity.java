package com.zhenl.crawler;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import java.io.ByteArrayInputStream;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
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

    private String iframeSrc;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        // init player
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        mOrientationListener = new OrientationListener(this);

        setContentView(R.layout.activity_main);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0x99000000));
        mVideoView = (VideoView) findViewById(R.id.buffer);
        controller = new AndroidMediaController(this, false);
        controller.setSupportActionBar(getSupportActionBar());
        pb = (ProgressBar) findViewById(R.id.probar);

        setTitle(getIntent().getStringExtra("title"));
        url = Constants.API_HOST + getIntent().getSerializableExtra("url");
        Log.e(TAG, url);
        load();

    }

    private void load() {
        wv = new WebView(this);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.addJavascriptInterface(new JsBridge(), "bridge");
        wv.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.e(TAG, "onPageFinished" + ":::" + url);
                if (iframeSrc != null && !iframeSrc.equals(url)) {
                    view.loadUrl(iframeSrc);
                    iframeSrc = null;
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    view.evaluateJavascript("window.ckplayerLoad=function(data){window.bridge.loadVideo(data.url);}", null);
                }
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (iframeSrc != null && iframeSrc.startsWith("http://api.tianxianle.com/guhuo/?v="))
                    return super.shouldInterceptRequest(view, url);
                if (url.startsWith("http://api.tianxianle.com/jx/sapi.php?id=") || url.startsWith("http://api.tianxianle.com/guhuo/?v="))
                    iframeSrc = url;
                if (url.equals(MainActivity.this.url) || url.equals(iframeSrc) || url.endsWith(".js"))
                    return super.shouldInterceptRequest(view, url);
                else
                    return new WebResourceResponse("text/html", "utf-8", new ByteArrayInputStream("".getBytes()));
            }
        });
        wv.loadUrl(url);
    }

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    finish();
                    break;
                case 1:
                    String path = (String) msg.obj;
                    play(path);
                    wv.destroy();
                    wv = null;
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
        MenuItem moreItem2 = menu.add(Menu.NONE, 2, 2, "ROTATE");
        moreItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        moreItem2.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == Menu.FIRST) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                enterPictureInPictureMode();
        } else if (id == 2) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mOrientationListener.disable();
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mOrientationListener.enable();
        } else {
            mOrientationListener.disable();
        }
    }

    @Override
    public void finish() {
        if (wv != null)
            wv.destroy();
        mVideoView.release(true);
        super.finish();
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
    }
}
