## State

### Hoist all the things

Compose is built upon the idea of a [unidirectional data flow](https://developer.android.com/jetpack/compose/state#state-hoisting), which can be summarised as: data/state flows down, and events fire up. To implement that, Compose advocates for the pattern of [hoisting state](https://developer.android.com/jetpack/compose/state#state-hoisting) upwards, enabling the majority of your composable functions to be stateless. This has many benefits, including far easier testing.

In practice, there are a few common things to look out for:

- Do not pass ViewModels (or objects from DI) down.
- Do not pass `MutableState<Bar>` instances down.
- Do not pass inherently mutable types, that can't be observed types, down.

Instead pass down the relevant data to the function, and optional lambdas for callbacks.

More information: [State and Jetpack Compose](https://developer.android.com/jetpack/compose/state)

Related rule: [compose:vm-forwarding-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ViewModelForwarding.kt)

### State should be remembered in composables

Be careful when using `mutableStateOf` (or any of the other state builders) to make sure that you `remember` the instance. If you don't `remember` the state instance, a new state instance will be created when the function is recomposed.

!!! info "Rule"

    | Linter | Rule Name |
    |--------|-----------|
    | ktlint | [compose:remember-missing-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/RememberStateMissing.kt) |
    | detekt | [RememberMissing](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/RememberStateMissing.kt) |

!!! info "Rule"

    |--------|-----------|
    | ktlint | [compose:remember-missing-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/RememberStateMissing.kt) |
    | detekt | [RememberMissing](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/RememberStateMissing.kt) |


### Use Immutable annotation whenever possible

The Compose Compiler tries to infer immutability and stability on value classes, but sometimes it gets it wrong, which then means that your UI will be doing more work than it needs. To force the compiler to see a class as 'immutable' you can apply the `@Immutable` annotation to the class.

More info: [Immutable docs](https://developer.android.com/reference/kotlin/androidx/compose/runtime/Immutable) and [Composable metrics blog post](https://chris.banes.me/composable-metrics/)

Related rule: TBD

### Use mutableStateOf type-specific variants when possible

`mutableIntStateOf`, `mutableLongStateOf`, `mutableDoubleStateOf`, `mutableFloatStateOf` are essentially counterparts to `mutableStateOf`, but with the added advantage of circumventing autoboxing on JVM platforms. This distinction renders them more memory efficient, making them the preferable choice when dealing with primitive types such as double, float, int, and long.

Functionally are the same, but they are preferred when dealing with these specific types.

Related rule: [compose:mutable-state-autoboxing](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/MutableStateAutoboxing.kt)

## Composables

### Do not use inherently mutable types as parameters

This practice follows on from the 'Hoist all the things' item above, where we said that state flows down. It might be tempting to pass mutable state down to a function to mutate the value.

This is an anti-pattern though as it breaks the pattern of state flowing down, and events firing up. The mutation of the value is an event which should be modelled within the function API (a lambda callback).

There are a few reasons for this, but the main one is that it is very easy to use a mutable object which does not trigger recomposition. Without triggering recomposition, your composables will not automatically update to reflect the updated value.

Passing `ArrayList<T>` or `ViewModel` are common examples of this (but not limited to those types).

Related rule: [compose:mutable-params-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/MutableParameters.kt)

### Do not use MutableState as a parameter

This practice also follows on from the 'Hoist all the things' item above. When using `MutableState<T>` in a @Composable function signature as a parameter, this is promoting joint ownership over a state between a component and its user.

Instead, if possible, consider making the component stateless and concede the state change to the caller. If mutation of the parent’s owned property is required in the component, consider creating a ComponentState class with the domain specific meaningful field that is backed by `mutableStateOf(...)`.

When a component accepts MutableState as a parameter, it gains the ability to change it. This results in the split ownership of the state, and the usage side that owns the state now has no control over how and when it will be changed from within the component’s implementation.

More info: [Compose API guidelines](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md#mutablestate_t_as-a-parameter)

Related rule: [compose:mutable-state-param-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/MutableStateParameter.kt)

### Be mindful of the arguments you use inside of a restarting effect

In Compose, effects like `LaunchedEffect`, `produceState`, or `DisposableEffect` can take multiple keys as arguments to control when the effect restarts. The typical form for these APIs is:

```kotlin
EffectName(key1, key2, key3, ...) { block }
```
Using the wrong keys to restart the effect can lead to:

- Bugs if the effect restarts less often than needed.
- Inefficiency if the effect restarts more often than necessary.

To ensure proper behavior:

- Include mutable and immutable variables from the effect block as parameters.
- Additional parameters can be added for explicit restart control.
- Use `rememberUpdatedState` to prevent unnecessary restarts.
  - This is usually useful whenever it wouldn't be a good idea to restart the effect, e.g. it's invoked inside of a flow collector method.
- If a variable never changes due to remember with no keys, no need to pass it as a key to the effect.

Let's see some sample cases.

```kotlin
// ❌ onClick changes, but the effect won't be pointing to the right one!
@Composable
fun MyComposable(onClick: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(10.seconds) // something that takes time, a flow collection, etc
        onClick()
    }
    // ...
}
// ✅ onClick changes and the LaunchedEffect won't be rebuilt -- but will point at the correct onClick!
@Composable
fun MyComposable(onClick: () -> Unit) {
    val latestOnClick by rememberUpdatedState(onClick)
    LaunchedEffect(Unit) {
        delay(10.seconds) // something that takes time, a flow collection, etc
        latestOnClick()
    }
    // ...
}
// ✅ _If we don't care about rebuilding the effect_, we can also use the parameter as key
@Composable
fun MyComposable(onClick: () -> Unit) {
    // This effect will be rebuilt every time onClick changes, so it will always point to the latest one.
    LaunchedEffect(onClick) {
        delay(10.seconds) // something that takes time, a flow collection, etc
        onClick()
    }
}
```

More info: [Restarting effects](https://developer.android.com/jetpack/compose/side-effects#restarting-effects) and [rememberUpdatedState](https://developer.android.com/jetpack/compose/side-effects#rememberupdatedstate)

Related rule: [compose:lambda-param-in-effect](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/LambdaParameterInRestartableEffect.kt)

### Do not emit content and return a result

Composable functions should either emit layout content, or return a value, but not both.

If a composable should offer additional control surfaces to its caller, those control surfaces or callbacks should be provided as parameters to the composable function by the caller.

More info: [Compose API guidelines](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md#emit-xor-return-a-value)

Related rule: [compose:content-emitter-returning-values-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/MultipleContentEmitters.kt)

> **Note**: To add your custom composables so they are used in this rule (things like your design system composables), you can add `composeEmitters` to this rule config in Detekt, or `compose_emitters` to your .editorconfig in ktlint.

### Do not emit multiple pieces of content

A composable function should emit either 0 or 1 pieces of layout, but no more. A composable function should be cohesive, and not rely on what function it is called from.

You can see an example of what not to do below. `InnerContent()` emits a number of layout nodes and assumes that it will be called from a `Column`:

```kotlin
// This will render:
// <text>
// <image>
// <button>
Column {
    InnerContent()
}

// ❌ Unclear UI, as we emit multiple pieces of content at the same time
@Composable
private fun InnerContent() {
    Text(...)
    Image(...)
    Button(...)
}
```

However InnerContent could just as easily be called from a `Row` or a `Box` which would break all assumptions. Some other examples of interaction with `InnerContent` could be:

```kotlin
// ❌ This will render: <text><image><button>
Row {
    InnerContent()
}
// ❌ This will render all elements on top of each other.
Box {
    InnerContent()
}
```

Instead, InnerContent should be cohesive and emit a single layout node itself:

```kotlin
// ✅
@Composable
private fun InnerContent() {
    Column {
        Text(...)
        Image(...)
        Button(...)
    }
}
```
Nesting of layouts has a drastically lower cost vs the view system, so developers should not try to minimize UI layers at the cost of correctness.

There is a slight exception to this rule, which is when the function is defined as an extension function of an appropriate scope, like so:
```kotlin
// ✅
@Composable
private fun ColumnScope.InnerContent() {
    Text(...)
    Image(...)
    Button(...)
}
```
This effectively ties the function to be called from a Column, but is still not recommended (although permitted).

Related rule: [compose:multiple-emitters-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/MultipleContentEmitters.kt)

> **Note**: To add your custom composables so they are used in this rule (things like your design system composables), you can add `composeEmitters` to this rule config in Detekt, or `compose_emitters` to your .editorconfig in ktlint.

### Slots for main content should be the trailing lambda

The slots used to display the main content for a composable, which are typically in the form of `content: @Composable () -> Unit` (or their nullable counterpart) should always be placed as the last parameter of a composable function, so they can be written as the trailing lambda. This makes following the flow of the main pieces of UI / content more natural and easy to reason about.

```kotlin
// ❌
@Composable
fun Avatar(content: @Composable () -> Unit, subtitle: String, modifier: Modifier = Modifier) { ... }

// ✅ The usage of the main content as a trailing lambda is more natural
@Composable
fun Avatar(subtitle: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) { ... }

@Composable
fun Profile(user: User, modifier: Modifier = Modifier) {
    Column(modifier) {
        Avatar(subtitle = user.name) {
            AsyncImage(url = user.avatarUrl)
        }
    }
}
```

Related rule: [compose:content-trailing-lambda](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ContentTrailingLambda.kt)

### Naming CompositionLocals properly

`CompositionLocal`s should be named by using the adjective `Local` as prefix, followed by a descriptive noun that describes the value they hold. This makes it easier to know when a value comes from a `CompositionLocal`. Given that these are implicit dependencies, we should make them obvious.

More information: [Naming CompositionLocals](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-api-guidelines.md#naming-compositionlocals)

Related rule: [compose:compositionlocal-naming](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/composerules/CompositionLocalNaming.kt)

### Naming multipreview annotations properly

Multipreview annotations should be named by using `Previews` as a prefix. These annotations have to be explicitly named to make sure that they are clearly identifiable as a `@Preview` alternative on its usages.

More information: [Multipreview annotations](https://developer.android.com/jetpack/compose/tooling#preview-multipreview) and [Google's own predefined annotations](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui-tooling-preview/src/androidMain/kotlin/androidx/compose/ui/tooling/preview/MultiPreviews.kt?q=MultiPreviews.kt)

Related rule: [compose:preview-annotation-naming](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/PreviewAnnotationNaming.kt)

### Naming @Composable functions properly

Composable functions that return `Unit` should start with an uppercase letter. They are considered declarative entities that can be either present or absent in a composition and therefore follow the naming rules for classes.

However, Composable functions that return a value should start with a lowercase letter instead. They should follow the standard [Kotlin Coding Conventions](https://kotlinlang.org/docs/reference/coding-conventions.html#function-names) for the naming of functions for any function annotated `@Composable` that returns a value other than `Unit`

More information: [Naming Unit @Composable functions as entities](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md#naming-unit-composable-functions-as-entities) and [Naming @Composable functions that return values](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md#naming-composable-functions-that-return-values)

Related rule: [compose:naming-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/Naming.kt)

### Naming Composable annotations properly

Custom Composable annotations (tagged with [`@ComposableTargetMarker`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/ComposableTargetMarker#description())) should have the `Composable` suffix (for example, `@GoogleMapComposable` or `@MosaicComposable`).

Related rule: [compose:composable-annotation-naming-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ComposableAnnotationNaming.kt)

### Ordering @Composable parameters properly

When writing Kotlin, it's a good practice to write the parameters for your methods by putting the mandatory parameters first, followed by the optional ones (aka the ones with default values). By doing so, [we minimize the number times we will need to write the name for arguments explicitly](https://kotlinlang.org/docs/functions.html#default-arguments).

Modifiers occupy the first optional parameter slot to set a consistent expectation for developers that they can always provide a modifier as the final positional parameter to an element call for any given element's common case.

Additionally, if there is a `content` lambda, it should be used as a trailing lambda.

1. Required parameters (no default values)
2. Optional parameters (have default values)
   1. `modifier: Modifier = Modifier`
   2. The rest of optional params
3. [Optionally] A trailing lambda. If there is a `content` slot, it should be it.

```mermaid
  flowchart LR
    A[Required parameters] --> B[Modifier, if any]
    B --> C[Optional parameters]
    C --> D[Optionally a trailing lambda, eg a content slot]
```

An example of the above could be this:

```kotlin
// ✅
@Composable
fun Avatar(
    imageUrl: String,               // Required parameters go first
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,  // Optional parameters, start with modifier
    enabled: Boolean = true,        // Other optional parameters
    loadingContent: @Composable (() -> Unit)? = null,
    errorContent: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit, // A trailing lambda _can_ be last. Recommended for `content` slots.
) { ... }
```

More information: [Kotlin default arguments](https://kotlinlang.org/docs/functions.html#default-arguments), [Modifier docs](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier) and [Elements accept and respect a Modifier parameter](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md#why-8).

Related rule: [compose:param-order-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ParameterOrder.kt)

### Naming parameters properly

The parameters in composable functions that send events are typically named `on` + verb in the present tense, like in the very common examples in Compose foundation code: `onClick` or `onTextChange`.
To try to enforce common standard, and for consistency’s sake, we'll want to adjust the tense of the verbs to present.

```kotlin
// ❌
@Composable
fun Avatar(onShown: () -> Unit, onChanged: () -> Unit) { ... }

// ✅
@Composable
fun Avatar(onShow: () -> Unit, onChange: () -> Unit) { ... }
```

Related rule: [compose:parameter-naming](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ParameterNaming.kt)

### Movable content should be remembered

The methods used to create movable composable content (`movableContentOf` and `movableContentWithReceiverOf`) need to be used inside a `remember` function.

To work as intended, they need to persist through compositions - as if they get detached from the composition, they will be immediately recycled.

Related rule: [compose:remember-content-missing-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/RememberContentMissing.kt)


### Make dependencies explicit

#### ViewModels

When designing our composables, we should always try to be explicit about the dependencies they take in. If you acquire a ViewModel or an instance from DI in the body of the composable, you are making this dependency implicit, which has the downsides of making it hard to test and harder to reuse.

To solve this problem, you should inject these dependencies as default values in the composable function.

Let's see it with an example.

```kotlin
// ❌ The VM dependency is implicit here.
@Composable
private fun MyComposable() {
    val viewModel = viewModel<MyViewModel>()
    // ...
}
```
In this composable, the dependencies are implicit. When testing it you would need to fake the internals of viewModel somehow to be able to acquire your intended ViewModel.

But, if you change it to pass these instances via the composable function parameters, you could provide the instance you want directly in your tests without any extra effort. It would also have the upside of the function being explicit about its external dependencies in its signature.

```kotlin
// ✅ The VM dependency is explicit
@Composable
private fun MyComposable(
    viewModel: MyViewModel = viewModel(),
) {
    // ...
}
```

Related rule: [compose:vm-injection-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ViewModelInjection.kt)

#### `CompositionLocal`s

`CompositionLocal` makes a composable's behavior harder to reason about. As they create implicit dependencies, callers of composables that use them need to make sure that a value for every CompositionLocal is satisfied.

Although uncommon, there are [legit usecases](https://developer.android.com/jetpack/compose/compositionlocal#deciding) for them, so this rule provides an allowlist so that you can add your `CompositionLocal` names to it so that they are not flagged by the rule.

Related rule: [compose:compositionlocal-allowlist](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/CompositionLocalAllowlist.kt)

> **Note**: To add your custom `CompositionLocal` to your allowlist, you can add `allowedCompositionLocals` to this rule config in Detekt, or `compose_allowed_composition_locals` to your .editorconfig in ktlint.

### Preview composables should not be public

When a composable function exists solely because it's a `@Preview`, it doesn't need to have public visibility because it won't be used in actual UI. To prevent folks from using it unknowingly, we should restrict its visibility to `private`.

Related rule: [compose:preview-public-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/PreviewPublic.kt)

> **Note**: If you are using Detekt, this may conflict with Detekt's [UnusedPrivateMember rule](https://detekt.dev/docs/rules/style/#unusedprivatemember).
Be sure to set Detekt's [ignoreAnnotated configuration](https://detekt.dev/docs/introduction/compose/#unusedprivatemember) to ['Preview'] for compatibility with this rule.

## Modifiers

### When should I expose modifier parameters?

Modifiers are the beating heart of Compose UI. They encapsulate the idea of composition over inheritance, by allowing developers to attach logic and behavior to layouts.

They are especially important for your public components, as they allow callers to customize the component to their wishes.

More info: [Always provide a Modifier parameter](https://chrisbanes.me/posts/always-provide-a-modifier/)

Related rule: [compose:modifier-missing-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ModifierMissing.kt)

### Modifier order matters

The order of modifier functions is very important. Each function makes changes to the Modifier returned by the previous function, the sequence affects the final result. Let's see an example of this:

```kotlin
// ❌ The UI will be off, as the pressed state ripple will extend beyond the intended shape
@Composable
fun MyCard(modifier: Modifier = Modifier) {
    Column(
        modifier
            // Tapping on it does a ripple, the ripple is bound incorrectly to the composable
            .clickable { /* TODO */ }
            // Create rounded corners
            .clip(shape = RoundedCornerShape(8.dp))
            // Background with rounded corners
            .background(color = backgroundColor, shape = RoundedCornerShape(8.dp))
    ) {
        // rest of the implementation
    }
}
```
The entire area, including the clipped area and the clipped background, responds to clicks. This means that the ripple will fill it all, even the areas that we wanted to trim from the shape.

We can address this by simply reordering the modifiers.

```kotlin
// ✅ The UI will be now correct, as the pressed state ripple will have the same shape as the element
@Composable
fun MyCard(modifier: Modifier = Modifier) {
    Column(
        modifier
            // Create rounded corners
            .clip(shape = RoundedCornerShape(8.dp))
            // Background with rounded corners
            .background(color = backgroundColor, shape = RoundedCornerShape(8.dp))
            // Tapping on it does a ripple, the ripple is bound correctly now to the composable
            .clickable { /* TODO */ }
    ) {
        // rest of the implementation
    }
}
```

More info: [Modifier documentation](https://developer.android.com/jetpack/compose/modifiers#order-modifier-matters)

Related rule: [compose:modifier-clickable-order](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ModifierClickableOrder.kt)

### Modifiers should be used at the top-most layout of the component

Modifiers should be applied once as a first modifier in the chain to the root-most layout in the component implementation.
Since modifiers aim to modify the external behaviors and appearance of the component, they must be applied to the top-most layout and be the first modifiers in the hierarchy. It is allowed to chain other modifiers to the modifier passed as a param if needed.

More info: [Compose Component API Guidelines](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-component-api-guidelines.md#modifier-parameter)

Related rule: [compose:modifier-not-used-at-root](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ModifierNotUsedAtRoot.kt)

### Don't re-use modifiers

Modifiers which are passed in are designed so that they should be used by a single layout node in the composable function. If the provided modifier is used by multiple composables at different levels, unwanted behaviour can happen.

In the following example we've exposed a public modifier parameter, and then passed it to the root Column, but we've also passed it to each of the descendant calls, with some extra modifiers on top:

```kotlin
// ❌ When changing `modifier` at the call site, it will the whole layout in unintended ways
@Composable
private fun InnerContent(modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(modifier.clickable(), ...)
        Image(modifier.size(), ...)
        Button(modifier, ...)
    }
}
```
This is not recommended. Instead, the provided modifier should only be used on the Column. The descendant calls should use newly built modifiers, by using the empty Modifier object:

```kotlin
// ✅ When changing `modifier` at the call site, it will only affect the external container of the UI
@Composable
private fun InnerContent(modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(Modifier.clickable(), ...)
        Image(Modifier.size(), ...)
        Button(Modifier, ...)
    }
}
```

Related rule: [compose:modifier-reused-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ModifierReused.kt)

### Modifiers should have default parameters

Composables that accept a Modifier as a parameter to be applied to the whole component represented by the composable function should name the parameter modifier and assign the parameter a default value of `Modifier`. It should appear as the first optional parameter in the parameter list; after all required parameters (except for trailing lambda parameters) but before any other parameters with default values. Any default modifiers desired by a composable function should come after the modifier parameter's value in the composable function's implementation, keeping Modifier as the default parameter value.

More info: [Modifier documentation](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier)

Related rule: [compose:modifier-without-default-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ModifierWithoutDefault.kt)

### Naming modifiers properly

Composables that accept a Modifier as a parameter to be applied to the whole component represented by the composable function should name the parameter `modifier`.

In cases where Composables accept modifiers to be applied to a specific subcomponent should name the parameter `xModifier` (e.g. `fooModifier` for a `Foo` subcomponent) and follow the same guidelines above for default values and behavior.

More info: [Modifier documentation](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier)

Related rule: [compose:modifier-naming](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ModifierNaming.kt)

### Avoid Modifier extension factory functions

Using `@Composable` builder functions for modifiers is not recommended, as they cause unnecessary recompositions. To avoid this, you should use [Modifier.Node](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier.Node) instead. It will allow you to accomplish the same things while being very performant.

There is another API for creating custom modifiers, `composed {}`. This API is no longer recommended due to the performance issues it created, and like with the extension factory functions case, Modifier.Node is recommended instead.

More info: [Modifier.Node](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier.Node), [Compose Modifier.Node and where to find it, by Merab Tato Kutalia](https://proandroiddev.com/compose-modifier-node-and-where-to-find-it-merab-tato-kutalia-66f891c0e8), [Compose modifiers deep dive, with Leland Richardson](https://www.youtube.com/watch?v=BjGX2RftXsU) and [Composed modifier docs](https://developer.android.com/reference/kotlin/androidx/compose/ui/package-summary#(androidx.compose.ui.Modifier).composed(kotlin.Function1,kotlin.Function1)).

Related rules: [compose:modifier-composable-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ModifierComposable.kt) and [compose:modifier-composed-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ModifierComposed.kt)

## ComponentDefaults

### ComponentDefaults object should match the composable visibility

If your composable has an associated `Defaults` object to contain its default values, this object should have the same visibility as the composable itself. This will allow consumers to be able to interact or build upon the original intended defaults, as opposed to having to maintain their own set of defaults by copy-pasting.

More info: [Compose Component API Guidelines](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-component-api-guidelines.md#default-expressions)

Related rule: [compose:defaults-visibility](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/DefaultsVisibility.kt)

## Opt-in rules

> **Note**: These rules are disabled by default, you'll need to explicitly enable them individually in detekt/ktlint.

### Don't use Material 2

Material Design 3 is the next evolution of Material Design. It includes updated theming, components, and Material You personalization features like dynamic color. It supersedes Material 2, and using Material 3 usage is recommended instead of Material 2.

Enabling: [ktlint](https://mrmans0n.github.io/compose-rules/ktlint/#enabling-the-material-2-detector), [detekt](https://mrmans0n.github.io/compose-rules/detekt/#enabling-rules)

More info: [Migration to Material 3](https://developer.android.com/develop/ui/compose/designsystems/material2-material3)

Related rule: [compose:material-two](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/Material2.kt)

### Avoid using unstable collections

> **Note**: This rule will become unnecessary from the Compose version where strong skipping is enabled by default.

Collections are defined as interfaces (e.g. `List<T>`, `Map<T>`, `Set<T>`) in Kotlin, which can't guarantee that they are actually immutable. For example, you could write:

```kotlin
// ❌ The compiler won't be able to infer that the list is immutable
val list: List<String> = mutableListOf<String>()
```

The variable is constant, its declared type is not mutable but its implementation is still mutable. The Compose compiler cannot be sure of the immutability of this class as it just sees the declared type and as such declares it as unstable.

To force the compiler to see a collection as truly 'immutable' you have a couple of options.

You can use [Kotlinx Immutable Collections](https://github.com/Kotlin/kotlinx.collections.immutable):

```kotlin
// ✅ The compiler knows that this list is immutable
val list: ImmutableList<String> = persistentListOf<String>()
```

Alternatively, you can wrap your collection in an annotated stable class to mark it as immutable for the Compose compiler.

```kotlin
// ✅ The compiler knows that this class is immutable
@Immutable
data class StringList(val items: List<String>)
// ...
val list: StringList = StringList(yourList)
```
> **Note**: It is preferred to use Kotlinx Immutable Collections for this. As you can see, the wrapped case only includes the immutability promise with the annotation, but the underlying List is still mutable.

More info: [Jetpack Compose Stability Explained](https://medium.com/androiddevelopers/jetpack-compose-stability-explained-79c10db270c8), [Kotlinx Immutable Collections](https://github.com/Kotlin/kotlinx.collections.immutable)

Related rule: [compose:unstable-collections](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/UnstableCollections.kt)
