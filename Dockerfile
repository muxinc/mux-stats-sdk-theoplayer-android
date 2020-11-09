FROM muxinc/theoplayer-android:20201023

ENV ANDROID_HOME /usr/local/android-sdk-linux
ENV ANDROID_SDK /usr/local/android-sdk-linux
ENV PATH ${ANDROID_HOME}/tools:${ANDROID_HOME}/tools/bin:$ANDROID_HOME/platform-tools:$PATH

# Support Gradle
ENV TERM dumb
ENV JAVA_OPTS -Xms256m -Xmx512m

COPY . /data
WORKDIR /data

# Configure the Android SDK and ack the license agreement
RUN echo "sdk.dir=$ANDROID_HOME" > local.properties

# Pull all our dependencies into the image
RUN ./gradlew --info androidDependencies

# Run build task when this is built
RUN ./gradlew --info clean build

# And figure out how to copy the damn stuff
#CMD
