package com.zhenl.crawler.engines;

import com.zhenl.crawler.Constants;

/**
 * Created by lin on 2018/8/22.
 */
public class SearchEngineFactory {

    public static int type;

    public static SearchEngine create() {
        SearchEngine engine;
        switch (type) {
            case 2:
                engine = new SearchEngineImpl2();
                break;
            case 3:
                engine = new SearchEngineImpl3();
                break;
            default:
                engine = new SearchEngineImpl1();
                break;
        }
        return engine;
    }

    public static String getHost() {
        String host;
        switch (type) {
            case 2:
                host = Constants.API_HOST2;
                break;
            case 3:
                host = Constants.API_HOST3;
                break;
            default:
                host = Constants.API_HOST;
                break;
        }
        return host;
    }
}
