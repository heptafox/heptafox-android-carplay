# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Hepta is a sideloaded Android Auto app: tap **Speak** on the car screen, say "play X on Spotify/YouTube", and the phone starts playback. See README.md for phone/Android Auto setup and limits.

## Commands

```sh
./gradlew assembleDebug        # build
./gradlew installDebug         # build + install on the connected phone
./gradlew test                 # JVM unit tests (only CommandParserTest today)
./gradlew :app:testDebugUnitTest --tests 'com.heptafox.carplay.command.CommandParserTest'
./gradlew :app:testDebugUnitTest --tests '*CommandParserTest.search for is a verb'   # single test
./gradlew lint
```

Test on a desktop without the car: in Android Auto developer settings start the head unit server, then
`adb forward tcp:5277 tcp:5277 && $ANDROID_HOME/extras/google/auto/desktop-head-unit`.

Check Spotify's play-from-search intent directly:
```sh
adb shell am start -a android.media.action.MEDIA_PLAY_FROM_SEARCH -p com.spotify.music \
  -e query "believer imagine dragons" -e android.intent.extra.focus "vnd.android.cursor.item/*"
```

## Build setup

- AGP 9 with its built-in Kotlin support: there is no `kotlin-android` plugin applied, and none is needed.
- Single `:app` module, package `com.heptafox.carplay`, minSdk 33, target/compileSdk 36, `minCarApiLevel` 7.
- `YT_API_KEY` is read from git-ignored `local.properties` into `BuildConfig.YT_API_KEY`. A missing key only fails at YouTube search time, with a message on the car screen.
- The Gradle daemon runs on Java 25, pinned in `gradle/gradle-daemon-jvm.properties` and auto-provisioned via foojay. The launcher JVM can be older.
- Open source (MIT), hosted on GitHub as `Heptafox-android-carplay`. Never commit `local.properties`: it holds the YouTube key.
- Keep the Gradle wrapper (`gradle/wrapper/*`) committed so a fresh clone can run `./gradlew`.

## Architecture

One request flows through these layers. Each layer only knows about the next one:

```
HeptaCarAppService (wires a Session)
  └ VoiceScreen ──push──> ListeningScreen ──> SpeechListener.listen()  (voice)
                                │                 └ CarMicSource: CarAudioRecord → pipe → SpeechRecognizer EXTRA_AUDIO_SOURCE
                                └──> VoiceAssistant.handle(transcript)  (assistant)
                                        ├ CommandParser.parse → PlayCommand(provider, query)  (command, pure Kotlin)
                                        └ MediaLaunchers → SpotifyLauncher | YouTubeLauncher(+YouTubeSearch)  (playback)
  <──pop with AssistantState result── ListeningScreen
```

- **`VoiceAssistant` is UI- and mic-agnostic**: it maps a transcript to an `AssistantState`. `setup/MainActivity` reuses it for its typed-command test box, so that is the quickest way to exercise parsing and playback on the phone without a car.
- **`AssistantState`** (sealed) is the only result type, and each variant carries the text shown to the driver. `PlaybackException` messages go straight onto the car screen, so keep them short and user-facing. `IOException` becomes "network error".
- **Car template budget**: Android Auto allows only 5 template changes per task. `VoiceScreen` never calls `invalidate()`. Every request runs on a freshly pushed `ListeningScreen`, which pops itself with the result, and content changes on a pop are free. Keep this pattern when you add UI.
- `ListeningScreen` launches on `Dispatchers.Main`, deliberately not `Main.immediate`, so that an instant result pops itself and not the home screen. Pressing back cancels the request through `lifecycleScope`.
- `CarMicSource` holds `AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE` while recording, so music pauses and isn't transcribed. It pumps audio on its own thread and must always be `close()`d (`SpeechListener` does this in `finally`).
- `SpeechListener` works around a recognizer quirk. When Google's on-device recognizer is fed `EXTRA_AUDIO_SOURCE`, it only reports partial results and never finishes on its own. So end-of-speech triggers `stopListening()`, the last partial is used as the transcript, and a hard cutoff stops listening after 8 s. Don't "simplify" this back to `onResults` only. It must run on the main thread.
- Starting Spotify/YouTube from the background needs the "Display over other apps" grant (`SYSTEM_ALERT_WINDOW`) as well as `RECORD_AUDIO`. Both are checked in `setup/Permissions` and granted from `MainActivity`.
- Spotify uses `MEDIA_PLAY_FROM_SEARCH` (works on the free tier). YouTube does one Data API v3 search, which costs 100 quota units (about 100/day), and opens the top video in the YouTube app (audio only in the car).

### Adding a provider

Add it to the `Provider` enum, then update the `providerMention` regex in `CommandParser` and the `when` in `MediaLaunchers`. Add a `MediaLauncher` implementation and the app's package to `<queries>` in `AndroidManifest.xml`. Cover the new phrasing in `CommandParserTest`.

## Conventions

- Keep the package-per-concern split (`car`, `assistant`, `voice`, `command`, `playback`, `setup`). Put pure logic in `command` so it stays JVM-unit-testable.
- Kotlin test names use backtick sentences, e.g. `` `on youtube picks youtube and is removed from the query` ``.
- `HostValidator.ALLOW_ALL_HOSTS_VALIDATOR` and the IOT category are intentional for a personal sideloaded app. Don't "fix" them unless publishing is the goal.
