package org.pilot.sdk

import org.json.JSONObject

open class PilotWidget<T : PilotWidget<T>> internal constructor(
    internal val ui: PilotUI,
    type: String
) {
    internal val internalId: Int = ui.nextId()
    var publicId: String = "$type-$internalId"
        private set
    val type: String = type
    internal val json: JSONObject = JSONObject().apply {
        put("type", type)
        put("id", internalId)
    }

    private var providerKey: String? = null
    private var provider: PilotValueProvider? = null
    private var cachedValue: String? = null

    protected fun put(key: String, value: Any?) {
        json.put(key, value)
        ui.incrementRevision()
    }

    @Suppress("UNCHECKED_CAST")
    fun setId(id: String): T {
        publicId = id
        return this as T
    }

    protected fun setProvider(key: String, provider: PilotValueProvider?) {
        providerKey = key
        this.provider = provider
        if (provider != null) {
            ui.registerProvider(this)
        } else {
            ui.unregisterProvider(this)
        }
    }

    internal fun pollProvider(): Boolean {
        val p = provider ?: return false
        return try {
            val newValue = p.getValue()?.toString()
            if (newValue != cachedValue) {
                cachedValue = newValue
                providerKey?.let { json.put(it, newValue) }
                true
            } else false
        } catch (e: Exception) {
            PilotLog.e("Value provider failed for widget $internalId", e)
            false
        }
    }

    internal fun toJson(): JSONObject = json
}
