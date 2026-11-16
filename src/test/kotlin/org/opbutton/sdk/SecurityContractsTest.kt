package org.opbutton.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SecurityContractsTest {
    @Test fun productionBaseUrlRequiresHttpsAndHasNoAmbiguousSuffix() {
        assertThrows(IllegalArgumentException::class.java) {
            OpButtonConfig.Builder("http://example.com", "token").build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            OpButtonConfig.Builder("https://user@example.com", "token").build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            OpButtonConfig.Builder("https://example.com?next=evil", "token").build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            OpButtonConfig.Builder("https://example.com#fragment", "token").build()
        }

        val local = OpButtonConfig.Builder("http://127.0.0.1:8000", "token")
            .setAllowInsecureLocalhost(true)
            .build()
        assertEquals("http://127.0.0.1:8000", local.baseUrl)
        val ipv6 = OpButtonConfig.Builder("http://[::1]:8000", "token")
            .setAllowInsecureLocalhost(true)
            .build()
        assertEquals("http://[::1]:8000", ipv6.baseUrl)
    }

    @Test fun deviceSecretUsesProtocolBounds() {
        assertThrows(IllegalArgumentException::class.java) {
            OpButtonConfig.Builder("https://example.com", "token")
                .setDeviceSecret("short")
                .build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            OpButtonConfig.Builder("https://example.com", "token")
                .setDeviceSecret("x".repeat(257))
                .build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            OpButtonConfig.Builder("https://example.com", "token")
                .setDeviceSecret("x".repeat(32) + "\n")
                .build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            OpButtonConfig.Builder("https://example.com", "token\r\nInjected: true").build()
        }
    }

    @Test fun automaticCredentialRequiresAndroidContext() {
        val store = OpButtonDeviceCredentialStore(null)
        assertThrows(IllegalStateException::class.java) {
            store.getOrCreate("https://example.com", "device", null)
        }
        val configured = "x".repeat(64)
        assertEquals(configured, store.getOrCreate("https://example.com", "device", configured))
    }
}
