# Hepta

Tap-to-talk for Android Auto: tap **Speak** on the car screen, say *"play believer on Spotify"* or
*"play cricket highlights on YouTube"*, and the song starts. Built because Gemini on Android Auto keeps
failing at exactly that.

Hepta is a sideloaded app: you build it yourself and install it on your own phone. It is not on the Play Store.

## What you need

- A Mac, Windows or Linux computer with [Android Studio](https://developer.android.com/studio).
- An Android phone running **Android 13 or newer**, with a USB cable.
- The **Android Auto** app on the phone, and a car (or the desktop emulator, see below).
- **Spotify** and/or **YouTube** installed on the phone. Spotify's free tier works.
- A Google account, for the free YouTube API key.

## Setup, step by step

### 1. Get the code

```sh
git clone https://github.com/heptafox/Heptafox-android-carplay.git
cd Heptafox-android-carplay
```

Open the folder in Android Studio. It downloads the Android SDK and Gradle on first sync. Gradle also
downloads the Java 25 runtime it needs, so you don't install Java yourself.

In Android Studio, open **Tools → SDK Manager → SDK Tools** and install
**Android Auto Desktop Head Unit Emulator**. You only need it to test without a car.

### 2. Get a YouTube API key

Hepta asks the YouTube Data API for the top video that matches what you said. The key is free.
Spotify needs no key: Hepta finds the track through a Google web search.

1. Open the [Google Cloud Console](https://console.cloud.google.com/) and sign in.
2. At the top, click the project picker, then **New project**. Name it anything, e.g. `hepta`, and create it.
3. Make sure the new project is selected. Go to **APIs & Services → Library**.
4. Search for **YouTube Data API v3**, open it, and click **Enable**.
5. Go to **APIs & Services → Credentials → Create credentials → API key**. Copy the key. It starts with `AIza`.
6. Click **Edit API key** (or open the key from the Credentials list) and set:
   - **API restrictions → Restrict key → YouTube Data API v3**. This limits what a leaked key can do.
   - **Application restrictions → None**. Don't pick "Android apps": Hepta calls the API over plain HTTPS,
     so that restriction would reject every request.
7. Click **Save**. A new key can take a few minutes to start working.

Now give the key to the build. Create or open `local.properties` in the project root and add one line:

```properties
YT_API_KEY=AIzaYourKeyHere
```

`local.properties` is git-ignored, so the key never gets committed. Android Studio usually creates this file
with an `sdk.dir=...` line already in it. Keep that line.

Without a key the app still builds and Spotify still works. A YouTube request then shows
"YouTube API key missing" on the car screen.

> The key is compiled into your APK. Don't share an APK built with your key; share the source instead.

**Quota:** the free quota is 10,000 units a day and each search costs 100, so about **100 YouTube
requests a day**. It resets at midnight Pacific time.

### 3. Prepare the phone

1. **Developer mode:** Settings → About phone → tap **Build number** 7 times.
2. **USB debugging:** Settings → System → Developer options → turn on **USB debugging**.
   The menu location varies a little by phone brand.
3. Connect the phone by USB and accept the "Allow USB debugging?" prompt.

### 4. Build and install

Click **Run** in Android Studio, or from a terminal:

```sh
./gradlew installDebug
```

### 5. Grant Hepta's permissions

Open **Hepta** on the phone and tap both buttons:

- **Allow microphone**, so it can hear you through the car mic.
- **Allow background launch**, which opens the "Display over other apps" setting. Turn it on for Hepta.
  Android blocks apps from starting other apps in the background without it.

Both lines on the screen should show ✅. You can type a command such as `play believer on spotify` in the
test box to check playback without a car.

### 6. Let Android Auto show sideloaded apps

1. Open the **Android Auto** settings on the phone.
2. Scroll down and tap **Version** 10 times, then confirm. This unlocks developer mode.
3. Tap **⋮ → Developer settings** and turn on **Unknown sources**.

Connect to the car. Hepta now shows up in the car's app launcher.

## Using it

Tap **Hepta → Speak** and say:

| You say | What happens |
|---|---|
| "play believer" | Plays on Spotify, the default |
| "play believer on spotify" | Plays on Spotify |
| "play cricket highlights on youtube" | Plays the top YouTube video's audio |
| "search for lofi beats" | Plays on Spotify |

Your music pauses while Hepta listens, so it isn't transcribed by mistake.

## Test without a car

1. In Android Auto's **Developer settings** (step 6), tap **⋮ → Start head unit server**.
2. With the phone on USB, run:

```sh
adb forward tcp:5277 tcp:5277
$ANDROID_HOME/extras/google/auto/desktop-head-unit
```

A car screen opens on your computer, and your computer's microphone stands in for the car mic.

## Check that Spotify plays from search

Spotify is started with Android's standard "play from search" intent. Some Spotify versions only open the
search results instead of playing. Check yours with the phone connected:

```sh
adb shell am start -a android.media.action.MEDIA_PLAY_FROM_SEARCH -p com.spotify.music \
  -e query "believer imagine dragons" -e android.intent.extra.focus "vnd.android.cursor.item/*"
```

## Development

```sh
./gradlew assembleDebug   # build
./gradlew test            # unit tests for the command parser
./gradlew lint
```

| Package | Job |
|---|---|
| `car` | Android Auto screens. `VoiceScreen` (home) pushes `ListeningScreen` per request |
| `assistant` | `VoiceAssistant`: transcript → command → playback, returns an `AssistantState` |
| `voice` | `CarMicSource` (car mic → pipe) and `SpeechListener` (pipe → Android speech recognizer) |
| `command` | `CommandParser`: pure text parsing, unit tested |
| `playback` | `SpotifyLauncher`, `YouTubeLauncher` + `YouTubeSearch` (Data API) |
| `setup` | Phone screen for permissions and a typed-command test box |

[CLAUDE.md](CLAUDE.md) explains the architecture in more depth, including how to add another music app.

## Troubleshooting

- **"Didn't catch that" right after tapping Speak:** Android Auto's mic connection is stuck. Force-stop the
  Android Auto app on the phone and reconnect (or restart the head unit server).
- **YouTube says "HTTP 403":** the API isn't enabled for the key's project, the key's restrictions block it,
  or today's quota is used up.
- **Nothing starts playing:** check that both permissions in step 5 show ✅.

## Limits

- YouTube plays audio only, because Android Auto blocks video. It keeps playing on a locked phone only with
  **YouTube Premium**.
- About 100 YouTube searches a day on the free API quota.
- The steering-wheel voice button and "Hey Google" belong to Android Auto. Use the on-screen **Speak** button.
- Personal sideload only. The app uses the IOT category and accepts any Android Auto host, so it can't be
  published to the Play Store as-is.

## License

[MIT](LICENSE)
