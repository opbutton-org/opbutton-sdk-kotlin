package org.opbutton.sdk

import android.content.Context
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

internal class OpButtonDeviceCredentialStore(context: Context?) {
    private val context = context?.applicationContext
    private val memory = ConcurrentHashMap<String, String>()

    fun hasStoredCredential(baseUrl: String, deviceId: String): Boolean =
        credentialFile(key(baseUrl, deviceId))?.isFile == true

    fun getOrCreate(baseUrl: String, deviceId: String, configured: String?): String {
        if (configured != null) return configured
        check(context != null) {
            "Automatic device credential persistence requires Android Context"
        }
        val key = key(baseUrl, deviceId)
        memory[key]?.takeIf(::valid)?.let { return it }
        val file = credentialFile(key)
        if (file?.isFile == true) {
            if (file.length() > MAX_SECRET_LENGTH) {
                throw OpButtonException("Stored device credential is invalid")
            }
            try {
                val value = file.readText(Charsets.UTF_8).trim()
                if (valid(value)) {
                    memory[key] = value
                    return value
                }
            } catch (error: Exception) {
                throw OpButtonException("Unable to read stored device credential", error)
            }
            throw OpButtonException("Stored device credential is invalid")
        }
        val bytes = ByteArray(48).also { SecureRandom().nextBytes(it) }
        val value = bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }
        saveByKey(key, value)
        return value
    }

    fun save(baseUrl: String, deviceId: String, value: String) {
        require(valid(value)) { "device credential must contain 32 to 256 characters" }
        saveByKey(key(baseUrl, deviceId), value)
    }

    private fun saveByKey(key: String, value: String) {
        val file = credentialFile(key) ?: run {
            throw OpButtonException("Android Context is required to persist device credential")
        }
        try {
            val temporary = File(file.parentFile, "${file.name}.tmp")
            temporary.outputStream().use { output ->
                output.write(value.toByteArray(Charsets.UTF_8))
                output.fd.sync()
            }
            if (!temporary.renameTo(file)) {
                if (file.exists() && !file.delete()) error("Unable to replace credential")
                if (!temporary.renameTo(file)) error("Unable to install credential")
            }
        } catch (error: Exception) {
            if (error is OpButtonException) throw error
            throw OpButtonException("Unable to persist device credential", error)
        }
        memory[key] = value
    }

    private fun credentialFile(key: String): File? {
        val appContext = context ?: return null
        val directory = appContext.noBackupFilesDir
        return File(directory, "opbutton-device-$key")
    }

    private fun key(baseUrl: String, deviceId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$baseUrl\u0000$deviceId".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return digest
    }

    private fun valid(value: String?): Boolean = value != null &&
        value.length in MIN_SECRET_LENGTH..MAX_SECRET_LENGTH &&
        value.all { it.code in 0x21..0x7e }

    private companion object {
        const val MIN_SECRET_LENGTH = 32
        const val MAX_SECRET_LENGTH = 256
    }
}
