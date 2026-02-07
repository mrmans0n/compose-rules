Add the dependency using `detektPlugins` in your [detekt Gradle Plugin](https://detekt.dev/docs/gettingstarted/gradle) configuration:

```groovy
dependencies {
    detektPlugins "io.nlopez.compose.rules:detekt:<VERSION>"
}
```

## Supported versions matrix

| Version | Detekt version | Kotlin (Syntax) |
|---------|----------------|-----------------|
| 0.5.6+  | 2.0.0-alpha.2  | 2.3.0           |
| 0.5.0+  | 2.0.0-alpha.1  | 2.2.21          |
| 0.4.23  | 1.23.8         | 2.0.21          |
| 0.4.16+ | 1.23.7         | 2.0.21          |
| 0.4.12+ | 1.23.7         | 2.0.20          |
| 0.4.2+  | 1.23.6         | 2.0.0           |
| 0.3.13+ | 1.23.6         | 1.9.23          |

Older version support can be found in the [release notes](https://github.com/mrmans0n/compose-rules/releases).

## Using with detekt CLI / detekt IDE plugin

The [releases](https://github.com/mrmans0n/compose-rules/releases) page contains an [uber jar](https://stackoverflow.com/questions/11947037/what-is-an-uber-jar) for each version that can be used with the [detekt CLI](https://detekt.dev/docs/gettingstarted/cli):

```shell
detekt -p detekt-compose-<VERSION>-all.jar -c your/config/detekt.yml
```

For the IDE plugin, add the uber jar to the custom plugins list and enable the rules in the yml config.

## Enabling rules

Enable the rules in your `detekt.yml` configuration file:

```yaml
Compose:
  ComposableAnnotationNaming:
    active: true
  ComposableNaming:
    active: true
    # -- You can optionally disable the checks in this rule for regex matches against the composable name (e.g. molecule presenters)
    # allowedComposableFunctionNames: .*Presenter,.*MoleculePresenter
  ComposableParamOrder:
    active: true
    # -- You can optionally have a list of types to be treated as lambdas (e.g. typedefs or fun interfaces not picked up automatically)
    # treatAsLambda: MyLambdaType
    # -- You can optionally have a list of types to be treated as composable lambdas (e.g. typedefs or fun interfaces not picked up automatically).
    # -- The difference with treatAsLambda is that those need `@Composable` MyLambdaType in the definition, while these won't.
    # treatAsComposableLambda: MyComposableLambdaType
  CompositionLocalAllowlist:
    active: true
    # -- You can optionally define a list of CompositionLocals that are allowed here
    # allowedCompositionLocals: LocalSomething,LocalSomethingElse
  CompositionLocalNaming:
    active: true
  ContentEmitterReturningValues:
    active: true
    # -- You can optionally add your own composables here
    # contentEmitters: MyComposable,MyOtherComposable
  ContentTrailingLambda:
    active: true
    # -- You can optionally have a list of types to be treated as lambdas (e.g. typedefs or fun interfaces not picked up automatically)
    # treatAsLambda: MyLambdaType
    # -- You can optionally have a list of types to be treated as composable lambdas (e.g. typedefs or fun interfaces not picked up automatically).
    # -- The difference with treatAsLambda is that those need `@Composable` MyLambdaType in the definition, while these won't.
    # treatAsComposableLambda: MyComposableLambdaType
  ContentSlotReused:
    active: true
    # -- You can optionally have a list of types to be treated as composable lambdas (e.g. typedefs or fun interfaces not picked up automatically).
    # -- The difference with treatAsLambda is that those need `@Composable` MyLambdaType in the definition, while these won't.
    # treatAsComposableLambda: MyComposableLambdaType
  DefaultsVisibility:
    active: true
  LambdaParameterEventTrailing:
    active: true
    # -- You can optionally add your own composables here
    # contentEmitters: MyComposable,MyOtherComposable
    # -- You can add composables here that you don't want to count as content emitters (e.g. custom dialogs or modals)
    # contentEmittersDenylist: MyNonEmitterComposable
  LambdaParameterInRestartableEffect:
    active: true
    # -- You can optionally have a list of types to be treated as lambdas (e.g. typedefs or fun interfaces not picked up automatically)
    # treatAsLambda: MyLambdaType
  Material2:
    active: false # Opt-in, disabled by default. Turn on if you want to disallow Material 2 usages.
    # -- You can optionally allow parts of it, if you are in the middle of a migration.
    # allowedFromM2: icons.Icons,TopAppBar
  ModifierClickableOrder:
    active: true
    # -- You can optionally add your own Modifier types
    # customModifiers: BananaModifier,PotatoModifier
  ModifierComposed:
    active: true
    # -- You can optionally add your own Modifier types
    # customModifiers: BananaModifier,PotatoModifier
  ModifierMissing:
    active: true
    # -- You can optionally control the visibility of which composables to check for here
    # -- Possible values are: `only_public`, `public_and_internal` and `all` (default is `only_public`)
    # checkModifiersForVisibility: only_public
    # -- You can optionally add your own Modifier types
    # customModifiers: BananaModifier,PotatoModifier
    # -- You can suppress this check in functions annotated with these annotations
    # ignoreAnnotated: ['Potato', 'Banana']
  ModifierNaming:
    active: true
    # -- You can optionally add your own Modifier types
    # customModifiers: BananaModifier,PotatoModifier
  ModifierNotUsedAtRoot:
    active: true
    # -- You can optionally add your own composables here
    # contentEmitters: MyComposable,MyOtherComposable
    # -- You can optionally add your own Modifier types
    # customModifiers: BananaModifier,PotatoModifier
  ModifierReused:
    active: true
    # -- You can optionally add your own Modifier types
    # customModifiers: BananaModifier,PotatoModifier
  ModifierWithoutDefault:
    active: true
  MultipleEmitters:
    active: true
    # -- You can optionally add your own composables here that will count as content emitters
    # contentEmitters: MyComposable,MyOtherComposable
    # -- You can add composables here that you don't want to count as content emitters (e.g. custom dialogs or modals)
    # contentEmittersDenylist: MyNonEmitterComposable
  MutableParams:
    active: true
  MutableStateAutoboxing:
    active: true
  MutableStateParam:
    active: true
  ParameterNaming:
    active: true
    # -- You can optionally have a list of types to be treated as lambdas (e.g. typedefs or fun interfaces not picked up automatically)
    # treatAsLambda: MyLambdaType
    # -- You can optionally add your allowed lambda names (e.g., usages from the past found in the official Compose code)
    # allowedLambdaParameterNames: onSizeChanged, onGloballyPositioned
  PreviewAnnotationNaming:
    active: true
  PreviewNaming:
    active: false # Opt-in, disabled by default.
    # -- You can optionally configure the naming strategy for previews.
    # -- Possible values are: `suffix`, `prefix`, `anywhere`. By default, it will be `suffix`.
    # previewNamingStrategy: suffix
  PreviewPublic:
    active: true
  RememberContentMissing:
    active: true
  RememberMissing:
    active: true
  StateParam:
    active: true
  UnstableCollections:
    active: false # Opt-in, disabled by default. Turn on if you want to enforce this (e.g. you have strong skipping disabled)
  ViewModelForwarding:
    active: true
    # -- You can optionally use this rule on things other than types ending in "ViewModel" or "Presenter" (which are the defaults). You can add your own via a regex here:
    # allowedStateHolderNames: .*ViewModel,.*Presenter
    # -- You can optionally add an allowlist for Composable names that won't be affected by this rule
    # allowedForwarding: .*Content,.*FancyStuff
    # -- You can optionally add an allowlist for ViewModel/StateHolder names that won't be affected by this rule
    # allowedForwardingOfTypes: PotatoViewModel,(Apple|Banana)ViewModel,.*FancyViewModel
  ViewModelInjection:
    active: true
    # -- You can optionally add your own ViewModel factories here
    # viewModelFactories: hiltViewModel,potatoViewModel
```


## Disabling a specific rule

Follow the [detekt documentation](https://detekt.dev/docs/introduction/suppressing-rules) to suppress specific rules. For example:

```kotlin
@Suppress("ComposableNaming")
@Composable
fun myNameIsWrong() { }
```

You can also disable rules altogether by changing the `active: true` to `active: false` in your detekt configuration yml file.
