package org.pilot.sdk

import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class PilotUI internal constructor() {
    private val tabs = mutableListOf<PilotTab>()
    private val callbacks = ConcurrentHashMap<Int, PilotWidgetCallback>()
    private val providers: MutableSet<PilotWidget<*>> = ConcurrentHashMap.newKeySet()
    private val idCounter = AtomicInteger(0)
    private val version = 2
    private val revision = AtomicInteger(1)
    private var sentRevision = 0

    internal fun nextId(): Int = idCounter.incrementAndGet()

    // ── Tab management ──

    fun addTab(title: String): PilotTab {
        tabs.removeAll { it.title == title }
        val tab = PilotTab(this, title)
        tabs.add(tab)
        revision.incrementAndGet()
        return tab
    }

    fun getTab(id: String): PilotTab? = tabs.firstOrNull { it.id == id }

    fun removeTab(id: String) {
        tabs.removeAll { it.id == id }
        revision.incrementAndGet()
    }

    // ── Widget callbacks ──

    internal fun registerCallback(widgetId: Int, callback: PilotWidgetCallback?) {
        if (callback != null) {
            callbacks[widgetId] = callback
        } else {
            callbacks.remove(widgetId)
        }
    }

    internal fun dispatchAction(action: PilotAction): Boolean {
        val cb = callbacks[action.widgetId] ?: return false
        cb.onPilotWidgetAction(action)
        return true
    }

    // ── Value providers ──

    internal fun registerProvider(widget: PilotWidget<*>) {
        providers.add(widget)
    }

    internal fun unregisterProvider(widget: PilotWidget<*>) {
        providers.remove(widget)
    }

    internal fun pollValues() {
        for (w in providers) {
            if (w.pollProvider()) {
                revision.incrementAndGet()
            }
        }
    }

    // ── Revision tracking ──

    internal fun incrementRevision() {
        revision.incrementAndGet()
    }

    internal fun hasUnsent(): Boolean = revision.get() != sentRevision

    internal fun markSent() {
        sentRevision = revision.get()
    }

    internal fun hasTabs(): Boolean = tabs.isNotEmpty()

    fun getRevision(): Int = revision.get()

    // ── Serialization ──

    fun toJson(): JSONObject = JSONObject().apply {
        put("version", version)
        put("revision", revision.get())
        val tabsArr = JSONArray()
        for (tab in tabs) {
            tabsArr.put(tab.toJson())
        }
        put("tabs", tabsArr)
    }
}
