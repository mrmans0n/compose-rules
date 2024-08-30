![Compose Rules](art/logo.webp)

# The Jetpack Compose Rules - Static analysis of @Composable functions for ktlint and detekt

[![Build & test](https://github.com/mrmans0n/compose-rules/actions/workflows/build.yaml/badge.svg?branch=main)](https://github.com/mrmans0n/compose-rules/actions/workflows/build.yaml?query=branch%3Amain)
[![Maven Central](https://img.shields.io/maven-central/v/io.nlopez.compose.rules/common)](https://central.sonatype.com/search?q=g%3Aio.nlopez.compose.rules)

> **Note**
> This repository started a fork of [Twitter Jetpack Compose Rules](https://github.com/twitter/compose-rules) by the original main maintainer, and its development will continue here from now on. If you are migrating from that project, check out the [migration guide](https://mrmans0n.github.io/compose-rules/).

## Overview

It can be challenging for big teams to start adopting Compose, particularly because not everyone will start at same time or with the same patterns. We tried to ease the pain by creating a set of Compose static checks.

Compose has lots of superpowers but also has a bunch of footguns to be aware of [as seen in this Twitter Thread](https://twitter.com/mrmans0n/status/1507390768796909571).

This is where our static checks come in. We want to detect as many potential issues as we can, as quickly as we can. In this case we want an error to show prior to engineers having to review code. Similar to other static check libraries we hope this leads to a "don't shoot the messengers" philosphy which will foster healthy Compose adoption.

Check out the project website for more information: https://mrmans0n.github.io/compose-rules

## What custom rules are bundled in this repo?

You can find the comprehensive list of rules in [the rules documentation](https://mrmans0n.github.io/compose-rules/rules). It contains both the what and why for each rule, we encourage you to read it prior to adopting our rules.

There are 2 ways you can add these static checks to your build - using ktlint or using detekt.

## Using with ktlint

You can refer to the [Using with ktlint](https://mrmans0n.github.io/compose-rules/ktlint) documentation.

## Using with detekt

You can refer to the [Using with detekt](https://mrmans0n.github.io/compose-rules/detekt) documentation.

## Contributing

We love sharing and learning from others; contributing new rules or fixes is welcome. See the [Contributing](CONTRIBUTING.md) instructions for more information.

## License

```
    Copyright 2022 Twitter, Inc.
    Copyright 2024 Nacho Lopez.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
```
