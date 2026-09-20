#!/bin/zsh
# alas-name: Test (Gradle)
# alas-on-exit: close

set -euo pipefail

./gradlew test
