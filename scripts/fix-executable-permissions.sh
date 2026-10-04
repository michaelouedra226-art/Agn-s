#!/usr/bin/env bash
set -e

# Fix executable permissions on gradlew and scripts
if [ -f "./gradlew" ]; then
  chmod +x ./gradlew
fi

find ./scripts -type f -name "*.sh" -exec chmod +x {} + 2>/dev/null || true

echo "Executable permissions applied successfully."
