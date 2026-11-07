# Tamil Music – Offline Player

Kotlin + Jetpack Compose + AndroidX Media3. 100% offline, no ads, no login, no subscriptions.

## Build the APK
1. Install Android Studio (Koala or newer; bundled JDK 17).
2. File > Open > select this `TamilMusic` folder. Wait for Gradle sync (needs internet once).
3. Debug APK: Build > Build APK(s)  — or terminal: `./gradlew assembleDebug`
4. APK path: `app/build/outputs/apk/debug/app-debug.apk`
5. Run on phone/emulator with the green Run button.

## Features
Home (recent + categories) · Library (search, sort, artist/album/genre) · Player (play/pause/next/prev/shuffle/repeat/seek) ·
Background playback + notification + lock-screen controls (Media3 MediaSessionService) · Favourites & playlists (Room) ·
Equalizer + bass boost (where device supports) · Sleep timer · Playback speed · Light/Dark/System theme ·
Import audio via system file picker · Album art (embedded or MediaStore) · Lyrics you paste yourself (stored locally).

## Music & legal
- No commercial songs are included. No sample audio is bundled because licences could not be verified.
- To bundle licensed tracks (CC0 / public domain / permissive CC), drop files into `app/src/main/assets/samples/`
  and rebuild. They appear as "Sample tracks". Only add files whose licence allows redistribution.
- The app plays music already on your device (MediaStore) and files you import.

## Permissions
READ_MEDIA_AUDIO (Android 13+) / READ_EXTERNAL_STORAGE (<=12), POST_NOTIFICATIONS (13+),
FOREGROUND_SERVICE(_MEDIA_PLAYBACK), WAKE_LOCK.

## Notes
- minSdk 24, target/compile 34. Genre tags come from MediaStore on Android 11+; older versions show "Unknown genre".
- Release build is unsigned; use debug APK for sideloading, or sign via Build > Generate Signed Bundle / APK.
