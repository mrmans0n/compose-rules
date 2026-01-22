#!/usr/bin/env bash
# Copyright 2024 Nacho Lopez
# SPDX-License-Identifier: Apache-2.0
#
# Helper script to extract a version from gradle/libs.versions.toml
# Usage: get-version-from-toml.sh <version-key>
# Example: get-version-from-toml.sh ktlint

set -euo pipefail

if [[ $# -ne 1 ]]; then
    echo "Usage: $0 <version-key>" >&2
    echo "Example: $0 ktlint" >&2
    exit 1
fi

VERSION_KEY="$1"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
TOML_FILE="$ROOT_DIR/gradle/libs.versions.toml"

if [[ ! -f "$TOML_FILE" ]]; then
    echo "Error: $TOML_FILE not found" >&2
    exit 1
fi

# Extract version from [versions] section
# Match lines like: ktlint = "1.8.0"
VERSION=$(grep -E "^${VERSION_KEY}\s*=\s*\"" "$TOML_FILE" | sed -E 's/^[^"]*"([^"]+)".*/\1/' || true)

if [[ -z "$VERSION" ]]; then
    echo "Error: Version key '${VERSION_KEY}' not found in $TOML_FILE" >&2
    exit 1
fi

echo "$VERSION"
