The Compose Rules is a set of custom ktlint rules to ensure that your composables don't fall into common pitfalls, that might be easy to miss in code reviews.

## Why
It can be challenging for big teams to start adopting Compose, particularly because not everyone will start at same time or with the same patterns. We tried to ease the pain by creating a set of Compose static checks.

Compose has lots of superpowers but also has a bunch of footguns to be aware of [as seen in this Twitter Thread](https://twitter.com/mrmans0n/status/1507390768796909571).

This is where our static checks come in. We want to detect as many potential issues as we can, as quickly as we can. In this case we want an error to show prior to engineers having to review code. Similar to other static check libraries we hope this leads to a "don't shoot the messengers" philosphy which will foster healthy Compose adoption.

## Using with ktlint

You can refer to the [Using with ktlint](https://mrmans0n.github.io/compose-rules/ktlint) documentation.

## Using with Detekt

You can refer to the [Using with Detekt](https://mrmans0n.github.io/compose-rules/detekt) documentation.

## Migrating from Twitter Compose Rules

The process to migrate to these rules coming from the Twitter ones is simple.

- Change the project coordinates in your gradle build scripts
    - For Detekt, `com.twitter.compose.rules:detekt:$version` becomes `io.nlopez.compose.rules:detekt:$version`
    - For Ktlint, `com.twitter.compose.rules:ktlint:$version` becomes `io.nlopez.compose.rules:ktlint:$version`.
- Update `$version` to the latest: ![Latest version](https://img.shields.io/maven-central/v/io.nlopez.compose.rules/common) - see the project [releases page](https://github.com/mrmans0n/compose-rules/releases).
- **If you are using Detekt**: update the config file (e.g. `detekt.yml`) so that the rule set name `TwitterCompose` becomes `Compose`. Keep in mind that there are a lot of new rules in this repo that weren't in Twitter's, so you'd be better copying over from the [example configuration](https://mrmans0n.github.io/compose-rules/detekt).
- Done!
