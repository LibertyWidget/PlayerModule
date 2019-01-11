package com.util.player.util;

import com.util.player.view.BaseUniversalPlayerView;

/**
 * Put JZVideoPlayer into layout
 * From a JZVideoPlayer to another JZVideoPlayer
 * Created by Nathen on 16/7/26.
 */
public class UniversalPlayerMgr {

    public static BaseUniversalPlayerView FIRST_FLOOR_JZVD;
    public static BaseUniversalPlayerView SECOND_FLOOR_JZVD;

    public static BaseUniversalPlayerView getFirstFloor() {
        return FIRST_FLOOR_JZVD;
    }

    public static void setFirstFloor(BaseUniversalPlayerView jzvd) {
        FIRST_FLOOR_JZVD = jzvd;
    }

    public static BaseUniversalPlayerView getSecondFloor() {
        return SECOND_FLOOR_JZVD;
    }

    public static void setSecondFloor(BaseUniversalPlayerView jzvd) {
        SECOND_FLOOR_JZVD = jzvd;
    }

    public static BaseUniversalPlayerView getCurrentJzvd() {
        if (getSecondFloor() != null) {
            return getSecondFloor();
        }
        return getFirstFloor();
    }

    public static void completeAll() {
        if (SECOND_FLOOR_JZVD != null) {
            SECOND_FLOOR_JZVD.onCompletion();
            SECOND_FLOOR_JZVD = null;
        }
        if (FIRST_FLOOR_JZVD != null) {
            FIRST_FLOOR_JZVD.onCompletion();
            FIRST_FLOOR_JZVD = null;
        }
    }
}
