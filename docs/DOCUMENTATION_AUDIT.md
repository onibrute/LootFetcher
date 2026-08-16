# Documentation Audit

This note compares the uploaded academic presentation with the checked-in repository so portfolio claims stay aligned with the implementation.

## Confirmed by both presentation and repository

The project presentation describes a JavaFX desktop application that combines:

- Blizzard Game Data API integration;
- OAuth 2.0 authentication;
- SQLite session persistence;
- LuaJ parsing of a World of Warcraft SavedVariables file;
- a custom companion LootLogger addon workflow;
- Horde/Alliance themes;
- Maven dependency management;
- farming-session history and loot/currency tracking.

The repository contains corresponding JavaFX/FXML/CSS code, Blizzard API client code, SQLite persistence, LuaJ parsing, Maven configuration and themed resources.

## Version clarification

The presentation describes the stack as **Java 21 + JavaFX**.

The checked-in Maven build currently specifies:

```text
maven.compiler.source = 17
maven.compiler.target = 17
javafx.version = 21.0.2
```

For reproducibility, the README and setup documentation therefore describe the project as targeting **Java 17 bytecode with JavaFX 21.0.2**.

## OAuth/token wording

The presentation mentions OAuth 2.0 and local token caching.

The checked-in `BlizzardApiClient` obtains a client-credentials access token and caches it in memory in the client object. The repository does not currently show persistent token caching to disk.

Portfolio wording should therefore say **OAuth 2.0 client-credentials authentication with in-memory token reuse**, rather than persistent local token caching.

## Companion addon scope

The presentation documents a custom World of Warcraft addon that listens to loot-related events and stores results in `SavedVariables/LootLogger.lua`.

The Java application clearly consumes that format, but the addon source itself is not currently included in the repository. The README explicitly identifies this limitation instead of implying that the entire game-side component is reproducible from this checkout.

## Mob count limitation

The presentation describes sessions containing time, loot, instance and related activity information. The current desktop parser extracts loot and currency from the latest LootLogger export, but the checked-in parser explicitly sets `mobsKilled` to zero because the addon export does not currently provide a reliable mob-kill count.

The repository documentation therefore avoids claiming that mob kills are accurately derived from the current import path.

## Blizzard API coverage

The presentation notes that Blizzard does not expose complete information for every zone/monster/context. The checked-in application includes journal-instance, zone, encounter, item and media requests, but API completeness remains dependent on what Blizzard exposes.

## Current portfolio-safe claims

The repository safely supports the following claims:

- JavaFX desktop application development;
- REST API integration using Java HTTP client;
- OAuth 2.0 client-credentials authentication;
- JSON processing with Jackson;
- SQLite/JDBC local persistence;
- Lua SavedVariables parsing with LuaJ;
- Maven dependency/build management;
- themed FXML/CSS user interfaces;
- local farming-session tracking;
- cross-language integration between a Lua-generated file and a Java desktop application.

## Claims to avoid unless the implementation is expanded

- real-time direct communication with the running WoW process;
- complete Blizzard loot coverage for all content;
- persistent OAuth token caching;
- accurate mob-kill counting from the current addon export;
- a fully reproducible addon + desktop system from the repository alone.
