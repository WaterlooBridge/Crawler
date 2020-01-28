package com.zhenl.crawler;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by lin on 20-1-28.
 */
public class UnitTest {

    @Test
    public void testM3u8AES() {
        String str = "#EXT-X-KEY:METHOD=AES-128,URI=\"key.key\"";
        Pattern p = Pattern.compile("(?<=URI=\").*?(?=\")");
        Matcher m = p.matcher(str);
        if (m.find()) {
            String key = m.group();
            System.out.println(key);
            str = str.replace(key, "/sdcard/" + key);
        }
        System.out.println(str);
    }
}
