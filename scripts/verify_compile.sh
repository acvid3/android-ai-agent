#!/usr/bin/env sh
set -eu

if [ ! -x "./gradlew" ]; then
  echo "gradlew is missing; add the Gradle wrapper before running compile verification." >&2
  exit 1
fi

./gradlew assembleDebug lint
