# CinéIA Studio Mobile

Application Android de création cinématographique par Intelligence Artificielle (Gemini & Agnes).

## Architecture

- **Architecture** : MVVM + Clean Architecture avec Coroutines et Flow
- **UI** : Jetpack Compose avec Material Design 3, animations custom et fond cinéma dynamique 3 couches
- **Base de données** : Room (SQLite) avec DAO réactifs
- **Réseau & API** : Retrofit + OkHttp avec Rate Limiter (Token Bucket), gestion des 429 et reprise sur incident
- **Chargement Médias** : Coil pour le cache d'images et lecteur vidéo natif haute performance

## Compilation locale

```bash
./scripts/fix-executable-permissions.sh
./gradlew clean assembleDebug
```

## Intégration Continue (CI)

Le workflow GitHub Actions `.github/workflows/android.yml` assure la compilation automatique et la génération de l'APK debug sous Java 21 et Android SDK 36.1.
