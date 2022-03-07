# Mux Stats SDK THEOPlayer

This is the Mux wrapper around THEOPlayer, built on top of Mux's core Java library,
providing Mux Data performance analytics for applications utilizing
[THEOPlayer](https://www.theoplayer.com/sdk/android).

## Setup and Usage 
### Add the Mux THEOPlayer SDK to your build 
#### Using `settings.gradle`
Add the following lines to your `dependencyResolutionManagement {...}` block
```groovy
maven {
  url "https://muxinc.jfrog.io/artifactory/default-maven-release-local"
}
```

#### Using `build.gradle`
Add the following lines to your project's `build.gradle` 
```groovy
allprojects {
    repositories {
        maven {
          url "https://muxinc.jfrog.io/artifactory/default-maven-release-local"
        }
    }
}
```

### Add the SDK as a Dependency in your application
Add one the following lines to the `dependencies` block in your app module's `build.gradle`, depending on which THEOPlayer library you are using. The current version can be found in our [Integration Guide](https://docs.mux.com/docs/theoplayer-integration-guide)

```groovy
// Use this line for the minApi 16 version of Theoplayer
implementation 'com.mux.stats.sdk.muxstats:muxstatssdktheoplayer_minapi16:[CurrentVersion]'
// Use this line for the minApi 21 version of Theoplayer
implementation 'com.mux.stats.sdk.muxstats:muxstatssdktheoplayer_minapi21:[CurrentVersion]'
```

### Monitor THEOPlayer using Mux Data
The Mux Data SDK for THEOPlayer can be used by creating a new instance of `MuxStatsSDKTHEOPlayer` with the desired configuration. The constructor requires a `THEOplayerView`, which Mux will observe for data events.

```java
// Initialize with data about you, your video, and your app
CustomerPlayerData customerPlayerData = new CustomerPlayerData();
customerPlayerData.setEnvironmentKey("YOUR_ENVIRONMENT_KEY_HERE");
CustomerVideoData customerVideoData = new CustomerVideoData();
customerVideoData.setVideoTitle("VIDEO_TITLE_HERE");
CustomData customData = new CustomData();
customData.setCustomData1("YOUR_CUSTOM_STRING_HERE");
CustomerData customerData = new CustomerData(customerPlayerData, customerVideoData, null);
customerData.setCustomData(customData);

// Create a new Mux Stats monitor 
muxStatsSDKTHEOplayer = new MuxStatsSDKTHEOPlayer(this,
        theoPlayerView, "demo-view-player",
        customerData);

// Set the size of the screen
Point size = new Point();
getWindowManager().getDefaultDisplay().getSize(size);
muxStatsSDKTHEOplayer.setScreenSize(size.x, size.y);
```

You must also release the `MuxStatsTHEOPlayer` object when your component's lifecycle is ending. For example:

```java
@Override
public void onDestroy() {
    muxStatsTHEOplayer.release();
    super.onDestroy();
}
```

### Further Reading
See full integration instructions here: https://docs.mux.com/docs/theoplayer-integration-guide.

## Theoplayer Version Support
Version `0.1.0` of the Mux THEOPlayer SDK has been tested against `3.x` and `2.x` versions of the THEOPlayer SDK

## Releases
The current version of the SDK is `v0.1.2`, as of 3/7/2022

All release notes can be found in our [changelog](RELEASENOTES.md)

## Contributing
### Developer Quick Start
- Open this project in Android Studio, and let Gradle run to configure the application.
- Build variants can be selected to support different versions of THEOPlayer. There is a separate `MuxStatsTHEOPlayer` implementation for each variant, though they inherit from a common base class 

### Style
The code in this repo conforms to the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html). Run the reformatter on files before committing.
The code was formatted in Android Studio/IntelliJ using the [Google Java Style for IntelliJ](https://github.com/google/styleguide/blob/gh-pages/intellij-java-google-style.xml). The style can be installed via the Java-style section of the IDE preferences (`Editor -> Code Style - >Java`).

## Known Limitations
- No supported version of THEOPlayer can be specified in the package metatdata. Version-specific library flavors are planned for v1.0.0

## Documentation
See [our docs](https://docs.mux.com/docs/theoplayer-integration-guide) for more information.
