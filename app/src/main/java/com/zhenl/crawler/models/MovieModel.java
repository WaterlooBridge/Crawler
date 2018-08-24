package com.zhenl.crawler.models;

/**
 * Created by lin on 2018/2/22.
 */

public class MovieModel {

    public String url;
    private String img;
    public String title;
    public String date;

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = handleImg(img);
    }

    public static String handleImg(String img) {
        if (img != null && img.startsWith("//"))
            img = "http:" + img;
        return img;
    }
}
