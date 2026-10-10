# SMSAlert

An Android app that automatically places a phone call when it receives an SMS containing a given keyword.

Typical use case: monitoring and alerting systems often send alerts by SMS, which are easy to miss at night when the phone is on silent — but an incoming call rings. Turning alert texts into a phone call makes sure you actually wake up.

## Features

- **Keyword matching**: triggers when the message body contains the keyword (case-insensitive). Multi-part messages are reassembled per sender before matching.
- **Sender filter** (optional): only handle messages from a specific number. Matching is by trailing digits, so `+8613800000000` and `13800000000` are treated as the same number. Leave empty to accept any sender.
- **Automatic dialing**: places the call directly, without going through the dialer for confirmation.
- **Cooldown**: after a trigger, further matching messages within 60 seconds are ignored, so a burst of alerts for the same incident doesn't cause back-to-back calls.
- **Activity log**: keeps the 30 most recent triggers / failures, shown on the main screen.
- **Test call**: verify with one tap that dialing works.

The app doesn't need to stay running in the background: the SMS receiver is registered statically, so the system launches the app when a message arrives even if its process isn't running.

## Usage

> The app UI is in Chinese; the original labels are shown in parentheses below.

1. Install the APK and open it.
2. Fill in **Keyword** (关键字) and **Number to call** (拨打号码), and optionally **Sender** (发件人).
3. Turn on the switch, tap **Save** (保存), and grant the SMS and phone permissions when prompted.
4. Tap **Test call** (测试拨号) to confirm a call can be placed.
5. Complete the following settings as indicated by the status panel (requirements vary by device; enabling all of them is recommended):
   - **Display over other apps** (允许显示在其他应用上层): required by some ROMs to place calls from the background.
   - **Ignore battery optimization** (忽略电池优化): prevents the system from killing the process and dropping SMS broadcasts while the device is idle.
   - **App details** (应用详情): on many Chinese Android ROMs, also enable options such as "Autostart" and "Display pop-up windows while running in the background".

## Permissions

| Permission | Purpose |
| --- | --- |
| `RECEIVE_SMS` | Receive SMS and match the keyword |
| `CALL_PHONE` | Place calls directly |
| `SYSTEM_ALERT_WINDOW` | Fallback for background calling on some ROMs |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Ask to be exempted from battery optimization |

The app has no network access. Settings and logs are stored only on the device (SharedPreferences), and system backup is disabled.

## Building

Requirements: JDK 17, Android SDK (compileSdk 35)

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/`.

- Minimum supported version: Android 8.0 (API 26)
- targetSdk is 34, intended for personal sideloading. Because it uses SMS and call permissions, it is not suitable for publishing on Google Play.

## Implementation notes

Calls are placed with `TelecomManager.placeCall` first. It doesn't start an Activity, so it isn't affected by the Android 10+ restriction on starting activities from the background, and works from within the SMS broadcast. On the few ROMs where this fails, it falls back to `ACTION_CALL`, which requires the "Display over other apps" permission.
