# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-printmapping mapping.txt

-dontwarn com.google.ads.**

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep public class com.mux.stats.sdk.muxstats.theoplayer.MuxStatsSDKTHEOplayer { public protected *; }
-keep public class com.mux.stats.sdk.muxstats.MuxErrorException { public protected *; }
-keep public class com.mux.stats.sdk.core.model.CustomerPlayerData { public protected *; }
-keep public class com.mux.stats.sdk.core.model.CustomerVideoData { public protected *; }
-keep public class com.mux.stats.sdk.core.model.CustomerViewData { public protected *; }
-keep public class com.mux.stats.sdk.core.MuxSDKViewOrientation { public protected *; }
