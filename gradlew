#!/usr/bin/env sh
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname "$0")" && pwd)"
PROPS_FILE="$SCRIPT_DIR/gradle/wrapper/gradle-wrapper.properties"

if [ ! -f "$PROPS_FILE" ]; then
  echo "Missing $PROPS_FILE" >&2
  exit 1
fi

DIST_URL="$(sed -n 's/^distributionUrl=//p' "$PROPS_FILE" | sed 's#\\:#:#g')"
DIST_FILE="$(basename "$DIST_URL")"
WRAPPER_DIR="$SCRIPT_DIR/.gradle-wrapper"
ZIP_PATH="$WRAPPER_DIR/$DIST_FILE"
INSTALL_DIR="$WRAPPER_DIR/dist"

mkdir -p "$WRAPPER_DIR" "$INSTALL_DIR"

if ! find "$INSTALL_DIR" -maxdepth 2 -path '*/bin/gradle' | grep -q .; then
  if [ ! -f "$ZIP_PATH" ]; then
    if command -v curl >/dev/null 2>&1; then
      curl -fsSL "$DIST_URL" -o "$ZIP_PATH"
    elif command -v wget >/dev/null 2>&1; then
      wget -qO "$ZIP_PATH" "$DIST_URL"
    else
      echo "curl or wget is required to download Gradle." >&2
      exit 1
    fi
  fi
  unzip -oq "$ZIP_PATH" -d "$INSTALL_DIR"
fi

GRADLE_BIN="$(find "$INSTALL_DIR" -maxdepth 2 -path '*/bin/gradle' | head -n 1)"

if [ -z "$GRADLE_BIN" ]; then
  echo "Unable to locate Gradle binary after extraction." >&2
  exit 1
fi

exec "$GRADLE_BIN" "$@"
