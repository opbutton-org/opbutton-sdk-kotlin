# OpButton SDK for Android (Kotlin)

See [Architecture](docs/architecture.md) for runtime boundaries, delivery receipts and command extension points.

Requires Android API 24+ and Java 8+. Initialize on the application lifecycle:

```kotlin
val config = OpButtonConfig.Builder("https://opbutton.example.com", "opbutton_your_token")
    .setAutoConnect(true)
    .build()
OpButton.initialize(config, applicationContext)
```

An explicit `setDeviceId(...)` takes priority. Automatic identity uses a package-scoped SHA-256 of `ANDROID_ID`. It normally survives reinstalling the same signed application on the same Android user/device. Factory reset, signing-key changes, profile changes and the Android 8 OTA/reinstall exception can change it.

If the system ID is unavailable, the SDK persists a UUID in `noBackupFilesDir`. It survives restarts, but resets on uninstall/data clearing. Backup exclusion prevents cloning the ID onto another phone. Without Context or writable storage, a process UUID survives reconnects and SDK reinitialization only within that process.

Upgrading from the old model-based default creates a new device request that needs approval once. See [Android's identifier contract](https://developer.android.com/reference/android/provider/Settings.Secure#ANDROID_ID).

Panel updates acknowledge their captured revision. Attribute snapshots remain pending until a successful request. Recovery polls deliver returned commands and preserve metric sampling.
