package org.opbutton.sdk

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DeviceIdentityTest {
    @get:Rule val directory = TemporaryFolder()

    @Test fun explicitIdentityWinsAndStrategiesAreOrdered() {
        val identity = OpButtonDeviceIdentity(
            OpButtonDeviceIdentity.Source { null },
            OpButtonDeviceIdentity.Source { "installation" },
            OpButtonDeviceIdentity.Source { error("Unexpected strategy") }
        )
        assertEquals("host-id", identity.resolve("host-id"))
        assertEquals("installation", identity.resolve(null))
    }

    @Test fun secureIdentityIsStableAndScopedToPackageAndDevice() {
        val id = OpButtonDeviceIdentity.scopedAndroidId("game.one", "0123456789abcdef")
        assertNotNull(id)
        assertEquals(id, OpButtonDeviceIdentity.scopedAndroidId("game.one", "0123456789ABCDEF"))
        assertNotEquals(id, OpButtonDeviceIdentity.scopedAndroidId("game.two", "0123456789abcdef"))
        assertNotEquals(id, OpButtonDeviceIdentity.scopedAndroidId("game.one", "fedcba9876543210"))
        assertNull(OpButtonDeviceIdentity.scopedAndroidId("game.one", "00000000"))
        assertNull(OpButtonDeviceIdentity.scopedAndroidId("game.one", "9774d56d682e549c"))
    }

    @Test fun installationIdentityPersistsWithoutCloningAnotherInstallation() {
        val first = directory.newFolder()
        val id = OpButtonDeviceIdentity.installationIdentity(first)
        assertNotNull(id)
        assertEquals(id, OpButtonDeviceIdentity.installationIdentity(first))
        assertNotEquals(id, OpButtonDeviceIdentity.installationIdentity(directory.newFolder()))
    }

    @Test fun reinstallDoesNotReuseAndroidIdentityWithoutItsCredential() {
        val androidId = "android-v1:existing-device"
        val firstDirectory = directory.newFolder()
        val secondDirectory = directory.newFolder()
        val first = OpButtonDeviceIdentity.withInstallationIdentity(
            OpButtonDeviceIdentity.Source { androidId },
            OpButtonDeviceIdentity.Source { OpButtonDeviceIdentity.installationIdentity(firstDirectory) }
        ) { false }
        val second = OpButtonDeviceIdentity.withInstallationIdentity(
            OpButtonDeviceIdentity.Source { androidId },
            OpButtonDeviceIdentity.Source { OpButtonDeviceIdentity.installationIdentity(secondDirectory) }
        ) { false }

        val firstId = first.resolve(null)
        assertTrue(firstId.startsWith("installation-v1:"))
        assertEquals(firstId, first.resolve(null))
        assertNotEquals(firstId, second.resolve(null))
        assertNotEquals(androidId, second.resolve(null))
    }

    @Test fun existingAndroidIdentityKeepsItsStoredCredential() {
        val androidId = "android-v1:approved-device"
        val identity = OpButtonDeviceIdentity.withInstallationIdentity(
            OpButtonDeviceIdentity.Source { androidId },
            OpButtonDeviceIdentity.Source { error("Unexpected installation identity") },
            androidId::equals
        )

        assertEquals(androidId, identity.resolve(null))
    }

    @Test fun contextlessFallbackSurvivesSdkReinitializationInOneProcess() {
        assertEquals(OpButtonDeviceIdentity().resolve(null), OpButtonDeviceIdentity().resolve(null))
    }

    @Test fun unavailableStorageStrategyFallsThroughInsteadOfBreakingConnection() {
        OpButtonLog.setLoggerListener { _, _, _, _ -> }
        try {
            val identity = OpButtonDeviceIdentity(
                OpButtonDeviceIdentity.Source { error("Credential storage is locked") },
                OpButtonDeviceIdentity.Source { "fallback" }
            )
            assertEquals("fallback", identity.resolve(null))
        } finally {
            OpButtonLog.setLoggerListener(null)
        }
    }
}
