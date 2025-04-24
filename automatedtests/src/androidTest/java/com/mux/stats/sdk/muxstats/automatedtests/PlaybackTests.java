package com.mux.stats.sdk.muxstats.automatedtests;

import android.os.Bundle;
import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.mux.stats.sdk.core.events.playback.PauseEvent;
import com.mux.stats.sdk.core.events.playback.PlayEvent;
import com.mux.stats.sdk.core.events.playback.PlayingEvent;
import com.mux.stats.sdk.core.events.playback.RebufferEndEvent;
import com.mux.stats.sdk.core.events.playback.RebufferStartEvent;
import com.mux.stats.sdk.core.events.playback.ViewStartEvent;

import com.theoplayer.android.api.timerange.TimeRange;
import com.theoplayer.android.api.timerange.TimeRanges;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import static org.junit.Assert.fail;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class PlaybackTests extends TestBase {

    public static final String TAG = "playbackTest";

    @Before
    public void init(){
        if (currentTestName.getMethodName().equalsIgnoreCase("testVodPlayback")) {
            // When testing seeking the player will not seek unless the seek point is already in the buffer.
            // This is a player bug so we will increase the video bandwidth so that it can preload entire video before
            // any player requests are made (play/ pause/ seek/ etc)
            bandwidthLimitInBitsPerSecond = 17000000;
        }
        super.init();
    }

    /*
     * Test Seeking, event order
     */
    @Test
    public void testSeekingOnLowBufferAndViewEndEvent() {
        try {
            if (!testActivity.waitForPlaybackToStart(waitForPlaybackToStartInMS)) {
                fail("Playback did not start in " + waitForPlaybackToStartInMS + " milliseconds !!!");
            }
            // play x seconds, stage 1
            Thread.sleep(PLAY_PERIOD_IN_MS);
            // Seek to the end by triggering touch event
            testActivity.runOnUiThread(() -> {
                double duration = pView.getPlayer().getDuration();
                pView.getPlayer().setCurrentTime(duration - (PLAY_PERIOD_IN_MS / 1000));
            });
            testActivity.waitForPlaybackToFinish();
            finishActivity();
            // TODO check View end event and seking events
        } catch (Exception e) {
            fail(getExceptionFullTraceAndMessage( e ));
        }
    }

    /*
     * According to the self validation guid: https://docs.google.com/document/d/1FU_09N3Cg9xfh784edBJpgg3YVhzBA6-bd5XHLK7IK4/edit#
     * We are implementing vod playback scenario.
     */
    @Test
    public void testVodPlayback() {
        try {
            if(!testActivity.waitForPlaybackToStart(waitForPlaybackToStartInMS)) {
                fail("Playback did not start in " + waitForPlaybackToStartInMS + " milliseconds !!!");
            }
            // play x seconds, stage 1
            Thread.sleep(PLAY_PERIOD_IN_MS);
            pausePlayer();
            // Pause x seconds, stage 2
            Thread.sleep(PAUSE_PERIOD_IN_MS);
            // Resume video, stage 3
            resumePlayer();
            // Play another x seconds
            Thread.sleep(PLAY_PERIOD_IN_MS);

            // Seek backward, stage 4
            testActivity.runOnUiThread(() -> {
                double duration = testActivity.getCurrentPosition();
                pView.getPlayer().setCurrentTime( duration / 2 );
            });

            // Play another x seconds, stage 5
            Thread.sleep(PLAY_PERIOD_IN_MS);
            final AtomicBoolean seekCompleted = new AtomicBoolean(false);
            // seek forward in the video, stage 6
            testActivity.runOnUiThread(() -> {
                double currentPlaybackPosition = testActivity.getCurrentPosition();
                double videoDuration = pView.getPlayer().getDuration();
                double seekToInFuture = currentPlaybackPosition + ((videoDuration - currentPlaybackPosition) / 2);
                TimeRanges onBuffer = pView.getPlayer().getBuffered();
                TimeRanges seekable = pView.getPlayer().getSeekable();
                pView.getPlayer().setCurrentTime(seekToInFuture);
                Log.w(TAG, "Seek in the future completed");
                seekCompleted.set(true);
            });

            // Play another x seconds, stage 7
            Thread.sleep(PLAY_PERIOD_IN_MS);

            CheckupResult result;
            int viewStartEventIndex = networkRequest.getIndexForFirstEvent(ViewStartEvent.TYPE);
            if (viewStartEventIndex == -1) {
                fail("Missing Viewstart event !!!");
            }

            // Check first playback period, stage 1
            result = checkPlaybackPeriodAtIndex(viewStartEventIndex, PLAY_PERIOD_IN_MS);

            // Check pause period, stage 2
            result = checkPausePeriodAtIndex(result.eventIndex, PAUSE_PERIOD_IN_MS);

            // Check playback period, stage 3
            result = checkPlaybackPeriodAtIndex(result.eventIndex -1, PLAY_PERIOD_IN_MS);

            // Check SeekEvents, stage 4
            result = checkSeekAtIndex(result.eventIndex);

            // check playback period stage 5
            result = checkPlaybackPeriodAtIndex(result.eventIndex,
                    PLAY_PERIOD_IN_MS - result.seekPeriod);

            // check seeking, stage 6
            if (seekCompleted.get()) {
                result = checkSeekAtIndex(result.eventIndex);
            } else {
                Log.w(TAG, "Player failed to seek in the future, not good.");
            }

            // Exit the player with back button
//            testScenario.close();
            Log.w(TAG, "See what event should be dispatched on view closed !!!");
            // TODO check player end event
        } catch (Exception e) {
            fail(getExceptionFullTraceAndMessage( e ));
        }
        Log.e(TAG, "All done !!!");
    }

    @Test
    public void testRebufferingAndStartupTime() {
        try {
            testActivity.waitForActivityToInitialize();
            long testStartedAt = System.currentTimeMillis();
            if (!testActivity.waitForPlaybackToStart(waitForPlaybackToStartInMS)) {
                fail("Playback did not start in " + waitForPlaybackToStartInMS + " milliseconds !!!");
            }
            long expectedStartupTime = System.currentTimeMillis() - testStartedAt;

            // play x seconds
            Thread.sleep(PLAY_PERIOD_IN_MS);
            jamNetwork();
            testActivity.waitForPlaybackToStartBuffering();
            long rebufferStartedAT = System.currentTimeMillis();

            // Wait for rebuffer to complete
            if(!testActivity.waitForPlaybackToStart(waitForPlaybackToStartInMS)) {
                fail("Playback did not start in " + waitForPlaybackToStartInMS + " milliseconds !!!");
            }

            long measuredRebufferPeriod = System.currentTimeMillis() - rebufferStartedAT;
            // play x seconds
            Thread.sleep(PLAY_PERIOD_IN_MS * 2);
//            exitActivity();
//            testScenario.close();

            // Startup time check
            int viewstartIndex = networkRequest.getIndexForFirstEvent(ViewStartEvent.TYPE);
            int playIndex = networkRequest.getIndexForFirstEvent(PlayEvent.TYPE);
            int playingIndex = networkRequest.getIndexForFirstEvent(PlayingEvent.TYPE);
            // Check if viewstart and playing events are received
            if (viewstartIndex == -1) {
                fail("viewstart event not received !!!");
            }
            if (playIndex == -1) {
                fail("play event not received !!!");
            }
            if (playingIndex == -1) {
                fail("playing event not received !!!");
            }

            long reportedStartupTime = networkRequest.getCreationTimeForEvent(playingIndex) -
                    networkRequest.getCreationTimeForEvent(viewstartIndex);
            // Check if startup time match with in 200 ms precission
            if (Math.abs(reportedStartupTime - expectedStartupTime) > 1000) {
                fail("Reported startup time and expected startup time do not match within 1000 ms,"
                        + "reported time: " + reportedStartupTime + ", measured startup time: " + expectedStartupTime);
            }

            // check rebuffering events
            int rebufferStartEventIndex = networkRequest.getIndexForFirstEvent(RebufferStartEvent.TYPE);
            int rebufferEndEventIndex = networkRequest.getIndexForFirstEvent(RebufferEndEvent.TYPE);
            // Check if rebuffer events are received
            if (rebufferStartEventIndex == -1) {
                fail("rebufferstart event not received !!!");
            }
            if(rebufferEndEventIndex == -1) {
                fail("rebufferend event not received !!!");
            }
            if (rebufferStartEventIndex > rebufferEndEventIndex) {
                fail("rebufferend received before rebufferstart event !!!");
            }
            int secondPlayIndex = networkRequest.getIndexForLastEvent(PlayEvent.TYPE);
            int secondPlayingIndex = networkRequest.getIndexForLastEvent(PlayingEvent.TYPE);
            if (secondPlayIndex != playIndex) {
                fail("Play event received after rebufferend this is not good  ! event: "
                        + networkRequest.getReceivedEventNames());
            }
            if (secondPlayingIndex == playingIndex) {
                fail("Playing event not received after ebufferEnd ! events: "
                        + networkRequest.getReceivedEventNames());
            }
            // TODO see what is the best way to calculate rebuffer period
        } catch (Exception e) {
            fail(getExceptionFullTraceAndMessage( e ));
        }
    }
}