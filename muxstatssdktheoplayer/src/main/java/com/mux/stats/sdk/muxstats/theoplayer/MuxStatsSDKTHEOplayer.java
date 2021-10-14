package com.mux.stats.sdk.muxstats.theoplayer;

import android.content.Context;

import com.mux.stats.sdk.core.CustomOptions;
import com.mux.stats.sdk.core.MuxSDKViewOrientation;
import com.mux.stats.sdk.core.model.CustomerData;
import com.mux.stats.sdk.core.model.CustomerPlayerData;
import com.mux.stats.sdk.core.model.CustomerVideoData;
import com.mux.stats.sdk.core.model.CustomerViewData;
import com.mux.stats.sdk.muxstats.INetworkRequest;
import com.mux.stats.sdk.muxstats.MuxErrorException;
import com.theoplayer.android.api.THEOplayerView;

import java.lang.ref.WeakReference;

public class MuxStatsSDKTHEOplayer extends MuxBaseSDKTheoPlayer {

    public MuxStatsSDKTHEOplayer(Context ctx, THEOplayerView player, String playerName,
    CustomerData data) {
        this(ctx, player, playerName, data, new CustomOptions(), new MuxNetworkRequests());
    }

    public MuxStatsSDKTHEOplayer(Context ctx, THEOplayerView player, String playerName,
        CustomerData data,
        CustomOptions options,
        INetworkRequest networkRequests) {
        super(ctx, player, playerName, data, options, networkRequests);
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

