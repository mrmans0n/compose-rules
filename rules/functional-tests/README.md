# Functional Tests

This module contains functional tests for compose-rules that run actual Gradle builds using Gradle TestKit.

## Overview

These functional tests verify that compose-rules work correctly in real-world scenarios by:

1. **Creating real Gradle projects** with proper build files, dependencies, and source code
2. **Executing actual Gradle builds** (spotless, detekt, etc.)
3. **Verifying rule detection** in actual project contexts

## Test Structure

```
rules/functional-tests/
├── build.gradle.kts                          # Module configuration
├── README.md                                  # This file
└── src/functionalTest/kotlin/
    └── io/nlopez/compose/rules/functional/
        ├── FunctionalTestUtils.kt            # Shared utilities
        ├── KtlintFunctionalTest.kt           # Ktlint integration tests (4 tests)
        └── DetektFunctionalTest.kt           # Detekt integration tests (5 tests)
```

## Test Coverage

### KtlintFunctionalTest (4 tests)

- ✅ Rule detection via spotless/ktlint
- ✅ Multiple rules in separate files
- ✅ Clean code passing checks
- ✅ Gradle integration (up-to-date checks)

### DetektFunctionalTest (5 tests)

- ✅ Rule detection via detekt
- ✅ Multiple violation types
- ✅ Clean code passing checks
- ✅ Gradle integration (up-to-date checks)
- ✅ Custom detekt configuration

**Total: 9 tests**

## What These Tests Focus On

These tests verify **compose-rules behavior**, not Gradle/ktlint/detekt behavior:

✅ **Test:** Do rules correctly detect violations?
✅ **Test:** Can rules be configured?
✅ **Test:** Do rules work with ktlint/detekt plugins?
✅ **Test:** Basic sanity check that Gradle integration works

❌ **Don't test:** Incremental compilation (Gradle's responsibility)
❌ **Don't test:** Build cache implementation (Gradle's responsibility)
❌ **Don't test:** File watching (ktlint/detekt's responsibility)

## Prerequisites

Before running functional tests, you must publish compose-rules artifacts to Maven Local:

```bash
./gradlew publishToMavenLocal
```

This makes the latest version of the rules available for the functional tests to use.

## Running Tests

### Run all functional tests

```bash
./gradlew :rules:functional-tests:functionalTest
```

### Run specific test class

```bash
./gradlew :rules:functional-tests:functionalTest --tests KtlintFunctionalTest
./gradlew :rules:functional-tests:functionalTest --tests DetektFunctionalTest
```

### Run specific test method

```bash
./gradlew :rules:functional-tests:functionalTest --tests "KtlintFunctionalTest.clean code passes ktlint checks"
```

### Run with verbose output

```bash
./gradlew :rules:functional-tests:functionalTest --info
```

## How It Works

Tests use Gradle TestKit to:

1. Create a temporary directory with a complete Gradle project structure
2. Write `build.gradle.kts`, source files, and configuration
3. Run Gradle commands via `GradleRunner`
4. Assert on task outcomes (`SUCCESS`, `FAILED`, `UP_TO_DATE`)
5. Assert on build output content

### Example Test

```kotlin
@Test
fun `ModifierMissing is detected via ktlint`() {
    setupKtlintProject()

    projectDir.writeFile(
        "src/main/kotlin/Violations.kt",
        """
        @Composable
        fun MissingModifier() {
            Row { }  // Missing modifier parameter
        }
        """
    )

    val result = createGradleRunner(
        projectDir = projectDir,
        arguments = listOf("spotlessKotlinCheck")
    ).buildAndFail()

    result.assertOutputContains("compose:modifier-missing-check")
}
```

## Benefits Over Shell Scripts

The existing `scripts/test-detekt-sample.sh` and `scripts/test-ktlint-sample.sh` work well for their purpose. Functional
tests provide complementary benefits:

| Feature | Shell Scripts | Functional Tests |
|---------|--------------|------------------|
| Quick smoke test | ✅ | ⚠️ (slower) |
| IDE Integration | ❌ | ✅ Run/debug in IDE |
| Type Safety | ❌ Bash | ✅ Kotlin |
| Assertions | ⚠️ grep/text | ✅ Structured |
| Parameterization | ⚠️ Limited | ✅ Easy |
| Diagnostics | ⚠️ stdout | ✅ Clear errors |
| Test variations | ❌ | ✅ Easy to add |

## Integration with Existing Tests

The functional tests complement other testing approaches:

- **Unit Tests** (`rules/*/src/test/`) - Test rule logic in isolation with code strings
- **Sample Projects** (`samples/`) - Manual testing and documentation examples
- **Shell Scripts** (`scripts/`) - Quick CI smoke tests
- **Functional Tests** (`rules/functional-tests/`) - End-to-end Gradle integration verification

## CI Integration

Functional tests run as part of the `check` task:

```bash
./gradlew check  # Includes functionalTest
```

## Debugging

When tests fail:

1. TestKit preserves the temporary project directory
2. Full Gradle logs are available in test output
3. You can manually run Gradle commands in the temp directory

The test output shows the project directory location if you need to inspect it:

```bash
cd /tmp/compose-rules-functional-test-xyz123
./gradlew spotlessKotlinCheck --stacktrace
```

## Performance

Functional tests are slower than unit tests because they:

- Execute real Gradle builds
- Compile Kotlin code
- Download dependencies (first run)

However, they provide much higher confidence that rules work correctly in real projects. The test suite uses:

- Gradle TestKit's build caching
- Parallel test execution (JUnit 5)
- Dependency resolution caching

Typical run time: ~1 minute for all 9 tests.

## Future Enhancements

Potential additions:

- **Version matrix testing** - Test against multiple ktlint/detekt/Kotlin versions
- **Multi-module projects** - Test behavior across module boundaries
- **Android projects** - Test with Android Gradle Plugin
- **Performance benchmarks** - Track build time impact of rules

## Development Workflow

1. Make changes to compose-rules
2. Publish to Maven Local: `./gradlew publishToMavenLocal`
3. Run functional tests: `./gradlew :rules:functional-tests:functionalTest`
4. View test report if needed: `open rules/functional-tests/build/reports/tests/functionalTest/index.html`

## Troubleshooting

**Tests fail with "version not found"**

- Run `./gradlew publishToMavenLocal` first

**Tests are slow**

- First run downloads dependencies, subsequent runs are cached
- Run individual test classes instead of all tests

**Gradle daemon issues**

- TestKit spawns separate Gradle daemons
- These are cleaned up automatically
- Use `./gradlew --stop` if needed

## References

- [Gradle TestKit Documentation](https://docs.gradle.org/current/userguide/test_kit.html)
- [JUnit 5 @TempDir](https://junit.org/junit5/docs/current/user-guide/#writing-tests-built-in-extensions-TempDirectory)
