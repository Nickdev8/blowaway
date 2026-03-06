# BlowAway

Minimal Android app that listens for a high-importance notification, opens the microphone for 8 seconds, and dismisses that notification when it hears a blow-like amplitude spike.

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
4. Trigger a high-importance notification from another app.
5. Blow into the microphone within 8 seconds to dismiss it.

## Notes

- `BlowNotificationListener` ignores this app's own foreground-service notification to avoid feedback loops.
- The listener treats `IMPORTANCE_HIGH` and above as the simplest heads-up approximation.
- `BIND_NOTIFICATION_LISTENER_SERVICE` is declared on the listener service, not as a normal `uses-permission`, because third-party apps cannot request that permission directly.
- On Android 12+ and especially Android 14+, the system can block starting a microphone foreground service while the app is in the background. The sample logs that failure instead of crashing.
