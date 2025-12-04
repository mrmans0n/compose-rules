Compose Rules is a set of custom ktlint / detekt rules to ensure that your composables don't fall into common pitfalls that might be easy to miss in code reviews.

## Why

It can be challenging for big teams to adopt Compose, particularly because not everyone starts at the same time or with the same patterns. We created these static checks to ease that pain.

Compose has lots of superpowers but also has a bunch of footguns to be aware of, [as seen in this Twitter thread](https://twitter.com/mrmans0n/status/1507390768796909571).

This is where static checks come in. We want to detect as many potential issues as possible, as early as possible—ideally before code review. Similar to other static analysis libraries, we hope this fosters a "don't shoot the messenger" philosophy and healthy Compose adoption.

## Using with ktlint

You can refer to the [Using with ktlint](https://mrmans0n.github.io/compose-rules/ktlint) documentation.

## Using with detekt

You can refer to the [Using with detekt](https://mrmans0n.github.io/compose-rules/detekt) documentation.

## Migrating from Twitter Compose Rules

Migrating from the Twitter Compose Rules is straightforward:

1. Update the project coordinates in your Gradle build scripts:
      - For detekt: `com.twitter.compose.rules:detekt:$version` → `io.nlopez.compose.rules:detekt:$version`
      - For ktlint: `com.twitter.compose.rules:ktlint:$version` → `io.nlopez.compose.rules:ktlint:$version`
2. Update `$version` to the latest: ![Latest version](https://img.shields.io/maven-central/v/io.nlopez.compose.rules/ktlint) — see the [releases page](https://github.com/mrmans0n/compose-rules/releases).
3. **If using detekt**: Update your config file (e.g., `detekt.yml`) to rename the rule set from `TwitterCompose` to `Compose`. Since this repo has many new rules that weren't in Twitter's, consider copying from the [example configuration](https://mrmans0n.github.io/compose-rules/detekt).
4. Done!
