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

An explicit `setDeviceId(...)` takes priority. With Application Context, the SDK saves an installation UUID and a high-entropy device credential in `noBackupFilesDir`. Both survive restarts. Reinstalling or clearing app data creates a new pair and requires approval.

The previous package-scoped Android identity remains usable when its credential is stored locally or supplied by the host. Upgrading from a version that never saved its credential creates a new device request.

Without Context, supply a 32–256 character printable-ASCII credential through `setDeviceSecret(...)` and a matching `setDeviceId(...)` for identity across processes. The default process UUID lasts until the app process exits.

Panel updates acknowledge their captured revision. Attribute snapshots remain pending until a successful request. Recovery polls deliver returned commands and preserve metric sampling.
