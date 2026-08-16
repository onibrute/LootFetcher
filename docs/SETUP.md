# Setup and Run Guide

## Requirements

- JDK 17 or newer
- Maven
- Internet access for Blizzard Game Data API-backed views
- Blizzard API client credentials
- Optional: a `LootLogger.lua` SavedVariables file when importing gameplay loot

## 1. Clone the repository

```bash
git clone https://github.com/onibrute/LootFetcher.git
cd LootFetcher
```

## 2. Configure Blizzard API credentials

LootFetcher reads the Battle.net API credentials from environment variables:

```text
BLIZZARD_CLIENT_ID
BLIZZARD_CLIENT_SECRET
```

### PowerShell example

```powershell
$env:BLIZZARD_CLIENT_ID="your_client_id"
$env:BLIZZARD_CLIENT_SECRET="your_client_secret"
```

Do not commit real API credentials to the repository.

## 3. Configure LootLogger import

To import gameplay data exported by the companion addon, set:

```text
WOW_LOOTLOGGER_PATH
```

Example:

```powershell
$env:WOW_LOOTLOGGER_PATH="C:\path\to\World of Warcraft\_retail_\WTF\Account\<ACCOUNT>\SavedVariables\LootLogger.lua"
```

Alternatively, provide the path as a JVM property:

```text
-Dwow.lootlogger.path="C:\path\to\LootLogger.lua"
```

The application can still start without a LootLogger file; only the addon-import workflow depends on it.

## 4. Run the application

```bash
mvn clean javafx:run
```

## 5. Build

```bash
mvn clean package
```

The Maven Assembly Plugin is configured to produce a JAR containing project dependencies during the package phase.

## Local application data

LootFetcher creates a local SQLite database:

```text
session_data.db
```

This contains session history and is intentionally ignored by Git.

## UI themes

The JavaFX interface includes separate stylesheets for:

- Horde
- Alliance

They are stored under:

```text
src/main/resources/styles/
```

## Troubleshooting

### Blizzard API views return no data

Check that:

1. `BLIZZARD_CLIENT_ID` and `BLIZZARD_CLIENT_SECRET` are defined in the environment used to launch Maven.
2. The machine has internet access.
3. The credentials belong to a valid Blizzard API client.

### Loot import returns empty data

Check that:

1. `WOW_LOOTLOGGER_PATH` points to an existing `LootLogger.lua` file.
2. The companion addon has written `LootLoggerDB` data into that SavedVariables file.
3. World of Warcraft has flushed the SavedVariables data to disk.

The original project documentation notes a workflow limitation: because the application reads the SavedVariables file, freshly generated loot may not be available to the desktop application until the game has written its SavedVariables state.

## Repository scope

The current repository contains the Java desktop application. The presentation documents the custom LootLogger addon, but its source is not currently part of this repository. Adding the addon under a dedicated `addon/` directory would make the full workflow reproducible from one checkout.
