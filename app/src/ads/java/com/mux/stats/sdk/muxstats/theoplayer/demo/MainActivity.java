package com.mux.stats.sdk.muxstats.theoplayer.demo;

import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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

public class MainActivity extends AppCompatActivity {

    static final String TAG = "MainActivity";

    THEOplayerView theoPlayerView;
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
        adTypeList = findViewById(R.id.ad_type_selection);
        theoPlayerView.getSettings().setFullScreenOrientationCoupled(true);

//        String preRollAdTagUriString = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/ad_rule_samples&ciu_szs=300x250&ad_rule=1&impl=s&gdfp_req=1&env=vp&output=vmap&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ar%3Dpreonly&cmsid=496&vid=short_onecue&correlator=";

        String preRollAdTagUriString = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dlinear&correlator=";

        TypedSource typedSource = TypedSource.Builder
                .typedSource()
//                .src("https://cdn.theoplayer.com/video/dash/big_buck_bunny/BigBuckBunny_10s_simple_2014_05_09.mpd")
                .src("https://html5demos.com/assets/dizzy.mp4")
                .type(SourceType.MP4)
                .build();

        SourceDescription sourceDescription = SourceDescription.Builder
                .sourceDescription(typedSource)
//                .ads(ad)
                .build();

        theoPlayerView.getPlayer().setSource(sourceDescription);

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

        theoPlayerView.getPlayer().addEventListener(PlayerEventTypes.PLAY, new EventListener<PlayEvent>() {
            @Override
            public void handleEvent(PlayEvent playEvent) {
                txtPlayStatus.setText("Playing");
            }
        });

        theoPlayerView.getPlayer().addEventListener(PlayerEventTypes.PAUSE, new EventListener<PauseEvent>() {
            @Override
            public void handleEvent(PauseEvent pauseEvent) {
                txtPlayStatus.setText("Paused");
            }
        });

        theoPlayerView.getPlayer().addEventListener(PlayerEventTypes.TIMEUPDATE, new EventListener<TimeUpdateEvent>() {
            @Override
            public void handleEvent(TimeUpdateEvent timeUpdateEvent) {
                currentPlaybackTime = timeUpdateEvent.getCurrentTime();
                txtTimeUpdate.setText(String.valueOf(timeUpdateEvent.getCurrentTime()));
            }
        });

        CustomerPlayerData customerPlayerData = new CustomerPlayerData();
        customerPlayerData.setEnvironmentKey("YOUR_ENVIRONMENT_KEY");
        CustomerVideoData customerVideoData = new CustomerVideoData();
        customerVideoData.setVideoTitle("Dizzy");
        muxStatsSDKTHEOplayer = new MuxStatsSDKTHEOplayer(this, theoPlayerView, "demo-view-player", customerPlayerData, customerVideoData);
        muxStatsSDKTHEOplayer.enableMuxCoreDebug(true, false);

        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        muxStatsSDKTHEOplayer.setScreenSize(size.x, size.y);

        createAdsLoader();
        initAdTypeList();
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
                imaLoader.setAdTagUri(adTagUri);
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
        imaAdsListener = new AdsImaSDKListener();
        imaLoader = new ImaAdsLoader(this, theoPlayerView);
        imaLoader.addAdsErrorListener(imaAdsListener);
        imaLoader.addAdsEventListener(imaAdsListener);
        muxStatsSDKTHEOplayer.setAdsListener(imaAdsListener);
//        imaLoader.setAdTagUri("https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/ad_rule_samples&ciu_szs=300x250&ad_rule=1&impl=s&gdfp_req=1&env=vp&output=vmap&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ar%3Dpremidpost&cmsid=496&vid=short_onecue&correlator=" );
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

