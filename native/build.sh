#!/bin/bash
set -e

# Determine project directory
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NATIVES_OUTPUT="$PROJECT_DIR/../common/src/main/resources/natives"

# Check if CMake is available
command -v cmake >/dev/null || { echo "CMake not found. Please install CMake."; exit 1; }

OS_TYPE=$(uname -s)

# Build for the current host platform
build_current() {
    # Detect JAVA_HOME if not already set
    if [ -z "$JAVA_HOME" ]; then
        if command -v java >/dev/null 2>&1; then
            JAVA_BIN=$(command -v java)
            if [[ "$OS_TYPE" == MINGW* || "$OS_TYPE" == MSYS* ]]; then
                JAVA_HOME=$(dirname "$(dirname "$(cd "$(dirname "$JAVA_BIN")" && pwd -W)")")
            else
                JAVA_HOME=$(dirname "$(dirname "$(readlink -f "$JAVA_BIN")")")
            fi
        fi
        # macOS-specific fallback
        if [ "$OS_TYPE" = "Darwin" ] && [ -z "$JAVA_HOME" ]; then
            JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null || true)
        fi
        if [ -z "$JAVA_HOME" ]; then
            echo "Error: JAVA_HOME not set and could not be detected."
            exit 1
        fi
    fi

    # Fix path for Homebrew OpenJDK on macOS
    if echo "$JAVA_HOME" | grep -q "opt/homebrew/opt/openjdk" && [ ! -d "$JAVA_HOME/lib" ]; then
        if [ -d "$JAVA_HOME/libexec/openjdk.jdk/Contents/Home" ]; then
            JAVA_HOME="$JAVA_HOME/libexec/openjdk.jdk/Contents/Home"
        fi
    fi

    export JAVA_HOME

    # Configure and build with CMake
    mkdir -p build
    cd build
    cmake .. -DCMAKE_BUILD_TYPE=Release
    cmake --build . --config Release
    cmake --install . --config Release

    # Verify output directory exists
    if [ ! -d "$NATIVES_OUTPUT" ]; then
        echo "Error: Native resources directory not created: $NATIVES_OUTPUT"
        exit 1
    fi
}

# Build Linux natives using Docker
build_linux() {
    docker build -f - -t dreamdisplays-linux-builder <<'EOF'
FROM ubuntu:22.04
RUN apt-get update && apt-get install -y cmake make build-essential openjdk-21-jdk
WORKDIR /src
CMD ["bash", "-c", "cd native && ./build.sh"]
EOF
    docker run --rm -v "$PROJECT_DIR/..:/src" dreamdisplays-linux-builder
}

# Build Windows natives using Docker
build_windows() {
    docker build -f - -t dreamdisplays-windows-builder <<'EOF'
FROM mcr.microsoft.com/windows/servercore:ltsc2022
RUN powershell -Command "choco install -y cmake mingw openjdk21 --no-progress"
WORKDIR C:\src
CMD powershell -Command "cd native; .\build.sh"
EOF
    docker run --rm -v "$PROJECT_DIR/..:C:\src" dreamdisplays-windows-builder
}

# Main execution logic
case "${1:-}" in
    current|"")
        build_current
        ;;
    linux)
        build_linux
        ;;
    windows)
        build_windows
        ;;
    all)
        build_current
        build_linux
        build_windows
        ;;
    *)
        echo "Usage: $0 [current|linux|windows|all]"
        echo "  current  - build for current platform (default)"
        echo "  linux    - build for Linux using Docker"
        echo "  windows  - build for Windows using Docker"
        echo "  all      - build for all platforms"
        exit 1
        ;;
esac
