package com.mux.stats.sdk.muxstats.automatedtests.ui;

import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;

import com.mux.stats.sdk.core.model.CustomerData;
import com.mux.stats.sdk.muxstats.theoplayer.MuxStatsSDKTHEOPlayer;
import com.theoplayer.android.api.THEOplayerView;
import com.mux.stats.sdk.core.model.CustomerPlayerData;
import com.mux.stats.sdk.core.model.CustomerVideoData;
import com.mux.stats.sdk.muxstats.automatedtests.BuildConfig;
import com.mux.stats.sdk.muxstats.automatedtests.R;
import com.mux.stats.sdk.muxstats.automatedtests.mockup.MockNetworkRequest;
import com.theoplayer.android.api.event.ads.AdsEventTypes;
import com.theoplayer.android.api.event.player.PlayerEventTypes;
import com.theoplayer.android.api.player.Player;
import com.theoplayer.android.api.player.ReadyState;
import com.theoplayer.android.api.source.SourceDescription;
import com.theoplayer.android.api.source.TypedSource;
import com.theoplayer.android.api.source.addescription.AdDescription;
import com.theoplayer.android.api.source.addescription.THEOplayerAdDescription;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.theoplayer.android.api.source.SourceDescription.Builder.sourceDescription;
import static com.theoplayer.android.api.source.TypedSource.Builder.typedSource;
import static com.theoplayer.android.api.source.addescription.THEOplayerAdDescription.Builder.adDescription;

public class SimplePlayerTestActivity extends AppCompatActivity
{
    static final String TAG = "SimplePlayerActivity";

    protected static final String PLAYBACK_CHANNEL_ID = "playback_channel";
    protected static final int PLAYBACK_NOTIFICATION_ID = 1;
    protected static final String ARG_URI = "uri_string";
    protected static final String ARG_TITLE = "title";
    protected static final String ARG_START_POSITION = "start_position";

    String videoTitle = "Test Video";
    String urlToPlay;
    THEOplayerView theoPlayerView;
    Player player;
    SourceDescription testMediaSource;
    MuxStatsSDKTHEOPlayer muxStats;
    Uri loadedAdTagUri;
    boolean playWhenReady = true;
    MockNetworkRequest mockNetwork;
    AtomicBoolean onResumedCalled = new AtomicBoolean(false);
    double currentPosition = 0;
    protected ReadyState previousReadyState;
    long playbackStartPosition = 0;

    Lock activityLock = new ReentrantLock();
    Condition playbackEnded = activityLock.newCondition();
    Condition playbackStarted = activityLock.newCondition();
    Condition playbackBuffering = activityLock.newCondition();
    Condition activityClosed = activityLock.newCondition();
    Condition activityInitialized = activityLock.newCondition();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_player_test);
        disableUserActions();

        theoPlayerView = findViewById(R.id.player_view);
        player = theoPlayerView.getPlayer();


        player.getAds().addEventListener(AdsEventTypes.AD_ERROR, event -> {
            Log.e(TAG, "Ads error: " + event.getError());
        });

        player.addEventListener(PlayerEventTypes.READYSTATECHANGE, (stateChange -> {
            ReadyState state = stateChange.getReadyState();
            if (state != null) {
                if (previousReadyState != null
                    && (state.ordinal() < ReadyState.HAVE_ENOUGH_DATA.ordinal()
                    || (state.ordinal() < previousReadyState.ordinal()))
                ) {
                    signalPlaybackBuffering();
                }
                previousReadyState = state;
            }
        }));

        player.addEventListener(PlayerEventTypes.PLAYING, (playEvent -> {
            signalPlaybackStarted();
        }));

        player.addEventListener(PlayerEventTypes.ERROR, (errorEvent -> {
            Log.e(TAG, "Got error: " + errorEvent.getError());
        }));

        player.addEventListener(PlayerEventTypes.TIMEUPDATE, (timeEvent -> {
            synchronized ( SimplePlayerTestActivity.this ) {
                currentPosition = timeEvent.getCurrentTime();
            }
        }));

        // See if we need this
