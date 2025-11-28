# Sample Projects

This directory contains sample projects used to verify that the compose-rules work correctly with both detekt and ktlint/spotless in CI.

## Structure

- **detekt-sample/** - A minimal project configured to use detekt with compose-rules
- **ktlint-sample/** - A minimal project configured to use spotless/ktlint with compose-rules

Each sample contains separate files for different intentional violations to ensure each rule type is tested.

## Intentional Violations

Each sample project contains separate violation files:

| File | Rule | Description |
|------|------|-------------|
| `ModifierMissingViolation.kt` | ModifierMissing | Public composable without a Modifier parameter |
| `RememberMissingViolation.kt` | RememberMissing | Using `mutableStateOf` without `remember` |
| `MutableParamsViolation.kt` | MutableParams | Using mutable `ArrayList` as a parameter |
| `NamingViolation.kt` | ComposableNaming | Composable returning a value with uppercase name |
| `ParameterNamingViolation.kt` | ParameterNaming | Lambda parameter with past tense (`onClicked` vs `onClick`) |
| `MultipleEmittersViolation.kt` | MultipleEmitters | Multiple content emitters in one composable |

**Note:** Due to a limitation in Spotless, the ktlint sample only reports one lint error per file. By splitting violations into separate files, we ensure each rule type is validated.

## Testing Locally

The samples use the root project's Gradle wrapper and version catalog.

### Using Validation Scripts (Recommended)

The `scripts/` directory contains validation scripts that run the samples and verify the expected rule violations are detected:

```bash
# Test detekt sample
./scripts/test-detekt-sample.sh

# Test ktlint sample
./scripts/test-ktlint-sample.sh
```

These scripts return exit code 0 only if all expected compose-rules violations are found, making them suitable for CI.

### Manual Testing

#### Detekt Sample

```bash
cd samples/detekt-sample
../../gradlew check
```

This should fail with compose-rules violations detected (plus some standard detekt violations).

#### Ktlint Sample

```bash
cd samples/ktlint-sample
../../gradlew spotlessCheck
```

This should fail with compose-rules violations detected (6 lint errors, one per file).

## CI Integration

The validation scripts in `scripts/` can be used in CI:

```yaml
- name: Test detekt sample
  run: ./scripts/test-detekt-sample.sh

- name: Test ktlint sample
  run: ./scripts/test-ktlint-sample.sh
```

These scripts:
1. Run the appropriate Gradle task on the sample project
2. Parse the output to verify specific rule IDs are present
3. Return exit code 0 only if all expected violations are detected
4. Return non-zero if rules are missing or if the build passes unexpectedly

This approach ensures that CI distinguishes between "rules working correctly" and other failures (compilation errors, configuration issues, etc.).
