Compose Rules is a set of custom ktlint / detekt rules to ensure that your composables don't fall into common pitfalls that might be easy to miss in code reviews.

## Why

It can be challenging for big teams to adopt Compose, particularly because not everyone starts at the same time or with the same patterns. We created these static checks to ease that pain.

Compose has lots of superpowers but also has a bunch of footguns to be aware of, [as seen in this Twitter thread](https://twitter.com/mrmans0n/status/1507390768796909571).

This is where static checks come in. We want to detect as many potential issues as possible, as early as possible—ideally before code review. Similar to other static analysis libraries, we hope this fosters a "don't shoot the messenger" philosophy and healthy Compose adoption.

## Using with ktlint

You can refer to the [Using with ktlint](https://mrmans0n.github.io/compose-rules/ktlint) documentation.

## Using with detekt

You can refer to the [Using with detekt](https://mrmans0n.github.io/compose-rules/detekt) documentation.