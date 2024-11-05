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

The [releases](https://github.com/mrmans0n/compose-rules/releases) page contains an [uber jar](https://stackoverflow.com/questions/11947037/what-is-an-uber-jar) for each version release that can be used for these purposes. In the [releases](https://github.com/mrmans0n/compose-rules/releases/) page you can identify them by the suffix `-all.jar`.

To use with [ktlint CLI](https://ktlint.github.io/#getting-started):
```shell
ktlint -R ktlint-compose-<VERSION>-all.jar
```

You can use this same [uber jar from the releases page](https://github.com/mrmans0n/compose-rules/releases/) with the [ktlint IntelliJ plugin](https://plugins.jetbrains.com/plugin/15057-ktlint) if the rules are compiled against the same ktlint version used for that release. You can configure the custom ruleset in the preferences page of the plugin.

## Supported versions matrix

| Version  | Ktlint version | Kotlin (Syntax) |
|----------|----------------|-----------------|
| 0.4.18+  | 1.4.1          | 2.0.21          |
| 0.4.17   | 1.4.0          | 2.0.21          |
| 0.4.12+  | 1.3.1          | 2.0.20          |
| 0.4.6+   | 1.3.1          | 2.0.0           |
| 0.4.5    | 1.3.0          | 2.0.0           |
| 0.3.12+  | 1.2.1          | 1.9.23          |
| 0.3.9+   | 1.1.1          | 1.9.22          |

Older version support can be found in the [release notes](https://github.com/mrmans0n/compose-rules/releases).

## Configuring rules

### Providing custom content emitters

There are some rules (`compose:content-emitter-returning-values-check`, `compose:modifier-not-used-at-root` and `compose:multiple-emitters-check`) that use predefined list of known composables that emit content. But you can add your own too! In your `.editorconfig` file, you'll need to add a `compose_content_emitters` property followed by a list of composable names separated by commas. You would typically want the composables that are part of your custom design system to be in this list.

```editorconfig
[*.{kt,kts}]
compose_content_emitters = MyComposable,MyOtherComposable
```

### Providing exceptions to content emitters

Sometimes we'll want to not count a Composable towards the multiple content emitters (`compose:multiple-emitters-check`) rule. This is useful, for example, if the composable function actually emits content but that content is painted in a different window (like a dialog or a modal). For those cases, we can use a denylist `compose_content_emitters_denyylist` to add those composable names separated by commas.

```editorconfig
[*.{kt,kts}]
compose_content_emitters_denylist = MyModalComposable,MyDialogComposable
```

### Providing custom ViewModel factories

The `vm-injection-check` rule will check against common ViewModel factories (eg `viewModel` from AAC, `weaverViewModel` from Weaver, `hiltViewModel` from Hilt + Compose, etc), but you can configure your `.editorconfig` file to add your own, as a list of comma-separated strings:

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

### Ignore annotated functions with specific annotations for missing Modifier checks

In the `modifier-missing-check` rule, you can define a list of annotations that, if present, will make it so the function is exempt from this rule.

```editorconfig
[*.{kt,kts}]
compose_modifier_missing_ignore_annotated = Potato,Banana
```

### Allowing matching function names

The `naming-check` rule requires all composables that return a value to be lowercased. If you want to allow certain patterns though, you can configure a comma-separated list of matching regexes in your `.editorconfig` file:

```editorconfig
[*.{kt,kts}]
compose_allowed_composable_function_names = .*Presenter,.*SomethingElse
```

### Allowing custom state holder names

The `vm-forwarding-check` rule will, by default, design as a state holder any class ending on "ViewModel" or "Presenter". You can, however, add new types of names to the mix via a comma-separated list of matching regexes in your `.editorconfig` file:

```editorconfig
[*.{kt,kts}]
compose_allowed_state_holder_names = .*ViewModel,.*Presenter,.*Component,.*SomethingElse
```

### Allowlist for composable names that aren't affected by the ViewModelForwarding rule

The `vm-forwarding-check` will catch VMs/state holder classes that are relayed to other composables. However, in some situations this can be a valid use-case. The rule can be configured so that all the names of composables that match a list of regexes are exempt to this rule. You can configure this in your `.editorconfig` file:

```editorconfig
[*.{kt,kts}]
compose_allowed_forwarding = .*Content,.*SomethingElse
```

### Allowlist for ViewModel/state holder names that aren't affected by the ViewModelForwarding rule

The `vm-forwarding-check` will catch VMs/state holder classes that are relayed to other composables. However, in some situations this can be a valid use-case. The rule can be configured so that all the names of ViewModels that match a list of regexes are exempt to this rule. You can configure this in your `.editorconfig` file:

```editorconfig
[*.{kt,kts}]
compose_allowed_forwarding_of_types = .*MyViewModel,PotatoViewModel
```

### Configure the visibility of the composables where to check for missing modifiers

The `modifier-missing-check` rule will, by default, only look for missing modifiers for public composables. If you want to lower the visibility threshold to check also internal compoosables, or all composables, you can configure it in your `.editorconfig` file:

```editorconfig
[*.{kt,kts}]
compose_check_modifiers_for_visibility = only_public
```

Possible values are:

- `only_public`: (default) Will check for missing modifiers only for public composables.
- `public_and_internal`: Will check for missing modifiers in both public and internal composables.
- `all`: Will check for missing modifiers in all composables.

### Configure custom Modifier names

Most of the modifier-related rules will look for modifiers based their type: either Modifier or GlanceModifier type. Some libraries might add their own flavor of Modifier to the mix, and it might make sense to enforce the same rules we have for the other default modifiers. To support that, you can configure this in your `.editorconfig` file:

```editorconfig
[*.{kt,kts}]
compose_custom_modifiers = BananaModifier,PotatoModifier
```

### Configure types to treat as lambdas (e.g. for ParamOrder check)

The `param-order-check` rule will do its best to identify trailing lambdas. However, in cases where a typedef / functional interface is being used, we might want to have this rule to treat them as if they were lambdas: not reporting them if they are the last in a method signature and they don't have a default value. To give ktlint some hints, you can configure this in your `.editorconfig` file:

```editorconfig
[*.{kt,kts}]
compose_treat_as_lambda = MyLambdaType,MyOtherLambdaType
```

### Configure types to treat as composable lambdas (e.g. for ContentTrailingLambda check)

The `content-trailing-lambda` rule will do its best to identify `@Composable` trailing lambdas. However, in cases where a typedef / functional interface is being used, we might want to have this rule to treat them as if they were composable lambdas: not reporting them if they are the last in a method signature and they don't have a default value. To give ktlint some hints, you can configure this in your `.editorconfig` file:

```editorconfig
[*.{kt,kts}]
compose_treat_as_composable_lambda = MyLambdaComposableType,MyOtherComposableLambdaType
```

### Enabling the Material 2 detector

The `material-two` rule will flag any usage of a Material 2 API. This rule is disabled by default, so you'll need to explicitly enable it in your `.editorconfig` file:

```editorconfig
[*.{kt,kts}]
compose_disallow_material2 = true
```

You might also want to disallow Material 2, but allow a specific set / subset of APIs. The rule allows this too.

For example, let's say you want to allow all filled icons (whose fully qualified names are androidx.compose.material.icons.filled.*) and the `Button` composable (androidx.compose.material.Button). This is how you'd allow those:

```editorconfig
[*.{kt,kts}]
compose_disallow_material2 = true
compose_allowed_from_m2 = icons.filled,Button
```

### Enabling the unstable collections detector

The `unstable-collections` rule will flag any usage of any unstable collection (e.g. List/Set/Map). This rule is disabled by default, so you'll need to explicitly enable it in your `.editorconfig` file:

```editorconfig
[*.{kt,kts}]
compose_disallow_unstable_collections = true
```

### Enabling and configuring the preview naming detector

If you want to enforce a naming strategy for previews, you can enable the `compose:preview-naming` rule and configure the `compose_preview_naming_strategy` property in your `.editorconfig` file:

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

The function name for a Composable starts with an uppercase. This causes the ktlint rule `standard:function-naming` to report a violation. This rule can be configured to ignore Composable functions in your `.editorconfig` file:

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
