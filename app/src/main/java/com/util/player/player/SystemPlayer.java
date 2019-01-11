package com.util.player.player;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.Surface;

import com.util.player.util.PlayerMediaInterface;
import com.util.player.util.UniversalPlayerMgr;
import com.util.player.view.BaseUniversalPlayerView;
import com.util.player.view.MediaManager;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 实现系统的播放引擎
 */
@RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class SystemPlayer extends PlayerMediaInterface implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener, MediaPlayer.OnVideoSizeChangedListener {

    public MediaPlayer mediaPlayer;

    @Override
    public void start() {
        mediaPlayer.start();
    }

    @Override
    public void prepare() {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setLooping(jzDataSource.looping);
            mediaPlayer.setOnPreparedListener(SystemPlayer.this);
            mediaPlayer.setOnCompletionListener(SystemPlayer.this);
            mediaPlayer.setOnBufferingUpdateListener(SystemPlayer.this);
            mediaPlayer.setScreenOnWhilePlaying(true);
            mediaPlayer.setOnSeekCompleteListener(SystemPlayer.this);
            mediaPlayer.setOnErrorListener(SystemPlayer.this);
            mediaPlayer.setOnInfoListener(SystemPlayer.this);
            mediaPlayer.setOnVideoSizeChangedListener(SystemPlayer.this);
            Class<MediaPlayer> clazz = MediaPlayer.class;
            Method method = clazz.getDeclaredMethod("setDataSource", String.class, Map.class);
//            if (dataSourceObjects.length > 2) {
            method.invoke(mediaPlayer, jzDataSource.getCurrentUrl().toString(), jzDataSource.headerMap);
//            } else {
//                method.invoke(mediaPlayer, currentDataSource.toString(), null);
//            }
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void pause() {
        mediaPlayer.pause();
    }

    @Override
    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    @Override
    public void seekTo(long time) {
        try {
            mediaPlayer.seekTo((int) time);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void release() {
        if (mediaPlayer != null)
            mediaPlayer.release();
    }

    @Override
    public long getCurrentPosition() {
        if (mediaPlayer != null) {
            return mediaPlayer.getCurrentPosition();
        } else {
            return 0;
        }
    }

    @Override
    public long getDuration() {
        if (mediaPlayer != null) {
            return mediaPlayer.getDuration();
        } else {
            return 0;
        }
    }

    @Override
    public void setSurface(Surface surface) {
        mediaPlayer.setSurface(surface);
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        mediaPlayer.setVolume(leftVolume, rightVolume);
    }


    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaPlayer.start();
        if (jzDataSource.getCurrentUrl().toString().toLowerCase().contains("mp3") ||
                jzDataSource.getCurrentUrl().toString().toLowerCase().contains("wav")) {
            MediaManager.instance().mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (UniversalPlayerMgr.getCurrentJzvd() != null) {
                        UniversalPlayerMgr.getCurrentJzvd().onPrepared();
                    }
                }
            });
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        MediaManager.instance().mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (UniversalPlayerMgr.getCurrentJzvd() != null) {
                    UniversalPlayerMgr.getCurrentJzvd().onAutoCompletion();
                }
            }
        });
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, final int percent) {
        MediaManager.instance().mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (UniversalPlayerMgr.getCurrentJzvd() != null) {
                    UniversalPlayerMgr.getCurrentJzvd().setBufferProgress(percent);
                }
            }
        });
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
        MediaManager.instance().mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (UniversalPlayerMgr.getCurrentJzvd() != null) {
                    UniversalPlayerMgr.getCurrentJzvd().onSeekComplete();
                }
            }
        });
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, final int what, final int extra) {
        MediaManager.instance().mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (UniversalPlayerMgr.getCurrentJzvd() != null) {
                    UniversalPlayerMgr.getCurrentJzvd().onError(what, extra);
                }
            }
        });
        return true;
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, final int what, final int extra) {
        MediaManager.instance().mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (UniversalPlayerMgr.getCurrentJzvd() != null) {
                    if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                        if (UniversalPlayerMgr.getCurrentJzvd().currentState == BaseUniversalPlayerView.CURRENT_STATE_PREPARING
                                || UniversalPlayerMgr.getCurrentJzvd().currentState == BaseUniversalPlayerView.CURRENT_STATE_PREPARING_CHANGING_URL) {
                            UniversalPlayerMgr.getCurrentJzvd().onPrepared();
                        }
                    } else {
                        UniversalPlayerMgr.getCurrentJzvd().onInfo(what, extra);
                    }
                }
            }
        });
        return false;
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mediaPlayer, int width, int height) {
        MediaManager.instance().currentVideoWidth = width;
        MediaManager.instance().currentVideoHeight = height;
        MediaManager.instance().mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (UniversalPlayerMgr.getCurrentJzvd() != null) {
                    UniversalPlayerMgr.getCurrentJzvd().onVideoSizeChanged();
                }
            }
        });
    }
}
