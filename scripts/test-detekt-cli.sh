#!/usr/bin/env bash
# Copyright 2024 Nacho Lopez
# SPDX-License-Identifier: Apache-2.0
#
# Script to validate that the detekt CLI correctly detects compose-rules violations
# when using the fat JAR as a plugin.
# Returns 0 if all expected violations are found, non-zero otherwise.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SAMPLE_DIR="$ROOT_DIR/samples/detekt-sample"
BUILD_DIR="$ROOT_DIR/rules/detekt/build/libs"
CACHE_DIR="$ROOT_DIR/.gradle/cli-cache"

# Get detekt version from libs.versions.toml
DETEKT_VERSION=$("$SCRIPT_DIR/get-version-from-toml.sh" detekt)
DETEKT_CLI_URL="https://github.com/detekt/detekt/releases/download/v${DETEKT_VERSION}/detekt-cli-${DETEKT_VERSION}-all.jar"
DETEKT_CLI_JAR="$CACHE_DIR/detekt-cli-${DETEKT_VERSION}-all.jar"

echo "=== Testing detekt CLI ==="

# Create cache directory if it doesn't exist
mkdir -p "$CACHE_DIR"

# Download detekt CLI if not cached
if [[ ! -f "$DETEKT_CLI_JAR" ]]; then
    echo "Downloading detekt CLI ${DETEKT_VERSION}..."
    curl -sL "$DETEKT_CLI_URL" -o "$DETEKT_CLI_JAR"
    echo "Downloaded detekt CLI to $DETEKT_CLI_JAR"
else
    echo "Using cached detekt CLI ${DETEKT_VERSION}..."
fi

# Build the fat JAR if it doesn't exist
FAT_JAR=$(find "$BUILD_DIR" -name "*-all.jar" 2>/dev/null | head -n 1 || true)
if [[ -z "$FAT_JAR" ]]; then
    echo "Building compose-rules detekt fat JAR..."
    "$ROOT_DIR/gradlew" :rules:detekt:shadowJar -PuberJar > /dev/null 2>&1
    FAT_JAR=$(find "$BUILD_DIR" -name "*-all.jar" | head -n 1)
else
    echo "Using existing fat JAR: $FAT_JAR"
fi

echo "Running detekt CLI on sample project..."

# Run detekt CLI and capture output (expecting it to fail with violations)
set +e
OUTPUT=$(java -jar "$DETEKT_CLI_JAR" \
    --input "$SAMPLE_DIR/src" \
    --config "$SAMPLE_DIR/detekt.yml" \
    --plugins "$FAT_JAR" \
    2>&1)
EXIT_CODE=$?
set -e

# Expected compose-rules violations (rule IDs that should appear in output)
EXPECTED_RULES=(
    "ModifierMissing"
    "RememberMissing"
    "MutableParams"
    "ComposableNaming"
    "ParameterNaming"
    "MultipleEmitters"
    "MutableStateAutoboxing"
)

echo ""
echo "Validating expected rule violations..."

MISSING_RULES=()
FOUND_RULES=()

for rule in "${EXPECTED_RULES[@]}"; do
    if echo "$OUTPUT" | grep -q "$rule"; then
        FOUND_RULES+=("$rule")
        echo "  ✓ Found: $rule"
    else
        MISSING_RULES+=("$rule")
        echo "  ✗ Missing: $rule"
    fi
done

echo ""
echo "=== Summary ==="
echo "Found ${#FOUND_RULES[@]}/${#EXPECTED_RULES[@]} expected rule violations"

if [[ ${#MISSING_RULES[@]} -gt 0 ]]; then
    echo ""
    echo "ERROR: The following expected rules were NOT detected:"
    for rule in "${MISSING_RULES[@]}"; do
        echo "  - $rule"
    done
    echo ""
    echo "This could indicate:"
    echo "  1. A rule is broken or disabled"
    echo "  2. The sample violation file was modified"
    echo "  3. The rule ID changed"
    echo "  4. The fat JAR is not properly built"
    echo ""
    echo "Full output:"
    echo "----------------------------------------"
    echo "$OUTPUT"
    echo "----------------------------------------"
    exit 1
fi

# Also verify the CLI actually failed (not a false positive from a passing run)
if [[ $EXIT_CODE -eq 0 ]]; then
    echo ""
    echo "ERROR: The detekt CLI passed unexpectedly!"
    echo "The sample project should fail with compose-rules violations."
    exit 1
fi

echo ""
echo "SUCCESS: All expected compose-rules violations were detected via CLI!"
exit 0
