# LootFetcher Architecture

## Overview

LootFetcher is a desktop integration project that combines three data sources/components:

1. **World of Warcraft + companion LootLogger addon** — records gameplay loot/currency into a SavedVariables Lua file.
2. **JavaFX desktop application** — imports local gameplay data, provides the UI, tracks sessions, and coordinates persistence/API access.
3. **Blizzard Game Data API + SQLite** — enriches the UI with official metadata while SQLite stores local farming-session history.

```text
World of Warcraft
       |
       | loot / currency events
       v
Companion LootLogger addon
       |
       v
SavedVariables/LootLogger.lua
       |
       | LuaJ
       v
+--------------------------------------+
|          LootFetcher / JavaFX        |
|                                      |
|  MainController                      |
|  Session tracking                    |
|  Horde / Alliance themes             |
|  Loot import                         |
+------------------+-------------------+
                   |
           +-------+-------+
           |               |
           v               v
      SQLite/JDBC     Blizzard Game Data API
      local history      OAuth 2.0 + REST
```

## Application layers

### Presentation layer

The desktop interface is implemented with JavaFX, FXML and CSS. The repository contains separate Horde and Alliance stylesheets and an FXML-defined main view.

Primary user-facing workflows include:

- browsing game instances and encounters;
- reviewing metadata returned by Blizzard APIs;
- starting/stopping farming sessions;
- importing loot/currency from the companion addon output;
- saving and reviewing session history;
- switching between Horde and Alliance visual themes.

## Blizzard API integration

`BlizzardApiClient` uses Java's HTTP client and Jackson to access Blizzard Game Data API endpoints.

The current implementation:

- reads `BLIZZARD_CLIENT_ID` and `BLIZZARD_CLIENT_SECRET` from environment variables;
- authenticates through the OAuth 2.0 client-credentials flow;
- keeps the acquired access token in memory for the lifetime of the client instance;
- queries journal-instance, zone, item and media endpoints;
- uses the EU region and `en_GB` locale in the checked-in implementation.

The application does not persist API credentials in the repository.

## LootLogger integration

The project documentation describes a companion World of Warcraft addon that listens for loot-related game events and writes data to `SavedVariables/LootLogger.lua`.

The desktop application reads this export through `LootLoggerParser` and LuaJ. The current parser extracts:

- loot entries from the latest exported session;
- gold/silver/copper values;
- normalized total currency in copper.

The current addon export does not provide a reliable mob-kill count to the desktop parser, so the imported model currently uses `0` for that field.

The companion addon source is not currently included in this repository; therefore the repository documents, but does not fully contain, the complete game-to-desktop pipeline.

## Persistence

`DatabaseHelper` stores farming-session history in a local SQLite database named `session_data.db`.

The `sessions` table stores:

- date/time;
- duration in seconds;
- mob-count field;
- total currency in copper;
- loot as a serialized text field.

The database is local runtime data and is excluded from Git through `.gitignore`.

## Build architecture

The project uses Maven for dependency and build management.

Checked-in configuration:

- Java compiler target: **17**;
- JavaFX: **21.0.2**;
- Jackson Databind: JSON processing;
- Xerial SQLite JDBC: local persistence;
- LuaJ: Lua SavedVariables parsing;
- JavaFX Maven Plugin: local execution;
- Maven Assembly Plugin: JAR-with-dependencies packaging.

The original project presentation describes the development stack as Java 21 + JavaFX. The repository itself currently compiles to Java 17 bytecode while using JavaFX 21.0.2; repository documentation follows the checked-in build configuration.

## Design strengths

The project demonstrates several integration concerns in one desktop application:

- REST API authentication and consumption;
- local relational persistence;
- cross-language data exchange between Lua and Java;
- desktop UI architecture with FXML/CSS;
- environment-based secret configuration;
- local session tracking and data enrichment.

## Current architectural limitations

- Companion addon source is not included in the repository.
- Addon output is imported from SavedVariables rather than streamed live while the game is running.
- API token refresh/expiry handling is minimal; the token is simply cached in memory after authentication.
- SQLite loot serialization currently uses comma-separated text rather than a normalized relational schema.
- Automated tests are not yet part of the repository.
