/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mux.stats.sdk.muxstats.theoplayer.demo;

import android.content.Context;
import android.net.Uri;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.google.ads.interactivemedia.v3.api.Ad;
import com.google.ads.interactivemedia.v3.api.AdDisplayContainer;
import com.google.ads.interactivemedia.v3.api.AdError;
import com.google.ads.interactivemedia.v3.api.AdError.AdErrorCode;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent.AdErrorListener;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent.AdEventListener;
import com.google.ads.interactivemedia.v3.api.AdEvent.AdEventType;
import com.google.ads.interactivemedia.v3.api.AdPodInfo;
import com.google.ads.interactivemedia.v3.api.AdsLoader;
import com.google.ads.interactivemedia.v3.api.AdsLoader.AdsLoadedListener;
import com.google.ads.interactivemedia.v3.api.AdsManager;
import com.google.ads.interactivemedia.v3.api.AdsManagerLoadedEvent;
import com.google.ads.interactivemedia.v3.api.AdsRenderingSettings;
import com.google.ads.interactivemedia.v3.api.AdsRequest;
import com.google.ads.interactivemedia.v3.api.CompanionAdSlot;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.api.ImaSdkSettings;
import com.google.ads.interactivemedia.v3.api.StreamRequest;
import com.google.ads.interactivemedia.v3.api.UiElement;

//import com.google.ads.interactivemedia.v3.api.player.AdMediaInfo;

import com.google.ads.interactivemedia.v3.api.player.AdMediaInfo;
import com.google.ads.interactivemedia.v3.api.player.ContentProgressProvider;
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import com.mux.stats.sdk.core.events.playback.EndedEvent;
import com.theoplayer.android.api.THEOplayerConfig;
import com.theoplayer.android.api.THEOplayerView;
import com.theoplayer.android.api.event.EventListener;
import com.theoplayer.android.api.event.player.PauseEvent;
import com.theoplayer.android.api.event.player.PlayEvent;
import com.theoplayer.android.api.event.player.PlayerEventTypes;
import com.theoplayer.android.api.player.RequestCallback;
import com.theoplayer.android.api.source.SourceDescription;
import com.theoplayer.android.api.source.SourceType;
import com.theoplayer.android.api.source.TypedSource;
import com.theoplayer.android.api.source.addescription.GoogleImaAdDescription;
import com.theoplayer.android.api.timerange.TimeRanges;

import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.theoplayer.android.api.source.SourceDescription.Builder.sourceDescription;
import static com.theoplayer.android.api.source.TypedSource.Builder.typedSource;


