package com.util.player.data;

public class DataPlayInfo {
    private String name;
    private String url;
    private String tag;

    public DataPlayInfo(String name, String url) {
        setName(name);
        setUrl(url);
    }

    public DataPlayInfo(String name, String url, String tag) {
        setName(name);
        setUrl(url);
        setTag(tag);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
