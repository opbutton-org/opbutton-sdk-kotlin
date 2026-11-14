package org.opbutton.sdk

class OpButtonSessionAttributeBuilder {
    internal val staticAttributes = LinkedHashMap<String, Any>()
    internal val dynamicAttributes = LinkedHashMap<String, OpButtonValueProvider>()

    fun put(key: String, value: Any): OpButtonSessionAttributeBuilder = apply {
        staticAttributes[key] = value
    }

    fun putProvider(key: String, provider: OpButtonValueProvider): OpButtonSessionAttributeBuilder = apply {
        dynamicAttributes[key] = provider
    }
}
