package com.mux.stats.sdk.muxstats.theoplayer;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.mux.stats.sdk.core.events.EventBus;
import com.mux.stats.sdk.core.events.IEvent;
import com.mux.stats.sdk.core.events.InternalErrorEvent;
import com.mux.stats.sdk.core.events.playback.AdBreakEndEvent;
import com.mux.stats.sdk.core.events.playback.AdBreakStartEvent;
import com.mux.stats.sdk.core.events.playback.AdEndedEvent;
import com.mux.stats.sdk.core.events.playback.AdErrorEvent;
import com.mux.stats.sdk.core.events.playback.AdPauseEvent;
import com.mux.stats.sdk.core.events.playback.AdPlayEvent;
import com.mux.stats.sdk.core.events.playback.AdPlayingEvent;
import com.mux.stats.sdk.core.events.playback.EndedEvent;
import com.mux.stats.sdk.core.events.playback.PauseEvent;
import com.mux.stats.sdk.core.events.playback.PlayEvent;
import com.mux.stats.sdk.core.events.playback.PlayingEvent;
import com.mux.stats.sdk.core.events.playback.SeekedEvent;
import com.mux.stats.sdk.core.events.playback.SeekingEvent;
import com.mux.stats.sdk.core.events.playback.TimeUpdateEvent;
import com.mux.stats.sdk.core.events.playback.VideoChangeEvent;
import com.mux.stats.sdk.core.model.CustomerPlayerData;
import com.mux.stats.sdk.core.model.CustomerVideoData;
import com.mux.stats.sdk.core.util.MuxLogger;
import com.mux.stats.sdk.muxstats.IDevice;
import com.mux.stats.sdk.muxstats.IPlayerListener;
import com.mux.stats.sdk.muxstats.MuxErrorException;
import com.mux.stats.sdk.muxstats.MuxStats;
import com.mux.stats.sdk.muxstats.demo.theoplayer.BuildConfig;
import com.theoplayer.android.api.THEOplayerView;
import com.theoplayer.android.api.ads.Ads;
import com.theoplayer.android.api.event.player.PlayerEventTypes;
import com.theoplayer.android.api.event.ads.AdsEventTypes;
import com.theoplayer.android.api.source.TypedSource;

import java.lang.ref.WeakReference;
import java.util.List;

import static com.mux.stats.sdk.muxstats.theoplayer.Util.secondsToMs;

public class MuxStatsSDKTHEOplayer extends EventBus implements IPlayerListener {
    public static final String TAG = "MuxStatsSDKTHEOplayer";

    private PlayerState state;
    private MuxStats muxStats;
    private WeakReference<THEOplayerView> player;

    private static final int ERROR_UNKNOWN = -1;
    private static final int ERROR_DRM = -2;
    private static final int ERROR_IO = -3;

    private String mimeType;
    private int sourceWidth;
    private int sourceHeight;
    private long sourceDuration;
    private boolean playWhenReady;

    private double playbackPosition;

    private boolean inAdBreak = false;

    private int streamType = -1;

    public enum PlayerState {
        BUFFERING, ERROR, PAUSED, PLAY, PLAYING, INIT
    }

