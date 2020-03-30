package com.mux.stats.sdk.muxstats.theoplayer.demo;

import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;

import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

//import androidx.appcompat.app.AppCompatActivity;

import androidx.appcompat.app.AppCompatActivity;

import com.google.ads.interactivemedia.v3.api.AdsLoader;
import com.mux.stats.sdk.core.MuxSDKViewOrientation;
import com.mux.stats.sdk.core.model.CustomerPlayerData;
import com.mux.stats.sdk.core.model.CustomerVideoData;
import com.mux.stats.sdk.muxstats.theoplayer.AdsImaSDKListener;
import com.mux.stats.sdk.muxstats.theoplayer.MuxStatsSDKTHEOplayer;
import com.theoplayer.android.api.THEOplayerView;
import com.theoplayer.android.api.event.EventListener;
import com.theoplayer.android.api.event.player.PauseEvent;
import com.theoplayer.android.api.event.player.PlayEvent;
import com.theoplayer.android.api.event.player.PlayerEventTypes;
import com.theoplayer.android.api.event.player.TimeUpdateEvent;
import com.theoplayer.android.api.player.Player;
import com.theoplayer.android.api.player.RequestCallback;
import com.theoplayer.android.api.source.SourceDescription;
import com.theoplayer.android.api.source.SourceType;
import com.theoplayer.android.api.source.TypedSource;
import com.theoplayer.android.api.source.addescription.GoogleImaAdDescription;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.theoplayer.android.api.source.SourceDescription.Builder.sourceDescription;
import static com.theoplayer.android.api.source.TypedSource.Builder.typedSource;
import static com.theoplayer.android.api.source.addescription.GoogleImaAdDescription.Builder.googleImaAdDescription;

public class MainActivity extends AppCompatActivity {

    static final String TAG = "MainActivity";

    THEOplayerView theoPlayerView;
    Player theoPlayer;
    Button btnPlayPause;
    TextView txtPlayStatus, txtTimeUpdate;
    ListView adTypeList;
    double currentPlaybackTime = -1;
    HashMap<String, String> adsUriTagMap = new HashMap<>();

    MuxStatsSDKTHEOplayer muxStatsSDKTHEOplayer;
    private AdsImaSDKListener imaAdsListener;
    ImaAdsLoader imaLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        theoPlayerView = findViewById(R.id.theoplayer);
        theoPlayer = theoPlayerView.getPlayer();
        adTypeList = findViewById(R.id.ad_type_selection);

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

        txtPlayStatus = findViewById(R.id.txt_playstatus);
        txtTimeUpdate = findViewById(R.id.txt_timeupdate);

//        configureMuxSdk();
//        createAdsLoader();
        initAdTypeList();
    }

    private void configureMuxSdk() {
        CustomerPlayerData customerPlayerData = new CustomerPlayerData();
        customerPlayerData.setEnvironmentKey("YOUR ENVIRONMENT KEY HERE");
        CustomerVideoData customerVideoData = new CustomerVideoData();
        customerVideoData.setVideoTitle("Dizzy");
        muxStatsSDKTHEOplayer = new MuxStatsSDKTHEOplayer(this, theoPlayerView, "demo-view-player", customerPlayerData, customerVideoData);

        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        muxStatsSDKTHEOplayer.setScreenSize(size.x, size.y);
    }

    private void configureTHEOplayer() {
        // Coupling the orientation of the device with the fullscreen state.
        // The player will go fullscreen when the device is rotated to landscape
        // and will also exit fullscreen when the device is rotated back to portrait.
        theoPlayerView.getSettings().setFullScreenOrientationCoupled(true);

        // Creating a TypedSource builder that defines the location of a single stream source.
        TypedSource.Builder typedSource = typedSource(getString(R.string.adsSourceUrl));

        // Creating a SourceDescription builder that contains the settings to be applied as a new
        // THEOplayer source.
        SourceDescription.Builder sourceDescription = sourceDescription(typedSource.build());
        // Skip the default poster
//                .poster(getString(R.string.defaultPosterUrl));

        // Configuring THEOplayer with defined SourceDescription object.
        theoPlayer.setSource(sourceDescription.build());

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
                txtPlayStatus.setText("Error: " + event.getError())
        );

        theoPlayer.addEventListener(PlayerEventTypes.TIMEUPDATE, event ->
                txtTimeUpdate.setText(String.valueOf(event.getCurrentTime()))
        );
    }

    void initAdTypeList() {
        JsonReader reader;
        try {
            InputStream in = getAssets().open("media.json");
            reader = new JsonReader(new InputStreamReader(in, java.nio.charset.Charset.forName("UTF-8")));
            reader.beginArray();
            while (reader.hasNext()) {
                reader.beginObject();
                String name = null;
                String adTagUri = null;
                while (reader.hasNext()) {
                    String attributeName = reader.nextName();
                    String attributeValue = reader.nextString();
                    if (attributeName.equalsIgnoreCase("name")) {
                        name = attributeValue;
                    }
                    if (attributeName.equalsIgnoreCase("ad_tag_uri")) {
                        adTagUri = attributeValue;
                    }
                }
                if (name != null && adTagUri != null) {
                    adsUriTagMap.put(name, adTagUri);
                }
                reader.endObject();
            }
            reader.close();
            String[] values = adsUriTagMap.keySet().toArray(new String[0]);
            adTypeList.setAdapter(new ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_1,
                    values));
            adTypeList.setOnItemClickListener((parent, view, position, id) -> {
                // Reset player playback
                theoPlayerView.getPlayer().stop();
                String selectedAdType = (String)adTypeList.getAdapter().getItem(position);
                String adTagUri = adsUriTagMap.get(selectedAdType);
                GoogleImaAdDescription.Builder adDescription = googleImaAdDescription(adTagUri)
                        .timeOffset("0");
                TypedSource.Builder typedSource = typedSource(getString(R.string.adsSourceUrl));
                SourceDescription.Builder sourceDescription =
                        sourceDescription(typedSource.build())
                                .ads(adDescription.build());
                theoPlayer.setSource(sourceDescription.build());
//                imaLoader.setAdTagUri(adTagUri);
            });
            adTypeList.performItemClick(
                    adTypeList.findViewWithTag(
                            adTypeList.getAdapter().
                                    getItem(0)),
                    0,
                    adTypeList.getAdapter().getItemId(0));
            adTypeList.setSelection(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void createAdsLoader() {
//        imaAdsListener = new AdsImaSDKListener();
//        imaLoader = new ImaAdsLoader(this, theoPlayerView);
//        imaLoader.addAdsErrorListener(imaAdsListener);
//        imaLoader.addAdsEventListener(imaAdsListener);
//        muxStatsSDKTHEOplayer.setAdsListener(imaAdsListener);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (muxStatsSDKTHEOplayer == null) {
            return;
        }
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            muxStatsSDKTHEOplayer.orientationChange(MuxSDKViewOrientation.LANDSCAPE);
        }
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            muxStatsSDKTHEOplayer.orientationChange(MuxSDKViewOrientation.PORTRAIT);
        }
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
    protected void onDestroy() {
        super.onDestroy();
        theoPlayerView.onDestroy();
    }
}

