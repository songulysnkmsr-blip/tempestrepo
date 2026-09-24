#!/bin/sh

# Gradle wrapper script for portable execution

DIRNAME=""
case "$0" in
  /*) DIRNAME=$(dirname "$0") ;;
  *)  DIRNAME=$(dirname "$(pwd)/$0") ;;
esac

APP_BASE_NAME=$(basename "$0")
APP_HOME=$(cd "$DIRNAME" && pwd)

# Execute gradle if installed, or download via wrapper
if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
else
    # If gradle is not installed, install wrapper jar or run through gradle wrapper
    WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
    if [ ! -f "$WRAPPER_JAR" ]; then
        echo "Downloading Gradle Wrapper..."
        mkdir -p "$APP_HOME/gradle/wrapper"
        curl -sSL "https://raw.githubusercontent.com/gradle/gradle/v8.7.0/gradle/wrapper/gradle-wrapper.jar" -o "$WRAPPER_JAR" || true
    fi
    if [ -f "$WRAPPER_JAR" ]; then
        exec java -jar "$WRAPPER_JAR" "$@"
    else
        echo "Error: Gradle or Java not found." >&2
        exit 1
    fi
fi
