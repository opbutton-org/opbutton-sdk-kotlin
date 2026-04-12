package org.pilot.sdk

class PilotLogAttributeBuilder {
    internal val staticAttributes = LinkedHashMap<String, Any>()
    internal val dynamicAttributes = LinkedHashMap<String, PilotValueProvider>()

    fun put(key: String, value: Any): PilotLogAttributeBuilder = apply {
        staticAttributes[key] = value
    }

    fun putProvider(key: String, provider: PilotValueProvider): PilotLogAttributeBuilder = apply {
        dynamicAttributes[key] = provider
    }
}
