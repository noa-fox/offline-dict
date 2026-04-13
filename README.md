# OfflineDict 📱

Android offline dictionary app with built-in English dictionary data.

## Tech Stack

- **Kotlin** + **Jetpack Compose**
- **Room** + **SQLite FTS5** for full-text search
- **MVVM** + **Clean Architecture**
- Minimum SDK: 29 (Android 10)

## Features

- Pure offline, no network required
- Built-in ECDICT open-source dictionary (CC-BY-SA)
- Word lookup with definitions, phonetics, and examples
- Prefix matching and fuzzy search

## Project Structure

```
app/src/main/java/com/noabot/offlinedict/
├── data/local/entity/    # Room entities (DictEntry + FTS5)
├── data/local/           # Database & DAO
├── data/repository/      # Data access layer
├── domain/               # Business logic
└── ui/                   # Compose UI + ViewModels
```

## License

ECDICT data is licensed under CC-BY-SA.
