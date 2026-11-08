# LexiNode

Android-приложение для словарных коллекций (Kotlin, Jetpack Compose, Material 3, Room).

- Коллекции слов: создание, переименование, удаление.
- Слова с типом, переводом, озвучкой (TextToSpeech, иврит / язык по умолчанию).
- Полная поддержка иврита и RTL.
- Экспорт коллекции в .txt (UTF-8): `тип | слово | перевод`.

## Сборка

```
./gradlew assembleDebug
```

APK собирается автоматически в GitHub Actions (workflow **Build APK**): вкладка Actions → последняя сборка → Artifacts → `LexiNode-debug-apk`.
