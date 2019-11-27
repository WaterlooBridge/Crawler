package com.zhenl.crawler;

import android.Manifest;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.zhenl.crawler.core.RecordAgent;
import com.zhenl.crawler.engines.SearchEngine;
import com.zhenl.crawler.engines.SearchEngineFactory;
import com.zhenl.crawler.utils.FileUtil;

import java.lang.ref.WeakReference;
import java.net.URLEncoder;

import tv.danmaku.ijk.media.player.AVOptions;
import tv.danmaku.ijk.media.player.IIjkMediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.widget.AndroidMediaController;
import tv.danmaku.ijk.media.widget.IPCVideoView;
import tv.danmaku.ijk.media.widget.VideoControlHelper;

public class MainActivity extends AppCompatActivity implements IPCVideoView.OnInfoListener {

    public static void start(Context context, String title, String url) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("url", url);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    private final String TAG = getClass().getSimpleName();

    private static final int MENU_MORE = 2;
    private static final int REQUEST_CODE_FILE = 101;

    private String url;
    private IPCVideoView mVideoView;
    private AndroidMediaController controller;
    private VideoControlHelper controlHelper;
    private ProgressBar pb;
    private ImageButton btn_lock;
    private SearchEngine engine;
    private boolean mStopped;
    private boolean isPlaying;
    private boolean isLock;
    private boolean bgEnable;
    private String videoPath;
    private MainHandler handler;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        mOrientationListener = new OrientationListener(this);
        handler = new MainHandler(this);

