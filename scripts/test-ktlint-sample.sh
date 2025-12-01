#!/usr/bin/env bash
# Copyright 2024 Nacho Lopez
# SPDX-License-Identifier: Apache-2.0
#
# Script to validate that the ktlint sample project correctly detects compose-rules violations.
# Returns 0 if all expected violations are found, non-zero otherwise.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SAMPLE_DIR="$ROOT_DIR/samples/ktlint-sample"

echo "=== Testing ktlint sample ==="

# Build the main project first if JARs don't exist
if ! ls "$ROOT_DIR"/rules/ktlint/build/libs/*.jar >/dev/null 2>&1; then
    echo "Building main project to ensure dependencies are available..."
    "$ROOT_DIR/gradlew" assemble > /dev/null 2>&1
else
    echo "Using existing JAR files (skipping build)..."
fi

echo "Running spotless check on sample project..."

# Run spotless and capture output (expecting it to fail with violations)
set +e
OUTPUT=$("$ROOT_DIR/gradlew" -p "$SAMPLE_DIR" spotlessKotlinCheck 2>&1)
EXIT_CODE=$?
set -e

# Expected compose-rules violations (rule IDs that should appear in output)
# Note: Spotless only reports one error per file, so we check for the rules
# that should be triggered based on our separate violation files
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
    echo ""
    echo "Full output:"
    echo "----------------------------------------"
    echo "$OUTPUT"
    echo "----------------------------------------"
    exit 1
fi

# Also verify the build actually failed (not a false positive from a passing build)
if [[ $EXIT_CODE -eq 0 ]]; then
    echo ""
    echo "ERROR: The spotless check passed unexpectedly!"
    echo "The sample project should fail with compose-rules violations."
    exit 1
fi

# Verify we got the expected number of lint errors (6 files = 6 errors)
if echo "$OUTPUT" | grep -q "There were 6 lint error"; then
    echo "✓ Verified: 6 lint errors detected (one per violation file)"
else
    echo ""
    echo "WARNING: Expected '6 lint error(s)' but got different count."
    echo "This might indicate a configuration issue."
fi

echo ""
echo "SUCCESS: All expected compose-rules violations were detected!"
exit 0

