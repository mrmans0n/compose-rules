When using the [Detekt Gradle Plugin](https://detekt.dev/docs/gettingstarted/gradle), you can specify the dependency on this set of rules by using `detektPlugins`.

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
  CompositionLocalAllowlist:
    active: true
    # You can optionally define a list of CompositionLocals that are allowed here
    # allowedCompositionLocals: LocalSomething,LocalSomethingElse
  ContentEmitterReturningValues:
    active: true
    # You can optionally add your own composables here
    # contentEmitters: MyComposable,MyOtherComposable
  DefaultsVisibility:
    active: true
  ModifierComposable:
    active: true
  ModifierMissing:
    active: true
    # You can optionally control the visibility of which composables to check for here
    # Possible values are: `only_public`, `public_and_internal` and `all` (default is `only_public`)
    # checkModifiersForVisibility: only_public
  ModifierNaming:
    active: true
  ModifierReused:
    active: true
  ModifierWithoutDefault:
    active: true
  MultipleEmitters:
    active: true
    # You can optionally add your own composables here
    # contentEmitters: MyComposable,MyOtherComposable
  MutableParams:
    active: true
  ComposableNaming:
    active: true
    # You can optionally disable the checks in this rule for regex matches against the composable name (e.g. molecule presenters)
    # allowedComposableFunctionNames: .*Presenter,.*MoleculePresenter
  ComposableParamOrder:
    active: true
  PreviewNaming:
    active: true
  PreviewPublic:
    active: true
  RememberMissing:
    active: true
  UnstableCollections:
    active: true
  ViewModelForwarding:
    active: true
  ViewModelInjection:
    active: true
    # You can optionally add your own ViewModel factories here
    # viewModelFactories: hiltViewModel,potatoViewModel
```

### Disabling a specific rule

To disable a rule you have to follow the [instructions from the Detekt documentation](https://detekt.dev/docs/introduction/suppressing-rules), and use the id of the rule you want to disable.

For example, to disable `ComposableNaming`:

```kotlin
@Suppress("ComposableNaming")
@Composable
fun myNameIsWrong() { }
```
