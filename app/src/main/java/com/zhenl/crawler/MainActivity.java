package com.zhenl.crawler;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Switch;

import com.zhenl.crawler.core.RecordAgent;
import com.zhenl.crawler.engines.SearchEngine;
import com.zhenl.crawler.engines.SearchEngineFactory;

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

    private final int MENU_LOCK = 2;
    private final int MENU_MORE = 3;

    private String url;
    private VideoView mVideoView;
    private AndroidMediaController controller;
    private ProgressBar pb;
    private MenuItem lockMenu;
    private SearchEngine engine;
    private boolean mStopped;
    private boolean isLock;
    private boolean bgEnable;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        mOrientationListener = new OrientationListener(this);

        setContentView(R.layout.activity_main);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(0x40000000));
        mVideoView = (VideoView) findViewById(R.id.buffer);
        mVideoView.setUserAgent(Constants.USER_AGENT);
        mVideoView.setOnErrorListener((mp, what, extra) -> {
            isLock = false;
            record((int) mp.getDuration(), (int) mp.getCurrentPosition());
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
        pb = (ProgressBar) findViewById(R.id.probar);

        setTitle(getIntent().getStringExtra("title"));
        url = SearchEngineFactory.getHost() + getIntent().getSerializableExtra("url");
        Log.e(TAG, "[INFO:CONSOLE]" + url);
        engine = SearchEngineFactory.create();
        engine.load(url, new SearchEngine.Callback() {
            @Override
            public void play(String path) {
                MainActivity.this.play(path);
            }

            @Override
            public void finish() {
                MainActivity.this.finish();
            }
        });
    }

    /**
     * TODO: Set the path variable to a streaming video URL or a local media file
     * path.
     */
    private void play(String path) {
        Uri uri = Uri.parse(path);
        mVideoView.setVideoURI(uri);
        mVideoView.setMediaController(controller);
        mVideoView.requestFocus();
        mVideoView.setOnInfoListener(this);
        mVideoView.setOnPreparedListener(mp ->
                pb.setVisibility(View.GONE));

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
                if (!mStopped || bgEnable)
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
        lockMenu = menu.add(Menu.NONE, MENU_LOCK, MENU_LOCK, null);
        lockMenu.setIcon(R.drawable.ic_lock_24dp);
        lockMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
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
        } else if (id == MENU_LOCK) {
            isLock = !isLock;
            lockMenu.setIcon(isLock ? R.drawable.ic_lock_open_24dp : R.drawable.ic_lock_24dp);
            controller.setLock(isLock);
            controller.hide();
        } else if (id == MENU_MORE) {
            if (!isLock)
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
        engine.destroy();
        record(mVideoView.getDuration(), mVideoView.getCurrentPosition());
        mVideoView.release(true);
        super.finish();
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
        if (!bgEnable)
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isLock)
            return true;
        return super.onKeyDown(keyCode, event);
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
    }
}