    public MuxStatsSDKTHEOplayer(Context ctx, THEOplayerView player, String playerName,
                           CustomerPlayerData customerPlayerData, CustomerVideoData customerVideoData) {
        super();
        this.player = new WeakReference<>(player);
        MuxStats.setHostDevice(new MuxDevice(ctx, player.getVersion()));
        MuxStats.setHostNetworkApi(new MuxNetworkRequests());
        muxStats = new MuxStats(this, playerName, customerPlayerData, customerVideoData);
        addListener(muxStats);

        // Setup listeners

        player.getPlayer().addEventListener(PlayerEventTypes.SOURCECHANGE, (sourceChangeEvent -> {
            player.getPlayer().requestVideoWidth((playerSourceWidth -> {
                sourceWidth = playerSourceWidth;
            }));

            player.getPlayer().requestVideoHeight((playerSourceHeight -> {
                sourceHeight = playerSourceHeight;
            }));
            dispatch(new VideoChangeEvent(null));
        }));

        player.getPlayer().addEventListener(PlayerEventTypes.TIMEUPDATE,
            timeUpdateEvent -> {
                player.getPlayer().requestCurrentTime(time -> playbackPosition = time);
                dispatch(new TimeUpdateEvent(null));
            });

        player.getPlayer().addEventListener(PlayerEventTypes.PLAY, playEvent -> play());
        player.getPlayer().addEventListener(PlayerEventTypes.PLAYING, playingEvent -> playing());
        player.getPlayer().addEventListener(PlayerEventTypes.PAUSE, pauseEvent -> pause());
        player.getPlayer().addEventListener(PlayerEventTypes.SEEKING, seekingEvent -> {
            Log.v(TAG, "seeking");
            dispatch(new SeekingEvent(null));
        });
        player.getPlayer().addEventListener(PlayerEventTypes.SEEKED, seekedEvent -> {
            Log.v(TAG, "seeked");
            dispatch(new SeekedEvent(null));
        });
        player.getPlayer().addEventListener(PlayerEventTypes.ENDED, endedEvent -> {
            if (inAdBreak) {
                Log.v(TAG, "adended");
                dispatch(new AdEndedEvent(null));
            } else {
                Log.v(TAG, "ended");
                dispatch(new EndedEvent(null));
            }
        });

        player.getPlayer().addEventListener(PlayerEventTypes.ERROR, errorEvent -> {
            Log.v(TAG, "error");
            internalError(new MuxErrorException(0, errorEvent.getError()));
        });

        // And ad listeners
        Ads ads = player.getPlayer().getAds();
        ads.addEventListener(AdsEventTypes.AD_BREAK_BEGIN, adBreakBeginEvent -> {
            Log.v(TAG, "Ad Break Begin");
            inAdBreak = true;
            dispatch(new PauseEvent(null));
            dispatch(new AdBreakStartEvent(null));
        });
        ads.addEventListener(AdsEventTypes.AD_BREAK_END, adBreakEndEvent -> {
            Log.v(TAG, "Ad Break End");
            dispatch(new AdBreakEndEvent(null));
            inAdBreak = false;
        });
        ads.addEventListener(AdsEventTypes.AD_END, adEndEvent -> {
            Log.v(TAG, "Ad End");
            dispatch(new AdEndedEvent(null));
        });
        ads.addEventListener(AdsEventTypes.AD_ERROR, adErrorEvent ->
                dispatch(new AdErrorEvent(null)));
    }

    // IPlayerListener

    @Override
    public long getCurrentPosition() {
        return secondsToMs(playbackPosition);
    }

    @Override
    public String getMimeType() {
        List<TypedSource> sources = player.get().getPlayer().getSource().getSources();
        return sources.size() > 0 ? sources.get(0).getType().toString() : "";
    }

    @Override
    public Integer getSourceWidth() {
        return sourceHeight;
    }

    @Override
    public Integer getSourceHeight() {
        return sourceHeight;
    }

    @Override
    public Long getSourceDuration() {
        return secondsToMs(player.get().getPlayer().getDuration());
    }

    @Override
    public boolean isPaused() {
        return player.get().getPlayer().isPaused() || inAdBreak;
    }

    @Override
    public boolean isBuffering() {
        return player.get().getPlayer().isSeeking();
    }

    @Override
    public int getPlayerViewWidth() {
        if(player != null && player.get() != null) {
            return player.get().getMeasuredWidth();
        }
        return 0;
    }

    @Override
    public int getPlayerViewHeight() {
        if(player != null && player.get() != null) {
            return player.get().getMeasuredHeight();
        }
        return 0;
    }

    // EventBus

    @Override
    public void dispatch(IEvent event) {
        if (player != null && player.get() != null && muxStats != null) {
            super.dispatch(event);
        }
    }



    // Internal methods to change stats
    protected void buffering() {
        state = PlayerState.BUFFERING;
        dispatch(new TimeUpdateEvent(null));
    }

