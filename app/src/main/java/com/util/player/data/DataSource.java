package com.util.player.data;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class DataSource {

    public static final String URL_KEY_DEFAULT = "URL_KEY_DEFAULT";

    public int currentUrlIndex;
    public LinkedHashMap urlsMap = new LinkedHashMap();
    public String title = "";//播放名字
    public String playId; //播放id
    public String playImg; //播放图片
    public int playForm; //播放来源
    public HashMap headerMap = new HashMap();
    public boolean looping = false;
    public Object[] objects;

    public DataSource(String url) {
        urlsMap.put(URL_KEY_DEFAULT, url);
    }

    public DataSource(String url, String title) {
        urlsMap.put(URL_KEY_DEFAULT, url);
        this.title = title;
    }

    public DataSource(Object url) {
        urlsMap.put(URL_KEY_DEFAULT, url);
    }

    public DataSource(LinkedHashMap urlsMap) {
        this.urlsMap.clear();
        this.urlsMap.putAll(urlsMap);
    }

    public DataSource(LinkedHashMap urlsMap, String title) {
        this.urlsMap.clear();
        this.urlsMap.putAll(urlsMap);
        this.title = title;
    }

    public Object getCurrentUrl() {
        return getValueFromLinkedMap(currentUrlIndex);
    }


    public String getKeyFromDataSource(int index) {
        int currentIndex = 0;
        for (Object key : urlsMap.keySet()) {
            if (currentIndex == index) {
                return key.toString();
            }
            currentIndex++;
        }
        return null;
    }

    public Object getValueFromLinkedMap(int index) {
        int currentIndex = 0;
        for (Object key : urlsMap.keySet()) {
            if (currentIndex == index) {
                return urlsMap.get(key);
            }
            currentIndex++;
        }
        return null;
    }

    public boolean containsTheUrl(Object object) {
        if (object != null) {
            return urlsMap.containsValue(object);
        }
        return false;
    }
}
