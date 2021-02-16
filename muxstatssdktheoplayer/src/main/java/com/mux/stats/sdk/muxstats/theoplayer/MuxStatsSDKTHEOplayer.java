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
import com.mux.stats.sdk.core.events.playback.RenditionChangeEvent;
import com.mux.stats.sdk.core.events.playback.SeekedEvent;
import com.mux.stats.sdk.core.events.playback.SeekingEvent;
import com.mux.stats.sdk.core.events.playback.TimeUpdateEvent;
import com.mux.stats.sdk.core.events.playback.VideoChangeEvent;
import com.mux.stats.sdk.core.model.CustomerPlayerData;
import com.mux.stats.sdk.core.model.CustomerVideoData;
import com.mux.stats.sdk.core.model.CustomerViewData;
import com.mux.stats.sdk.core.model.ViewData;
import com.mux.stats.sdk.core.util.MuxLogger;
import com.mux.stats.sdk.muxstats.IDevice;
import com.mux.stats.sdk.muxstats.INetworkRequest;
import com.mux.stats.sdk.muxstats.IPlayerListener;
import com.mux.stats.sdk.muxstats.MuxErrorException;
import com.mux.stats.sdk.muxstats.MuxStats;
import com.theoplayer.android.api.THEOplayerView;
import com.theoplayer.android.api.event.EventListener;
import com.theoplayer.android.api.event.ads.AdsEventTypes;
import com.theoplayer.android.api.event.player.PlayerEventTypes;
import com.theoplayer.android.api.event.track.mediatrack.video.ActiveQualityChangedEvent;
import com.theoplayer.android.api.event.track.mediatrack.video.VideoTrackEventTypes;
import com.theoplayer.android.api.event.track.mediatrack.video.list.AddTrackEvent;
import com.theoplayer.android.api.event.track.mediatrack.video.list.VideoTrackListEventTypes;
import com.theoplayer.android.api.player.Player;
import com.theoplayer.android.api.player.track.mediatrack.quality.VideoQuality;
import com.theoplayer.android.api.source.TypedSource;

import java.lang.ref.WeakReference;
import java.util.List;

import static android.os.SystemClock.elapsedRealtime;
import static com.mux.stats.sdk.muxstats.theoplayer.Util.secondsToMs;

public class MuxStatsSDKTHEOplayer extends MuxBaseSDKTheoPlayer {

    public MuxStatsSDKTHEOplayer(Context ctx, THEOplayerView player, String playerName,
                                 CustomerPlayerData customerPlayerData, CustomerVideoData customerVideoData) {
        this(ctx, player, playerName, customerPlayerData, customerVideoData,
                null, true);
    }

    public MuxStatsSDKTHEOplayer(Context ctx, THEOplayerView player, String playerName,
                             CustomerPlayerData customerPlayerData,
                             CustomerVideoData customerVideoData,
                             CustomerViewData customerViewData) {
        this(ctx, player, playerName, customerPlayerData, customerVideoData,
                customerViewData, true);
    }

    public MuxStatsSDKTHEOplayer(Context ctx, THEOplayerView player, String playerName,
                             CustomerPlayerData customerPlayerData,
                             CustomerVideoData customerVideoData,
                             boolean sentryEnabled) {
        this(ctx, player, playerName, customerPlayerData, customerVideoData,
                null, sentryEnabled);
    }

    public MuxStatsSDKTHEOplayer(Context ctx, THEOplayerView player, String playerName,
                             CustomerPlayerData customerPlayerData,
                             CustomerVideoData customerVideoData,
                             CustomerViewData customerViewData, boolean sentryEnabled) {
        this(ctx, player, playerName, customerPlayerData, customerVideoData, null,
                sentryEnabled, new MuxNetworkRequests());
    }

    public MuxStatsSDKTHEOplayer(Context ctx, THEOplayerView player, String playerName,
                             CustomerPlayerData customerPlayerData,
                             CustomerVideoData customerVideoData,
                             CustomerViewData customerViewData, boolean sentryEnabled,
                             INetworkRequest networkRequest) {
        super(ctx, player, playerName, customerPlayerData, customerVideoData, customerViewData,
                sentryEnabled, networkRequest);
    }

    @SuppressWarnings("unused")
    public void updateCustomerData(CustomerPlayerData customPlayerData, CustomerVideoData customVideoData) {
        muxStats.updateCustomerData(customPlayerData, customVideoData);
    }

    @SuppressWarnings("unused")
    public void updateCustomerData(CustomerPlayerData customerPlayerData,
                                   CustomerVideoData customerVideoData,
                                   CustomerViewData customerViewData) {
        muxStats.updateCustomerData(customerPlayerData, customerVideoData, customerViewData);
    }

    @SuppressWarnings("unused")
    public CustomerVideoData getCustomerVideoData() {
        return muxStats.getCustomerVideoData();
    }

    @SuppressWarnings("unused")
    public CustomerPlayerData getCustomerPlayerData() {
        return muxStats.getCustomerPlayerData();
    }

    @SuppressWarnings("unused")
    public CustomerViewData getCustomerViewData() {
        return muxStats.getCustomerViewData();
    }

    public void enableMuxCoreDebug(boolean enable, boolean verbose) {
        muxStats.allowLogcatOutput(enable, verbose);
    }

    // Exposed methods to change stats
    @SuppressWarnings("unused")
    public void videoChange(CustomerVideoData customerVideoData) {
        muxStats.videoChange(customerVideoData);
    }

    @SuppressWarnings("unused")
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
}

