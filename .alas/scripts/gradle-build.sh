#!/bin/zsh
# alas-name: Build (Gradle)
# alas-on-exit: close

set -euo pipefail

./gradlew build
