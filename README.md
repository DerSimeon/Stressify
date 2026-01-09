# Stressify

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Security Policy](https://img.shields.io/badge/Security-Policy-blue.svg)](SECURITY.md)

Stressify is a Kotlin-based desktop tool for testing Minecraft servers and plugins by simulating multiple client connections.
It is built for plugin testing, connection behavior analysis, and controlled load testing on servers you own or explicitly have permission to test.

Stressify is not intended for malicious use, public griefing, or denial-of-service attacks.

## Purpose

Stressify exists to help developers answer questions like:

- How does my plugin behave when many players join at once?
- Does my server handle reconnects, respawns, or disconnect storms correctly?
- Are there protocol or lifecycle bugs that only show up under load?

It simulates real client behavior using mcprotocollib, not fake packet spam.

## What it does

- Spawns multiple Minecraft-like client sessions
- Queries server status (MOTD, version, player counts)
- Manages bot lifecycle and reconnect logic
- Provides a Compose Desktop UI
- Keeps protocol and bot logic separate from UI

## Platforms and binaries

Stressify produces native desktop binaries:

- Windows: .msi
- macOS: .dmg
- Linux: .deb and .rpm

Native binaries are built using Compose Desktop and jpackage.
Cross-compilation is not supported.

## Getting Stressify

Prebuilt binaries are available on the GitHub Releases page.
Always prefer a release build over running from source.

## Compilation

Requirements:
- JDK 17 or newer
- Gradle wrapper (included)

Run locally:
./gradlew run

Build native binaries for your OS:
./gradlew packageReleaseDistributionForCurrentOS

Artifacts are written to:
build/compose/binaries/main-release/

## Versioning and releases

Versions are tagged as vX.Y.Z.
Each tag triggers a multi-OS release build.

## Contributing

See CONTRIBUTING.md.

## Security

See SECURITY.md.

## License

MIT License.
