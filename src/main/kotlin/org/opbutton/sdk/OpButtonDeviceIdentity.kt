package org.opbutton.sdk

import android.content.Context
import android.provider.Settings
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

/** Uses an installation identity unless the host or a stored credential supplies an existing one. */
internal class OpButtonDeviceIdentity(vararg sources: Source) {
    fun interface Source { fun resolve(): String? }
    private val sources = sources.copyOf()

    fun resolve(override: String?): String {
        if (!override.isNullOrBlank()) return override
        for (source in sources) {
            try {
                source.resolve()?.takeIf(String::isNotEmpty)?.let { return it }
            } catch (_: RuntimeException) {
                OpButtonLog.w("Device identity source unavailable; trying fallback")
            }
        }
        return processId
    }

    companion object {
        private val processId = "process-v1:${UUID.randomUUID()}"

        fun forContext(
            context: Context?,
            config: OpButtonConfig,
            credentials: OpButtonDeviceCredentialStore
        ): OpButtonDeviceIdentity {
            if (context == null) return OpButtonDeviceIdentity()
            val owner = context.applicationContext ?: context
            return withInstallationIdentity(
                Source {
                    try {
                        scopedAndroidId(owner.packageName, Settings.Secure.getString(owner.contentResolver, Settings.Secure.ANDROID_ID))
                    } catch (_: RuntimeException) {
                        OpButtonLog.w("Android ID unavailable; using installation identity")
                        null
                    }
                },
                Source { installationIdentity(owner.noBackupFilesDir) }
            ) { id -> config.deviceSecret != null || credentials.hasStoredCredential(config.baseUrl, id) }
        }

        fun withInstallationIdentity(
            androidIdentity: Source,
            installation: Source,
            hasCredential: (String) -> Boolean
        ): OpButtonDeviceIdentity =
            OpButtonDeviceIdentity(Source { androidIdentity.resolve()?.takeIf(hasCredential) }, installation)

        fun scopedAndroidId(packageName: String, androidId: String?): String? {
            if (androidId == null || !androidId.matches(Regex("(?i)[0-9a-f]{1,16}")) ||
                androidId.matches(Regex("0+")) || androidId.equals("9774d56d682e549c", ignoreCase = true)) return null
            val value = "opbutton-device:v1\u0000$packageName\u0000${androidId.lowercase(Locale.ROOT)}"
            return "android-v1:" + MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }
        }

        // Excluding this file from backup prevents identity collisions after device migration.
        @Synchronized
        fun installationIdentity(directory: File): String? {
            var temporary: File? = null
            return try {
                val file = File(directory, "opbutton-installation-id")
                if (file.isFile) return "installation-v1:${UUID.fromString(file.readText().trim())}"
                val id = UUID.randomUUID().toString()
                val pending = File.createTempFile("opbutton-identity-", ".tmp", directory)
                temporary = pending
                FileOutputStream(pending).use {
                    it.write(id.toByteArray(Charsets.UTF_8))
                    it.fd.sync()
                }
                check(pending.renameTo(file)) { "Unable to save installation identity" }
                "installation-v1:$id"
            } catch (_: Exception) {
                OpButtonLog.w("Installation identity unavailable; using process identity")
                null
            } finally {
                temporary?.let {
                    if (it.exists() && !it.delete()) OpButtonLog.w("Unable to remove temporary identity file")
                }
            }
        }
    }
}
