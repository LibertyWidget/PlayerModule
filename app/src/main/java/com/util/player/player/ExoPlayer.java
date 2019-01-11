package com.util.player.player;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.view.Surface;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoListener;
import com.util.player.util.PlayerMediaInterface;
import com.util.player.util.UniversalPlayerMgr;
import com.util.player.view.MediaManager;


@RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class ExoPlayer extends PlayerMediaInterface implements Player.EventListener, VideoListener {
    private SimpleExoPlayer simpleExoPlayer;
    private Handler mainHandler;
    private Runnable callback;
    private String TAG = "ExoPlayer";
    private MediaSource videoSource;
    private long previousSeek = 0;


    @Override
    public void start() {
        simpleExoPlayer.setPlayWhenReady(true);
    }

    @Override
    public void prepare() {
        mainHandler = new Handler();
        Context context = UniversalPlayerMgr.getCurrentJzvd().getContext();

        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

        LoadControl loadControl = new DefaultLoadControl(new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE), 360000, 600000, 1000, 5000, C.LENGTH_UNSET, false);

        // 2. Create the player

        RenderersFactory renderersFactory = new DefaultRenderersFactory(context);
        simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector, loadControl);
        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, Util.getUserAgent(context, "tag"));

        String currUrl = jzDataSource.getCurrentUrl().toString();
        if (currUrl.contains(".m3u8")) {
            videoSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(currUrl), mainHandler, null);
        } else {
            videoSource = new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(currUrl));
        }
        simpleExoPlayer.addVideoListener(this);
        simpleExoPlayer.addListener(this);

        simpleExoPlayer.prepare(videoSource);
        simpleExoPlayer.setPlayWhenReady(true);
        callback = new onBufferingUpdate();
    }


    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
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

    @Override
    public void onRenderedFirstFrame() {
    }

    private class onBufferingUpdate implements Runnable {
        @Override
        public void run() {
            final int percent = simpleExoPlayer.getBufferedPercentage();
            MediaManager.instance().mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (UniversalPlayerMgr.getCurrentJzvd() != null) {
                        UniversalPlayerMgr.getCurrentJzvd().setBufferProgress(percent);
                    }
                }
            });
            if (percent < 100) {
                mainHandler.postDelayed(callback, 300);
            } else {
                mainHandler.removeCallbacks(callback);
            }
        }
    }

    @Override
    public void pause() {
        simpleExoPlayer.setPlayWhenReady(false);
    }

    /**
     * 设置倍速
     */
    public void setDoubleSpeed(float speed) {
        if (simpleExoPlayer != null) {
            PlaybackParameters playbackParameters = new PlaybackParameters(speed, 1.0F);
            simpleExoPlayer.setPlaybackParameters(playbackParameters);
        }
    }


    @Override
    public boolean isPlaying() {
        return simpleExoPlayer.getPlayWhenReady();
    }

    @Override
    public void seekTo(long time) {
        if (time != previousSeek) {
            simpleExoPlayer.seekTo(time);
            previousSeek = time;
            UniversalPlayerMgr.getCurrentJzvd().seekToInAdvance = time;
        }
    }

    @Override
    public void release() {
        if (simpleExoPlayer != null) {
            simpleExoPlayer.release();
        }
        if (mainHandler != null)
            mainHandler.removeCallbacks(callback);
    }

    @Override
    public long getCurrentPosition() {
        if (simpleExoPlayer != null)
            return simpleExoPlayer.getCurrentPosition();
        else return 0;
    }

    @Override
    public long getDuration() {
        if (simpleExoPlayer != null)
            return simpleExoPlayer.getDuration();
        else return 0;
    }

    @Override
    public void setSurface(Surface surface) {
        simpleExoPlayer.setVideoSurface(surface);
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        simpleExoPlayer.setVolume(leftVolume);
        simpleExoPlayer.setVolume(rightVolume);
    }

    @Override
    public void onTimelineChanged(final Timeline timeline, Object manifest, final int reason) {
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
    }

    @Override
    public void onPlayerStateChanged(final boolean playWhenReady, final int playbackState) {
        MediaManager.instance().mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (UniversalPlayerMgr.getCurrentJzvd() != null) {
                    switch (playbackState) {
                        case Player.STATE_IDLE: {
                        }
                        break;
                        case Player.STATE_BUFFERING: {
                            mainHandler.post(callback);
                        }
                        break;
                        case Player.STATE_READY: {
                            if (playWhenReady) {
                                UniversalPlayerMgr.getCurrentJzvd().onPrepared();
                            } else {
                            }
                        }
                        break;
                        case Player.STATE_ENDED: {
                            UniversalPlayerMgr.getCurrentJzvd().onAutoCompletion();
                        }
                        break;
                    }
                }
            }
        });
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        MediaManager.instance().mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (UniversalPlayerMgr.getCurrentJzvd() != null) {
                    UniversalPlayerMgr.getCurrentJzvd().onError(1000, 1000);
                }
            }
        });
    }

    @Override
    public void onPositionDiscontinuity(int reason) {

    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onSeekProcessed() {
        MediaManager.instance().mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (UniversalPlayerMgr.getCurrentJzvd() != null) {
                    UniversalPlayerMgr.getCurrentJzvd().onSeekComplete();
                }
            }
        });
    }

}
