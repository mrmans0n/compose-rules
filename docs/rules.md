## State

### Hoist all the things

Compose is built upon the idea of a [unidirectional data flow](https://developer.android.com/jetpack/compose/state#state-hoisting), which can be summarised as: data/state flows down, and events fire up. To implement that, Compose advocates for the pattern of [hoisting state](https://developer.android.com/jetpack/compose/state#state-hoisting) upwards, enabling the majority of your composable functions to be stateless. This has many benefits, including far easier testing.

In practice, there are a few common things to look out for:

- Do not pass ViewModels (or objects from DI) down.
- Do not pass `State<Foo>` or `MutableState<Bar>` instances down.

Instead pass down the relevant data to the function, and optional lambdas for callbacks.

More information: [State and Jetpack Compose](https://developer.android.com/jetpack/compose/state)

Related rule: [compose:vm-forwarding-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ComposeViewModelForwarding.kt)

### State should be remembered in composables

Be careful when using `mutableStateOf` (or any of the other state builders) to make sure that you `remember` the instance. If you don't `remember` the state instance, a new state instance will be created when the function is recomposed.

Related rule: [compose:remember-missing-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ComposeRememberStateMissing.kt)

### Use Immutable annotation whenever possible

The Compose Compiler tries to infer immutability and stability on value classes, but sometimes it gets it wrong, which then means that your UI will be doing more work than it needs. To force the compiler to see a class as 'immutable' you can apply the `@Immutable` annotation to the class.

More info: [Immutable docs](https://developer.android.com/reference/kotlin/androidx/compose/runtime/Immutable) and [Composable metrics blog post](https://chris.banes.dev/composable-metrics/)

Related rule: TBD

### Avoid using unstable collections

Collections are defined as interfaces (e.g. `List<T>`, `Map<T>`, `Set<T>`) in Kotlin, which can't guarantee that they are actually immutable. For example, you could write:

```kotlin
    val list: List<String> = mutableListOf<String>()
```

The variable is constant, its declared type is not mutable but its implementation is still mutable. The Compose compiler cannot be sure of the immutability of this class as it just sees the declared type and as such declares it as unstable.

To force the compiler to see a collection as truly 'immutable' you have a couple of options.

You can use [Kotlinx Immutable Collections](https://github.com/Kotlin/kotlinx.collections.immutable):

```kotlin
    val list: ImmutableList<String> = persistentListOf<String>()
```

Alternatively, you can wrap your collection in an annotated stable class to mark it as immutable for the Compose compiler.

```kotlin
    @Immutable
    data class StringList(val items: List<String>)
    // ...
    val list: StringList = StringList(yourList)
```
> **Note**: It is preferred to use Kotlinx Immutable Collections for this. As you can see, the wrapped case only includes the immutability promise with the annotation, but the underlying List is still mutable.

More info: [Jetpack Compose Stability Explained](https://medium.com/androiddevelopers/jetpack-compose-stability-explained-79c10db270c8), [Kotlinx Immutable Collections](https://github.com/Kotlin/kotlinx.collections.immutable)

Related rule: [compose:unstable-collections](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ComposeUnstableCollections.kt)

## Composables

### Do not use inherently mutable types as parameters

This practice follows on from the 'Hoist all the things' item above, where we said that state flows down. It might be tempting to pass mutable state down to a function to mutate the value.

This is an anti-pattern though as it breaks the pattern of state flowing down, and events firing up. The mutation of the value is an event which should be modelled within the function API (a lambda callback).

There are a few reasons for this, but the main one is that it is very easy to use a mutable object which does not trigger recomposition. Without triggering recomposition, your composables will not automatically update to reflect the updated value.

Passing `ArrayList<T>`, `MutableState<T>`, `ViewModel` are common examples of this (but not limited to those types).

Related rule: [compose:mutable-params-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ComposeMutableParameters.kt)

### Do not emit content and return a result

Composable functions should either emit layout content, or return a value, but not both.

If a composable should offer additional control surfaces to its caller, those control surfaces or callbacks should be provided as parameters to the composable function by the caller.

More info: [Compose API guidelines](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md#emit-xor-return-a-value)

Related rule: [compose:content-emitter-returning-values-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ComposeMultipleContentEmitters.kt)

> **Note**: To add your custom composables so they are used in this rule (things like your design system composables), you can add `composeEmitters` to this rule config in Detekt, or `compose_emitters` to your .editorconfig in ktlint.

### Do not emit multiple pieces of content

A composable function should emit either 0 or 1 pieces of layout, but no more. A composable function should be cohesive, and not rely on what function it is called from.

You can see an example of what not to do below. `InnerContent()` emits a number of layout nodes and assumes that it will be called from a Column:

```kotlin
Column {
    InnerContent()
}

@Composable
private fun InnerContent() {
    Text(...)
    Image(...)
    Button(...)
}
```

However InnerContent could just as easily be called from a Row which would break all assumptions. Instead, InnerContent should be cohesive and emit a single layout node itself:

```kotlin
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
@Composable
private fun ColumnScope.InnerContent() {
    Text(...)
    Image(...)
    Button(...)
}
```
This effectively ties the function to be called from a Column, but is still not recommended (although permitted).

Related rule: [compose:multiple-emitters-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ComposeMultipleContentEmitters.kt)

> **Note**: To add your custom composables so they are used in this rule (things like your design system composables), you can add `composeEmitters` to this rule config in Detekt, or `compose_emitters` to your .editorconfig in ktlint.

### Naming multipreview annotations properly

Multipreview annotations should be named by using `Previews` as a prefix. These annotations have to be explicitly named to make sure that they are clearly identifiable as a `@Preview` alternative on its usages.

More information: [Multipreview annotations](https://developer.android.com/jetpack/compose/tooling#preview-multipreview) and [Google's own predefined annotations](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui-tooling-preview/src/androidMain/kotlin/androidx/compose/ui/tooling/preview/MultiPreviews.kt?q=MultiPreviews.kt)

Related rule: [compose:preview-annotation-naming](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ComposePreviewAnnotationNaming.kt)

### Naming @Composable functions properly

Composable functions that return `Unit` should start with an uppercase letter. They are considered declarative entities that can be either present or absent in a composition and therefore follow the naming rules for classes.

However, Composable functions that return a value should start with a lowercase letter instead. They should follow the standard [Kotlin Coding Conventions](https://kotlinlang.org/docs/reference/coding-conventions.html#function-names) for the naming of functions for any function annotated `@Composable` that returns a value other than `Unit`

More information: [Naming Unit @Composable functions as entities](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md#naming-unit-composable-functions-as-entities) and [Naming @Composable functions that return values](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md#naming-composable-functions-that-return-values)

Related rule: [compose:naming-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ComposeNaming.kt)

### Ordering @Composable parameters properly

When writing Kotlin, it's a good practice to write the parameters for your methods by putting the mandatory parameters first, followed by the optional ones (aka the ones with default values). By doing so, [we minimize the number times we will need to write the name for arguments explicitly](https://kotlinlang.org/docs/functions.html#default-arguments).

Modifiers occupy the first optional parameter slot to set a consistent expectation for developers that they can always provide a modifier as the final positional parameter to an element call for any given element's common case.

More information: [Kotlin default arguments](https://kotlinlang.org/docs/functions.html#default-arguments), [Modifier docs](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier) and [Elements accept and respect a Modifier parameter](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md#why-8).

Related rule: [compose:param-order-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ComposeParameterOrder.kt)

### Movable content should be remembered

The methods used to create movable composable content (`movableContentOf` and `movableContentWithReceiverOf`) need to be used inside a `remember` function.

To work as intended, they need to persist through compositions - as if they get detached from the composition, they will be immediately recycled.

Related rule: [compose:remember-content-missing-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ComposeRememberContentMissing.kt)


### Make dependencies explicit

#### ViewModels

When designing our composables, we should always try to be explicit about the dependencies they take in. If you acquire a ViewModel or an instance from DI in the body of the composable, you are making this dependency implicit, which has the downsides of making it hard to test and harder to reuse.

To solve this problem, you should inject these dependencies as default values in the composable function.

Let's see it with an example.

```kotlin
@Composable
private fun MyComposable() {
    val viewModel = viewModel<MyViewModel>()
    // ...
}
```
In this composable, the dependencies are implicit. When testing it you would need to fake the internals of viewModel somehow to be able to acquire your intended ViewModel.

But, if you change it to pass these instances via the composable function parameters, you could provide the instance you want directly in your tests without any extra effort. It would also have the upside of the function being explicit about its external dependencies in its signature.

```kotlin
@Composable
private fun MyComposable(
    viewModel: MyViewModel = viewModel(),
) {
    // ...
}

```

Related rule: [compose:vm-injection-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ComposeViewModelInjection.kt)

#### `CompositionLocal`s

`CompositionLocal` makes a composable's behavior harder to reason about. As they create implicit dependencies, callers of composables that use them need to make sure that a value for every CompositionLocal is satisfied.

Although uncommon, there are [legit usecases](https://developer.android.com/jetpack/compose/compositionlocal#deciding) for them, so this rule provides an allowlist so that you can add your `CompositionLocal` names to it so that they are not flagged by the rule.

Related rule: [compose:compositionlocal-allowlist](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ComposeCompositionLocalAllowlist.kt)

> **Note**: To add your custom `CompositionLocal` to your allowlist, you can add `allowedCompositionLocals` to this rule config in Detekt, or `allowed_composition_locals` to your .editorconfig in ktlint.

### Preview composables should not be public

When a composable function exists solely because it's a `@Preview`, it doesn't need to have public visibility because it won't be used in actual UI. To prevent folks from using it unknowingly, we should restrict its visibility to `private`.

Related rule: [compose:preview-public-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ComposePreviewPublic.kt)

> **Note**: If you are using Detekt, this may conflict with Detekt's [UnusedPrivateMember rule](https://detekt.dev/docs/rules/style/#unusedprivatemember).
Be sure to set Detekt's [ignoreAnnotated configuration](https://detekt.dev/docs/introduction/compose/#unusedprivatemember) to ['Preview'] for compatibility with this rule.

## Modifiers

### When should I expose modifier parameters?

Modifiers are the beating heart of Compose UI. They encapsulate the idea of composition over inheritance, by allowing developers to attach logic and behavior to layouts.

They are especially important for your public components, as they allow callers to customize the component to their wishes.

More info: [Always provide a Modifier parameter](https://chris.banes.dev/posts/always-provide-a-modifier/)

Related rule: [compose:modifier-missing-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ComposeModifierMissing.kt)

### Modifier order matters

The order of modifier functions is very important. Each function makes changes to the Modifierreturned by the previous function, the sequence affects the final result. Let's see an example of this:

```kotlin
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

Related rule: [compose:modifier-clickable-order](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ComposeModifierClickableOrder.kt)

### Modifiers should be used at the top-most layout of the component

Modifiers should be applied once as a first modifier in the chain to the root-most layout in the component implementation.
Since modifiers aim to modify the external behaviors and appearance of the component, they must be applied to the top-most layout and be the first modifiers in the hierarchy. It is allowed to chain other modifiers to the modifier passed as a param if needed.

More info: [Compose Component API Guidelines](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-component-api-guidelines.md#modifier-parameter)

Related rule: [compose:modifier-not-used-at-root](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ComposeModifierNotUsedAtRoot.kt)

### Don't re-use modifiers

Modifiers which are passed in are designed so that they should be used by a single layout node in the composable function. If the provided modifier is used by multiple composables at different levels, unwanted behaviour can happen.

In the following example we've exposed a public modifier parameter, and then passed it to the root Column, but we've also passed it to each of the descendant calls, with some extra modifiers on top:

```kotlin
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
@Composable
private fun InnerContent(modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(Modifier.clickable(), ...)
        Image(Modifier.size(), ...)
        Button(Modifier, ...)
    }
}
```

Related rule: [compose:modifier-reused-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ComposeModifierReused.kt)

### Modifiers should have default parameters

Composables that accept a Modifier as a parameter to be applied to the whole component represented by the composable function should name the parameter modifier and assign the parameter a default value of `Modifier`. It should appear as the first optional parameter in the parameter list; after all required parameters (except for trailing lambda parameters) but before any other parameters with default values. Any default modifiers desired by a composable function should come after the modifier parameter's value in the composable function's implementation, keeping Modifier as the default parameter value.

More info: [Modifier documentation](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier)

Related rule: [compose:modifier-without-default-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ComposeModifierWithoutDefault.kt)

### Naming modifiers properly

Composables that accept a Modifier as a parameter to be applied to the whole component represented by the composable function should name the parameter `modifier`.

In cases where Composables accept modifiers to be applied to a specific subcomponent should name the parameter `xModifier` (e.g. `fooModifier` for a `Foo` subcomponent) and follow the same guidelines above for default values and behavior.

More info: [Modifier documentation](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier)

Related rule: [compose:modifier-naming](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ComposeModifierNaming.kt)

### Avoid Modifier extension factory functions

Using `@Composable` builder functions for modifiers is not recommended, as they cause unnecessary recompositions. To avoid this, you should use [Modifier.Node](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier.Node) instead. It will allow you to accomplish the same things while being very performant, and not using a `@Composable` unnecessarily.

More info: [Modifier.Node](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier.Node), [Compose Modifier.Node and where to find it, by Merab Tato Kutalia](https://proandroiddev.com/compose-modifier-node-and-where-to-find-it-merab-tato-kutalia-66f891c0e8) and [Compose modifiers deep dive, with Leland Richardson](https://www.youtube.com/watch?v=BjGX2RftXsU)

Related rule: [compose:modifier-composable-check](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ComposeModifierComposable.kt)

## ComponentDefaults

### ComponentDefaults object should match the composable visibility

If your composable has an associated `Defaults` object to contain its default values, this object should have the same visibility as the composable itself. This will allow consumers to be able to interact or build upon the original intended defaults, as opposed to having to maintain their own set of defaults by copy-pasting.

More info: [Compose Component API Guidelines](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-component-api-guidelines.md#default-expressions)

Related rule: [compose:defaults-visibility](https://github.com/mrmans0n/compose-rules/blob/main/rules/common/src/main/kotlin/io/nlopez/compose/rules/ComposeDefaultsVisibility.kt)
