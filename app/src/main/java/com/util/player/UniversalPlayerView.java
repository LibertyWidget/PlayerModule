package com.util.player;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.util.player.action.ActionUser;
import com.util.player.action.ActionUserUniversalPlayerView;
import com.util.player.data.DataPlayInfo;
import com.util.player.data.DataSource;
import com.util.player.player.ExoPlayer;
import com.util.player.util.UniversalPlayerMgr;
import com.util.player.util.Utils;
import com.util.player.view.BaseUniversalPlayerView;
import com.util.player.view.MediaManager;
import com.util.player.view.PlayListAdapter;
import com.util.player.view.UniversalPlayerTextureView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


/**
 * 播放器
 */
@RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class UniversalPlayerView extends BaseUniversalPlayerView {

    protected static Timer DISMISS_CONTROL_VIEW_TIMER;
    public static long LAST_GET_BATTERY_LEVEL_TIME = 0;
    public static int LAST_GET_BATTERY_LEVEL_PERCENT = 70;
    public LinearLayout batteryTimeLayout, mRetryLayout;
    private View llScreenshotsView, llLockView, mDownLoadView;
    protected DismissControlViewTimerTask mDismissControlViewTimerTask;
    protected Dialog mProgressDialog, mVolumeDialog, mBrightnessDialog;
    protected ProgressBar mDialogProgressBar, mDialogVolumeProgressBar, mDialogBrightnessProgressBar, bottomProgressBar, loadingProgressBar;

    private TextView mDialogSeekTime;
    private TextView mDialogTotalTime;
    private TextView mDialogVolumeTextView;
    private TextView mDialogBrightnessTextView;
    private TextView videoCurrentTime;
    private TextView replayTextView;
    private TextView fillingView, speedView;
    public TextView titleTextView;

    public ImageView mDialogIcon;
    public ImageView mDialogVolumeImageView;
    public ImageView batteryLevel, lockView;
    public LinearLayout backButton;
    public ImageView thumbImageView;
    public ImageView tinyBackImageView;
    //播放类型
    private int mTypePlayer = 0;
    //倍速
    private float mTypeSpeed = 1.0f;
    private boolean whereFromDownPlay = false;


    protected static ArrayList<DataPlayInfo> mEpisodeInfo;
    protected static ArrayList<DataPlayInfo> mClarityInfo;

    private static IOnUniversalPlayerViewListener mIOnUniversalPlayerViewListener;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                int level = intent.getIntExtra("level", 0);
                int scale = intent.getIntExtra("scale", 100);
                LAST_GET_BATTERY_LEVEL_PERCENT = level * 100 / scale;
                setBatteryLevel();
                getContext().unregisterReceiver(mBroadcastReceiver);
            }
        }
    };

    public UniversalPlayerView(Context context) {
        super(context);
    }

    public UniversalPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void init(Context context) {
        super.init(context);
        this.batteryTimeLayout = findViewById(R.id.battery_time_layout);
        this.bottomProgressBar = findViewById(R.id.bottom_progress);
        this.fillingView = findViewById(R.id.fillingView);
        this.speedView = findViewById(R.id.speedView);
        this.llScreenshotsView = findViewById(R.id.llScreenshotsView);
        this.llLockView = findViewById(R.id.llLockView);

        this.lockView = findViewById(R.id.lockView);
        this.mEpisodeInfoView = findViewById(R.id.episodeView);
        this.mClarityInfoView = findViewById(R.id.clarityView);
        this.titleTextView = findViewById(R.id.title);
        this.titleTextView.setVisibility(GONE);
        this.backButton = findViewById(R.id.back);
        this.thumbImageView = findViewById(R.id.thumb);
        this.loadingProgressBar = findViewById(R.id.loading);
        this.tinyBackImageView = findViewById(R.id.back_tiny);
        this.batteryLevel = findViewById(R.id.battery_level);
        this.videoCurrentTime = findViewById(R.id.video_current_time);
        this.replayTextView = findViewById(R.id.replay_text);
        this.mRetryLayout = findViewById(R.id.retry_layout);

        this.mDownLoadView = this.findViewById(R.id.downLoadView);
        this.mDownLoadView.setOnClickListener(this);
        this.findViewById(R.id.retry_btn).setOnClickListener(this);

        this.thumbImageView.setOnClickListener(this);
        this.backButton.setOnClickListener(this);
        this.tinyBackImageView.setOnClickListener(this);

        this.fillingView.setOnClickListener(this);
        this.speedView.setOnClickListener(this);
        this.mClarityInfoView.setOnClickListener(this);
        this.mEpisodeInfoView.setOnClickListener(this);
        this.llScreenshotsView.setOnClickListener(this);
        this.llLockView.setOnClickListener(this);
    }

    public void play(DataSource jzDataSource, int screen) {
        super.play(jzDataSource, screen);
        this.whereFromDownPlay = false;
        if (currentScreen == SCREEN_WINDOW_FULLSCREEN) {
            this.setSpeed();
            this.fullscreenButton.setImageResource(R.drawable.jz_shrink);
            this.titleTextView.setVisibility(VISIBLE);
            this.titleTextView.setText(jzDataSource.title);
            this.backButton.setVisibility(View.VISIBLE);
            this.tinyBackImageView.setVisibility(View.INVISIBLE);
            this.batteryTimeLayout.setVisibility(View.VISIBLE);
            changeStartButtonSize((int) getResources().getDimension(R.dimen.jz_start_button_w_h_fullscreen));
        } else if (currentScreen == SCREEN_WINDOW_NORMAL || currentScreen == SCREEN_WINDOW_LIST) {
            this.fullscreenButton.setImageResource(R.drawable.jz_enlarge);
            this.backButton.setVisibility(View.GONE);
            this.tinyBackImageView.setVisibility(View.INVISIBLE);
            this.changeStartButtonSize((int) getResources().getDimension(R.dimen.jz_start_button_w_h_normal));
            this.batteryTimeLayout.setVisibility(View.GONE);
            this.mClarityInfoView.setVisibility(View.GONE);
            this.mEpisodeInfoView.setVisibility(View.GONE);
        } else if (currentScreen == SCREEN_WINDOW_TINY) {
            this.tinyBackImageView.setVisibility(View.VISIBLE);
            this.setAllControlsVisibility(View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE);
            this.batteryTimeLayout.setVisibility(View.GONE);
            this.mClarityInfoView.setVisibility(View.GONE);
            this.mEpisodeInfoView.setVisibility(View.GONE);
        }
        setSystemTimeAndBattery();
        if (tmp_test_back) {
            tmp_test_back = false;
            UniversalPlayerMgr.setFirstFloor(this);
            backPress();
        }
    }

    public void changeStartButtonSize(int size) {
        ViewGroup.LayoutParams lp = startButton.getLayoutParams();
        lp.height = size;
        lp.width = size;
        lp = loadingProgressBar.getLayoutParams();
        lp.height = size;
        lp.width = size;
    }

    @Override
    public int getLayoutId() {
        return R.layout.jz_layout_standard;
    }

    @Override
    public void onStateNormal() {
        super.onStateNormal();
        changeUiToNormal();
    }

    @Override
    public void onStatePreparing() {
        super.onStatePreparing();
        changeUiToPreparing();
    }

    @Override
    public void changeUrl(int urlMapIndex, long seekToInAdvance) {
        super.changeUrl(urlMapIndex, seekToInAdvance);
        loadingProgressBar.setVisibility(VISIBLE);
        startButton.setVisibility(INVISIBLE);
    }

    @Override
    public void changeUrl(DataSource jzDataSource, long seekToInAdvance) {
        super.changeUrl(jzDataSource, seekToInAdvance);
        this.titleTextView.setText(jzDataSource.title);
        this.loadingProgressBar.setVisibility(VISIBLE);
        this.startButton.setVisibility(INVISIBLE);
    }

    @Override
    public void onStatePlaying() {
        super.onStatePlaying();
        changeUiToPlayingClear();
    }

    @Override
    public void onStatePause() {
        super.onStatePause();
        changeUiToPauseShow();
        cancelDismissControlViewTimer();
    }

    @Override
    public void onStateError() {
        super.onStateError();
        changeUiToError();
    }

    @Override
    public void onStateAutoComplete() {
        super.onStateAutoComplete();
        changeUiToComplete();
        cancelDismissControlViewTimer();
        bottomProgressBar.setProgress(100);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int id = v.getId();
        if (!isLock) {
            if (id == R.id.surface_container) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        break;
                    case MotionEvent.ACTION_MOVE:
                        break;
                    case MotionEvent.ACTION_UP:
                        startDismissControlViewTimer();
                        if (mChangePosition) {
                            long duration = getDuration();
                            int progress = (int) (mSeekTimePosition * 100 / (duration == 0 ? 1 : duration));
                            bottomProgressBar.setProgress(progress);
                        }
                        if (!mChangePosition && !mChangeVolume) {
                            onEvent(ActionUserUniversalPlayerView.ON_CLICK_BLANK);
                            onClickUiToggle();
                        }
                        break;
                }
            } else if (id == R.id.bottom_seek_progress) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        cancelDismissControlViewTimer();
                        break;
                    case MotionEvent.ACTION_UP:
                        startDismissControlViewTimer();
                        break;
                }
            }
            if (!whereFromDownPlay) {
                //mEpisodeInfoView
                if (null != mEpisodeInfoView)
                    if (null == mEpisodeInfo || mEpisodeInfo.size() == 0) {
                        if (mEpisodeInfoView.getVisibility() != GONE)
                            mEpisodeInfoView.setVisibility(GONE);
                    } else {
                        if (mEpisodeInfoView.getVisibility() != VISIBLE && currentScreen == SCREEN_WINDOW_FULLSCREEN)
                            mEpisodeInfoView.setVisibility(VISIBLE);
                    }
                //mClarityInfoView
                if (null != mClarityInfoView)
                    if (null == mClarityInfo || mClarityInfo.size() == 0) {
                        if (mClarityInfoView.getVisibility() != GONE)
                            mClarityInfoView.setVisibility(GONE);
                    } else {
                        if (mClarityInfoView.getVisibility() != VISIBLE && currentScreen == SCREEN_WINDOW_FULLSCREEN)
                            mClarityInfoView.setVisibility(VISIBLE);
                    }
            }
        }
        return super.onTouch(v, event);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        int i = v.getId();
        if (i == R.id.thumb) {
            if (null == jzDataSource || jzDataSource.urlsMap == null || jzDataSource.urlsMap.isEmpty() || jzDataSource.getCurrentUrl() == null) {
                Toast.makeText(getContext(), getResources().getString(R.string.no_url), Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentState == CURRENT_STATE_NORMAL) {
                if (!jzDataSource.getCurrentUrl().toString().startsWith("file") &&
                        !jzDataSource.getCurrentUrl().toString().startsWith("/") &&
                        !Utils.isWifiConnected(getContext()) && !WIFI_TIP_DIALOG_SHOWED) {
                    showWifiDialog();
                    return;
                }
                startVideo();
                onEvent(ActionUserUniversalPlayerView.ON_CLICK_START_THUMB);//开始的事件应该在播放之后，此处特殊
            } else if (currentState == CURRENT_STATE_AUTO_COMPLETE) {
                onClickUiToggle();
            }
        } else if (i == R.id.surface_container) {
            startDismissControlViewTimer();
        } else if (i == R.id.back) {
            backPress();
            titleTextView.setVisibility(GONE);
        } else if (i == R.id.back_tiny) {
            if (UniversalPlayerMgr.getFirstFloor().currentScreen == BaseUniversalPlayerView.SCREEN_WINDOW_LIST) {
                quitFullscreenOrTinyWindow();
            } else {
                backPress();
            }
        } else if (i == R.id.retry_btn) {
            if (jzDataSource.urlsMap.isEmpty() || jzDataSource.getCurrentUrl() == null) {
                Toast.makeText(getContext(), getResources().getString(R.string.no_url), Toast.LENGTH_SHORT).show();
                return;
            }
            if (!jzDataSource.getCurrentUrl().toString().startsWith("file") && !
                    jzDataSource.getCurrentUrl().toString().startsWith("/") &&
                    !Utils.isWifiConnected(getContext()) && !WIFI_TIP_DIALOG_SHOWED) {
                showWifiDialog();
                return;
            }
            initTextureView();//和开始播放的代码重复
            addTextureView();
            MediaManager.setDataSource(jzDataSource);
            onStatePreparing();
            onEvent(ActionUser.ON_CLICK_START_ERROR);
        } else if (i == R.id.fillingView) {
            if (mTypePlayer == 1) {
                mTypePlayer += 1;
                setVideoImageDisplayType(BaseUniversalPlayerView.VIDEO_IMAGE_DISPLAY_TYPE_FILL_PARENT);
                fillingView.setText("填充");
            } else if (mTypePlayer == 2) {
                mTypePlayer += 1;
                setVideoImageDisplayType(BaseUniversalPlayerView.VIDEO_IMAGE_DISPLAY_TYPE_FILL_SCROP);
                fillingView.setText("裁剪");
            } else {
                mTypePlayer = 1;
                setVideoImageDisplayType(BaseUniversalPlayerView.VIDEO_IMAGE_DISPLAY_TYPE_ORIGINAL);
                fillingView.setText("全屏");
            }
        } else if (i == R.id.speedView) {
            setSpeed();
        } else if (i == R.id.downLoadView) {
            if (null != jzDataSource && jzDataSource.getCurrentUrl() instanceof String)
                showDownLoadDialog(jzDataSource.title, (String) jzDataSource.getCurrentUrl());
        } else if (i == R.id.episodeView) {
            if (null != mEpisodeInfo)
                showEpisodeDialog(mEpisodeInfo);
        } else if (i == R.id.clarityView) {
            if (null != mClarityInfo)
                showClarityDialog(mClarityInfo);
        } else if (i == R.id.llLockView) {
            if (isLock) {
                isLock = false;
                lockView.setBackgroundResource(R.drawable.u_lock);
                changeUiToPauseShow();
            } else {
                isLock = true;
                lockView.setBackgroundResource(R.drawable.n_lock);
                changeUiToPlayingClear();
            }
        } else if (i == R.id.llScreenshotsView) {
            onScreenshots();
        }
    }

    public void goneBackDown() {
        this.mDownLoadView.setVisibility(GONE);
        this.backButton.setVisibility(View.GONE);
        this.fullscreenButton.setVisibility(View.GONE);
        this.whereFromDownPlay = true;
    }

    /**
     * 截图
     */
    private void onScreenshots() {
        UniversalPlayerTextureView textureView = MediaManager.textureView;
        if (null != textureView) {
            Bitmap bitmap = textureView.getBitmap();
            if (null != bitmap)
                saveBitmap(bitmap);
        }
    }

    /**
     * 保存bitmap到本地
     */
    private String saveBitmap(Bitmap mBitmap) {
        File mPicturesImage;
        try {
            mPicturesImage = new File(Environment.getExternalStorageDirectory() + "/Pictures/" + jzDataSource.title);
            if (!mPicturesImage.exists()) {
                mPicturesImage.mkdirs();
            }
            mPicturesImage = new File(mPicturesImage, UUID.randomUUID().toString() + ".jpg");
            if (!mPicturesImage.exists()) {
                mPicturesImage.getParentFile().mkdirs();
                mPicturesImage.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(mPicturesImage);
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            Toast.makeText(getContext(), "保存成功", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return mPicturesImage.getAbsolutePath();
    }

    public static void setEpisodeInfo(ArrayList<DataPlayInfo> mInfo) {
        mEpisodeInfo = mInfo;
    }


    public static void setClarityInfo(LinkedHashMap mInfo, IOnUniversalPlayerViewListener listener) {
        ArrayList<DataPlayInfo> playInfo = new ArrayList<>();
        mIOnUniversalPlayerViewListener = listener;
        if (null != mInfo && mInfo.size() != 0)
            for (Object o1 : mInfo.entrySet()) {
                Map.Entry entry = (Map.Entry) o1;
                Object key = entry.getKey();
                Object val = entry.getValue();
                if (null != key && null != val) {
                    playInfo.add(new DataPlayInfo(key.toString(), val.toString()));
                }
            }
        if (null == mClarityInfo)
            mClarityInfo = new ArrayList<>();
        mClarityInfo.clear();
        mClarityInfo.addAll(playInfo);
    }


    public void onChangeClarityUrl(int index) {
        setClarityText(index, jzDataSource.urlsMap);
        changeUrl(index, getCurrentPositionWhenPlaying());
    }

    public void onChangeClarityUrl(int index, DataSource dataSource) {
        jzDataSource = dataSource;
        setClarityText(index, dataSource.urlsMap);
        changeUrl(index, getCurrentPositionWhenPlaying());
    }

    /**
     * 更新选中
     */
    public void setClarityText(int index, LinkedHashMap map) {
        int i = 0;
        for (Object o : map.entrySet()) {
            if (i == index) {
                Map.Entry entry = (Map.Entry) o;
                mClarityText = (String) entry.getKey();
            }
            i++;
        }
    }


    /**
     * 倍速
     */
    @SuppressLint("SetTextI18n")
    private void setSpeed() {
        if (mTypeSpeed == 0.5) {
            mTypeSpeed = 0.75f;
            speedView.setText("0.50x");
            if (MediaManager.instance().jzMediaInterface instanceof ExoPlayer) {
                ExoPlayer exoPlayer = (ExoPlayer) MediaManager.instance().jzMediaInterface;
                exoPlayer.setDoubleSpeed(0.5f);
            }
        } else if (mTypeSpeed == 0.75) {
            mTypeSpeed = 1.0f;
            speedView.setText("0.75x");
            if (MediaManager.instance().jzMediaInterface instanceof ExoPlayer) {
                ExoPlayer exoPlayer = (ExoPlayer) MediaManager.instance().jzMediaInterface;
                exoPlayer.setDoubleSpeed(0.75f);
            }
        } else if (mTypeSpeed == 1.0) {
            mTypeSpeed = 1.25f;
            speedView.setText("倍速");
            if (MediaManager.instance().jzMediaInterface instanceof ExoPlayer) {
                ExoPlayer exoPlayer = (ExoPlayer) MediaManager.instance().jzMediaInterface;
                exoPlayer.setDoubleSpeed(1.0f);
            }
        } else if (mTypeSpeed == 1.25) {
            mTypeSpeed = 1.5f;
            speedView.setText("1.25x");
            if (MediaManager.instance().jzMediaInterface instanceof ExoPlayer) {
                ExoPlayer exoPlayer = (ExoPlayer) MediaManager.instance().jzMediaInterface;
                exoPlayer.setDoubleSpeed(1.25f);
            }
        } else if (mTypeSpeed == 1.5) {
            mTypeSpeed = 2.0f;
            speedView.setText("1.50x");
            if (MediaManager.instance().jzMediaInterface instanceof ExoPlayer) {
                ExoPlayer exoPlayer = (ExoPlayer) MediaManager.instance().jzMediaInterface;
                exoPlayer.setDoubleSpeed(1.5f);
            }
        } else if (mTypeSpeed == 2.0) {
            mTypeSpeed = 0.5f;
            speedView.setText("2.00x");
            if (MediaManager.instance().jzMediaInterface instanceof ExoPlayer) {
                ExoPlayer exoPlayer = (ExoPlayer) MediaManager.instance().jzMediaInterface;
                exoPlayer.setDoubleSpeed(2.0f);
            }
        }
    }

    @Override
    public void showWifiDialog() {
        super.showWifiDialog();
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(getResources().getString(R.string.tips_not_wifi));
        builder.setPositiveButton(getResources().getString(R.string.tips_not_wifi_confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                onEvent(ActionUserUniversalPlayerView.ON_CLICK_START_WIFIDIALOG);
                startVideo();
                WIFI_TIP_DIALOG_SHOWED = true;
            }
        });
        builder.setNegativeButton(getResources().getString(R.string.tips_not_wifi_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                clearFloatScreen();
                onStatePause();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }


    @Override
    public void showClarityDialog(ArrayList<DataPlayInfo> mInfo) {
        super.showClarityDialog(mInfo);

        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext(), R.style.jz_pl_dialog);
        dialog.setCancelable(true);
        final AlertDialog alertDialog = dialog.create();

        PlayListAdapter playListAdapter = new PlayListAdapter(getContext(), new OnClickListener() {
            @Override
            public void onClick(View view) {
                seekToInAdvance = (int) getCurrentPositionWhenPlaying();
                changeUiToPlayingShow();
                if (null != alertDialog) {
                    alertDialog.dismiss();
                }
                if (null != mIOnUniversalPlayerViewListener) {
                    mIOnUniversalPlayerViewListener.onClarity((Integer) view.getTag());
                }
            }
        });
        playListAdapter.setData(mInfo);
        View view = View.inflate(getContext(), R.layout.jz_layout_down_clarity_item, null);
        ListView listView = view.findViewById(R.id.listView);
        listView.setAdapter(playListAdapter);
        alertDialog.setView(view);
        alertDialog.show();

        Window window = alertDialog.getWindow();
        if (null != window) {
            window.setGravity(Gravity.RIGHT);
            WindowManager.LayoutParams p = alertDialog.getWindow().getAttributes(); //获取对话框当前的参数值
            p.width = 300;
            alertDialog.getWindow().setAttributes(p); //设置生效
        }
        changeUiToPlayingClear();
    }

    @Override
    public void showEpisodeDialog(ArrayList<DataPlayInfo> mInfo) {
        super.showEpisodeDialog(mInfo);
        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext(), R.style.jz_pl_dialog);
        dialog.setCancelable(true);
        final AlertDialog alertDialog = dialog.create();

        PlayListAdapter playListAdapter = new PlayListAdapter(getContext(), new OnClickListener() {
            @Override
            public void onClick(View view) {
                changeUiToPlayingShow();
                if (null != alertDialog)
                    alertDialog.dismiss();

                if (null != mIOnUniversalPlayerViewListener) {
                    mIOnUniversalPlayerViewListener.onEpisode((Integer) view.getTag());
                }
            }
        });
        playListAdapter.setData(mInfo);
        playListAdapter.setTitleText(jzDataSource.title);
        View view = View.inflate(getContext(), R.layout.jz_layout_down_clarity_item, null);
        ListView listView = view.findViewById(R.id.listView);
        listView.setAdapter(playListAdapter);
        alertDialog.setView(view);
        alertDialog.show();

        Window window = alertDialog.getWindow();
        if (null != window) {
            window.setGravity(Gravity.RIGHT);
            WindowManager m = ((Activity) getContext()).getWindowManager();
            Display d = m.getDefaultDisplay(); //为获取屏幕宽、高
            WindowManager.LayoutParams p = alertDialog.getWindow().getAttributes(); //获取对话框当前的参数值
            p.height = d.getHeight(); //宽度设置为屏幕
            p.width = 500;
            alertDialog.getWindow().setAttributes(p); //设置生效
        }
        changeUiToPlayingClear();
    }

    @Override
    public void showDownLoadDialog(final String title, final String url) {
        super.showDownLoadDialog(title, url);
        //暂停视频
        stopView();
        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext(), R.style.jz_pl_dialog);
        final AlertDialog alertDialog = dialog.create();
        View view = View.inflate(getContext(), R.layout.jz_layout_down_load_item, null);
        TextView viewById = view.findViewById(R.id.video_item);
        TextView titleView = view.findViewById(R.id.video_title);
        titleView.setText(getResources().getString(R.string.tips_down_load));
        viewById.setText(title);
        view.findViewById(R.id.cancelView);
        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
                startView();

            }
        });
        view.findViewById(R.id.confirmView).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
                stopView();
                if (null != mIOnUniversalPlayerViewListener) {
                    mIOnUniversalPlayerViewListener.onDownLoad(title, url);
                }
            }
        });
        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                startView();
            }
        });
        alertDialog.setView(view);
        alertDialog.show();
    }


    public static void setIOnUniversalPlayerViewListener(IOnUniversalPlayerViewListener listener) {
        mIOnUniversalPlayerViewListener = listener;
    }

    public interface IOnUniversalPlayerViewListener {
        void onDownLoad(String title, String url);

        void onEpisode(Integer info);

        void onClarity(Integer info);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        super.onStartTrackingTouch(seekBar);
        cancelDismissControlViewTimer();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        super.onStopTrackingTouch(seekBar);
        startDismissControlViewTimer();
    }

    public void onClickUiToggle() {
        if (bottomContainer.getVisibility() != View.VISIBLE) {
            setSystemTimeAndBattery();
        }
        if (currentState == CURRENT_STATE_PREPARING) {
            changeUiToPreparing();
            if (bottomContainer.getVisibility() != View.VISIBLE) {
                setSystemTimeAndBattery();
            }
        } else if (currentState == CURRENT_STATE_PLAYING) {
            if (bottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToPlayingClear();
            } else {
                changeUiToPlayingShow();
            }
        } else if (currentState == CURRENT_STATE_PAUSE) {
            if (bottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToPauseClear();
            } else {
                changeUiToPauseShow();
            }
        }
    }

    public void setSystemTimeAndBattery() {
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat mDateFormat = new SimpleDateFormat("HH:mm");
        Date date = new Date();
        videoCurrentTime.setText(mDateFormat.format(date));
        if ((System.currentTimeMillis() - LAST_GET_BATTERY_LEVEL_TIME) > 30000) {
            LAST_GET_BATTERY_LEVEL_TIME = System.currentTimeMillis();
            getContext().registerReceiver(mBroadcastReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        } else {
            setBatteryLevel();
        }
    }

    public void setBatteryLevel() {
        int percent = LAST_GET_BATTERY_LEVEL_PERCENT;
        if (percent < 15) {
            batteryLevel.setBackgroundResource(R.drawable.jz_battery_level_10);
        } else if (percent >= 15 && percent < 40) {
            batteryLevel.setBackgroundResource(R.drawable.jz_battery_level_30);
        } else if (percent >= 40 && percent < 60) {
            batteryLevel.setBackgroundResource(R.drawable.jz_battery_level_50);
        } else if (percent >= 60 && percent < 80) {
            batteryLevel.setBackgroundResource(R.drawable.jz_battery_level_70);
        } else if (percent >= 80 && percent < 95) {
            batteryLevel.setBackgroundResource(R.drawable.jz_battery_level_90);
        } else if (percent >= 95 && percent <= 100) {
            batteryLevel.setBackgroundResource(R.drawable.jz_battery_level_100);
        }
    }

    public void onCLickUiToggleToClear() {
        if (currentState == CURRENT_STATE_PREPARING) {
            if (bottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToPreparing();
            }
        } else if (currentState == CURRENT_STATE_PLAYING) {
            if (bottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToPlayingClear();
            }
        } else if (currentState == CURRENT_STATE_PAUSE) {
            if (bottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToPauseClear();
            }
        } else if (currentState == CURRENT_STATE_AUTO_COMPLETE) {
            if (bottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToComplete();
            }
        }
    }

    @Override
    public void onProgress(int progress, long position, long duration) {
        super.onProgress(progress, position, duration);
        if (progress != 0) bottomProgressBar.setProgress(progress);
    }

    @Override
    public void setBufferProgress(int bufferProgress) {
        super.setBufferProgress(bufferProgress);
        if (bufferProgress != 0) bottomProgressBar.setSecondaryProgress(bufferProgress);
    }

    @Override
    public void resetProgressAndTime() {
        super.resetProgressAndTime();
        bottomProgressBar.setProgress(0);
        bottomProgressBar.setSecondaryProgress(0);
    }

    public void changeUiToNormal() {
        switch (currentScreen) {
            case SCREEN_WINDOW_NORMAL:
            case SCREEN_WINDOW_LIST:
                setAllControlsVisibility(View.VISIBLE, View.INVISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.VISIBLE, View.INVISIBLE, View.INVISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisibility(View.VISIBLE, View.INVISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.VISIBLE, View.INVISIBLE, View.INVISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }
    }

    public void changeUiToPreparing() {
        switch (currentScreen) {
            case SCREEN_WINDOW_NORMAL:
            case SCREEN_WINDOW_LIST:
                setAllControlsVisibility(View.INVISIBLE, View.INVISIBLE, View.INVISIBLE,
                        View.VISIBLE, View.VISIBLE, View.INVISIBLE, View.INVISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisibility(View.INVISIBLE, View.INVISIBLE, View.INVISIBLE,
                        View.VISIBLE, View.VISIBLE, View.INVISIBLE, View.INVISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }

    }

    public void changeUiToPlayingShow() {
        switch (currentScreen) {
            case SCREEN_WINDOW_NORMAL:
            case SCREEN_WINDOW_LIST:
                setAllControlsVisibility(View.VISIBLE, View.VISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisibility(View.VISIBLE, View.VISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }

    }

    public void changeUiToPlayingClear() {
        switch (currentScreen) {
            case SCREEN_WINDOW_NORMAL:
            case SCREEN_WINDOW_LIST:
                setAllControlsVisibility(View.INVISIBLE, View.INVISIBLE, View.INVISIBLE,
                        View.INVISIBLE, View.INVISIBLE, View.VISIBLE, View.INVISIBLE);
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisibility(View.INVISIBLE, View.INVISIBLE, View.INVISIBLE,
                        View.INVISIBLE, View.INVISIBLE, View.VISIBLE, View.INVISIBLE);
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }

    }

    public void changeUiToPauseShow() {
        switch (currentScreen) {
            case SCREEN_WINDOW_NORMAL:
            case SCREEN_WINDOW_LIST:
                setAllControlsVisibility(View.VISIBLE, View.VISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisibility(View.VISIBLE, View.VISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }
    }

    public void changeUiToPauseClear() {
        switch (currentScreen) {
            case SCREEN_WINDOW_NORMAL:
            case SCREEN_WINDOW_LIST:
                setAllControlsVisibility(View.INVISIBLE, View.INVISIBLE, View.INVISIBLE,
                        View.INVISIBLE, View.INVISIBLE, View.VISIBLE, View.INVISIBLE);
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisibility(View.INVISIBLE, View.INVISIBLE, View.INVISIBLE,
                        View.INVISIBLE, View.INVISIBLE, View.VISIBLE, View.INVISIBLE);
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }

    }

    public void changeUiToComplete() {
        switch (currentScreen) {
            case SCREEN_WINDOW_NORMAL:
            case SCREEN_WINDOW_LIST:
                setAllControlsVisibility(View.VISIBLE, View.INVISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.VISIBLE, View.INVISIBLE, View.INVISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisibility(View.VISIBLE, View.INVISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.VISIBLE, View.INVISIBLE, View.INVISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }

    }

    public void changeUiToError() {
        switch (currentScreen) {
            case SCREEN_WINDOW_NORMAL:
            case SCREEN_WINDOW_LIST:
                setAllControlsVisibility(View.INVISIBLE, View.INVISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.VISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_FULLSCREEN:
                setAllControlsVisibility(View.VISIBLE, View.INVISIBLE, View.VISIBLE,
                        View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.VISIBLE);
                updateStartImage();
                break;
            case SCREEN_WINDOW_TINY:
                break;
        }

    }

    public void setAllControlsVisibility(int topCon, int bottomCon, int startBtn, int loadingPro, int thumbImg, int bottomPro, int retryLayout) {
        topContainer.setVisibility(topCon);
        bottomContainer.setVisibility(bottomCon);
        startButton.setVisibility(startBtn);
        loadingProgressBar.setVisibility(loadingPro);
        thumbImageView.setVisibility(thumbImg);
        //   bottomProgressBar.setVisibility(bottomPro);
        mRetryLayout.setVisibility(retryLayout);

        //展示裁剪
        if (currentScreen == SCREEN_WINDOW_FULLSCREEN) {
            if (!isLock) {
                llLockView.setVisibility(topCon);
            }
            llScreenshotsView.setVisibility(topCon);
        }
    }

    public void updateStartImage() {
        if (currentState == CURRENT_STATE_PLAYING) {
            startButton.setVisibility(VISIBLE);
            startButton.setImageResource(R.drawable.jz_click_pause_selector);
            replayTextView.setVisibility(INVISIBLE);
        } else if (currentState == CURRENT_STATE_ERROR) {
            startButton.setVisibility(INVISIBLE);
            replayTextView.setVisibility(INVISIBLE);
        } else if (currentState == CURRENT_STATE_AUTO_COMPLETE) {
            startButton.setVisibility(VISIBLE);
            startButton.setImageResource(R.drawable.jz_click_replay_selector);
            replayTextView.setVisibility(VISIBLE);
        } else {
            startButton.setImageResource(R.drawable.jz_click_play_selector);
            replayTextView.setVisibility(INVISIBLE);
        }
    }

    @Override
    public void showProgressDialog(float deltaX, String seekTime, long seekTimePosition, String totalTime, long totalTimeDuration) {
        super.showProgressDialog(deltaX, seekTime, seekTimePosition, totalTime, totalTimeDuration);
        if (mProgressDialog == null) {
            @SuppressLint("InflateParams")
            View localView = LayoutInflater.from(getContext()).inflate(R.layout.jz_dialog_progress, null);
            mDialogProgressBar = localView.findViewById(R.id.duration_progressbar);
            mDialogSeekTime = localView.findViewById(R.id.tv_current);
            mDialogTotalTime = localView.findViewById(R.id.tv_duration);
            mDialogIcon = localView.findViewById(R.id.duration_image_tip);
            mProgressDialog = createDialogWithView(localView);
        }
        if (!mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }

        mDialogSeekTime.setText(seekTime);
        mDialogTotalTime.setText(String.format(" / %s", totalTime));
        mDialogProgressBar.setProgress(totalTimeDuration <= 0 ? 0 : (int) (seekTimePosition * 100 / totalTimeDuration));
        if (deltaX > 0) {
            mDialogIcon.setBackgroundResource(R.drawable.jz_forward_icon);
        } else {
            mDialogIcon.setBackgroundResource(R.drawable.jz_backward_icon);
        }
        onCLickUiToggleToClear();
    }

    @Override
    public void dismissProgressDialog() {
        super.dismissProgressDialog();
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void showVolumeDialog(float deltaY, int volumePercent) {
        super.showVolumeDialog(deltaY, volumePercent);
        if (mVolumeDialog == null) {
            @SuppressLint("InflateParams")
            View localView = LayoutInflater.from(getContext()).inflate(R.layout.jz_dialog_volume, null);
            mDialogVolumeImageView = localView.findViewById(R.id.volume_image_tip);
            mDialogVolumeTextView = localView.findViewById(R.id.tv_volume);
            mDialogVolumeProgressBar = localView.findViewById(R.id.volume_progressbar);
            mVolumeDialog = createDialogWithView(localView);
        }
        if (!mVolumeDialog.isShowing()) {
            mVolumeDialog.show();
        }
        if (volumePercent <= 0) {
            mDialogVolumeImageView.setBackgroundResource(R.drawable.jz_close_volume);
        } else {
            mDialogVolumeImageView.setBackgroundResource(R.drawable.jz_add_volume);
        }
        if (volumePercent > 100) {
            volumePercent = 100;
        } else if (volumePercent < 0) {
            volumePercent = 0;
        }
        mDialogVolumeTextView.setText(String.format("%d%%", volumePercent));
        mDialogVolumeProgressBar.setProgress(volumePercent);
        onCLickUiToggleToClear();
    }

    @Override
    public void dismissVolumeDialog() {
        super.dismissVolumeDialog();
        if (mVolumeDialog != null) {
            mVolumeDialog.dismiss();
        }
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void showBrightnessDialog(int brightnessPercent) {
        super.showBrightnessDialog(brightnessPercent);
        if (mBrightnessDialog == null) {
            @SuppressLint("InflateParams") View localView = LayoutInflater.from(getContext()).inflate(R.layout.jz_dialog_brightness, null);
            mDialogBrightnessTextView = localView.findViewById(R.id.tv_brightness);
            mDialogBrightnessProgressBar = localView.findViewById(R.id.brightness_progressbar);
            mBrightnessDialog = createDialogWithView(localView);
        }
        if (!mBrightnessDialog.isShowing()) {
            mBrightnessDialog.show();
        }
        if (brightnessPercent > 100) {
            brightnessPercent = 100;
        } else if (brightnessPercent < 0) {
            brightnessPercent = 0;
        }
        mDialogBrightnessTextView.setText(String.format("%d%%", brightnessPercent));
        mDialogBrightnessProgressBar.setProgress(brightnessPercent);
        onCLickUiToggleToClear();
    }

    @Override
    public void dismissBrightnessDialog() {
        super.dismissBrightnessDialog();
        if (mBrightnessDialog != null) {
            mBrightnessDialog.dismiss();
        }
    }

    public Dialog createDialogWithView(View localView) {
        Dialog dialog = new Dialog(getContext(), R.style.jz_style_dialog_progress);
        dialog.setContentView(localView);
        Window window = dialog.getWindow();
        if (null != window) {
            window.addFlags(Window.FEATURE_ACTION_BAR);
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            window.setLayout(-2, -2);
            WindowManager.LayoutParams localLayoutParams = window.getAttributes();
            localLayoutParams.gravity = Gravity.CENTER;
            window.setAttributes(localLayoutParams);
        }
        return dialog;
    }

    public void startDismissControlViewTimer() {
        cancelDismissControlViewTimer();
        DISMISS_CONTROL_VIEW_TIMER = new Timer();
        mDismissControlViewTimerTask = new DismissControlViewTimerTask();
        DISMISS_CONTROL_VIEW_TIMER.schedule(mDismissControlViewTimerTask, 2500);
    }

    public void cancelDismissControlViewTimer() {
        if (DISMISS_CONTROL_VIEW_TIMER != null) {
            DISMISS_CONTROL_VIEW_TIMER.cancel();
        }
        if (mDismissControlViewTimerTask != null) {
            mDismissControlViewTimerTask.cancel();
        }

    }

    @Override
    public void onAutoCompletion() {
        super.onAutoCompletion();
        cancelDismissControlViewTimer();
    }

    @Override
    public void onCompletion() {
        super.onCompletion();
        cancelDismissControlViewTimer();

    }

    public void downControlView() {
        if (currentState != CURRENT_STATE_NORMAL && currentState != CURRENT_STATE_ERROR && currentState != CURRENT_STATE_AUTO_COMPLETE) {
            post(new Runnable() {
                @Override
                public void run() {
                    bottomContainer.setVisibility(View.INVISIBLE);
                    topContainer.setVisibility(View.INVISIBLE);
                    startButton.setVisibility(View.INVISIBLE);
                    if (!isLock) {
                        llLockView.setVisibility(INVISIBLE);
                    } else {
                        llLockView.setVisibility(VISIBLE);
                    }
                    llScreenshotsView.setVisibility(INVISIBLE);

                    if (currentScreen != SCREEN_WINDOW_TINY) {
                        //隐藏
                        bottomProgressBar.setVisibility(View.GONE);
                    }
                }
            });
        }
    }

    public class DismissControlViewTimerTask extends TimerTask {

        @Override
        public void run() {
            downControlView();
        }
    }
}
