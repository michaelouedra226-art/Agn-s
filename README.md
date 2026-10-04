# CinéIA Studio Mobile

Application Android de création cinématographique assistée par Google Gemini et Veo.

## Génération réelle

- **Scénarios et amélioration de prompts** : Gemini Flash via l’API Google AI Studio.
- **Images** : Gemini 3.1 Flash Image. Les images générées sont téléchargées, vérifiées, enregistrées localement et ajoutées à la galerie.
- **Vidéos** : Veo 3.1 via l’API Gemini. L’application attend la fin de l’opération asynchrone, télécharge et vérifie le vrai fichier MP4 avant de l’ajouter à la galerie.
- **Image vers vidéo** : choisissez une image récente dans l’onglet Vidéo, ou touchez « Animer » sur une image de la galerie.
- **Lecture** : les MP4 sont lus par le lecteur vidéo Android avec ses commandes natives.

Aucune image de remplacement ni aucun faux fichier MP4 n’est créé. En cas d’échec, l’application affiche le code et le message d’erreur renvoyés par Google.

Documentation : [génération d’images Gemini](https://ai.google.dev/gemini-api/docs/image-generation) · [API vidéo Veo](https://ai.google.dev/gemini-api/docs/veo).

## Configuration requise

1. Créez une clé Google AI Studio sur [aistudio.google.com/apikey](https://aistudio.google.com/apikey).
2. Dans **Paramètres**, collez la clé puis touchez **Tester**. Le test interroge réellement l’API Google.
3. La même clé sert aux scénarios, aux images et aux vidéos. L’accès à Veo, sa facturation et son quota dépendent du compte Google et de la disponibilité du modèle dans votre région.
4. Veo accepte les clips de **4, 6 ou 8 secondes**. La sortie **1080p exige 8 secondes** ; à une autre durée, l’application utilise 720p.

Pour une configuration de build locale, copiez `.env.example` vers `.env` et remplacez `GEMINI_API_KEY`. Ne committez jamais votre clé.

## Architecture

- **Architecture** : MVVM + Room + Coroutines/Flow
- **UI** : Jetpack Compose et Material Design 3
- **Réseau** : OkHttp vers les API REST officielles Gemini et Veo
- **Galerie** : Room et fichiers privés à l’application
- **Chargement d’images** : Coil
- **Lecture vidéo** : `VideoView` / `MediaController` Android

## Compilation locale

```bash
bash ./scripts/fix-executable-permissions.sh
./gradlew clean assembleDebug
```

## Intégration continue

Le workflow GitHub Actions `.github/workflows/android.yml` compile l’APK debug sous Java 21 avec Android SDK 36.1.
