package com.util.player;


import android.content.pm.ActivityInfo;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.util.player.data.DataSource;
import com.util.player.player.ExoPlayer;
import com.util.player.view.BaseUniversalPlayerView;

import java.util.LinkedHashMap;

@RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class PlayManager {
    private static PlayManager instance;
    private UniversalPlayerView mUniversalPlayerView;

    private PlayManager() {
    }

    public static PlayManager $() {
        if (instance == null) {
            instance = new PlayManager();
        }
        return instance;
    }

    public void init(UniversalPlayerView universalPlayerView) {
        this.mUniversalPlayerView = universalPlayerView;
        /*默认全屏*/
        BaseUniversalPlayerView.FULLSCREEN_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        BaseUniversalPlayerView.NORMAL_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        //默认设置
        UniversalPlayerView.setMediaInterface(new ExoPlayer());
    }

    private void onSettingPlay(LinkedHashMap map, String title) {
        DataSource dataSource = new DataSource(map);
        dataSource.title = title;
        this.mUniversalPlayerView.play(dataSource, UniversalPlayerView.SCREEN_WINDOW_NORMAL);
    }

    private void onSettingPlay(String url, String title) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put(title, url);
        DataSource dataSource = new DataSource(map);
        dataSource.title = title;
        this.mUniversalPlayerView.play(dataSource, UniversalPlayerView.SCREEN_WINDOW_NORMAL);
    }

    protected void onResume() {
        onPause();
    }

    protected void onPause() {
        UniversalPlayerView.goOnPlayOnPause();
    }

    public void finish() {
        UniversalPlayerView.releaseAllVideos();
    }

    public void onBackPressed() {
        if (UniversalPlayerView.backPress()) {
            return;
        }
    }
}
