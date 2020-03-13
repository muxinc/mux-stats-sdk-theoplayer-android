package com.mux.stats.sdk.muxstats.theoplayer.demo;

import android.util.Log;

import com.google.ads.interactivemedia.v3.api.Ad;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.AdsManager;
import com.theoplayer.android.api.THEOplayerView;


public class AdsListener implements AdErrorEvent.AdErrorListener, AdEvent.AdEventListener {

    final static String TAG = "IMASDKs";

    private long adLoadTime = 0;
    private AdsManager adsManager;
    private THEOplayerView theoPlayerView;

    @Override
    public void onAdError(AdErrorEvent adErrorEvent) {
        //Log.i(TAG, "Error: " + adErrorEvent.getError().getMessage());
    }

    @Override
    public void onAdEvent(AdEvent adEvent) {
        Ad ad = adEvent.getAd();
        long currTime = System.currentTimeMillis();
        long delta = 0;
        if (adLoadTime > 0) {
            delta = currTime - adLoadTime;
            adLoadTime = currTime;
        }
        Log.e(TAG, "Event: " + adEvent.getType() + " at " + String.valueOf(currTime) + ", + " + String.valueOf(delta));
        if (ad != null) {
            Log.i(TAG, "++++++++++++Ad: " + ad.toString());
        }
        switch (adEvent.getType()) {
            case AD_BREAK_READY:
                adsManager.start();
                break;
            case LOADED:
                // AdEventType.LOADED will be fired when ads are ready to be played.
                // AdsManager.start() begins ad playback. This method is ignored for VMAP or
                // ad rules playlists, as the SDK will automatically start executing the
                // playlist.
                adLoadTime = System.currentTimeMillis();
                adsManager.start();
                break;
            case CONTENT_PAUSE_REQUESTED:
                // AdEventType.CONTENT_PAUSE_REQUESTED is fired immediately before a video
                // ad is played.
                theoPlayerView.onPause();
                break;
            case CONTENT_RESUME_REQUESTED:
                // AdEventType.CONTENT_RESUME_REQUESTED is fired when the ad is completed
                // and you should start playing your content.
                theoPlayerView.onResume();
                break;
            case ALL_ADS_COMPLETED:
                if (adsManager != null) {
                    adsManager.destroy();
                    adsManager = null;
                }
                break;
            default:
                break;
        }
    }
}
