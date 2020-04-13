package com.mux.stats.sdk.muxstats.theoplayer;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.mux.stats.sdk.core.MuxSDKViewOrientation;
import com.mux.stats.sdk.core.events.EventBus;
import com.mux.stats.sdk.core.events.IEvent;
import com.mux.stats.sdk.core.events.InternalErrorEvent;
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
import com.theoplayer.android.api.THEOplayerView;
import com.theoplayer.android.api.event.player.PlayerEventTypes;
import com.theoplayer.android.api.source.TypedSource;

import java.lang.ref.WeakReference;
import java.util.List;

import static android.os.SystemClock.elapsedRealtime;
import static com.mux.stats.sdk.muxstats.theoplayer.Util.secondsToMs;

public class MuxStatsSDKTHEOplayer extends EventBus implements IPlayerListener {
    public static final String TAG = "MuxStatsSDKTHEOplayer";

    protected PlayerState state;
    protected MuxStats muxStats;
    protected WeakReference<THEOplayerView> player;

    protected static final int ERROR_UNKNOWN = -1;
    protected static final int ERROR_DRM = -2;
    protected static final int ERROR_IO = -3;

    protected String mimeType;
    protected int sourceWidth;
    protected int sourceHeight;
    protected Integer sourceAdvertisedBitrate;
    protected Float sourceAdvertisedFramerate;
    protected AdsImaSDKListener imaListener;
    protected long sourceDuration;
    protected boolean playWhenReady;

    protected double playbackPosition;

    public int streamType = -1;

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


        player.getPlayer().addEventListener(PlayerEventTypes.PLAY, (playEvent -> play()));
        player.getPlayer().addEventListener(PlayerEventTypes.PLAYING, (playEvent -> playing()));
        player.getPlayer().addEventListener(PlayerEventTypes.PAUSE, (playEvent -> pause()));
        player.getPlayer().addEventListener(PlayerEventTypes.SEEKING, (playEvent -> {
            dispatch(new SeekingEvent(null));
        }));
        player.getPlayer().addEventListener(PlayerEventTypes.SEEKED, (playEvent -> {
            dispatch(new SeekedEvent(null));
        }));
        player.getPlayer().addEventListener(PlayerEventTypes.ENDED, (playEvent -> {
            dispatch(new EndedEvent(null));
        }));

        player.getPlayer().addEventListener(PlayerEventTypes.ERROR, (errorEvent ->
            internalError(new MuxErrorException(0, errorEvent.getError()))));
    }

    public void setAdsListener(AdsImaSDKListener listener) {
        imaListener = listener;
        imaListener.setTheoPlayerListener(this);
    }

    // IPlayerListener

    @Override
    public long getCurrentPosition() {
        return secondsToMs(playbackPosition);
    }

    @SuppressWarnings("unused")
    public void updateCustomerData(CustomerPlayerData customPlayerData, CustomerVideoData customVideoData) {
        muxStats.updateCustomerData(customPlayerData, customVideoData);
    }

    public CustomerVideoData getCustomerVideoData() {
        return muxStats.getCustomerVideoData();
    }

    public CustomerPlayerData getCustomerPlayerData() {
        return muxStats.getCustomerPlayerData();
    }

    public void enableMuxCoreDebug(boolean enable, boolean verbose) {
        muxStats.allowLogcatOutput(enable, verbose);
    }

    @Override
    public String getMimeType() {
        try {
            if (player.get().getPlayer().getSource() != null) {
                List<TypedSource> sources = player.get().getPlayer().getSource().getSources();
                return sources.size() > 0 ? sources.get(0).getType().toString() : "";
            }
        } catch (Exception e) {
//            e.printStackTrace();
        }
        return null;
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
    public Integer getSourceAdvertisedBitrate() {
        return sourceAdvertisedBitrate;
    }

    @Override
    public Float getSourceAdvertisedFramerate() {
        return sourceAdvertisedFramerate;
    }

    @Override
    public Long getSourceDuration() {
        return secondsToMs(player.get().getPlayer().getDuration());
    }

    @Override
    public boolean isPaused() {
        return player.get().getPlayer().isPaused();
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
        Log.d(TAG, "Pausing file");
        state = PlayerState.PAUSED;
        dispatch(new PauseEvent(null));
    }

    protected void play() {
        Log.d(TAG, "Playing file");
        state = PlayerState.PLAY;
        dispatch(new PlayEvent(null));
    }

    protected void playing() {
        if (state ==  PlayerState.PAUSED) {
            play();
        }
        state = PlayerState.PLAYING;
        dispatch(new PlayingEvent(null));
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

    public void orientationChange(MuxSDKViewOrientation orientation) {
        muxStats.orientationChange(orientation);
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
        public long getElapsedRealtime() {
            return elapsedRealtime();
        }

        @Override
        public void outputLog(String tag, String msg) {
            Log.v(tag, msg);
        }
    }
}

