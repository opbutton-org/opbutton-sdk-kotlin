# OpButton SDK for Android (Kotlin)

See [Architecture](docs/architecture.md) for runtime boundaries, delivery receipts and command extension points.

Requires Android API 24+ and JDK 17+ for builds. Initialize on the application lifecycle:

```kotlin
val config = OpButtonConfig.Builder("https://opbutton.example.com", "opbutton_your_token")
    .setAutoConnect(true)
    .build()
OpButton.initialize(config, applicationContext)
```

The server URL must use HTTPS. Plain HTTP is available only for loopback development after calling `setAllowInsecureLocalhost(true)`. Pin SDK and transitive dependencies to reviewed immutable versions; do not consume a moving branch or snapshot in production.

An explicit `setDeviceId(...)` takes priority. Automatic identity uses a package-scoped SHA-256 of `ANDROID_ID`. It normally survives reinstalling the same signed application on the same Android user/device. Factory reset, signing-key changes, profile changes and the Android 8 OTA/reinstall exception can change it.

If the system ID is unavailable, the SDK persists a UUID in `noBackupFilesDir`. It survives restarts, but resets on uninstall/data clearing. Backup exclusion prevents cloning the ID onto another phone. Without Context or writable storage, a process UUID survives reconnects and SDK reinitialization only within that process.

Upgrading from the old model-based default creates a new device request that needs approval once. See [Android's identifier contract](https://developer.android.com/reference/android/provider/Settings.Secure#ANDROID_ID).

Each installation also generates a high-entropy device credential. With Application Context, the SDK stores it in the app-private `noBackupFilesDir`, excluded from Android backup; the server stores only its SHA-256 digest. Without Context it survives only inside the current runtime, so the next initialization requires approval again. A host-managed secure store can provide a 32–256 character printable-ASCII value through `setDeviceSecret(...)`.

When the server migration first enables this protocol, existing approved devices are intentionally moved back to pending for one explicit re-approval.

Panel updates acknowledge their captured revision. Attribute snapshots remain pending until a successful request. Recovery polls deliver returned commands and preserve metric sampling.
