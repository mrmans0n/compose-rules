#!/bin/zsh
# alas-name: Check (Gradle)
# alas-on-exit: close

set -euo pipefail

./gradlew check
