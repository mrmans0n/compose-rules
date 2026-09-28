# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) and other AI assistants working on this repository.

## Project Overview

**compose-rules** is a set of static analysis rules for Jetpack Compose, available as ktlint and detekt plugins. It detects common Compose footguns and enforces best practices.

## Build & Test Commands

```bash
./gradlew build          # Build everything
./gradlew test           # Run all tests
./gradlew spotlessApply  # Format code (Spotless + ktlint)
./gradlew spotlessCheck  # Check formatting (used by CI)
```

## Project Structure

```
rules/
├── common/              # Shared rule implementations
├── common-detekt/       # Shared detekt rule implementations
├── common-ktlint/       # Shared ktlint rule implementations
├── detekt/              # Detekt plugin
├── functional-tests/    # Functional tests (Gradle TestKit)
└── ktlint/              # ktlint plugin
samples/                 # Sample projects for testing rules
docs/                    # mkdocs documentation
```

## Code Quality

- All new rules must have corresponding tests
- Run `./gradlew spotlessApply` before committing
- CI runs spotlessCheck, build, and test

## Commit Style

Use Conventional Commits. Keep code, comments, and commit messages in English.
