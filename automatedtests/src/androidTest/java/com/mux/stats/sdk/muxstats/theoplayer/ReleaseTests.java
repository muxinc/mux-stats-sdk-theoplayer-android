package com.mux.stats.sdk.muxstats.theoplayer;

import com.mux.stats.sdk.core.MuxSDKViewOrientation;
import com.mux.stats.sdk.core.events.playback.TimeUpdateEvent;
import com.mux.stats.sdk.core.model.CustomerPlayerData;
import com.mux.stats.sdk.core.model.CustomerVideoData;
import com.mux.stats.sdk.core.model.CustomerViewData;
import com.mux.stats.sdk.muxstats.MuxErrorException;
import com.mux.stats.sdk.muxstats.MuxSDKViewPresentation;
import com.mux.stats.sdk.muxstats.automatedtests.TestBase;

import org.junit.Before;
import org.junit.Test;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ReleaseTests extends TestBase {
    private MuxStatsSDKTHEOPlayer adapter;

    @Before
    @Override
    public void init() {
        testActivity = activityRule.getActivity();
        getInstrumentation().runOnMainSync(() -> {
            testActivity.initMuxSats();
            adapter = testActivity.getMuxStats();
        });
    }

    @Test
    public void publicCallsAfterAdapterReleaseAreSafe() {
        getInstrumentation().runOnMainSync(adapter::release);
        assertNull(adapter.muxStats);
        assertReleasedCallsAreSafe();
        getInstrumentation().runOnMainSync(adapter::release);
    }

    @Test
    public void publicCallsAfterInternalCoreReleaseAreSafe() {
        getInstrumentation().runOnMainSync(() -> adapter.muxStats.release());
        assertNotNull(adapter.muxStats);
        assertReleasedCallsAreSafe();
        getInstrumentation().runOnMainSync(adapter::release);
    }

    private void assertReleasedCallsAreSafe() {
        CustomerPlayerData playerData = new CustomerPlayerData();
        CustomerVideoData videoData = new CustomerVideoData();
        adapter.updateCustomerData(playerData, videoData);
        adapter.updateCustomerData(playerData, videoData, new CustomerViewData());
        adapter.orientationChange(MuxSDKViewOrientation.LANDSCAPE);
        adapter.presentationChange(MuxSDKViewPresentation.FULLSCREEN);
        adapter.videoChange(videoData);
        adapter.programChange(videoData);
        adapter.setPlayerSize(1920, 1080);
        adapter.setScreenSize(1920, 1080);
        adapter.error(new MuxErrorException(1, "Late error"));
        adapter.setAutomaticErrorTracking(false);
        adapter.enableMuxCoreDebug(false, false);
        adapter.dispatch(new TimeUpdateEvent(null));
        assertNull(adapter.getCustomerVideoData());
        assertNull(adapter.getCustomerPlayerData());
        assertNull(adapter.getCustomerViewData());
    }
}