//        player.addEventListener(PlayerEventTypes., (playEvent -> {
//            signalPlaybackBuffering();
//        });

        player.addEventListener(PlayerEventTypes.ENDED, (playEvent -> {
            signalPlaybackEnded();
        }));

    }

    public double getCurrentPosition() {
        synchronized ( this ) {
            return currentPosition;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        onResumedCalled.set(true);
        theoPlayerView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        theoPlayerView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        signalActivityClosed();
        if ( muxStats != null ) {
            muxStats.release();
        }
        theoPlayerView.onDestroy();
    }

    public void startPlayback() {
        Log.d(TAG, "startPlayback(): Starting playback for Ad Tag URI: " + loadedAdTagUri);
        if ( loadedAdTagUri != null ) {
            setupVMAPAd( loadedAdTagUri.toString() );
        } else {
            TypedSource.Builder typedSource = typedSource( urlToPlay );
            testMediaSource = SourceDescription.Builder
                    .sourceDescription(typedSource.build())
                    .build();
            player.setSource(testMediaSource);
            player.setCurrentTime(playbackStartPosition);
        }
        player.setAutoplay(playWhenReady);
    }

    void setupVMAPAd(String adTagUri) {
        TypedSource.Builder typedSource = typedSource( urlToPlay );
        AdDescription ad = THEOplayerAdDescription.Builder.adDescription(adTagUri).build();
        testMediaSource = SourceDescription.Builder
                .sourceDescription(typedSource.build())
                .ads(ad)
                .build();
        player.setSource(testMediaSource);
    }

//    void setupVASTAd(String adTagUri) {
//        TypedSource.Builder typedSource = typedSource( urlToPlay );
//        SourceDescription.Builder sourceDescription = sourceDescription(typedSource.build());
//        sourceDescription.ads(
//                adDescription(adTagUri)
//                        .timeOffset("start")
//                        .build());
//        player.setSource(sourceDescription.build());
//    }

    public void setVideoTitle(String title) {
        videoTitle = title;
    }

    public void setAdTag(String tag) {
        loadedAdTagUri = Uri.parse(tag);
    }

    public void setUrlToPlay(String url) {
        urlToPlay = url;
    }

//    public DefaultTrackSelector getTrackSelector() {
//        return trackSelector;
//    }

    public void setPlaybackStartPosition(long position) {
        playbackStartPosition = position;
    }

    public void setPlayWhenReady(boolean playWhenReady) {
        this.playWhenReady = playWhenReady;
    }

    public void releaseTheoPlayer() {
        theoPlayerView.onDestroy();
        muxStats.release();
    }

    public MuxStatsSDKTHEOPlayer getMuxStats() {
        return muxStats;
    }

    public void initMuxSats() {
        // Mux details
        mockNetwork = new MockNetworkRequest();
        CustomerPlayerData customerPlayerData = new CustomerPlayerData();
        if (BuildConfig.SHOULD_REPORT_INSTRUMENTATION_TEST_EVENTS_TO_SERVER) {
            customerPlayerData.setEnvironmentKey(BuildConfig.INSTRUMENTATION_TEST_ENVIRONMENT_KEY);
        } else {
            customerPlayerData.setEnvironmentKey("YOUR_ENVIRONMENT_KEY");
        }
        CustomerVideoData customerVideoData = new CustomerVideoData();
        customerVideoData.setVideoTitle(videoTitle);
        CustomerData customerData = new CustomerData();
        customerData.setCustomerPlayerData(customerPlayerData);
        customerData.setCustomerVideoData(customerVideoData);
        muxStats = new MuxStatsSDKTHEOPlayer(this,
                theoPlayerView,
                "demo-view-player",
                customerData,
                null,
                mockNetwork);

        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        muxStats.setScreenSize(size.x, size.y);
        muxStats.enableMuxCoreDebug(true, false);
    }

    public SourceDescription getTestMediaSource() {
        return testMediaSource;
    }

    public THEOplayerView getPlayerView() {
        return theoPlayerView;
    }

    public MockNetworkRequest getMockNetwork() {
        return mockNetwork;
    }

    public void waitForPlaybackToFinish() {
        try {
            activityLock.lock();
            playbackEnded.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            activityLock.unlock();
        }
    }

    public void waitForActivityToInitialize() {
        if (!onResumedCalled.get()) {
            try {
                activityLock.lock();
                activityInitialized.await();
                activityLock.unlock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean waitForPlaybackToStart(long timeoutInMs) {
        try {
            activityLock.lock();
            return playbackStarted.await(timeoutInMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        } finally {
            activityLock.unlock();
        }
    }

    public void waitForPlaybackToStartBuffering() {
//        if (playerView.getState() == PlayerState.PLAYING) {
        if (player.getReadyState() == ReadyState.HAVE_ENOUGH_DATA
                && player.isAutoplay()) {
            try {
                activityLock.lock();
                playbackBuffering.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                activityLock.unlock();
            }
        }
    }

    public void waitForActivityToClose() {
        try {
            activityLock.lock();
            activityClosed.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            activityLock.unlock();
        }
    }

    public void signalPlaybackStarted() {
        activityLock.lock();
        playbackStarted.signalAll();
        activityLock.unlock();
    }

    public void signalPlaybackBuffering() {
        activityLock.lock();
        playbackBuffering.signalAll();
        activityLock.unlock();
    }

    public void signalPlaybackEnded() {
        activityLock.lock();
        playbackEnded.signalAll();
        activityLock.unlock();
    }

    public void signalActivityClosed() {
        activityLock.lock();
        activityClosed.signalAll();
        activityLock.unlock();
    }

    private void disableUserActions() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private void enableUserActions() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }
}