        setContentView(R.layout.activity_main);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0x40000000));
        mVideoView = findViewById(R.id.buffer);
        AVOptions options = new AVOptions();
        options.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "user_agent", Constants.USER_AGENT);
        mVideoView.setOptions(options);
        mVideoView.setOnErrorListener((mp, what, extra) -> {
            isLock = false;
            try {
                record((int) mp.getDuration(), (int) mp.getCurrentPosition());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            if (what == -10000) {
                new AlertDialog.Builder(this).setMessage("播放异常，是否尝试浏览器播放")
                        .setNegativeButton("否", (dialog, which) -> finish())
                        .setPositiveButton("是", (dialog, which) -> jumpBrowser()).create().show();
                return true;
            }
            return false;
        });
        controller = new AndroidMediaController(this, false);
        controller.setInstantSeeking(false);
        controller.setSupportActionBar(getSupportActionBar());
        controller.setOnFullscreenClickListener((View v) -> {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mOrientationListener.disable();
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        });
        controlHelper = new VideoControlHelper(mVideoView);
        controlHelper.setMediaController(controller);

        pb = findViewById(R.id.probar);
        mVideoView.setBufferingIndicator(pb);
        btn_lock = findViewById(R.id.btn_lock);
        btn_lock.setOnClickListener(v -> {
            isLock = !isLock;
            btn_lock.setImageResource(isLock ? R.drawable.ic_lock_open_24dp : R.drawable.ic_lock_24dp);
            controller.setLock(isLock);
            if (isLock)
                controller.hide();
            else
                controller.show();
        });

        Uri data = getIntent().getData();
        if (data != null) {
            handleVideoFileIntent(data);
            return;
        }

        setTitle(getIntent().getStringExtra("title"));
        url = SearchEngineFactory.getHost() + getIntent().getSerializableExtra("url");
        Log.e(TAG, "[INFO:CONSOLE]" + url);
        engine = SearchEngineFactory.create();
        engine.load(url, new SearchEngine.Callback() {
            @Override
            public void play(String path) {
                mVideoView.setCacheEnable(true);
                mVideoView.setProxyCacheMode(path.contains(".m3u8"));
                MainActivity.this.play(path);
            }

            @Override
            public void finish() {
                MainActivity.this.finish();
            }
        });
    }

    private void handleVideoFileIntent(Uri uri) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_FILE);
            return;
        }
        Log.e(TAG, uri.toString());
        url = FileUtil.getFilePathFromContentUri(uri, getContentResolver());
        if (TextUtils.isEmpty(url)) {
            finish();
            return;
        }
        Log.e(TAG, url);
        AVOptions options = new AVOptions();
        options.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "allowed_extensions", "ALL");
        mVideoView.setOptions(options);
        play(url);
    }

    /**
     * TODO: Set the path variable to a streaming video URL or a local media file
     * path.
     */
    private void play(String path) {
        videoPath = path;
        Uri uri = Uri.parse(path);
        mVideoView.setVideoURI(uri);
        mVideoView.setMediaController(controller);
        mVideoView.setControlHelper(controlHelper);
        mVideoView.requestFocus();
        mVideoView.setOnInfoListener(this);
        mVideoView.setOnPreparedListener(mp -> {
            try {
                if (!mStopped || bgEnable)
                    mp.start();
                else
                    mp.pause();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });

        int pos = RecordAgent.getInstance().getRecord(url);
        if (pos > 0)
            mVideoView.seekTo(pos);
    }

    @Override
    public boolean onInfo(IIjkMediaPlayer mp, int what, int extra) {
        switch (what) {
            case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                if (mVideoView.isPlaying()) {
                    mVideoView.pause();
                }
                break;
            case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                if (!mStopped || bgEnable)
                    mVideoView.start();
                break;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (isLock)
            return;
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
        MenuItem moreMenu = menu.add(Menu.NONE, MENU_MORE, MENU_MORE, null);
        moreMenu.setIcon(R.drawable.ic_more_vert_24dp);
        moreMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == Menu.FIRST) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                enterPictureInPictureMode();
        } else if (id == MENU_MORE) {
            showSettingDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            controller.onFullscreenChanged(true);
            mOrientationListener.enable();
        } else {
            controller.onFullscreenChanged(false);
            mOrientationListener.disable();
        }
    }

    @Override
    public void finish() {
        if (engine != null)
            engine.destroy();
        record(mVideoView.getDuration(), mVideoView.getCurrentPosition());
        mVideoView.release(true);
        controller.release();
        super.finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        handleVideoFileIntent(getIntent().getData());
    }

    public void record(int duration, int curPos) {
        if (duration - curPos < 5000)
            curPos = 0;
        if (duration > 0)
            RecordAgent.getInstance().record(url, curPos);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mStopped = false;
        if (!bgEnable && isPlaying)
            mVideoView.start();
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
        mStopped = true;
        isPlaying = mVideoView.isPlaying();
        if (!bgEnable)
            mVideoView.pause();
        super.onStop();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            if (isLock && btn_lock.getVisibility() == View.GONE || controller.isShowing()) {
                btn_lock.setVisibility(View.VISIBLE);
                handler.removeMessages(MainHandler.MSG_FADE_OUT);
                handler.sendEmptyMessageDelayed(MainHandler.MSG_FADE_OUT, 3000);
            } else
                btn_lock.setVisibility(View.GONE);
        }
        return super.dispatchTouchEvent(ev);
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

    private Dialog settingDialog;

    public void showSettingDialog() {
        if (settingDialog == null)
            initSettingDialog();
        settingDialog.show();
    }

    public void initSettingDialog() {
        settingDialog = new Dialog(this, R.style.TransparentDialog);
        settingDialog.setContentView(R.layout.dialog_video_play_setting);
        Switch switch_play_background = settingDialog.findViewById(R.id.switch_play_background);
        switch_play_background.setOnCheckedChangeListener((buttonView, isChecked) ->
                bgEnable = isChecked);
        settingDialog.findViewById(R.id.view_open_browser).setOnClickListener(v -> {
            settingDialog.dismiss();
            jumpBrowser();
        });
        settingDialog.findViewById(R.id.view_copy_link).setOnClickListener(v -> {
            settingDialog.dismiss();
            ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (manager == null)
                return;
            manager.setPrimaryClip(ClipData.newPlainText("link", generateUrl()));
            Toast.makeText(getApplicationContext(), "Link Copied", Toast.LENGTH_SHORT).show();
        });
    }

    private void jumpBrowser() {
        if (TextUtils.isEmpty(videoPath))
            return;
        try {
            Uri uri = Uri.parse(generateUrl());
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String generateUrl() {
        return "https://waterloobridge.github.io/smile/video.html?path=" + URLEncoder.encode(videoPath);
    }

    private static class MainHandler extends Handler {

        static final int MSG_FADE_OUT = 0;

        WeakReference<MainActivity> wr;

        public MainHandler(MainActivity activity) {
            wr = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = wr.get();
            if (activity == null)
                return;
            switch (msg.what) {
                case MSG_FADE_OUT:
                    activity.btn_lock.setVisibility(View.GONE);
                    break;
            }
        }
    }
}
