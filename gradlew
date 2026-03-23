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
DIST_NAME="$(printf '%s' "$DIST_FILE" | sed 's/-\(bin\|all\)\.zip$//')"
WRAPPER_DIR="$SCRIPT_DIR/.gradle-wrapper"
ZIP_PATH="$WRAPPER_DIR/$DIST_FILE"
INSTALL_DIR="$WRAPPER_DIR/dist"
GRADLE_BIN="$INSTALL_DIR/$DIST_NAME/bin/gradle"

mkdir -p "$WRAPPER_DIR" "$INSTALL_DIR"

if [ ! -x "$GRADLE_BIN" ]; then
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

if [ ! -x "$GRADLE_BIN" ]; then
  echo "Unable to locate Gradle binary after extraction." >&2
  exit 1
fi

exec "$GRADLE_BIN" "$@"
