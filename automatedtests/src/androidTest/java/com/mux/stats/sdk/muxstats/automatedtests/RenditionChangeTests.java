package com.mux.stats.sdk.muxstats.automatedtests;

import android.util.Log;

import com.mux.stats.sdk.core.events.playback.PlayingEvent;
import com.mux.stats.sdk.core.events.playback.RenditionChangeEvent;
import com.mux.stats.sdk.core.model.VideoData;
import com.theoplayer.android.api.player.track.mediatrack.MediaTrack;
import com.theoplayer.android.api.player.track.mediatrack.quality.VideoQuality;

import com.theoplayer.android.api.source.SourceType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;

public class RenditionChangeTests extends TestBase {

    static final String TAG = "RenditionChangeTests";

    @Before
    public void init(){
        //urlToPlay = "http://localhost:5000/hls/google_glass/playlist.m3u8";
//        urlToPlay = "http://localhost:5000/dash/google_glass/playlist.mpd";
//        urlToPlay = "https://cdn.theoplayer.com/video/dash/big_buck_bunny/BigBuckBunny_10s_simple_2014_05_09.mpd";

        // These video have larger bitrate, make sure we do not cause any
        // rebuffering due to low bandwith
        bandwidthLimitInBitsPerSecond = 12000000;
        sourceType = SourceType.HLS;
//        sourceType = SourceType.DASH;
        super.init();
    }

    @Test
    public void testRenditionChange() {
        try {
            if (playWhenReady) {
                testActivity.runOnUiThread(() -> {
                    pView.getPlayer().play();
                });
            }
            if(!testActivity.waitForPlaybackToStart(waitForPlaybackToStartInMS)) {
                fail("Playback did not start in " + waitForPlaybackToStartInMS + " milliseconds !!!");
            }
            Thread.sleep(PAUSE_PERIOD_IN_MS);
            int startingQualityIndex = getLowestQualityIndex();
            int nextQualityIndex = getHighestQualityIndex();

            MediaTrack<VideoQuality> availableQualities = pView.getPlayer().getVideoTracks().getItem(0);
            VideoQuality startingQuality = availableQualities.getQualities().getItem(startingQualityIndex);
            VideoQuality changedQuality = availableQualities.getQualities().getItem(nextQualityIndex);
            testActivity.runOnUiThread(() -> {
                Log.e("MuxStatsEvent", "Switching quality to: "
              + startingQuality + "\n" + startingQuality.getWidth()
                  + "x" + startingQuality.getHeight()
                );
//                pView.setCurrentQuality(startingQualityIndex);
                pView.getPlayer().getVideoTracks().getItem(0).setTargetQuality(startingQuality);
            });
            Thread.sleep(PLAY_PERIOD_IN_MS);
            // Switch rendition
            testActivity.runOnUiThread(() -> {
                Log.e("MuxStatsEvent", "Switching quality to: "
                    + changedQuality + "\n" + changedQuality.getWidth()
                    + "x" + changedQuality.getHeight()
                );
//                pView.setCurrentQuality(nextQualityIndex);
                pView.getPlayer().getVideoTracks().getItem(0).setTargetQuality(changedQuality);
            });

            // Wait for beacon + time it takes for player to actually change renditions
            Thread.sleep(WAIT_FOR_NETWORK_PERIOD_IN_MS * 3);

            int playingIndex = networkRequest.getIndexForFirstEvent(PlayingEvent.TYPE);
            JSONArray receivedRenditionChangeEvents = new JSONArray();

            for (JSONObject jo : networkRequest.getAllEventsOfType(RenditionChangeEvent.TYPE)) {
                if (!jo.has(VideoData.VIDEO_SOURCE_HEIGHT) || !jo.has(VideoData.VIDEO_SOURCE_WIDTH)) {
                  Log.d(TAG, "Skipped renditionchange without dimensions");
                  continue;
                }
                int videoWidth = jo.getInt(VideoData.VIDEO_SOURCE_WIDTH);
                int videoHeight = jo.getInt(VideoData.VIDEO_SOURCE_HEIGHT);
                Log.i(TAG, "Recorded Rendition Change ");
                Log.v(TAG, "\tDimensions " + videoWidth + "x" + videoHeight);
                receivedRenditionChangeEvents.put(jo);
            }

            JSONObject jo = receivedRenditionChangeEvents.getJSONObject(
                receivedRenditionChangeEvents.length() - 1
            );
            int videoWidth = jo.getInt(VideoData.VIDEO_SOURCE_WIDTH);
            int videoHeight = jo.getInt(VideoData.VIDEO_SOURCE_HEIGHT);
            if (videoWidth != changedQuality.getWidth() && videoHeight != changedQuality.getHeight()) {
                fail("Last reported rendition change width and height (" + videoWidth + "x" +
                        videoHeight + ") do not match requested format resolution: (" +
                        changedQuality.getWidth() + "x" + changedQuality.getHeight() + ")");
            }
        } catch (Exception e) {
            fail(getExceptionFullTraceAndMessage( e ));
        }
    }

    private int getLowestQualityIndex() {
        int minWidth = 10000000;
        int minIndex = -1;
//        for ( VideoQuality vq : pView.getPlayer().getVideoTracks().getItem(0).getQualities() ) {
        for ( int index = 0;
              index < pView.getPlayer().getVideoTracks().getItem(0).getQualities().length(); index ++ ) {
            VideoQuality vq = pView.getPlayer().getVideoTracks().getItem(0).getQualities().getItem(index);
            if (vq.getWidth() < minWidth ) {
                minWidth = vq.getWidth();
                minIndex = index;
            }
        }
        return minIndex;
    }

    private int getHighestQualityIndex() {
        int maxWidth = -1;
        int maxIndex = -1;
        for ( int index = 0;
              index < pView.getPlayer().getVideoTracks().getItem(0).getQualities().length(); index ++ ) {
            VideoQuality vq = pView.getPlayer().getVideoTracks().getItem(0).getQualities().getItem(index);
            if (vq.getWidth() > maxWidth ) {
                maxWidth = vq.getWidth();
                maxIndex = index;
            }
        }
        return maxIndex;
    }
}
