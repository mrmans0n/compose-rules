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

> **Warning**: If using [Spotless](https://github.com/diffplug/spotless), there is [no current way of enabling a custom ruleset like ours](https://github.com/diffplug/spotless/issues/1220). You would need to use any of the alternatives listed here (like Kotlinter) to just run these rules.

## Using with ktlint CLI or the ktlint (unofficial) IntelliJ plugin

The [releases](https://github.com/mrmans0n/compose-rules/releases) page contains an [uber jar](https://stackoverflow.com/questions/11947037/what-is-an-uber-jar) for each version release that can be used for these purposes.

To use with [ktlint CLI](https://ktlint.github.io/#getting-started):
```shell
ktlint -R ktlint-compose-<VERSION>-all.jar
```

You can use this same jar in the [ktlint (unofficial) IntelliJ plugin](https://plugins.jetbrains.com/plugin/15057-ktlint-unofficial-) if the rules are compiled against the same ktlint version used for that release. You can configure the custom ruleset in the preferences page of the plugin.

## Configuring rules

### Providing custom content emitters

There are some rules (`compose:content-emitter-returning-values-check` and `compose:multiple-emitters-check`) that use predefined list of known composables that emit content. But you can add your own too! In your `.editorconfig` file, you'll need to add a `content_emitters` property followed by a list of composable names separated by commas. You would typically want the composables that are part of your custom design system to be in this list.

```editorconfig
[*.{kt,kts}]
compose_content_emitters = MyComposable,MyOtherComposable
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

### Allowing matching function names

The `naming-check` rule requires all composables that return a value to be lowercased. If you want to allow certain patterns though, you can configure a comma-separated list of matching regexes in your `.editorconfig` file:

```editorconfig
[*.{kt,kts}]
compose_allowed_composable_function_names = .*Presenter,.*SomethingElse
```

### Configure the visibility of the composables where to check for missing modifiers

The `modifier-missing-check` rule will, by default, only look for missing modifiers for public composables. If you want to lower the visibility threshold to check also internal compoosables, or all composables, you can configure it in your `.editorconfig` file:

```editorconfig
[*.{kt,kts}]
compose_check_modifiers_for_visibility = only_public
```

Possible values are:
* `only_public`: (default) Will check for missing modifiers only for public composables.
* `public_and_internal`: Will check for missing modifiers in both public and internal composables.
* `all`: Will check for missing modifiers in all composables.

## Disabling a specific rule

To disable a rule you have to follow the [instructions from the ktlint documentation](https://github.com/pinterest/ktlint#how-do-i-suppress-an-errors-for-a-lineblockfile), and use the id of the rule you want to disable with the `compose` tag.

For example, to disable the `naming-check` rule, the tag you'll need to disable is `compose:naming-check`.

```kotlin
    /* ktlint-disable compose:naming-check */
    ... your code here
    /* ktlint-enable compose:naming-check */
```
