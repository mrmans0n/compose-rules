#!/usr/bin/env bash
# Copyright 2024 Nacho Lopez
# SPDX-License-Identifier: Apache-2.0
#
# Script to validate that the ktlint CLI correctly detects compose-rules violations
# when using the fat JAR as a custom ruleset.
# Returns 0 if all expected violations are found, non-zero otherwise.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SAMPLE_DIR="$ROOT_DIR/samples/ktlint-sample"
BUILD_DIR="$ROOT_DIR/rules/ktlint/build/libs"
CACHE_DIR="$ROOT_DIR/.gradle/cli-cache"

# Get ktlint version from libs.versions.toml
KTLINT_VERSION=$("$SCRIPT_DIR/get-version-from-toml.sh" ktlint)
KTLINT_CLI_URL="https://github.com/pinterest/ktlint/releases/download/${KTLINT_VERSION}/ktlint"
KTLINT_CLI="$CACHE_DIR/ktlint-${KTLINT_VERSION}"

echo "=== Testing ktlint CLI ==="

# Create cache directory if it doesn't exist
mkdir -p "$CACHE_DIR"

# Download ktlint CLI if not cached
if [[ ! -f "$KTLINT_CLI" ]]; then
    echo "Downloading ktlint CLI ${KTLINT_VERSION}..."
    curl -sL "$KTLINT_CLI_URL" -o "$KTLINT_CLI"
    chmod +x "$KTLINT_CLI"
    echo "Downloaded ktlint CLI to $KTLINT_CLI"
else
    echo "Using cached ktlint CLI ${KTLINT_VERSION}..."
fi

# Build the fat JAR if it doesn't exist
FAT_JAR=$(find "$BUILD_DIR" -name "*-all.jar" 2>/dev/null | head -n 1 || true)
if [[ -z "$FAT_JAR" ]]; then
    echo "Building compose-rules ktlint fat JAR..."
    "$ROOT_DIR/gradlew" :rules:ktlint:shadowJar -PuberJar > /dev/null 2>&1
    FAT_JAR=$(find "$BUILD_DIR" -name "*-all.jar" | head -n 1)
else
    echo "Using existing fat JAR: $FAT_JAR"
fi

echo "Running ktlint CLI on sample project..."

# Run ktlint CLI and capture output (expecting it to fail with violations)
# ktlint uses -R or --ruleset for custom rulesets
set +e
OUTPUT=$("$KTLINT_CLI" \
    --ruleset="$FAT_JAR" \
    --editorconfig="$SAMPLE_DIR/.editorconfig" \
    "$SAMPLE_DIR/src/**/*.kt" \
    2>&1)
EXIT_CODE=$?
set -e

# Expected compose-rules violations (rule IDs that should appear in output)
EXPECTED_RULES=(
    "compose:modifier-missing-check"
    "compose:multiple-emitters-check"
    "compose:mutable-params-check"
    "compose:naming-check"
    "compose:parameter-naming"
    "compose:mutable-state-autoboxing"
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
    echo "ERROR: The ktlint CLI passed unexpectedly!"
    echo "The sample project should fail with compose-rules violations."
    exit 1
fi

echo ""
echo "SUCCESS: All expected compose-rules violations were detected via CLI!"
exit 0
