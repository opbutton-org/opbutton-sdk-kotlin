package org.pilot.sdk

class PilotSessionAttributeBuilder {
    internal val staticAttributes = LinkedHashMap<String, Any>()
    internal val dynamicAttributes = LinkedHashMap<String, PilotValueProvider>()

    fun put(key: String, value: Any): PilotSessionAttributeBuilder = apply {
        staticAttributes[key] = value
    }

    fun putProvider(key: String, provider: PilotValueProvider): PilotSessionAttributeBuilder = apply {
        dynamicAttributes[key] = provider
    }
}
