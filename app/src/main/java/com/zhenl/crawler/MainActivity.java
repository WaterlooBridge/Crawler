package com.zhenl.crawler;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.Vitamio;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;

public class MainActivity extends AppCompatActivity implements MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener {

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
    private TextView downloadRateView, loadRateView;
    private String m3u8Url;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Vitamio.isInitialized(getApplicationContext());

        setContentView(R.layout.activity_main);
        mVideoView = (VideoView) findViewById(R.id.buffer);
        pb = (ProgressBar) findViewById(R.id.probar);

        downloadRateView = (TextView) findViewById(R.id.download_rate);
        loadRateView = (TextView) findViewById(R.id.load_rate);

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
                    view.evaluateJavascript("javascript:(function(){return downurl})()", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            Log.e(TAG, "mp4 path:" + value);
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
                    view.evaluateJavascript("javascript:(function(){return playurl})()", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            Log.e(TAG, "m3u8 path:" + value);
                            if (!TextUtils.isEmpty(value) && !"null".equals(value))
                                m3u8Url = value.substring(1, value.length() - 1);
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
        mVideoView.setMediaController(new MediaController(this));
        mVideoView.requestFocus();
        mVideoView.setOnInfoListener(this);
        mVideoView.setOnBufferingUpdateListener(this);
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                // optional need Vitamio 4.0
                mediaPlayer.setPlaybackSpeed(1.0f);
            }
        });
        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                if (what == 1 && extra == -104 && m3u8Url != null) {
                    Toast.makeText(getApplicationContext(), "连接被重置，正在尝试m3u8地址播放", Toast.LENGTH_SHORT).show();
                    mVideoView.setVideoURI(Uri.parse(m3u8Url));
                    m3u8Url = null;
                    return true;
                }
                return false;
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
                    downloadRateView.setText("");
                    loadRateView.setText("");
                    downloadRateView.setVisibility(View.VISIBLE);
                    loadRateView.setVisibility(View.VISIBLE);

                }
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                mVideoView.start();
                pb.setVisibility(View.GONE);
                downloadRateView.setVisibility(View.GONE);
                loadRateView.setVisibility(View.GONE);
                break;
            case MediaPlayer.MEDIA_INFO_DOWNLOAD_RATE_CHANGED:
                downloadRateView.setText("" + extra + "kb/s" + "  ");
                break;
        }
        return true;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        loadRateView.setText(percent + "%");
    }
}
