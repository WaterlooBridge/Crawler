package com.zhenl.crawler.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lin on 2018/8/8.
 */
public class RecordAgent {

    private static RecordAgent agent;

    public static RecordAgent getInstance() {
        if (agent == null)
            agent = new RecordAgent();
        return agent;
    }

    Map<String, Integer> records = new HashMap<>();

    private RecordAgent() {

    }

    public void record(String url, int position) {
        records.put(url, position - 5000);
    }

    public int getRecord(String url) {
        Integer pos = records.get(url);
        return pos == null ? 0 : pos;
    }
}
