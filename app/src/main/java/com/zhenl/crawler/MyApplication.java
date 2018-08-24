package com.zhenl.crawler;

import android.app.Application;

/**
 * Created by lin on 2018/8/23.
 */
public class MyApplication extends Application {

    public static MyApplication application;

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
    }

    public static MyApplication getInstance() {
        return application;
    }
}
