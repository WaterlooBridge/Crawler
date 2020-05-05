package com.zhenl.crawler;

import org.junit.Test;

import java.net.URI;
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

    @Test
    public void testUri() {
        String newUrl = "/20200312/AxEgOHZ6/1200kb/hls/index.m3u8";
        URI uri = URI.create("https://www.nmgxwhz.com:65/20200312/AxEgOHZ6/index.m3u8");
        String redirectUrl = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() != -1 ? ":" + uri.getPort() : "") + newUrl;
        System.out.println(redirectUrl);
        uri = URI.create("http://g.co/androidstudio/not-mocked");
        redirectUrl = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() != -1 ? ":" + uri.getPort() : "") + newUrl;
        System.out.println(redirectUrl);
    }
}
