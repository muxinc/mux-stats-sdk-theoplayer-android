package com.mux.stats.sdk.muxstats.theoplayer.demo;

public class AdSample {

    String name;
    String uri;
    String adTagUri;

    public AdSample() {

    }

    public AdSample(String name, String uri, String adTagUri) {
        this.name = name;
        this.uri = uri;
        this.adTagUri = adTagUri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getAdTagUri() {
        return adTagUri;
    }

    public void setAdTagUri(String adTagUri) {
        this.adTagUri = adTagUri;
    }
}
