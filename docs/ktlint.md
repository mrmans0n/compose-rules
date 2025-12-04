## Using with Kotlinter

If using [kotlinter](https://github.com/jeremymailen/kotlinter-gradle), you can specify the dependency on this set of rules [by using the `buildscript` classpath](https://github.com/jeremymailen/kotlinter-gradle#custom-rules).

```groovy
buildscript {
    dependencies {
        classpath "io.nlopez.compose.rules:ktlint:<version>"
    }
}
```

## Using with ktlint-gradle

> **Note**: You need at least version [11.1.0](https://github.com/JLLeitschuh/ktlint-gradle/releases/tag/v11.1.0) of this plugin.

If using [ktlint-gradle](https://github.com/JLLeitschuh/ktlint-gradle), you can specify the dependency on this set of rules by using the `ktlintRuleset`.

```groovy
dependencies {
    ktlintRuleset "io.nlopez.compose.rules:ktlint:<VERSION>"
}
```

## Using with spotless

See [Spotless Ktlint Integration](https://github.com/diffplug/spotless/tree/main/plugin-gradle#ktlint).

## Using with ktlint CLI or the ktlint IntelliJ plugin

The [releases](https://github.com/mrmans0n/compose-rules/releases) page contains an [uber jar](https://stackoverflow.com/questions/11947037/what-is-an-uber-jar) for each version. Look for files with the `-all.jar` suffix.

To use with [ktlint CLI](https://ktlint.github.io/#getting-started):
```shell
ktlint -R ktlint-compose-<VERSION>-all.jar
```

You can also use this uber jar with the [ktlint IntelliJ plugin](https://plugins.jetbrains.com/plugin/15057-ktlint), provided the rules are compiled against the same ktlint version. Configure the custom ruleset in the plugin's preferences.

## Supported versions matrix

| Version | Ktlint version | Kotlin (Syntax) |
|---------|----------------|-----------------|
| 0.4.28+ | 1.8.0          | 2.2.21          |
| 0.4.26+ | 1.7.1          | 2.2.0           |
| 0.4.25  | 1.7.0          | 2.2.0           |
| 0.4.24  | 1.6.0          | 2.1.21          |
| 0.4.23  | 1.5.0          | 2.1.0           |
| 0.4.18+ | 1.4.1          | 2.0.21          |
| 0.4.17  | 1.4.0          | 2.0.21          |
| 0.4.12+ | 1.3.1          | 2.0.20          |
| 0.4.6+  | 1.3.1          | 2.0.0           |
| 0.4.5   | 1.3.0          | 2.0.0           |
| 0.3.12+ | 1.2.1          | 1.9.23          |
| 0.3.9+  | 1.1.1          | 1.9.22          |

Older version support can be found in the [release notes](https://github.com/mrmans0n/compose-rules/releases).

## Configuring rules

### Providing custom content emitters

Some rules (`compose:content-emitter-returning-values-check`, `compose:modifier-not-used-at-root`, `compose:multiple-emitters-check`) use a predefined list of composables that emit content. You can add your own (e.g., design system composables) in your `.editorconfig` file:

```editorconfig
[*.{kt,kts}]
compose_content_emitters = MyComposable,MyOtherComposable
```

### Providing exceptions to content emitters

Sometimes a composable emits content in a different window (like a dialog or modal) and shouldn't count toward the `compose:multiple-emitters-check` rule. Use the denylist to exclude these:

```editorconfig
[*.{kt,kts}]
compose_content_emitters_denylist = MyModalComposable,MyDialogComposable
```

### Providing custom ViewModel factories

The `vm-injection-check` rule checks common ViewModel factories (`viewModel`, `hiltViewModel`, etc.). Add your own:

```editorconfig
[*.{kt,kts}]
compose_view_model_factories = myViewModel,potatoViewModel
```

### Providing a list of allowed `CompositionLocal`s

For `compositionlocal-allowlist` rule you can define a list of `CompositionLocal`s that are allowed in your codebase.

```editorconfig
[*.{kt,kts}]
compose_allowed_composition_locals = LocalSomething,LocalSomethingElse
```

### Providing a list of allowed Composable Lambda Names

For `parameter-naming` rule you can define a list of parameter names for function types that are allowed in your codebase. For example, usages from the past found in the official Compose code.

```editorconfig
[*.{kt,kts}]
compose_allowed_lambda_parameter_names = onSizeChanged,onGloballyPositioned
```

### Ignore annotated functions with specific annotations for missing Modifier checks

In the `modifier-missing-check` rule, you can define a list of annotations that, if present, will make it so the function is exempt from this rule.

```editorconfig
[*.{kt,kts}]
compose_modifier_missing_ignore_annotated = Potato,Banana
```

### Allowing matching function names

The `naming-check` rule requires composables that return a value to be lowercased. To allow certain patterns, configure a comma-separated list of regexes:

```editorconfig
[*.{kt,kts}]
compose_allowed_composable_function_names = .*Presenter,.*SomethingElse
```

### Allowing custom state holder names

The `vm-forwarding-check` rule treats classes ending in "ViewModel" or "Presenter" as state holders by default. Add custom patterns via regexes:

```editorconfig
[*.{kt,kts}]
compose_allowed_state_holder_names = .*ViewModel,.*Presenter,.*Component,.*SomethingElse
```

### Allowlist for composable names that aren't affected by the ViewModelForwarding rule

The `vm-forwarding-check` catches ViewModels/state holders relayed to other composables. To allow specific composable names:

```editorconfig
[*.{kt,kts}]
compose_allowed_forwarding = .*Content,.*SomethingElse
```

### Allowlist for ViewModel/state holder names that aren't affected by the ViewModelForwarding rule

To allow specific ViewModel/state holder types to be forwarded:

```editorconfig
[*.{kt,kts}]
compose_allowed_forwarding_of_types = .*MyViewModel,PotatoViewModel
```

### Configure the visibility of the composables where to check for missing modifiers

The `modifier-missing-check` rule will, by default, only check public composables for missing modifiers. To also check internal or all composables, configure it in your `.editorconfig` file:

```editorconfig
[*.{kt,kts}]
compose_check_modifiers_for_visibility = only_public
```

Possible values are:

- `only_public`: (default) Will check for missing modifiers only for public composables.
- `public_and_internal`: Will check for missing modifiers in both public and internal composables.
- `all`: Will check for missing modifiers in all composables.

### Configure custom Modifier names

Most modifier-related rules identify modifiers by type (`Modifier` or `GlanceModifier`). If your library has a custom Modifier type, you can add it so the same rules apply:

```editorconfig
[*.{kt,kts}]
compose_custom_modifiers = BananaModifier,PotatoModifier
```

### Configure types to treat as lambdas (e.g. for ParamOrder check)

The `param-order-check` rule identifies trailing lambdas automatically. For typedefs or functional interfaces that should be treated as lambdas, add hints:

```editorconfig
[*.{kt,kts}]
compose_treat_as_lambda = MyLambdaType,MyOtherLambdaType
```

### Configure types to treat as composable lambdas (e.g. for ContentTrailingLambda check)

The `content-trailing-lambda` rule identifies `@Composable` trailing lambdas automatically. For typedefs or functional interfaces that should be treated as composable lambdas:

```editorconfig
[*.{kt,kts}]
compose_treat_as_composable_lambda = MyLambdaComposableType,MyOtherComposableLambdaType
```

### Enabling the Material 2 detector

The `material-two` rule flags Material 2 API usage. Disabled by default; enable it in your `.editorconfig` file:

```editorconfig
[*.{kt,kts}]
compose_disallow_material2 = true
```

You can also allow specific Material 2 APIs during migration. For example, to allow filled icons and `Button`:

```editorconfig
[*.{kt,kts}]
compose_disallow_material2 = true
compose_allowed_from_m2 = icons.filled,Button
```

### Enabling the unstable collections detector

The `unstable-collections` rule flags unstable collection usage (List/Set/Map). Disabled by default:

```editorconfig
[*.{kt,kts}]
compose_disallow_unstable_collections = true
```

### Enabling and configuring the preview naming detector

To enforce a naming strategy for previews, enable the rule and configure the strategy:

```editorconfig
[*.{kt,kts}]
compose_preview_naming_enabled = true
compose_preview_naming_strategy = suffix
```

Possible values of `compose_preview_naming_strategy` are:

- `suffix`: Previews should have `Preview` as suffix.
- `prefix`: Previews should have `Preview` as prefix.
- `anywhere`: Previews should contain `Preview` in their names.

### Disable `standard:function-naming` rule for Composable

Composable function names start with uppercase, which causes ktlint's `standard:function-naming` rule to report violations. Configure it to ignore Composable functions:

```editorconfig
[*.{kt,kts}]
ktlint_function_naming_ignore_when_annotated_with = Composable
```

## Disabling a specific rule

To disable a rule you have to follow the [instructions from the ktlint documentation](https://pinterest.github.io/ktlint/0.49.1/faq/#how-do-i-suppress-errors-for-a-lineblockfile), and use the id of the rule you want to disable with the `compose` tag.

For example, to disable the `naming-check` rule, the tag you'll need to disable is `ktlint:compose:naming-check`.

```kotlin
    @Suppress("ktlint:compose:naming-check")
    fun YourComposableHere() { ... }
```
