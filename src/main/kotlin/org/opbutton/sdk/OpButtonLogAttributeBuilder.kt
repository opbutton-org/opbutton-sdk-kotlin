package org.opbutton.sdk

class OpButtonLogAttributeBuilder {
    internal val staticAttributes = LinkedHashMap<String, Any>()
    internal val dynamicAttributes = LinkedHashMap<String, OpButtonValueProvider>()

    fun put(key: String, value: Any): OpButtonLogAttributeBuilder = apply {
        staticAttributes[key] = value
    }

    fun putProvider(key: String, provider: OpButtonValueProvider): OpButtonLogAttributeBuilder = apply {
        dynamicAttributes[key] = provider
    }
}
