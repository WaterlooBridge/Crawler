package com.zhenl.crawler;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
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
import android.view.View;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.ByteArrayInputStream;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.Vitamio;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;

public class MainActivity extends AppCompatActivity implements MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener,
        View.OnClickListener {

    public static void start(Context context, String url) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("url", url);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    private final String TAG = getClass().getSimpleName();

    private String url;
    private Uri uri;
    private VideoView mVideoView;
    private ProgressBar pb;
    private TextView rateView;
    private int extra, percent;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Vitamio.isInitialized(getApplicationContext());

        setContentView(R.layout.activity_main);
        mVideoView = (VideoView) findViewById(R.id.buffer);
        pb = (ProgressBar) findViewById(R.id.probar);

        rateView = (TextView) findViewById(R.id.rate);

        url = Constants.API_HOST + getIntent().getStringExtra("url").replace("movie4", "movieplay4");
        Log.e(TAG, url);
        load();

    }

    private void load() {
        WebView wv = new WebView(this);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                Log.e(TAG, "onPageFinished");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    view.evaluateJavascript("javascript:(function(){return playurl})()", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            Log.e(TAG, "m3u8 path:" + value);
                            if (TextUtils.isEmpty(value) || "null".equals(value))
                                handler.sendEmptyMessage(0);
                            else {
                                Message msg = handler.obtainMessage();
                                msg.what = 1;
                                msg.obj = value.substring(1, value.length() - 1);
                                msg.sendToTarget();
                            }
                        }
                    });
                }
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (url.equals(MainActivity.this.url) || url.endsWith(".js"))
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
        MediaController controller = new MediaController(this);
        controller.setFullscreenListener(this);
        mVideoView.setMediaController(controller);
        mVideoView.requestFocus();
        mVideoView.setOnInfoListener(this);
        mVideoView.setOnBufferingUpdateListener(this);
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                // optional need Vitamio 4.0
//                mediaPlayer.setPlaybackSpeed(1.0f);
            }
        });

    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                if (mVideoView.isPlaying()) {
                    mVideoView.pause();
                    pb.setVisibility(View.VISIBLE);
                    this.extra = percent = 0;
                    rateView.setText("");
                    rateView.setVisibility(View.VISIBLE);

                }
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                mVideoView.start();
                pb.setVisibility(View.GONE);
                rateView.setVisibility(View.GONE);
                break;
            case MediaPlayer.MEDIA_INFO_DOWNLOAD_RATE_CHANGED:
                this.extra = extra;
                rateView.setText(extra + "kb/s" + "  " + percent + "%");
                break;
        }
        return true;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        this.percent = percent;
        rateView.setText(extra + "kb/s" + "  " + percent + "%");
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.mediacontroller_fullscreen) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mVideoView.setVideoLayout(VideoView.VIDEO_LAYOUT_SCALE, 0);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            actionBarHandler.obtainMessage(0).sendToTarget();
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            actionBarHandler.obtainMessage(1).sendToTarget();
        }
    }

    @SuppressLint("HandlerLeak")
    Handler actionBarHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    getSupportActionBar().hide();
                    break;
                case 1:
                    getSupportActionBar().show();
                    break;
            }
        }
    };

    @Override
    public void onBackPressed() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            enterPictureInPictureMode();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        mVideoView.release(true);
        super.finish();
    }
}
