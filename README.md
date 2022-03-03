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
Add the following line to the `dependencies` block in your app module's `build.gradle`
```groovy
    implementation ''
```

### Theoplayer Version Support
Version `0.1.0` of the Mux THEOPlayer SDK supports all THEOPlayer versions `2.x.x`. Support for other versions is not guaranteed

## Releases

See full integration instructions here: https://docs.mux.com/docs/theoplayer-integration-guide.

#### v0.1.0
Initial Release 

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
