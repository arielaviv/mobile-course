# field survey

Android app for telecom field teams. Records infrastructure — poles, aerial spans, underground ducts and central offices — with GPS, a photo, and a short label. Every point is shared with the team and can be commented on.

School project. Kotlin, single activity, MVVM.

## build

JDK 17 and a recent Android SDK. Put your keys in `local.properties`:

```
MAPBOX_PUBLIC_TOKEN=pk.xxxx
ANTHROPIC_API_KEY=sk-ant-xxxx
```

Firebase config (`google-services.json`) lives in `app/`.

```
./gradlew assembleDebug
```

APK ends up in `app/build/outputs/apk/debug/`.

## stack

Room (local source of truth) + Firestore (sync) + Firebase Auth. Hilt, Retrofit, Navigation with safeArgs, Paging 3, Material 3. Maps are Mapbox v11. The photo → notes button sends a compressed base64 image to Claude Haiku 4.5 through the Anthropic Messages API.

## screens

Login, Register, Feed, Map, Add Point, Detail, Edit Point, My Points, Profile, Settings, Statistics.

## notes

- Room is what the UI reads from. Firestore syncs in the background so nothing blocks on network.
- Photos get compressed to ≤1600 px JPEG and stored as base64 inside the row.
- English + Hebrew, RTL works, language switches at runtime from Settings.
- `min sdk 26`, `target sdk 35`.
