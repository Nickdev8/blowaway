# BlowAway

Minimal Android app that keeps a foreground microphone service armed in the background, listens for a high-importance notification, opens the microphone for 8 seconds, and dismisses that notification when it hears a blow-like amplitude spike.

## Build

```bash
./gradlew assembleDebug
```

## Install

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Run

```bash
adb shell am start -n com.example.blowaway/.MainActivity
```

## Device Setup

1. Open the app.
2. Grant microphone permission.
3. Tap the notification access button and enable access for BlowAway.
4. Tap the background listening button once so BlowAway stays armed with a persistent foreground notification.
5. Close the app.
6. Trigger a high-importance notification from another app.
7. Blow into the microphone within 8 seconds to dismiss it.

## Notes

- `BlowNotificationListener` ignores this app's own foreground-service notification to avoid feedback loops.
- The listener treats `IMPORTANCE_HIGH` and above as the simplest heads-up approximation.
- `BIND_NOTIFICATION_LISTENER_SERVICE` is declared on the listener service, not as a normal `uses-permission`, because third-party apps cannot request that permission directly.
- On Android 14+, background microphone access only works reliably if the microphone foreground service was already started while the app was visible. The app now exposes an explicit arm/disarm step for that.
