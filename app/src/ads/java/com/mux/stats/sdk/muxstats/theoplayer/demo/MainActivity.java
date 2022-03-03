package com.mux.stats.sdk.muxstats.theoplayer.demo;

import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;

import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.mux.stats.sdk.core.MuxSDKViewOrientation;
import com.mux.stats.sdk.core.model.CustomData;
import com.mux.stats.sdk.core.model.CustomerData;
import com.mux.stats.sdk.core.model.CustomerPlayerData;
import com.mux.stats.sdk.core.model.CustomerVideoData;
import com.mux.stats.sdk.muxstats.theoplayer.MuxStatsSDKTHEOPlayer;
import com.theoplayer.android.api.THEOplayerView;
import com.theoplayer.android.api.event.ads.AdsEventTypes;
import com.theoplayer.android.api.event.player.PlayerEventTypes;
import com.theoplayer.android.api.player.Player;
import com.theoplayer.android.api.source.SourceDescription;
import com.theoplayer.android.api.source.TypedSource;
import com.theoplayer.android.api.source.addescription.AdDescription;
import com.theoplayer.android.api.source.addescription.GoogleImaAdDescription;
import com.theoplayer.android.api.source.addescription.THEOplayerAdDescription;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import static com.theoplayer.android.api.source.SourceDescription.Builder.sourceDescription;
import static com.theoplayer.android.api.source.TypedSource.Builder.typedSource;
import static com.theoplayer.android.api.source.addescription.GoogleImaAdDescription.Builder.googleImaAdDescription;
import static com.theoplayer.android.api.source.addescription.THEOplayerAdDescription.Builder.adDescription;

public class MainActivity extends AppCompatActivity {

    static final String TAG = "MainActivity";

    THEOplayerView theoPlayerView;
    Player theoPlayer;
    Button btnPlayPause;
    TextView txtPlayStatus, txtTimeUpdate;
    ListView adTypeList;
    double currentPlaybackTime = -1;
    ArrayList<AdSample> adSamples = new ArrayList<>();
    MuxStatsSDKTHEOPlayer muxStatsSDKTHEOplayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        theoPlayerView = findViewById(R.id.theoplayer);
        theoPlayer = theoPlayerView.getPlayer();
        adTypeList = findViewById(R.id.ad_type_selection);

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

        configureMuxSdk();
        initAdTypeList();
    }

    private void configureMuxSdk() {
        CustomerPlayerData customerPlayerData = new CustomerPlayerData();
        customerPlayerData.setEnvironmentKey("YOUR_ENVIRONMENT_KEY_HERE");
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
        muxStatsSDKTHEOplayer.enableMuxCoreDebug(true, false);
    }

    void initAdTypeList() {
        JsonReader reader;
        ArrayList<String> adNames = new ArrayList<>();
        try {
            InputStream in = getAssets().open("media.json");
            reader = new JsonReader(new InputStreamReader(in, java.nio.charset.Charset.forName("UTF-8")));
            reader.beginArray();
            while (reader.hasNext()) {
                AdSample adSample = new AdSample();
                reader.beginObject();
                String name = null;
                String adTagUri = null;
                while (reader.hasNext()) {
                    String attributeName = reader.nextName();
                    String attributeValue = reader.nextString();
                    if (attributeName.equalsIgnoreCase("name")) {
                        adSample.setName(attributeValue);
                    }
                    if (attributeName.equalsIgnoreCase("ad_tag_uri")) {
                        adSample.setAdTagUri(attributeValue);
                    }
                    if (attributeName.equalsIgnoreCase("uri")) {
                        adSample.setUri(attributeValue);
                    }
                }
                reader.endObject();
                adSamples.add(adSample);
            }
            reader.close();
            adTypeList.setAdapter(new AdListAdapter(this, adSamples, adTypeList));

            adTypeList.setOnItemClickListener((parent, view, position, id) -> {
                // Reset player playback
                theoPlayerView.getPlayer().stop();
                AdSample selectedAd = (AdSample) adTypeList.getAdapter().getItem(position);
                setupVASTAd("http://mux-justin-test.s3.amazonaws.com/vast.xml"); // Static test ad
                //if (selectedAd.getName().startsWith("VMAP")) {
                //    setupVMAPAd(selectedAd.getAdTagUri());
                //} else {
                //    setupVASTAd(selectedAd.getAdTagUri());
                //}
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

    void setupVMAPAd(String adTagUri) {
        adTagUri = "https://cdn.theoplayer.com/demos/ads/vmap/vmap.xml";
        Log.d(TAG, "VMAP AD: " + adTagUri);
        TypedSource.Builder typedSource = typedSource(getString(R.string.defaultSourceUrl));
        AdDescription ad = THEOplayerAdDescription.Builder
                .adDescription(adTagUri)
                //.timeOffset("start")
                .build();
        SourceDescription sourceDescription = SourceDescription.Builder
                .sourceDescription(typedSource.build())
                .ads(ad)
                .build();
        theoPlayer.setSource(sourceDescription);
    }

    void setupVASTAd(String adTagUri) {
        adTagUri = "https://cdn.theoplayer.com/demos/ads/vast/vast.xml";
        Log.d(TAG, "VAST AD: " + adTagUri);
        TypedSource.Builder typedSource = typedSource(getString(R.string.defaultSourceUrl));
        SourceDescription.Builder sourceDescription = sourceDescription(typedSource.build());
                    sourceDescription.ads(
                    adDescription(adTagUri)
                            .timeOffset("start")
                            .build());
        theoPlayer.setSource(sourceDescription.build());
    }
}

