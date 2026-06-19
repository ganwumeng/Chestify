#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
GRADLE_HOME=""

if [ -x "/opt/homebrew/opt/openjdk/bin/java" ]; then
  export JAVA_HOME="/opt/homebrew/opt/openjdk"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

for candidate in \
  "$HOME/.gradle/wrapper/dists/gradle-9.5.1-bin/iq79hdu3mqx29lgffhp8bfmx/gradle-9.5.1" \
  "$HOME/.gradle/wrapper/dists/gradle-9.4.0-bin/lcvyxq3t37f6mx9miaydrrgs/gradle-9.4.0" \
  "$HOME/.gradle/wrapper/dists/gradle-9.0-bin/3czi7dcr1pf879blyy9qczdy7/gradle-9.0.0"; do
  if [ -x "$candidate/bin/gradle" ]; then
    GRADLE_HOME="$candidate"
    break
  fi
done

if [ -z "$GRADLE_HOME" ]; then
  echo "No cached Gradle 9.x distribution found. Install Gradle or generate a standard wrapper, then run ./gradlew build." >&2
  exit 1
fi

exec "$GRADLE_HOME/bin/gradle" -p "$ROOT_DIR" "$@"
