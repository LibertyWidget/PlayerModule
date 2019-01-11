package com.util.player.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.util.player.UniversalPlayerView;
import com.util.player.action.ActionUser;
import com.util.player.data.DataPlayInfo;
import com.util.player.data.DataSource;
import com.util.player.util.PlayerMediaInterface;
import com.util.player.util.ScreenSwitchUtils;
import com.util.player.util.UniversalPlayerMgr;
import com.util.player.util.Utils;
import com.util.player.R;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


@RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public abstract class BaseUniversalPlayerView extends FrameLayout implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, View.OnTouchListener {

    public static final int THRESHOLD = 80;
    public static final int FULL_SCREEN_NORMAL_DELAY = 300;

    public static final int SCREEN_WINDOW_NORMAL = 0;
    public static final int SCREEN_WINDOW_LIST = 1;
    public static final int SCREEN_WINDOW_FULLSCREEN = 2;
    public static final int SCREEN_WINDOW_TINY = 3;

    public static final int CURRENT_STATE_IDLE = -1;
    public static final int CURRENT_STATE_NORMAL = 0;
    public static final int CURRENT_STATE_PREPARING = 1;
    public static final int CURRENT_STATE_PREPARING_CHANGING_URL = 2;
    public static final int CURRENT_STATE_PLAYING = 3;
    public static final int CURRENT_STATE_PAUSE = 5;
    public static final int CURRENT_STATE_AUTO_COMPLETE = 6;
    public static final int CURRENT_STATE_ERROR = 7;

    public static final int VIDEO_IMAGE_DISPLAY_TYPE_ADAPTER = 0;//default
    public static final int VIDEO_IMAGE_DISPLAY_TYPE_FILL_PARENT = 1;
    public static final int VIDEO_IMAGE_DISPLAY_TYPE_FILL_SCROP = 2;
    public static final int VIDEO_IMAGE_DISPLAY_TYPE_ORIGINAL = 3;
    public static boolean ACTION_BAR_EXIST = true;
    public static boolean TOOL_BAR_EXIST = true;
    public static int FULLSCREEN_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
    public static int NORMAL_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    public static boolean SAVE_PROGRESS = true;
    public static boolean WIFI_TIP_DIALOG_SHOWED = false;
    public static int VIDEO_IMAGE_DISPLAY_TYPE = 0;
    public static long CLICK_QUIT_FULLSCREEN_TIME = 0;
    public static long lastAutoFullscreenTime = 0;

    public ScreenSwitchUtils mScreenSwitchUtils;
    public TextView mClarityInfoView, mEpisodeInfoView;

    public static AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {//是否新建个class，代码更规矩，并且变量的位置也很尴尬
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    releaseAllVideos();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    try {
                        BaseUniversalPlayerView player = UniversalPlayerMgr.getCurrentJzvd();
                        if (player != null && player.currentState == BaseUniversalPlayerView.CURRENT_STATE_PLAYING) {
                            player.startButton.performClick();
                        }
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    break;
            }
        }
    };
    protected static ActionUser JZ_USER_EVENT;
    protected Timer UPDATE_PROGRESS_TIMER;
    public int currentState = -1;
    public int currentScreen = -1;
    public long seekToInAdvance = 0;
    public ImageView startButton;
    public SeekBar progressBar;
    public ImageView fullscreenButton;
    public TextView currentTimeTextView, totalTimeTextView;
    public ViewGroup textureViewContainer;
    public ViewGroup topContainer, bottomContainer;
    public int widthRatio = 0;
    public int heightRatio = 0;
    public DataSource jzDataSource;
    public int positionInList = -1;
    public int videoRotation = 0;
    protected int mScreenWidth;
    protected int mScreenHeight;
    protected AudioManager mAudioManager;
    protected ProgressTimerTask mProgressTimerTask;
    protected boolean mTouchingProgressBar;
    protected float mDownX;
    protected float mDownY;
    protected boolean mChangeVolume;
    protected boolean mChangePosition;
    protected boolean mChangeBrightness;
    protected long mGestureDownPosition;
    protected int mGestureDownVolume;
    protected float mGestureDownBrightness;
    protected long mSeekTimePosition;
    public boolean tmp_test_back = false;
    //是否锁
    protected boolean isLock = false;
    public static String mClarityText = "超清";

    public BaseUniversalPlayerView(Context context) {
        super(context);
        init(context);
    }

    public BaseUniversalPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public static void releaseAllVideos() {
        if ((System.currentTimeMillis() - CLICK_QUIT_FULLSCREEN_TIME) > FULL_SCREEN_NORMAL_DELAY) {
            UniversalPlayerMgr.completeAll();
            MediaManager.instance().positionInList = -1;
            MediaManager.instance().releaseMediaPlayer();
        }
    }

    public static void startFullscreen(Context context, Class _class, String url, String title) {
        startFullscreen(context, _class, new DataSource(url, title));
    }

    public static void startFullscreen(Context context, Class _class, DataSource jzDataSource) {
        hideSupportActionBar(context);
        Utils.setRequestedOrientation(context, FULLSCREEN_ORIENTATION);
        ViewGroup vp = (Utils.scanForActivity(context)).findViewById(Window.ID_ANDROID_CONTENT);
        View old = vp.findViewById(R.id.jz_fullscreen_id);
        if (old != null)
            vp.removeView(old);
        try {
            Constructor<BaseUniversalPlayerView> constructor = _class.getConstructor(Context.class);
            final BaseUniversalPlayerView jzvd = constructor.newInstance(context);
            jzvd.setId(R.id.jz_fullscreen_id);
            LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            vp.addView(jzvd, lp);
            jzvd.play(jzDataSource, UniversalPlayerView.SCREEN_WINDOW_FULLSCREEN);
            CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();
            jzvd.startButton.performClick();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean backPress() {
        if ((System.currentTimeMillis() - CLICK_QUIT_FULLSCREEN_TIME) < FULL_SCREEN_NORMAL_DELAY)
            return false;

        if (UniversalPlayerMgr.getSecondFloor() != null) {
            CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();
            if (UniversalPlayerMgr.getFirstFloor().jzDataSource.containsTheUrl(MediaManager.getDataSource().getCurrentUrl())) {
                BaseUniversalPlayerView jzvd = UniversalPlayerMgr.getSecondFloor();
                jzvd.onEvent(jzvd.currentScreen == UniversalPlayerView.SCREEN_WINDOW_FULLSCREEN ?
                        ActionUser.ON_QUIT_FULLSCREEN :
                        ActionUser.ON_QUIT_TINYSCREEN);
                UniversalPlayerMgr.getFirstFloor().playOnThisJzvd();
            } else {
                quitFullscreenOrTinyWindow();
            }
            return true;
        } else if (UniversalPlayerMgr.getFirstFloor() != null &&
                (UniversalPlayerMgr.getFirstFloor().currentScreen == SCREEN_WINDOW_FULLSCREEN ||
                        UniversalPlayerMgr.getFirstFloor().currentScreen == SCREEN_WINDOW_TINY)) {//以前我总想把这两个判断写到一起，这分明是两个独立是逻辑
            CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();
            quitFullscreenOrTinyWindow();
            return true;
        }
        return false;
    }

    public static void quitFullscreenOrTinyWindow() {
        //直接退出全屏和小窗
        UniversalPlayerMgr.getFirstFloor().clearFloatScreen();
        MediaManager.instance().releaseMediaPlayer();
        UniversalPlayerMgr.completeAll();
    }

    @SuppressLint("RestrictedApi")
    public static void showSupportActionBar(Context context) {
        if (ACTION_BAR_EXIST && Utils.getAppCompActivity(context) != null) {
            ActionBar ab = Utils.getAppCompActivity(context).getSupportActionBar();
            if (ab != null) {
                ab.setShowHideAnimationEnabled(false);
                ab.show();
            }
        }
        if (TOOL_BAR_EXIST) {
            Utils.getWindow(context).clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @SuppressLint("RestrictedApi")
    public static void hideSupportActionBar(Context context) {
        if (ACTION_BAR_EXIST && Utils.getAppCompActivity(context) != null) {
            ActionBar ab = Utils.getAppCompActivity(context).getSupportActionBar();
            if (ab != null) {
                ab.setShowHideAnimationEnabled(false);
                ab.hide();
            }
        }
        if (TOOL_BAR_EXIST) {
            Utils.getWindow(context).setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }


    public static void setJzUserAction(ActionUser jzUserEvent) {
        JZ_USER_EVENT = jzUserEvent;
    }

    public static void goOnPlayOnResume() {
        if (UniversalPlayerMgr.getCurrentJzvd() != null) {
            BaseUniversalPlayerView jzvd = UniversalPlayerMgr.getCurrentJzvd();
            if (jzvd.currentState == BaseUniversalPlayerView.CURRENT_STATE_PAUSE) {
                if (ON_PLAY_PAUSE_TMP_STATE == CURRENT_STATE_PAUSE) {
                    jzvd.onStatePause();
                    MediaManager.pause();
                } else {
                    jzvd.onStatePlaying();
                    MediaManager.start();
                }
                ON_PLAY_PAUSE_TMP_STATE = 0;
            }
        }
    }

    public static int ON_PLAY_PAUSE_TMP_STATE = 0;

    public static void goOnPlayOnPause() {
        if (UniversalPlayerMgr.getCurrentJzvd() != null) {
            BaseUniversalPlayerView jzvd = UniversalPlayerMgr.getCurrentJzvd();
            if (jzvd.currentState == BaseUniversalPlayerView.CURRENT_STATE_AUTO_COMPLETE ||
                    jzvd.currentState == BaseUniversalPlayerView.CURRENT_STATE_NORMAL ||
                    jzvd.currentState == BaseUniversalPlayerView.CURRENT_STATE_ERROR) {
//                JZVideoPlayer.releaseAllVideos();
            } else {
                ON_PLAY_PAUSE_TMP_STATE = jzvd.currentState;
                jzvd.onStatePause();
                MediaManager.pause();
            }
        }
    }

    public static void onScrollAutoTiny(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        int lastVisibleItem = firstVisibleItem + visibleItemCount;
        int currentPlayPosition = MediaManager.instance().positionInList;
        if (currentPlayPosition >= 0) {
            if ((currentPlayPosition < firstVisibleItem || currentPlayPosition > (lastVisibleItem - 1))) {
                if (UniversalPlayerMgr.getCurrentJzvd() != null &&
                        UniversalPlayerMgr.getCurrentJzvd().currentScreen != BaseUniversalPlayerView.SCREEN_WINDOW_TINY &&
                        UniversalPlayerMgr.getCurrentJzvd().currentScreen != BaseUniversalPlayerView.SCREEN_WINDOW_FULLSCREEN) {
                    if (UniversalPlayerMgr.getCurrentJzvd().currentState == BaseUniversalPlayerView.CURRENT_STATE_PAUSE) {
                        BaseUniversalPlayerView.releaseAllVideos();
                    } else {
                        UniversalPlayerMgr.getCurrentJzvd().startWindowTiny();
                    }
                }
            } else {
                if (UniversalPlayerMgr.getCurrentJzvd() != null &&
                        UniversalPlayerMgr.getCurrentJzvd().currentScreen == BaseUniversalPlayerView.SCREEN_WINDOW_TINY) {
                    BaseUniversalPlayerView.backPress();
                }
            }
        }
    }

    public static void onScrollReleaseAllVideos(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        int lastVisibleItem = firstVisibleItem + visibleItemCount;
        int currentPlayPosition = MediaManager.instance().positionInList;
        if (currentPlayPosition >= 0) {
            if ((currentPlayPosition < firstVisibleItem || currentPlayPosition > (lastVisibleItem - 1))) {
                if (UniversalPlayerMgr.getCurrentJzvd().currentScreen != BaseUniversalPlayerView.SCREEN_WINDOW_FULLSCREEN) {
                    BaseUniversalPlayerView.releaseAllVideos();//为什么最后一个视频横屏会调用这个，其他地方不会
                }
            }
        }
    }

    public static void onChildViewAttachedToWindow(View view, int jzvdId) {
        if (UniversalPlayerMgr.getCurrentJzvd() != null && UniversalPlayerMgr.getCurrentJzvd().currentScreen == BaseUniversalPlayerView.SCREEN_WINDOW_TINY) {
            BaseUniversalPlayerView jzvd = view.findViewById(jzvdId);
            if (jzvd != null && jzvd.jzDataSource.containsTheUrl(MediaManager.getCurrentUrl())) {
                BaseUniversalPlayerView.backPress();
            }
        }
    }

    public static void onChildViewDetachedFromWindow(View view) {
        if (UniversalPlayerMgr.getCurrentJzvd() != null && UniversalPlayerMgr.getCurrentJzvd().currentScreen != BaseUniversalPlayerView.SCREEN_WINDOW_TINY) {
            BaseUniversalPlayerView jzvd = UniversalPlayerMgr.getCurrentJzvd();
            if (((ViewGroup) view).indexOfChild(jzvd) != -1) {
                if (jzvd.currentState == BaseUniversalPlayerView.CURRENT_STATE_PAUSE) {
                    BaseUniversalPlayerView.releaseAllVideos();
                } else {
                    jzvd.startWindowTiny();
                }
            }
        }
    }

    public static void setTextureViewRotation(int rotation) {
        if (MediaManager.textureView != null) {
            MediaManager.textureView.setRotation(rotation);
        }
    }

    public static void setVideoImageDisplayType(int type) {
        BaseUniversalPlayerView.VIDEO_IMAGE_DISPLAY_TYPE = type;
        if (MediaManager.textureView != null) {
            MediaManager.textureView.requestLayout();
        }
    }

    public Object getCurrentUrl() {
        return jzDataSource.getCurrentUrl();
    }

    public abstract int getLayoutId();

    public void init(Context context) {
        this.mScreenSwitchUtils = ScreenSwitchUtils.$(context);
        View.inflate(context, getLayoutId(), this);
        this.startButton = findViewById(R.id.start);
        this.fullscreenButton = findViewById(R.id.fullscreen);
        this.progressBar = findViewById(R.id.bottom_seek_progress);
        this.currentTimeTextView = findViewById(R.id.current);
        this.totalTimeTextView = findViewById(R.id.total);
        this.bottomContainer = findViewById(R.id.layout_bottom);
        this.textureViewContainer = findViewById(R.id.surface_container);
        this.topContainer = findViewById(R.id.layout_top);
        this.startButton.setOnClickListener(this);
        this.fullscreenButton.setOnClickListener(this);
        this.progressBar.setOnSeekBarChangeListener(this);
        this.bottomContainer.setOnClickListener(this);
        this.textureViewContainer.setOnClickListener(this);
        this.textureViewContainer.setOnTouchListener(this);
        this.mScreenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        this.mScreenHeight = getContext().getResources().getDisplayMetrics().heightPixels;
        this.mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);

        try {
            if (isCurrentPlay()) {
                NORMAL_ORIENTATION = ((AppCompatActivity) context).getRequestedOrientation();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
        小屏幕
       BaseUniversalPlayerView.FULLSCREEN_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
       BaseUniversalPlayerView.NORMAL_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    */

    public void play(String url, String title, int screen) {
        /*默认全屏*/
        BaseUniversalPlayerView.FULLSCREEN_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        BaseUniversalPlayerView.NORMAL_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        /*默认全屏*/
        play(new DataSource(url, title), screen);
    }

    public void play(DataSource jzDataSource, int screen) {
        if (this.jzDataSource != null && jzDataSource.getCurrentUrl() != null && this.jzDataSource.containsTheUrl(jzDataSource.getCurrentUrl())) {
            return;
        }
        if (isCurrentJZVD() && jzDataSource.containsTheUrl(MediaManager.getCurrentUrl())) {
            long position = 0;
            try {
                position = MediaManager.getCurrentPosition();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            //history
            MediaManager.instance().releaseMediaPlayer();
        } else if (isCurrentJZVD() && !jzDataSource.containsTheUrl(MediaManager.getCurrentUrl())) {
            startWindowTiny();
        } else if (!isCurrentJZVD() && jzDataSource.containsTheUrl(MediaManager.getCurrentUrl())) {
            if (UniversalPlayerMgr.getCurrentJzvd() != null &&
                    UniversalPlayerMgr.getCurrentJzvd().currentScreen == BaseUniversalPlayerView.SCREEN_WINDOW_TINY) {
                //需要退出小窗退到我这里，我这里是第一层级
                this.tmp_test_back = true;
            }
        }
        this.jzDataSource = jzDataSource;
        //决定播放什么清晰度
        if (null != jzDataSource.urlsMap)
            this.jzDataSource.currentUrlIndex = jzDataSource.urlsMap.size() - 1;
        if (this.jzDataSource.currentUrlIndex < 0) {
            this.jzDataSource.currentUrlIndex = 0;
        }
        this.currentScreen = screen;
        onStateNormal();
    }

    /**
     * 停止视频
     */
    public void stopView() {
        onEvent(ActionUser.ON_CLICK_PAUSE);
        try {
            MediaManager.pause();
            onStatePause();
        } catch (Exception ignored) {

        }
    }

    /**
     * 开始视频
     */
    public void startView() {
        onEvent(ActionUser.ON_CLICK_RESUME);
        MediaManager.start();
        onStatePlaying();
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.start) {
            if (null == jzDataSource || jzDataSource.urlsMap == null || jzDataSource.urlsMap.isEmpty() || jzDataSource.getCurrentUrl() == null) {
                Toast.makeText(getContext(), getResources().getString(R.string.no_url), Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentState == CURRENT_STATE_NORMAL) {
                if (!jzDataSource.getCurrentUrl().toString().startsWith("file") && !jzDataSource.getCurrentUrl().toString().startsWith("/") && !Utils.isWifiConnected(getContext()) && !WIFI_TIP_DIALOG_SHOWED) {
                    showWifiDialog();
                    return;
                }
                startVideo();
                onEvent(ActionUser.ON_CLICK_START_ICON);//开始的事件应该在播放之后，此处特殊
            } else if (currentState == CURRENT_STATE_PLAYING) {
                onEvent(ActionUser.ON_CLICK_PAUSE);
                MediaManager.pause();
                onStatePause();
            } else if (currentState == CURRENT_STATE_PAUSE) {
                onEvent(ActionUser.ON_CLICK_RESUME);
                MediaManager.start();
                onStatePlaying();


            } else if (currentState == CURRENT_STATE_AUTO_COMPLETE) {
                onEvent(ActionUser.ON_CLICK_START_AUTO_COMPLETE);
                startVideo();
            }
        } else if (i == R.id.fullscreen) {
            if (null != mScreenSwitchUtils)
                mScreenSwitchUtils.start(getContext());
            if (this.currentState == CURRENT_STATE_AUTO_COMPLETE) return;
            if (this.currentScreen == SCREEN_WINDOW_FULLSCREEN) {
                //quit fullscreen
                backPress();
            } else {
                onEvent(ActionUser.ON_ENTER_FULLSCREEN);
                startWindowFullscreen();
            }
        }
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (isLock)
            return false;
        float x = event.getX();
        float y = event.getY();
        int id = v.getId();
        if (id == R.id.surface_container) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    this.mTouchingProgressBar = true;
                    this.mDownX = x;
                    this.mDownY = y;
                    this.mChangeVolume = false;
                    this.mChangePosition = false;
                    this.mChangeBrightness = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    float deltaX = x - mDownX;
                    float deltaY = y - mDownY;
                    float absDeltaX = Math.abs(deltaX);
                    float absDeltaY = Math.abs(deltaY);
                    if (this.currentScreen == SCREEN_WINDOW_FULLSCREEN) {
                        if (!this.mChangePosition && !this.mChangeVolume && !this.mChangeBrightness) {
                            if (absDeltaX > THRESHOLD || absDeltaY > THRESHOLD) {
                                cancelProgressTimer();
                                if (absDeltaX >= THRESHOLD) {
                                    // 全屏模式下的CURRENT_STATE_ERROR状态下,不响应进度拖动事件.
                                    // 否则会因为mediaplayer的状态非法导致App Crash
                                    if (this.currentState != CURRENT_STATE_ERROR) {
                                        this.mChangePosition = true;
                                        this.mGestureDownPosition = getCurrentPositionWhenPlaying();
                                    }
                                } else {
                                    //如果y轴滑动距离超过设置的处理范围，那么进行滑动事件处理
                                    if (this.mDownX < this.mScreenWidth * 0.5f) {//左侧改变亮度
                                        this.mChangeBrightness = true;
                                        WindowManager.LayoutParams lp = Utils.getWindow(getContext()).getAttributes();
                                        if (lp.screenBrightness < 0) {
                                            try {
                                                this.mGestureDownBrightness = Settings.System.getInt(getContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
                                            } catch (Settings.SettingNotFoundException e) {
                                                e.printStackTrace();
                                            }
                                        } else {
                                            this.mGestureDownBrightness = lp.screenBrightness * 255;
                                        }
                                    } else {//右侧改变声音
                                        this.mChangeVolume = true;
                                        this.mGestureDownVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                                    }
                                }
                            }
                        }
                    }
                    if (this.mChangePosition) {
                        long totalTimeDuration = getDuration();
                        this.mSeekTimePosition = (int) (this.mGestureDownPosition + deltaX * totalTimeDuration / this.mScreenWidth);
                        if (this.mSeekTimePosition > totalTimeDuration)
                            this.mSeekTimePosition = totalTimeDuration;
                        String seekTime = Utils.stringForTime(this.mSeekTimePosition);
                        String totalTime = Utils.stringForTime(totalTimeDuration);

                        showProgressDialog(deltaX, seekTime, this.mSeekTimePosition, totalTime, totalTimeDuration);
                    }
                    if (this.mChangeVolume) {
                        deltaY = -deltaY;
                        int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                        int deltaV = (int) (max * deltaY * 3 / this.mScreenHeight);
                        this.mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, this.mGestureDownVolume + deltaV, 0);
                        //dialog中显示百分比
                        int volumePercent = (int) (this.mGestureDownVolume * 100 / max + deltaY * 3 * 100 / this.mScreenHeight);
                        showVolumeDialog(-deltaY, volumePercent);
                    }

                    if (this.mChangeBrightness) {
                        deltaY = -deltaY;
                        int deltaV = (int) (255 * deltaY * 3 / this.mScreenHeight);
                        WindowManager.LayoutParams params = Utils.getWindow(getContext()).getAttributes();
                        if (((this.mGestureDownBrightness + deltaV) / 255) >= 1) {//这和声音有区别，必须自己过滤一下负值
                            params.screenBrightness = 1;
                        } else if (((this.mGestureDownBrightness + deltaV) / 255) <= 0) {
                            params.screenBrightness = 0.01f;
                        } else {
                            params.screenBrightness = (this.mGestureDownBrightness + deltaV) / 255;
                        }
                        Utils.getWindow(getContext()).setAttributes(params);
                        //dialog中显示百分比
                        int brightnessPercent = (int) (this.mGestureDownBrightness * 100 / 255 + deltaY * 3 * 100 / this.mScreenHeight);
                        showBrightnessDialog(brightnessPercent);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    this.mTouchingProgressBar = false;
                    dismissProgressDialog();
                    dismissVolumeDialog();
                    dismissBrightnessDialog();
                    if (this.mChangePosition) {
                        onEvent(ActionUser.ON_TOUCH_SCREEN_SEEK_POSITION);
                        MediaManager.seekTo(this.mSeekTimePosition);
                        long duration = getDuration();
                        int progress = (int) (this.mSeekTimePosition * 100 / (duration == 0 ? 1 : duration));
                        this.progressBar.setProgress(progress);
                    }
                    if (this.mChangeVolume) {
                        onEvent(ActionUser.ON_TOUCH_SCREEN_SEEK_VOLUME);
                    }
                    startProgressTimer();
                    break;
            }
        }
        return false;
    }

    public void startVideo() {
        UniversalPlayerMgr.completeAll();
        initTextureView();
        addTextureView();
        AudioManager mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        Utils.scanForActivity(getContext()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        MediaManager.setDataSource(jzDataSource);
        MediaManager.instance().positionInList = positionInList;
        onStatePreparing();
        UniversalPlayerMgr.setFirstFloor(this);
    }

    public void onPrepared() {
        onStatePrepared();
        onStatePlaying();
    }

    public void setState(int state) {
        setState(state, 0, 0);
    }

    public void setState(int state, int urlMapIndex, int seekToInAdvance) {
        switch (state) {
            case CURRENT_STATE_NORMAL:
                this.onStateNormal();
                break;
            case CURRENT_STATE_PREPARING:
                this.onStatePreparing();
                break;
            case CURRENT_STATE_PREPARING_CHANGING_URL:
                this.changeUrl(urlMapIndex, seekToInAdvance);
                break;
            case CURRENT_STATE_PLAYING:
                this.onStatePlaying();
                break;
            case CURRENT_STATE_PAUSE:
                this.onStatePause();
                break;
            case CURRENT_STATE_ERROR:
                this.onStateError();
                break;
            case CURRENT_STATE_AUTO_COMPLETE:
                this.onStateAutoComplete();
                break;
        }
    }

    public void onStateNormal() {
        this.currentState = CURRENT_STATE_NORMAL;
        this.cancelProgressTimer();
    }

    public void onStatePreparing() {
        this.currentState = CURRENT_STATE_PREPARING;
        this.resetProgressAndTime();
    }

    public void changeUrl(int urlMapIndex, long seekToInAdvance) {
        this.currentState = CURRENT_STATE_PREPARING_CHANGING_URL;
        this.seekToInAdvance = seekToInAdvance;
        this.jzDataSource.currentUrlIndex = urlMapIndex;
        MediaManager.setDataSource(jzDataSource);
        MediaManager.instance().prepare();
    }

    public void changeUrl(DataSource jzDataSource, long seekToInAdvance) {
        this.currentState = CURRENT_STATE_PREPARING_CHANGING_URL;
        this.seekToInAdvance = seekToInAdvance;
        this.jzDataSource = jzDataSource;
        if (UniversalPlayerMgr.getSecondFloor() != null && UniversalPlayerMgr.getFirstFloor() != null) {
            UniversalPlayerMgr.getFirstFloor().jzDataSource = jzDataSource;
        }
        MediaManager.setDataSource(jzDataSource);
        MediaManager.instance().prepare();
    }


    public synchronized void onStatePrepared() {//因为这个紧接着就会进入播放状态，所以不设置state
        if (this.seekToInAdvance != 0) {
            MediaManager.seekTo(this.seekToInAdvance);
            this.seekToInAdvance = 0;
        } else {
            //history
        }
    }

    public void onStatePlaying() {
        this.currentState = CURRENT_STATE_PLAYING;
        this.startProgressTimer();
    }

    public void onStatePause() {
        this.currentState = CURRENT_STATE_PAUSE;
        this.startProgressTimer();
    }

    public void onStateError() {
        this.currentState = CURRENT_STATE_ERROR;
        this.cancelProgressTimer();
    }

    public void onStateAutoComplete() {
        this.currentState = CURRENT_STATE_AUTO_COMPLETE;
        this.cancelProgressTimer();
        this.progressBar.setProgress(100);
        this.currentTimeTextView.setText(totalTimeTextView.getText());
    }

    public void onInfo(int what, int extra) {
    }

    public void onError(int what, int extra) {
        if (what != 38 && extra != -38 && what != -38 && extra != 38 && extra != -19) {
            onStateError();
            if (isCurrentPlay()) {
                MediaManager.instance().releaseMediaPlayer();
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.currentScreen == SCREEN_WINDOW_FULLSCREEN || this.currentScreen == SCREEN_WINDOW_TINY) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        if (this.widthRatio != 0 && this.heightRatio != 0) {
            int specWidth = MeasureSpec.getSize(widthMeasureSpec);
            int specHeight = (int) ((specWidth * (float) this.heightRatio) / this.widthRatio);
            setMeasuredDimension(specWidth, specHeight);

            int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(specWidth, MeasureSpec.EXACTLY);
            int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(specHeight, MeasureSpec.EXACTLY);
            getChildAt(0).measure(childWidthMeasureSpec, childHeightMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

    }

    public void onAutoCompletion() {
        Runtime.getRuntime().gc();
        this.onEvent(ActionUser.ON_AUTO_COMPLETE);
        this.dismissVolumeDialog();
        this.dismissProgressDialog();
        this.dismissBrightnessDialog();
        this.onStateAutoComplete();

        if (this.currentScreen == SCREEN_WINDOW_FULLSCREEN || currentScreen == SCREEN_WINDOW_TINY) {
            backPress();
        }
        MediaManager.instance().releaseMediaPlayer();

        //history

    }

    public void onCompletion() {
        if (this.currentState == CURRENT_STATE_PLAYING || currentState == CURRENT_STATE_PAUSE) {
            long position = getCurrentPositionWhenPlaying();
            //history
        }
        this.cancelProgressTimer();
        this.dismissBrightnessDialog();
        this.dismissProgressDialog();
        this.dismissVolumeDialog();
        this.onStateNormal();
        this.textureViewContainer.removeView(MediaManager.textureView);
        MediaManager.instance().currentVideoWidth = 0;
        MediaManager.instance().currentVideoHeight = 0;
        AudioManager mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        if (null != mAudioManager)
            mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
        Utils.scanForActivity(getContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.clearFullscreenLayout();
        Utils.setRequestedOrientation(getContext(), NORMAL_ORIENTATION);

        if (MediaManager.surface != null) MediaManager.surface.release();
        if (MediaManager.savedSurfaceTexture != null)
            MediaManager.savedSurfaceTexture.release();
        MediaManager.textureView = null;
        MediaManager.savedSurfaceTexture = null;
    }

    public void release() {
        if (this.jzDataSource.getCurrentUrl().equals(MediaManager.getCurrentUrl()) &&
                (System.currentTimeMillis() - CLICK_QUIT_FULLSCREEN_TIME) > FULL_SCREEN_NORMAL_DELAY) {
            //在非全屏的情况下只能backPress()
            if (UniversalPlayerMgr.getSecondFloor() != null && UniversalPlayerMgr.getSecondFloor().currentScreen == SCREEN_WINDOW_FULLSCREEN) {//点击全屏

            } else if (UniversalPlayerMgr.getSecondFloor() == null && UniversalPlayerMgr.getFirstFloor() != null && UniversalPlayerMgr.getFirstFloor().currentScreen == SCREEN_WINDOW_FULLSCREEN) {//直接全屏

            } else {
                releaseAllVideos();
            }
        }
    }

    public void initTextureView() {
        removeTextureView();
        MediaManager.textureView = new UniversalPlayerTextureView(getContext());
        MediaManager.textureView.setSurfaceTextureListener(MediaManager.instance());
    }

    public void addTextureView() {
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER);
        this.textureViewContainer.addView(MediaManager.textureView, layoutParams);
    }

    public void removeTextureView() {
        MediaManager.savedSurfaceTexture = null;
        if (MediaManager.textureView != null && MediaManager.textureView.getParent() != null) {
            ((ViewGroup) MediaManager.textureView.getParent()).removeView(MediaManager.textureView);
        }
    }

    public void clearFullscreenLayout() {
        ViewGroup vp = (Utils.scanForActivity(getContext())).findViewById(Window.ID_ANDROID_CONTENT);
        View oldF = vp.findViewById(R.id.jz_fullscreen_id);
        View oldT = vp.findViewById(R.id.jz_tiny_id);
        if (oldF != null) {
            vp.removeView(oldF);
        }
        if (oldT != null) {
            vp.removeView(oldT);
        }
        showSupportActionBar(getContext());
    }

    public void clearFloatScreen() {
        Utils.setRequestedOrientation(getContext(), NORMAL_ORIENTATION);
        showSupportActionBar(getContext());
        ViewGroup vp = (Utils.scanForActivity(getContext())).findViewById(Window.ID_ANDROID_CONTENT);
        BaseUniversalPlayerView fullJzvd = vp.findViewById(R.id.jz_fullscreen_id);
        BaseUniversalPlayerView tinyJzvd = vp.findViewById(R.id.jz_tiny_id);

        if (fullJzvd != null) {
            vp.removeView(fullJzvd);
            if (fullJzvd.textureViewContainer != null)
                fullJzvd.textureViewContainer.removeView(MediaManager.textureView);
        }
        if (tinyJzvd != null) {
            vp.removeView(tinyJzvd);
            if (tinyJzvd.textureViewContainer != null)
                tinyJzvd.textureViewContainer.removeView(MediaManager.textureView);
        }
        UniversalPlayerMgr.setSecondFloor(null);
    }

    public void onVideoSizeChanged() {
        if (MediaManager.textureView != null) {
            if (videoRotation != 0) {
                MediaManager.textureView.setRotation(videoRotation);
            }
            MediaManager.textureView.setVideoSize(MediaManager.instance().currentVideoWidth, MediaManager.instance().currentVideoHeight);
        }
    }

    public void startProgressTimer() {
        this.cancelProgressTimer();
        this.UPDATE_PROGRESS_TIMER = new Timer();
        this.mProgressTimerTask = new ProgressTimerTask();
        this.UPDATE_PROGRESS_TIMER.schedule(mProgressTimerTask, 0, 300);
    }

    public void cancelProgressTimer() {
        if (this.UPDATE_PROGRESS_TIMER != null) {
            this.UPDATE_PROGRESS_TIMER.cancel();
        }
        if (this.mProgressTimerTask != null) {
            this.mProgressTimerTask.cancel();
        }
    }

    public void onProgress(int progress, long position, long duration) {
        if (!this.mTouchingProgressBar) {
            if (this.seekToManulPosition != -1) {
                if (this.seekToManulPosition > progress) {
                    return;
                } else {
                    this.seekToManulPosition = -1;
                }
            } else {
                if (progress != 0)
                    this.progressBar.setProgress(progress);
            }
        }
        //设置
        if (null != mClarityInfoView && !"".equals(mClarityText)) {
            CharSequence text = mClarityInfoView.getText();
            if (!text.equals(mClarityText))
                this.mClarityInfoView.setText(mClarityText);
        }
        if (position != 0) currentTimeTextView.setText(Utils.stringForTime(position));
        this.totalTimeTextView.setText(Utils.stringForTime(duration));
    }

    public void setBufferProgress(int bufferProgress) {
        if (bufferProgress != 0) progressBar.setSecondaryProgress(bufferProgress);
    }

    public void resetProgressAndTime() {
        this.progressBar.setProgress(0);
        this.progressBar.setSecondaryProgress(0);
        this.currentTimeTextView.setText(Utils.stringForTime(0));
        this.totalTimeTextView.setText(Utils.stringForTime(0));
    }

    public long getCurrentPositionWhenPlaying() {
        long position = 0;
        //TODO 这块的判断应该根据MediaPlayer来
        if (currentState == CURRENT_STATE_PLAYING || currentState == CURRENT_STATE_PAUSE) {
            try {
                position = MediaManager.getCurrentPosition();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return position;
            }
        }
        return position;
    }

    public long getDuration() {
        long duration = 0;
        //TODO MediaPlayer 判空的问题
//        if (MediaManager.instance().mediaPlayer == null) return duration;
        try {
            duration = MediaManager.getDuration();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return duration;
        }
        return duration;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        cancelProgressTimer();
        ViewParent vpdown = getParent();
        while (vpdown != null) {
            vpdown.requestDisallowInterceptTouchEvent(true);
            vpdown = vpdown.getParent();
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        onEvent(ActionUser.ON_SEEK_POSITION);
        startProgressTimer();
        ViewParent vpup = getParent();
        while (vpup != null) {
            vpup.requestDisallowInterceptTouchEvent(false);
            vpup = vpup.getParent();
        }
        if (currentState != CURRENT_STATE_PLAYING && currentState != CURRENT_STATE_PAUSE) return;
        long time = seekBar.getProgress() * getDuration() / 100;
        seekToManulPosition = seekBar.getProgress();
        MediaManager.seekTo(time);
    }

    public int seekToManulPosition = -1;

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            //设置这个progres对应的时间，给textview
            long duration = getDuration();
            currentTimeTextView.setText(Utils.stringForTime(progress * duration / 100));
        }
    }

    public void startWindowFullscreen() {
        hideSupportActionBar(getContext());
        ViewGroup vp = (Utils.scanForActivity(getContext())).findViewById(Window.ID_ANDROID_CONTENT);
        View old = vp.findViewById(R.id.jz_fullscreen_id);
        if (old != null) {
            vp.removeView(old);
        }
        textureViewContainer.removeView(MediaManager.textureView);
        try {
            Constructor<BaseUniversalPlayerView> constructor = (Constructor<BaseUniversalPlayerView>) BaseUniversalPlayerView.this.getClass().getConstructor(Context.class);
            BaseUniversalPlayerView jzvd = constructor.newInstance(getContext());
            jzvd.setId(R.id.jz_fullscreen_id);
            LayoutParams lp = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            vp.addView(jzvd, lp);
            jzvd.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN);
            jzvd.play(jzDataSource, UniversalPlayerView.SCREEN_WINDOW_FULLSCREEN);
            jzvd.setState(currentState);
            jzvd.addTextureView();
            UniversalPlayerMgr.setSecondFloor(jzvd);
            Utils.setRequestedOrientation(getContext(), FULLSCREEN_ORIENTATION);

            onStateNormal();
            jzvd.progressBar.setSecondaryProgress(progressBar.getSecondaryProgress());
            jzvd.startProgressTimer();
            CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startWindowTiny() {
        onEvent(ActionUser.ON_ENTER_TINYSCREEN);
        if (currentState == CURRENT_STATE_NORMAL || currentState == CURRENT_STATE_ERROR || currentState == CURRENT_STATE_AUTO_COMPLETE)
            return;
        ViewGroup vp = (Utils.scanForActivity(getContext())).findViewById(Window.ID_ANDROID_CONTENT);
        View old = vp.findViewById(R.id.jz_tiny_id);
        if (old != null) {
            vp.removeView(old);
        }
        textureViewContainer.removeView(MediaManager.textureView);

        try {
            Constructor<BaseUniversalPlayerView> constructor = (Constructor<BaseUniversalPlayerView>) BaseUniversalPlayerView.this.getClass().getConstructor(Context.class);
            BaseUniversalPlayerView jzvd = constructor.newInstance(getContext());
            jzvd.setId(R.id.jz_tiny_id);
            LayoutParams lp = new LayoutParams(400, 400);
            lp.gravity = Gravity.RIGHT | Gravity.BOTTOM;
            vp.addView(jzvd, lp);
            jzvd.play(jzDataSource, UniversalPlayerView.SCREEN_WINDOW_TINY);
            jzvd.setState(currentState);
            jzvd.addTextureView();
            UniversalPlayerMgr.setSecondFloor(jzvd);
            onStateNormal();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isCurrentPlay() {
        return isCurrentJZVD() && jzDataSource.containsTheUrl(MediaManager.getCurrentUrl());//不仅正在播放的url不能一样，并且各个清晰度也不能一样
    }

    public boolean isCurrentJZVD() {
        return UniversalPlayerMgr.getCurrentJzvd() != null && UniversalPlayerMgr.getCurrentJzvd() == this;
    }

    //退出全屏和小窗的方法
    public void playOnThisJzvd() {
        if (null != mScreenSwitchUtils)
            mScreenSwitchUtils.stop();
        //1.清空全屏和小窗的jzvd
        currentState = UniversalPlayerMgr.getSecondFloor().currentState;
        clearFloatScreen();
        //2.在本jzvd上播放
        setState(currentState);
        addTextureView();
    }

    //重力感应的时候调用的函数，
    public void autoFullscreen(float x) {
        if (isCurrentPlay() && (currentState == CURRENT_STATE_PLAYING || currentState == CURRENT_STATE_PAUSE) && currentScreen != SCREEN_WINDOW_FULLSCREEN && currentScreen != SCREEN_WINDOW_TINY) {
            if (x > 0) {
                Utils.setRequestedOrientation(getContext(), ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                Utils.setRequestedOrientation(getContext(), ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            }
            onEvent(ActionUser.ON_ENTER_FULLSCREEN);
            startWindowFullscreen();
        }
    }

    public void autoQuitFullscreen() {
        if ((System.currentTimeMillis() - lastAutoFullscreenTime) > 2000 && isCurrentPlay() && currentState == CURRENT_STATE_PLAYING && currentScreen == SCREEN_WINDOW_FULLSCREEN) {
            lastAutoFullscreenTime = System.currentTimeMillis();
            backPress();
        }
    }

    public void onEvent(int type) {
        if (JZ_USER_EVENT != null && isCurrentPlay() && !jzDataSource.urlsMap.isEmpty()) {
            JZ_USER_EVENT.onEvent(type, jzDataSource.getCurrentUrl(), currentScreen);
        }
    }

    public static void setMediaInterface(PlayerMediaInterface mediaInterface) {
        MediaManager.instance().jzMediaInterface = mediaInterface;
    }

    //TODO 是否有用
    public void onSeekComplete() {

    }

    public void showWifiDialog() {
    }

    public void showDownLoadDialog(String title, String url) {

    }

    public void showClarityDialog(ArrayList<DataPlayInfo> info) {

    }

    public void showEpisodeDialog(ArrayList<DataPlayInfo> info) {

    }


    public void showProgressDialog(float deltaX, String seekTime, long seekTimePosition, String totalTime, long totalTimeDuration) {

    }

    public void dismissProgressDialog() {

    }

    public void showVolumeDialog(float deltaY, int volumePercent) {

    }

    public void dismissVolumeDialog() {

    }

    public void showBrightnessDialog(int brightnessPercent) {

    }

    public void dismissBrightnessDialog() {

    }

    public class ProgressTimerTask extends TimerTask {
        @Override
        public void run() {
            if (currentState == CURRENT_STATE_PLAYING || currentState == CURRENT_STATE_PAUSE) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        long position = getCurrentPositionWhenPlaying();
                        long duration = getDuration();
                        int progress = (int) (position * 100 / (duration == 0 ? 1 : duration));
                        onProgress(progress, position, duration);
                    }
                });
            }
        }
    }

}
