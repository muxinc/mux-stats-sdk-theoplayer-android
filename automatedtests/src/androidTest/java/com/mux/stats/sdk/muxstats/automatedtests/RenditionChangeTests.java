package com.mux.stats.sdk.muxstats.automatedtests;

import android.util.Log;

import com.mux.stats.sdk.core.events.playback.PlayingEvent;
import com.mux.stats.sdk.core.events.playback.RenditionChangeEvent;
import com.mux.stats.sdk.core.model.VideoData;

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

//    @Before
//    public void init(){
//        urlToPlay = "http://localhost:5000/hls/google_glass/playlist.m3u8";
//        // These video have larger bitrate, make sure we do not cause any
//        // rebuffering due to low bandwith
//        bandwidthLimitInBitsPerSecond = 12000000;
//        super.init();
//    }
//
//    @Test
//    public void testRenditionChange() {
//        try {
//            if(!testActivity.waitForPlaybackToStart(waitForPlaybackToStartInMS)) {
//                fail("Playback did not start in " + waitForPlaybackToStartInMS + " milliseconds !!!");
//            }
//            Thread.sleep(PAUSE_PERIOD_IN_MS);
//            int startingQualityIndex = getLowestQualityIndex();
//            int nextQualityIndex = getHighestQualityIndex();
//            List<QualityLevel> availableQualities = pView.getQualityLevels();
//            QualityLevel startingQuality = availableQualities.get(startingQualityIndex);
//            QualityLevel changedQuality = availableQualities.get(nextQualityIndex);
//            testActivity.runOnUiThread(() -> {
//                Log.e("MuxStatsEvent", "Switching quality to: " + startingQuality);
//                pView.setCurrentQuality(startingQualityIndex);
//            });
//            Thread.sleep(PLAY_PERIOD_IN_MS);
//            // Switch rendition
//            testActivity.runOnUiThread(() -> {
//                Log.e("MuxStatsEvent", "Switching quality to: " + changedQuality);
//                pView.setCurrentQuality(nextQualityIndex);
//            });
//
//            Thread.sleep(PLAY_PERIOD_IN_MS);
//            int renditionChangeIndex = 0;
//            int playinIndex = networkRequest.getIndexForFirstEvent(PlayingEvent.TYPE);
//            JSONArray receivedRenditionChangeEvents = new JSONArray();
//            while (true) {
//                renditionChangeIndex = networkRequest
//                        .getIndexForNextEvent(renditionChangeIndex + 1, RenditionChangeEvent.TYPE);
//                long lastRenditionChangeAt = networkRequest
//                        .getCreationTimeForEvent(renditionChangeIndex) - networkRequest
//                        .getCreationTimeForEvent(playinIndex);
//                if (renditionChangeIndex == -1) {
//                    fail("Failed to find RenditionChangeEvent dispatched after: "
//                            + PLAY_PERIOD_IN_MS + " ms since playback started, with valid data"
//                            + ", received events: " + receivedRenditionChangeEvents.toString());
//                }
//                JSONObject jo = networkRequest.getEventForIndex(renditionChangeIndex);
//                receivedRenditionChangeEvents.put(jo);
//                if (Math.abs(lastRenditionChangeAt - (PLAY_PERIOD_IN_MS + PAUSE_PERIOD_IN_MS)) < 1000) {
//                    // We found rendition change index we ware looking for, there may be more after,
//                    // because I dont know how to controll the player bitadaptive settings
//                    if ( !jo.has(VideoData.VIDEO_SOURCE_WIDTH) || ! jo.has(VideoData.VIDEO_SOURCE_HEIGHT)) {
//                        Log.w(TAG, "Missing video width and/or video height parameters on Rendition change event, "
//                                + " json: " + jo.toString());
//                        continue;
//                    }
//                    break;
//                }
//            }
//
//            JSONObject jo = networkRequest.getEventForIndex(renditionChangeIndex);
//            int videoWidth = jo.getInt(VideoData.VIDEO_SOURCE_WIDTH);
//            int videoHeight = jo.getInt(VideoData.VIDEO_SOURCE_HEIGHT);
//            if (videoWidth != changedQuality.getWidth() && videoHeight != changedQuality.getHeight()) {
//                fail("Last reported rendition change width and height (" + videoWidth + "x" +
//                        videoHeight + ") do not match requested format resolution: (" +
//                        changedQuality.getWidth() + "x" + changedQuality.getHeight() + ")");
//            }
//        } catch (Exception e) {
//            fail(getExceptionFullTraceAndMessage( e ));
//        }
//    }
//
//    private int getLowestQualityIndex() {
//        int minWidth = 10000000;
//        int minIndex = -1;
//        for (int index = 0; index < pView.getQualityLevels().size(); index++) {
//            QualityLevel ql = pView.getQualityLevels().get(index);
//            if (ql.getLabel().equalsIgnoreCase("auto")) {
//                continue;
//            }
//            if (ql.getWidth() < minWidth ) {
//                minWidth = ql.getWidth();
//                minIndex = index;
//            }
//        }
//        return minIndex;
//    }
//
//    private int getHighestQualityIndex() {
//        int maxWidth = -1;
//        int maxIndex = -1;
//        for (int index = 0; index < pView.getQualityLevels().size(); index++) {
//            QualityLevel ql = pView.getQualityLevels().get(index);
//            if (ql.getLabel().equalsIgnoreCase("auto")) {
//                continue;
//            }
//            if (ql.getWidth() > maxWidth ) {
//                maxWidth = ql.getWidth();
//                maxIndex = index;
//            }
//        }
//        return maxIndex;
//    }
}
