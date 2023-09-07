package com.mux.stats.sdk.muxstats.theoplayer.demo;

import android.graphics.Point;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.mux.stats.sdk.core.model.CustomData;
import com.mux.stats.sdk.core.model.CustomerData;
import com.mux.stats.sdk.core.model.CustomerPlayerData;
import com.mux.stats.sdk.core.model.CustomerVideoData;
import com.mux.stats.sdk.muxstats.theoplayer.MuxStatsSDKTHEOPlayer;
import com.theoplayer.android.api.THEOplayerView;
import com.theoplayer.android.api.event.player.PlayerEventTypes;
import com.theoplayer.android.api.player.Player;
import com.theoplayer.android.api.source.SourceDescription;
import com.theoplayer.android.api.source.SourceType;
import com.theoplayer.android.api.source.TypedSource;

public class MainActivity extends AppCompatActivity {

    static final String TAG = "MainActivity";

    THEOplayerView theoPlayerView;
    private Player theoPlayer;
    Button btnPlayPause;
    TextView txtPlayStatus, txtTimeUpdate;
    MuxStatsSDKTHEOPlayer muxStatsSDKTHEOplayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        theoPlayerView = findViewById(R.id.theoplayer);
        theoPlayer = theoPlayerView.getPlayer();

        // Configuring action bar.
//        setSupportActionBar(findViewById(R.id.toolbar));

        configureTHEOplayer();
        btnPlayPause = findViewById(R.id.btn_playpause);
        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (theoPlayerView.getPlayer().isPaused()) {
                    theoPlayerView.getPlayer().play();
                } else {
                    theoPlayerView.getPlayer().pause();
                }
            }
        });
//
        txtPlayStatus = findViewById(R.id.txt_playstatus);
        txtTimeUpdate = findViewById(R.id.txt_timeupdate);

        configureMuxSdk();
    }

    private void configureMuxSdk() {
        CustomerPlayerData customerPlayerData = new CustomerPlayerData();
        customerPlayerData.setEnvironmentKey("rhhn9fph0nog346n4tqb6bqda");
        CustomerVideoData customerVideoData = new CustomerVideoData();
        customerVideoData.setVideoTitle("VIDEO_TITLE_HERE");
        CustomData customData = new CustomData();
        customData.setCustomData1("YOUR_CUSTOM_STRING_HERE");
        CustomerData customerData = new CustomerData(customerPlayerData, customerVideoData, null);
        customerData.setCustomData(customData);

        muxStatsSDKTHEOplayer = new MuxStatsSDKTHEOPlayer(this,
            theoPlayerView, "demo-view-player",
            customerData);

        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        muxStatsSDKTHEOplayer.setScreenSize(size.x, size.y);
    }

    private void configureTHEOplayer() {
        // Coupling the orientation of the device with the fullscreen state.
        // The player will go fullscreen when the device is rotated to landscape
        // and will also exit fullscreen when the device is rotated back to portrait.
        theoPlayerView.getSettings().setFullScreenOrientationCoupled(true);

         //Creating a TypedSource builder that defines the location of a single stream source.
//        TypedSource.Builder typedSource =  new TypedSource.Builder(getString(R.string.defaultSourceUrl)).type(SourceType.DASH);
//        TypedSource.Builder typedSource =  new TypedSource.Builder("http://192.168.1.121:8000/playlist.mpd").type(SourceType.DASH);
//        TypedSource.Builder typedSource =  new TypedSource.Builder("http://qthttp.apple.com.edgesuite.net/1010qwoeiuryfg/sl.m3u8").type(SourceType.HLS);
      TypedSource.Builder typedSource =  new TypedSource.Builder("https://test-streams.mux.dev/tos_ismc/main.m3u8").type(SourceType.HLS);

        // Creating a SourceDescription builder that contains the settings to be applied as a new
        // THEOplayer source.
        SourceDescription.Builder sourceDescription = new SourceDescription.Builder(typedSource.build());
        // Skip the default poster
//                .poster(getString(R.string.defaultPosterUrl));

        // Configuring THEOplayer with defined SourceDescription object.
        theoPlayer.setSource(sourceDescription.build());
        theoPlayer.setAutoplay(true);

        theoPlayer.addEventListener(PlayerEventTypes.PLAYING, event ->
                txtPlayStatus.setText("Playing")
        );

        theoPlayer.addEventListener(PlayerEventTypes.PAUSE, event ->
                txtPlayStatus.setText("Paused")
        );

        theoPlayer.addEventListener(PlayerEventTypes.ENDED, event ->
                txtPlayStatus.setText("Ended")
        );

        theoPlayer.addEventListener(PlayerEventTypes.ERROR, event ->
                txtPlayStatus.setText("Error: " + event.getErrorObject().getLocalizedMessage())
        );

        theoPlayer.addEventListener(PlayerEventTypes.TIMEUPDATE, event ->
                txtTimeUpdate.setText(String.valueOf(event.getCurrentTime()))
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        theoPlayerView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        theoPlayerView.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        muxStatsSDKTHEOplayer.release();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        theoPlayerView.onDestroy();
    }
}

