package com.mux.stats.sdk.muxstats.theoplayer;

public class Util {
    public static long secondsToMs(double seconds) {
        return (long)Math.floor(seconds * 1000);
    }
}
