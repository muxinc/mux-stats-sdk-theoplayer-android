package com.mux.stats.sdk.muxstats.theoplayer;

import com.mux.stats.sdk.core.CustomOptions;
import com.mux.stats.sdk.core.events.playback.TimeUpdateEvent;
import com.mux.stats.sdk.core.model.CustomerData;
import com.mux.stats.sdk.core.model.CustomerPlayerData;
import com.mux.stats.sdk.core.model.CustomerVideoData;
import com.mux.stats.sdk.muxstats.IDevice;
import com.mux.stats.sdk.muxstats.INetworkRequest;
import com.mux.stats.sdk.muxstats.IPlayerListener;
import com.mux.stats.sdk.muxstats.MuxStats;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MuxStatsReleaseTest {
    private final AtomicBoolean failNextTimerTick = new AtomicBoolean();
    private final AtomicInteger heightReads = new AtomicInteger();
    private final AtomicInteger releases = new AtomicInteger();
    private final AtomicReference<Thread> eventThread = new AtomicReference<>();
    private final CountDownLatch readingPlayerData = new CountDownLatch(1);
    private final CountDownLatch finishReadingPlayerData = new CountDownLatch(1);
    private final CountDownLatch released = new CountDownLatch(1);
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private IDevice previousDevice;
    private INetworkRequest previousNetwork;
    private MuxStats stats;
    private String blockingGetter;

    @Before
    public void setUp() {
        previousDevice = MuxStats.getHostDevice();
        previousNetwork = MuxStats.getHostNetworkApi();
        MuxStats.setHostDevice((IDevice) Proxy.newProxyInstance(
                IDevice.class.getClassLoader(), new Class<?>[]{IDevice.class},
                (proxy, method, args) -> method.getReturnType() == long.class
                        ? System.nanoTime() / 1000000L : defaultValue(method.getReturnType())));
        MuxStats.setHostNetworkApi((INetworkRequest) Proxy.newProxyInstance(
                INetworkRequest.class.getClassLoader(), new Class<?>[]{INetworkRequest.class},
                (proxy, method, args) -> defaultValue(method.getReturnType())));
        IPlayerListener listener = (IPlayerListener) Proxy.newProxyInstance(
                IPlayerListener.class.getClassLoader(), new Class<?>[]{IPlayerListener.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("isPaused")) {
                        if (failNextTimerTick.compareAndSet(true, false)) {
                            throw new IllegalStateException("Simulated timer failure");
                        }
                        return true;
                    }
                    if (method.getName().equals("getPlayerViewHeight")) {
                        heightReads.incrementAndGet();
                    }
                    if (method.getName().equals(blockingGetter)
                            && Thread.currentThread() == eventThread.get()) {
                        readingPlayerData.countDown();
                        assertTrue(finishReadingPlayerData.await(5, TimeUnit.SECONDS));
                    }
                    return method.getName().equals("getVideoPartHoldback")
                            ? null : defaultValue(method.getReturnType());
                });
        CustomerPlayerData playerData = new CustomerPlayerData();
        playerData.setEnvironmentKey("test");
        CustomerData data = new CustomerData(playerData, new CustomerVideoData(), null);
        stats = MuxBaseSDKTheoPlayer.createMuxStats(listener, "release-test", data,
                new CustomOptions(), () -> {
                    releases.incrementAndGet();
                    released.countDown();
                });
    }

    @After
    public void tearDown() throws Exception {
        finishReadingPlayerData.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        try {
            if (stats != null) {
                stats.release();
            }
        } finally {
            MuxStats.setHostDevice(previousDevice);
            MuxStats.setHostNetworkApi(previousNetwork);
        }
    }

    @Test
    public void eventsAfterCoreReleaseDoNotReadClearedListener() {
        stats.release();
        int readsAfterRelease = heightReads.get();
        stats.handle(new TimeUpdateEvent(null));
        assertEquals(readsAfterRelease, heightReads.get());
        assertEquals(1, releases.get());
    }

    @Test
    public void repeatedCoreReleaseIsSafeAndPreservesFinalPlayerData() {
        int readsBeforeRelease = heightReads.get();
        stats.release();
        assertTrue(heightReads.get() > readsBeforeRelease);
        int readsAfterRelease = heightReads.get();
        stats.release();
        assertEquals(readsAfterRelease, heightReads.get());
        assertEquals(1, releases.get());
    }

    @Test
    public void coreReleaseWaitsForPlayerDataRead() throws Exception {
        assertReleaseWaitsForInFlightEvent("getPlayerViewWidth");
    }

    @Test
    public void coreReleaseWaitsForVideoDataRead() throws Exception {
        assertReleaseWaitsForInFlightEvent("getVideoPartHoldback");
    }

    private void assertReleaseWaitsForInFlightEvent(String getter) throws Exception {
        blockingGetter = getter;
        Future<?> event = executor.submit(() -> {
            eventThread.set(Thread.currentThread());
            stats.handle(new TimeUpdateEvent(null));
        });
        assertTrue(readingPlayerData.await(5, TimeUnit.SECONDS));
        CountDownLatch releaseStarted = new CountDownLatch(1);
        Future<?> release = executor.submit(() -> {
            releaseStarted.countDown();
            stats.release();
        });
        boolean releasedDuringEvent = false;
        try {
            assertTrue(releaseStarted.await(5, TimeUnit.SECONDS));
            release.get(100, TimeUnit.MILLISECONDS);
            releasedDuringEvent = true;
        } catch (TimeoutException expected) {
            assertEquals(0, releases.get());
        } finally {
            finishReadingPlayerData.countDown();
        }
        event.get(5, TimeUnit.SECONDS);
        release.get(5, TimeUnit.SECONDS);
        assertFalse(releasedDuringEvent);
        stats.handle(new TimeUpdateEvent(null));
        assertEquals(1, releases.get());
    }

    @Test
    public void actualTimerFailureNotifiesAdapterAndRejectsLaterEvents() throws Exception {
        failNextTimerTick.set(true);
        assertTrue(released.await(5, TimeUnit.SECONDS));
        stats.handle(new TimeUpdateEvent(null));
        assertEquals(1, releases.get());
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class || type == Integer.class) {
            return 0;
        }
        if (type == long.class || type == Long.class) {
            return 0L;
        }
        if (type == Float.class) {
            return 0f;
        }
        if (type == String.class) {
            return "test";
        }
        return null;
    }
}
