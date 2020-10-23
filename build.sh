#!/bin/bash

docker build -t mux-stats-sdk-theoplayer-android-build .
docker run -it --rm -v "$PWD":/data -w /data mux-stats-sdk-theoplayer-android-build
