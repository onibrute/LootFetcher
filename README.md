# LootFetcher

# WoWFetch 🎮📦

## Description

**WoWFetch** is a desktop application built in Java that displays and stores loot session data from World of Warcraft, using the official Blizzard Game Data API and a custom in-game addon.  
The project integrates real-time gameplay events, local session tracking, and modern UI design to offer a centralized and immersive loot log experience for players.

---

## 🧩 Features

- 🗺️ Browse dungeons, bosses and official loot via Blizzard API  
- 💾 Automatically log and save loot sessions from WoW using a custom Lua addon  
- 🧠 Parse and process SavedVariables Lua files with LuaJ  
- 🗃️ Store loot sessions (date, time, monsters, items, currency) in a local **SQLite** database  
- 🎨 Dynamic theme switch (Horde / Alliance)  
- 🔍 Filter and review farm sessions directly in the app

---

## 🔧 Technologies Used

| Technology        | Purpose                                              |
|-------------------|------------------------------------------------------|
| **Java 21 + JavaFX**       | GUI interface                               |
| **Blizzard Game Data API** | Fetch item/instance metadata                |
| **OAuth 2.0**              | API authentication                          |
| **SQLite**                 | Session database                            |
| **LuaJ**                   | Parse in-game loot files (`LootLogger.lua`) |
| **SceneBuilder**           | GUI layout design                           |
| **Maven**                  | Dependency management                       |
| **Lua Addon**              | Captures loot events in-game                |

---


## 📂 Architecture Overview

                  +-------------------+
                  |   WoW Lua Addon   |
                  | (Interface/AddOns)|
                  +-------------------+
                           |
                           v
             +---------------------------------+
             | LootLogger.lua (SavedVariables) |
             +---------------------------------+
                           |
                           v
         +----------------------------------------+
         | WoWFetch Desktop App (JavaFX + LuaJ)   |
         | - Parses .lua file                     |
         | - Displays data in UI                  |
         | - Saves sessions in SQLite             |
         | - Requests item metadata via API       |
         +----------------------------------------+