    protected void pause() {
        state = PlayerState.PAUSED;
        if (inAdBreak) {
            Log.v(TAG, "adpause");
            dispatch(new AdPauseEvent(null));
        } else {
            Log.v(TAG, "pause");
            dispatch(new PauseEvent(null));
        }
    }

    protected void play() {
        state = PlayerState.PLAY;
        if (inAdBreak) {
            Log.v(TAG, "adplay");
            dispatch(new AdPlayEvent(null));
        } else {
            Log.v(TAG, "play");
            dispatch(new PlayEvent(null));
        }
    }

    protected void playing() {
        if (state ==  PlayerState.PAUSED) {
            play();
        }
        state = PlayerState.PLAYING;
        if (inAdBreak) {
            Log.v(TAG, "adplaying");
            dispatch(new AdPlayingEvent(null));
        } else {
            Log.v(TAG, "playing");
            dispatch(new PlayingEvent(null));
        }
    }

    protected void internalError(Exception error) {
        Log.d(TAG, "Internal error");
        if (error instanceof MuxErrorException) {
            MuxErrorException muxError = (MuxErrorException) error;
            dispatch(new InternalErrorEvent(muxError.getCode(), muxError.getMessage()));
        } else {
            dispatch(new InternalErrorEvent(ERROR_UNKNOWN, error.getClass().getCanonicalName() + " - " + error.getMessage()));
        }
    }

    // Exposed methods to change stats

    public void videoChange(CustomerVideoData customerVideoData) {
        muxStats.videoChange(customerVideoData);
    }

    public void programChange(CustomerVideoData customerVideoData) {
        muxStats.programChange(customerVideoData);
    }

    public void setPlayerView(THEOplayerView playerView) {
        this.player = new WeakReference<>(playerView);
    }

    public void setPlayerSize(int width, int height) {
        muxStats.setPlayerSize(width, height);
    }

    public void setScreenSize(int width, int height) {
        muxStats.setScreenSize(width, height);
    }

    public void error(MuxErrorException e) {
        muxStats.error(e);
    }

    public void setAutomaticErrorTracking(boolean enabled) {
        muxStats.setAutomaticErrorTracking(enabled);
    }

    public void setStreamType(int type) {
        streamType = type;
    }

    public void release() {
        muxStats.release();
        muxStats = null;
        player = null;
    }

    static class MuxDevice implements IDevice {
        private static final String PLAYER_SOFTWARE = "THEOplayer";

        private String deviceId;
        private String appName = "";
        private String appVersion = "";
        private String theoVersion = "";

        MuxDevice(Context ctx, String theoVersion) {
            deviceId = Settings.Secure.getString(ctx.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            this.theoVersion = theoVersion;
            try {
                PackageInfo pi = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
                appName = pi.packageName;
                appVersion = pi.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                MuxLogger.d(TAG, "could not get package info");
            }
        }

        @Override
        public String getHardwareArchitecture() {
            return Build.HARDWARE;
        }

        @Override
        public String getOSFamily() {
            return "Android";
        }

        @Override
        public String getOSVersion() {
            return Build.VERSION.RELEASE + " (" + Build.VERSION.SDK_INT + ")";
        }

        @Override
        public String getManufacturer() {
            return Build.MANUFACTURER;
        }

        @Override
        public String getModelName() {
            return Build.MODEL;
        }

        @Override
        public String getPlayerVersion() {
            return this.theoVersion;
        }

        @Override
        public String getDeviceId() {
            return deviceId;
        }

        @Override
        public String getAppName() {
            return appName;
        }

        @Override
        public String getAppVersion() {
            return appVersion;
        }

        @Override
        public String getPluginName() {
            return BuildConfig.MUX_PLUGIN_NAME;
        }

        @Override
        public String getPluginVersion() {
            return BuildConfig.MUX_PLUGIN_VERSION;
        }

        @Override
        public String getPlayerSoftware() { return PLAYER_SOFTWARE; }

        @Override
        public void outputLog(String tag, String msg) {
            Log.v(tag, msg);
        }
    }
}