public final class ImaAdsLoader
    implements
        VideoAdPlayer,
        AdErrorListener,
        AdsLoadedListener,
        AdEventListener {


  static final String TAG = "ImaAdsLoader";

  Context context;
  AdsLoader adsLoader;
  AdDisplayContainer adDisplayContainer;
  THEOplayerView playerView;
  String adTagUri;
  boolean isAdDisplayed = false;
  boolean isAdRequestsed = false;
  double currentPlaybackTime;
  ImaSdkSettings imaSdkSettings;
  AdsManager adsManager;
  SourceDescription consumerContent;
  double consumerContentPosition;

  ArrayList<AdsLoadedListener> adsLoadedListeners = new ArrayList<>();
  ArrayList<AdErrorListener> adsErrorListeners = new ArrayList<>();
  ArrayList<VideoAdPlayerCallback> adsPlaybackListeners = new ArrayList<>();
  ArrayList<AdEventListener> adsEventListeners = new ArrayList<>();

  public ImaAdsLoader(
          Context context,
          THEOplayerView playerView) {
    this.context = context;
    this.playerView = playerView;
    imaSdkSettings = ImaSdkFactory.getInstance().createImaSdkSettings();
    adDisplayContainer = ImaSdkFactory.getInstance().createAdDisplayContainer();
    adDisplayContainer.setPlayer(this);
    adDisplayContainer.setAdContainer(playerView);

    initAdsLoader();
    hookPlayerEvents();
  }

  public void setAdTagUri(String adTagUri) {
    this.adTagUri = adTagUri;
    isAdRequestsed = false;
  }

  void initAdsLoader() {
    adsLoader = ImaSdkFactory.getInstance().createAdsLoader(
            context,
            imaSdkSettings,
            adDisplayContainer
    );
    adsLoader.addAdErrorListener(/* adErrorListener= */ this);
    adsLoader.addAdsLoadedListener(/* adsLoadedListener= */ this);
  }

  public void addAdsErrorListener(AdErrorListener listener) {
    if (!adsErrorListeners.contains(listener)) {
      adsErrorListeners.add(listener);
    }
  }

  public void addAdsEventListener(AdEventListener listener) {
    if (!adsEventListeners.contains(listener)) {
      adsEventListeners.add(listener);
    }
  }

  void releaseAdsLoader() {
    if (adsManager != null) {
      adsManager.destroy();
      adsManager = null;
    }
  }

  void hookPlayerEvents() {
    playerView.getPlayer().addEventListener(PlayerEventTypes.ENDED, (endedEvent -> {
      if (isAdDisplayed) {
        Log.i(TAG, "Ad content playback ended !!!");
        for (VideoAdPlayerCallback adCallback : adsPlaybackListeners) {
          adCallback.onEnded(new AdMediaInfo(playerView.getPlayer().getSrc()));
        }
      } else {
        Log.i(TAG, "Consumer content playback ended !!!");
        adsLoader.contentComplete();
      }
    }));

    playerView.getPlayer().addEventListener(PlayerEventTypes.PLAY, (playEvent -> {
      if (isAdDisplayed) {
        Log.i(TAG, "Ad content playback started !!!");
        for (VideoAdPlayerCallback adCallback : adsPlaybackListeners) {
          adCallback.onPlay(new AdMediaInfo(playerView.getPlayer().getSrc()));
        }
      } else {
        Log.i(TAG, "Consumer content playback started !!!");
        if (!isAdRequestsed) {
          createAdsRequests();
        }
      }
    }));

    playerView.getPlayer().addEventListener(PlayerEventTypes.TIMEUPDATE, (timeUpdateEvent -> {
      currentPlaybackTime = timeUpdateEvent.getCurrentTime();
    }));

    playerView.getPlayer().addEventListener(PlayerEventTypes.VOLUMECHANGE, (volumeChangeEvent -> {
      if (isAdDisplayed) {
        for (VideoAdPlayerCallback adCallback : adsPlaybackListeners) {
          adCallback.onVolumeChanged(new AdMediaInfo(playerView.getPlayer().getSrc()), (int)volumeChangeEvent.getVolume());
        }
      }
    }));

    playerView.getPlayer().addEventListener(PlayerEventTypes.PAUSE, (pauseEvent -> {
      if (isAdDisplayed) {
        Log.i(TAG, "Ad content playback paused !!!");
        for (VideoAdPlayerCallback adCallback : adsPlaybackListeners) {
          adCallback.onPause(new AdMediaInfo(playerView.getPlayer().getSrc()));
        }
      } else {
        Log.i(TAG, "Consumer content playback paused !!!");
      }
    }));

    playerView.getPlayer().addEventListener(PlayerEventTypes.ERROR, (errorEvent -> {
      if (isAdDisplayed) {
        Log.i(TAG, "Ad content playback error !!!");
        for (VideoAdPlayerCallback adCallback : adsPlaybackListeners) {
          adCallback.onError(new AdMediaInfo(playerView.getPlayer().getSrc()));
        }
      } else {
        Log.i(TAG, "Consumer content playback error !!!");
      }
    }));
  }

  private void createAdsRequests() {
    Log.i(TAG, "Creating ads request ...");
    isAdRequestsed = true;
    AdDisplayContainer adDisplayContainer = ImaSdkFactory.getInstance().createAdDisplayContainer();
    adDisplayContainer.setAdContainer(playerView);

    AdsRequest request = ImaSdkFactory.getInstance().createAdsRequest();
    request.setAdTagUrl(adTagUri);
    // Maybe set new adDisplay container instance
//    request.setAdDisplayContainer(adDisplayContainer);
    request.setContentProgressProvider(() -> {
        if (isAdDisplayed || playerView == null || playerView.getPlayer().getDuration() <= 0) {
          return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
        }
        return new VideoProgressUpdate((long)currentPlaybackTime,
                (long)playerView.getPlayer().getDuration());
      });

    // Request the ad. After the ad is loaded, onAdsManagerLoaded() will be called.
    adsLoader.requestAds(request);
  }

  @Override
  public void onAdsManagerLoaded(AdsManagerLoadedEvent adsManagerLoadedEvent) {
    Log.i(TAG, "onAdsManagerLoaded");
    adsManager = adsManagerLoadedEvent.getAdsManager();

    adsManager.addAdErrorListener(this);
    adsManager.addAdEventListener(this);
    adsManager.init();
  }

  @Override
  public void onAdError(AdErrorEvent adErrorEvent) {
    Log.e(TAG, "Ad Error: " + adErrorEvent.getError().getMessage());
    isAdDisplayed = true;
    resumeContentAfterAdPlayback();
    for (AdErrorListener listener : adsErrorListeners) {
      listener.onAdError(adErrorEvent);
    }
  }

  @Override
  public void onAdEvent(AdEvent adEvent) {
    Log.i(TAG, "Event: " + adEvent.getType());

    // These are the suggested event types to handle. For full list of all ad event
    // types, see the documentation for AdEvent.AdEventType.
    switch (adEvent.getType()) {
      case LOADED:
        // AdEventType.LOADED will be fired when ads are ready to be played.
        // AdsManager.start() begins ad playback. This method is ignored for VMAP or
        // ad rules playlists, as the SDK will automatically start executing the
        // playlist.
        adsManager.start();
        break;
      case CONTENT_PAUSE_REQUESTED:
        // AdEventType.CONTENT_PAUSE_REQUESTED is fired immediately before a video
        // ad is played.
        pauseContentForAdPlayback();
        break;
      case CONTENT_RESUME_REQUESTED:
        // AdEventType.CONTENT_RESUME_REQUESTED is fired when the ad is completed
        // and you should start playing your content.
        resumeContentAfterAdPlayback();
        break;
      case ALL_ADS_COMPLETED:
        break;
      default:
        break;
    }
    for (AdEventListener listener : adsEventListeners) {
      listener.onAdEvent(adEvent);
    }
  }

  void pauseContentForAdPlayback() {
    // Save current playback position and reset the value
    Log.i(TAG, "pauseContentForAdPlayback");
    isAdDisplayed = true;
    consumerContentPosition = currentPlaybackTime;
    currentPlaybackTime = 0;
    consumerContent = playerView.getPlayer().getSource();
    playerView.getPlayer().pause();
  }

  void resumeContentAfterAdPlayback() {
    if (isAdDisplayed) {
      Log.i(TAG, "resumeContentAfterAdPlayback");
      currentPlaybackTime = consumerContentPosition;
      // TODO maybe seek to consumerContentPosition
      playerView.getPlayer().setSource(consumerContent);
      playerView.getPlayer().setCurrentTime(consumerContentPosition);
      playerView.getPlayer().play();
      isAdDisplayed = false;
    } else {
      Log.i(TAG, "Why !!!!!!!!!!!!!1");
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////
  ///////////////////// VideoAdPlayer interface ////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////////////

  @Override
  public void loadAd(AdMediaInfo adMediaInfo, AdPodInfo adPodInfo) {
    Log.i(TAG, "loadAd: ");
    String addUrl = adMediaInfo.getUrl();
    TypedSource.Builder typedSource =  typedSource(addUrl);

    SourceDescription.Builder sourceDescription = sourceDescription(typedSource.build());
    playerView.getPlayer().setSource(sourceDescription.build());
  }

  @Override
  public void playAd(AdMediaInfo adMediaInfo) {
    Log.i(TAG, "playAd");
    playerView.getPlayer().play();
  }

  @Override
  public void pauseAd(AdMediaInfo adMediaInfo) {
    Log.i(TAG, "pauseAd");
    playerView.getPlayer().pause();
  }

  @Override
  public void stopAd(AdMediaInfo adMediaInfo) {
    Log.i(TAG, "stopAd");
    playerView.getPlayer().stop();
    for (VideoAdPlayerCallback adCallback : adsPlaybackListeners) {
      adCallback.onEnded(adMediaInfo);
    }
  }

  @Override
  public void release() {
    Log.i(TAG, "release Ad");
//    playerView.getPlayer().play();
    // TODO maybe resume playback content
  }

  @Override
  public void addCallback(VideoAdPlayerCallback listener) {
    if (!adsPlaybackListeners.contains(listener)) {
      adsPlaybackListeners.add(listener);
    }
  }

  @Override
  public void removeCallback(VideoAdPlayerCallback listener) {
    adsPlaybackListeners.remove(listener);
  }

  @Override
  public VideoProgressUpdate getAdProgress() {
//    Log.i(TAG, "getAdProgress");
    if (!isAdDisplayed || playerView == null || playerView.getPlayer().getDuration() <= 0) {
      return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
    }
    return new VideoProgressUpdate((long)currentPlaybackTime,
            (long)playerView.getPlayer().getDuration());
  }

  @Override
  public int getVolume() {
    Log.i(TAG, "getVolume");
    return (int)playerView.getPlayer().getVolume();
  }


}
