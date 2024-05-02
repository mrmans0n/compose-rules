When using the [detekt Gradle Plugin](https://detekt.dev/docs/gettingstarted/gradle), you can specify the dependency on this set of rules by using `detektPlugins`.

```groovy
dependencies {
    detektPlugins "io.nlopez.compose.rules:detekt:<VERSION>"
}
```

### Using with detekt CLI

The [releases](https://github.com/mrmans0n/compose-rules/releases) page contains an [uber jar](https://stackoverflow.com/questions/11947037/what-is-an-uber-jar) for each version release that can be used to run with the [CLI version of detekt](https://detekt.dev/docs/gettingstarted/cli).

```shell
detekt -p detekt-compose-<VERSION>-all.jar -c your/config/detekt.yml
```

### Enabling rules

For the rules to be picked up, you will need to enable them in your `detekt.yml` configuration file.

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
    # -- You can optionally have a list of types to be treated as composable lambdas (e.g. typedefs or fun interfaces not picked up automatically)
    # treatAsComposableLambda: MyComposableLambdaType
  DefaultsVisibility:
    active: true
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
  ModifierComposable:
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
  PreviewAnnotationNaming:
    active: true
  PreviewPublic:
    active: true
  RememberMissing:
    active: true
  RememberContentMissing:
    active: true
  UnstableCollections:
    active: true
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

### Disabling a specific rule

To disable a rule you have to follow the [instructions from the detekt documentation](https://detekt.dev/docs/introduction/suppressing-rules), and use the id of the rule you want to disable.

For example, to disable `ComposableNaming`:

```kotlin
@Suppress("ComposableNaming")
@Composable
fun myNameIsWrong() { }
```
