#!/bin/bash
set -e

command -v cmake >/dev/null || { echo "CMake not found."; exit 1; }
command -v make >/dev/null || { echo "Make not found."; exit 1; }

if [ -z "$JAVA_HOME" ]; then
    JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null || true)
    if [ -z "$JAVA_HOME" ]; then
        echo "Error: JAVA_HOME not set and could not be detected."
        exit 1
    fi
fi

if [[ "$JAVA_HOME" == /opt/homebrew/opt/openjdk* ]] && [[ ! -d "$JAVA_HOME/lib" ]]; then
    if [ -d "$JAVA_HOME/libexec/openjdk.jdk/Contents/Home" ]; then
        JAVA_HOME="$JAVA_HOME/libexec/openjdk.jdk/Contents/Home"
        echo "Adjusted JAVA_HOME to: $JAVA_HOME"
    fi
fi

export JAVA_HOME
echo "Using JAVA_HOME: $JAVA_HOME."

mkdir -p build
cd build

cmake ..
make
make install

ls -lh ../../common/src/main/resources/natives/ 2>/dev/null || echo "Files not found."
echo "Next: ./gradlew :fabric:build"
