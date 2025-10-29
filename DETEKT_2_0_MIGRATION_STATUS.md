# Detekt 2.0 Migration Status

## Summary

This document tracks the migration of compose-rules from Detekt 1.23.8 to 2.0.0-alpha.1.

## Current Status: In Progress (Phase 1 Complete, Phase 2 In Progress)

## Completed Work

### ✅ Phase 1: Preparation

- [x] Created feature branch `feature/detekt-2.0-migration`
- [x] Updated dependency versions in `gradle/libs.versions.toml`:
    - Detekt: `1.23.8` → `2.0.0-alpha.1`
    - kotlin-detekt: `2.0.20` → `2.2.10`
    - Maven coordinates: `io.gitlab.arturbosch.detekt` → `dev.detekt`

### ✅ Phase 2: Package Import Updates (COMPLETE)

- [x] Updated all package imports from `io.gitlab.arturbosch.detekt` to `dev.detekt` in:
    - All main source files (32 rule files)
    - All test files
    - Infrastructure files (DetektRule, DetektComposeKtConfig, ComposeRuleSetProvider)

### ✅ Phase 2: DetektComposeKtConfig Updates (COMPLETE)

- [x] Removed usage of deprecated `valueOrDefaultCommaSeparated` function
- [x] Implemented manual parsing logic for comma-separated config values

### ✅ Phase 2: Issue Declaration Removal (COMPLETE)

- [x] Removed `override val issue: Issue` from all 32 rule files
- [x] Removed unused imports (`Debt`, `Issue`, `Severity`) from all rule files

## Remaining Work

### ⚠️ Phase 2: Critical Infrastructure Updates (IN PROGRESS)

#### DetektRule Base Class

**File:** `rules/detekt/src/main/kotlin/io/nlopez/compose/rules/DetektRule.kt`

**Issues:**

1. `CodeSmell` and `CorrectableCodeSmell` constructors no longer accept `issue` parameter
2. `Entity.from()` and `Location.from()` have incompatible signatures
3. Need to understand new Finding/Issue creation pattern in detekt 2.0

**Required Changes:**

```kotlin
// Current (broken) code:
val finding = when {
    canBeAutoCorrected -> CorrectableCodeSmell(
        issue = issue,  // ← issue property no longer exists
        entity = Entity.from(finalElement, Location.from(finalElement)),
        message = message,
        autoCorrectEnabled = autoCorrect,
    )
    else -> CodeSmell(
        issue = issue,  // ← issue property no longer exists
        entity = Entity.from(finalElement, Location.from(finalElement)),
        message = message,
    )
}

// Need to determine new API pattern - likely:
val finding = when {
    canBeAutoCorrected -> CorrectableCodeSmell(
        entity = Entity.from(finalElement),
        message = message,
        autoCorrectEnabled = autoCorrect,
    )
    else -> CodeSmell(
        entity = Entity.from(finalElement),
        message = message,
    )
}
```

#### ComposeRuleSetProvider

**File:** `rules/detekt/src/main/kotlin/io/nlopez/compose/rules/detekt/ComposeRuleSetProvider.kt`

**Issues:**

1. `RuleSetProvider.ruleSetId` must return `RuleSet.Id` instead of `String`
2. `RuleSetProvider.instance()` now takes no parameters (Config is passed differently)
3. `RuleSet` constructor now expects `Map<RuleName, (Config) -> Rule>` instead of `List<Rule>`

**Current Code:**

```kotlin
class ComposeRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = CUSTOM_RULE_SET_ID  // ← Wrong type

    override fun instance(config: Config): RuleSet = RuleSet(  // ← Wrong signature
        CUSTOM_RULE_SET_ID,  // ← Wrong type
        listOf(  // ← Wrong type, should be Map
            ComposableAnnotationNamingCheck(config),
            // ... 31 more rules
        ),
    )
}
```

**Required Changes:**

```kotlin
class ComposeRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSet.Id = RuleSet.Id(CUSTOM_RULE_SET_ID)

    override fun instance(): RuleSet = RuleSet(
        RuleSet.Id(CUSTOM_RULE_SET_ID),
        mapOf(
            RuleName("ComposableAnnotationNaming") to { config -> ComposableAnnotationNamingCheck(config) },
            RuleName("CompositionLocalAllowlist") to { config -> CompositionLocalAllowlistCheck(config) },
            // ... 30 more rules
        ),
    )
}
```

### Phase 3: Rule Migration

- [ ] Test compilation after infrastructure fixes
- [ ] Run unit tests
- [ ] Fix any test failures related to API changes

### Phase 4: Testing & Validation

- [ ] Verify all 32 rules compile
- [ ] Ensure all unit tests pass
- [ ] Test ktlint integration (should be unaffected)
- [ ] Run on sample project

### Phase 5: Documentation

- [ ] Update docs/detekt.md with new version info
- [ ] Add migration notes if needed
- [ ] Update README.md version badges

## Key API Changes in Detekt 2.0

Based on the detekt 2.0.0-alpha.0 changelog:

1. **Package Names**: `io.gitlab.arturbosch.detekt.*` → `dev.detekt.*` ✅
2. **Maven Coordinates**: `io.gitlab.arturbosch.detekt:detekt-*` → `dev.detekt:detekt-*` ✅
3. **Debt Removed**: The `Debt` enum class has been completely removed ✅
4. **Severity Removed**: The `Severity` enum has been removed (now at RuleInstance level) ✅
5. **Issue Redesigned**: No longer a property of Rule ✅
6. **RuleSet API Changed**: Now uses factory pattern with Map instead of List ⚠️
7. **RuleSetProvider.instance()**: No longer takes Config parameter ⚠️
8. **CodeSmell/CorrectableCodeSmell**: Constructor signatures changed ⚠️
9. **Entity/Location**: API signatures may have changed ⚠️

## Known Issues & Blockers

1. **Alpha API Instability**: Detekt 2.0.0-alpha.1 is still in alpha, so APIs may change before final release
2. **Documentation**: Limited documentation available for detekt 2.0 custom rule authoring
3. **PsiElement Type Mismatch**: There's a conflict between org.jetbrains.kotlin.com.intellij.psi.PsiElement and
   com.intellij.psi.PsiElement

## Next Steps

1. Research the exact new API for CodeSmell/CorrectableCodeSmell in detekt 2.0 source code
2. Fix DetektRule base class
3. Fix ComposeRuleSetProvider
4. Test compilation
5. Fix any remaining issues
6. Run full test suite

## Testing Strategy

- Keep ktlint tests passing throughout (they should be unaffected)
- Test each phase incrementally
- Maintain rollback capability at each step

## References

- [Detekt 2.0.0 Changelog](https://detekt.dev/changelog-2.0.0)
- [Detekt 2.0.0-alpha.0 Release Notes](https://github.com/detekt/detekt/releases/tag/v2.0.0-alpha.0)
- [Detekt 2.0.0-alpha.1 Release](https://github.com/detekt/detekt/releases/tag/v2.0.0-alpha.1)
- [Migration Plan Document](plans/detekt-2.0.0-alpha.1-migration-plan.md)
- [Worker API Issue Thread](https://slack-chats.kotlinlang.org/t/29341088/)

## Notes

- The bulk package update was done via `sed` command for efficiency
- Issue declarations were removed via Python script for accuracy
- Kottlin 2.2.0 + Detekt 2.0 is known to have some false positive issues (can be mitigated by disabling worker API)
- Consider waiting for detekt 2.0.0 stable release if alpha proves too unstable
