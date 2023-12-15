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

## Using with ktlint CLI or the ktlint (unofficial) IntelliJ plugin

The [releases](https://github.com/mrmans0n/compose-rules/releases) page contains an [uber jar](https://stackoverflow.com/questions/11947037/what-is-an-uber-jar) for each version release that can be used for these purposes. In the [releases](https://github.com/mrmans0n/compose-rules/releases/) page you can identify them by the suffix `-all.jar`.

To use with [ktlint CLI](https://ktlint.github.io/#getting-started):
```shell
ktlint -R ktlint-compose-<VERSION>-all.jar
```

You can use this same [uber jar from the releases page](https://github.com/mrmans0n/compose-rules/releases/) with the [ktlint (unofficial) IntelliJ plugin](https://plugins.jetbrains.com/plugin/15057-ktlint-unofficial-) if the rules are compiled against the same ktlint version used for that release. You can configure the custom ruleset in the preferences page of the plugin.

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

The `vm-forwarding-check` will catch VMs/state holder classes that are relayed to other composables. However, in some situations this can be a valid use-case. The rule can be configured so that all the names that matches a list of regexes are exempt to this rule. You can configure this in your `.editorconfig` file:

```editorconfig
[*.{kt,kts}]
compose_allowed_forwarding = .*Content,.*SomethingElse
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

### Configure custom Modifier names

Most of the modifier-related rules will look for modifiers based their type: either Modifier or GlanceModifier type. Some libraries might add their own flavor of Modifier to the mix, and it might make sense to enforce the same rules we have for the other default modifiers. To support that, you can configure this in your `.editorconfig` file:

```editorconfig
[*.{kt,kts}]
compose_custom_modifiers = BananaModifier,PotatoModifier
```

## Disabling a specific rule

To disable a rule you have to follow the [instructions from the ktlint documentation](https://github.com/pinterest/ktlint#how-do-i-suppress-an-errors-for-a-lineblockfile), and use the id of the rule you want to disable with the `compose` tag.

For example, to disable the `naming-check` rule, the tag you'll need to disable is `compose:naming-check`.

```kotlin
    /* ktlint-disable compose:naming-check */
    ... your code here
    /* ktlint-enable compose:naming-check */
```
