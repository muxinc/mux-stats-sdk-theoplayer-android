package com.mux.stats.sdk.muxstats.theoplayer;

import android.content.Context;

import androidx.annotation.Nullable;

import com.mux.stats.sdk.core.CustomOptions;
import com.mux.stats.sdk.core.MuxSDKViewOrientation;
import com.mux.stats.sdk.core.model.CustomerData;
import com.mux.stats.sdk.core.model.CustomerPlayerData;
import com.mux.stats.sdk.core.model.CustomerVideoData;
import com.mux.stats.sdk.core.model.CustomerViewData;
import com.mux.stats.sdk.muxstats.INetworkRequest;
import com.mux.stats.sdk.muxstats.MuxErrorException;
import com.mux.stats.sdk.muxstats.MuxStats;
import com.theoplayer.android.api.THEOplayerView;

import java.lang.ref.WeakReference;

public class MuxStatsSDKTHEOPlayer extends MuxBaseSDKTheoPlayer {

    public MuxStatsSDKTHEOPlayer(Context ctx, THEOplayerView player, String playerName,
                                 CustomerData data) {
        this(ctx, player, playerName, data, new CustomOptions(), new MuxNetworkRequests());
    }

    public MuxStatsSDKTHEOPlayer(Context ctx, THEOplayerView player, String playerName,
                                 CustomerData data,
                                 CustomOptions options,
                                 INetworkRequest networkRequests) {
        super(ctx, player, playerName, data, options, networkRequests);
    }

    @SuppressWarnings("unused")
    public void updateCustomerData(CustomerPlayerData customPlayerData, CustomerVideoData customVideoData) {
        withMuxStats(stats -> stats.updateCustomerData(customPlayerData, customVideoData));
    }

    @SuppressWarnings("unused")
    public void updateCustomerData(CustomerPlayerData customerPlayerData,
                                   CustomerVideoData customerVideoData,
                                   CustomerViewData customerViewData) {
        withMuxStats(stats -> stats.updateCustomerData(customerPlayerData, customerVideoData, customerViewData));
    }

    @Nullable
    @SuppressWarnings("unused")
    public CustomerVideoData getCustomerVideoData() {
        return withMuxStats(MuxStats::getCustomerVideoData, null);
    }

    @Nullable
    @SuppressWarnings("unused")
    public CustomerPlayerData getCustomerPlayerData() {
        return withMuxStats(MuxStats::getCustomerPlayerData, null);
    }

    @Nullable
    @SuppressWarnings("unused")
    public CustomerViewData getCustomerViewData() {
        return withMuxStats(MuxStats::getCustomerViewData, null);
    }

    public void enableMuxCoreDebug(boolean enable, boolean verbose) {
        withMuxStats(stats -> stats.allowLogcatOutput(enable, verbose));
    }

    // Exposed methods to change stats
    @SuppressWarnings("unused")
    public void videoChange(CustomerVideoData customerVideoData) {
        withMuxStats(stats -> stats.videoChange(customerVideoData));
    }

    @SuppressWarnings("unused")
    public void programChange(CustomerVideoData customerVideoData) {
        withMuxStats(stats -> stats.programChange(customerVideoData));
    }

    @Override
    public void orientationChange(MuxSDKViewOrientation orientation) {
        super.orientationChange(orientation);
    }

    public void setPlayerView(THEOplayerView playerView) {
        this.player = new WeakReference<>(playerView);
    }

    public void setPlayerSize(int width, int height) {
        withMuxStats(stats -> stats.setPlayerSize(width, height));
    }

    public void setScreenSize(int width, int height) {
        withMuxStats(stats -> stats.setScreenSize(width, height));
    }

    public void error(MuxErrorException e) {
        withMuxStats(stats -> stats.error(e));
    }

    public void setAutomaticErrorTracking(boolean enabled) {
        withMuxStats(stats -> stats.setAutomaticErrorTracking(enabled));
    }

    public void setStreamType(int type) {
        streamType = type;
    }
}

