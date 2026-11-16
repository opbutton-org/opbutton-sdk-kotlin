package org.opbutton.sdk

import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class OpButtonUI internal constructor() {
    private val tabs = mutableListOf<OpButtonTab>()
    private val callbacks = ConcurrentHashMap<Int, OpButtonWidgetCallback>()
    private val providers: MutableSet<OpButtonWidget<*>> = ConcurrentHashMap.newKeySet()
    private val idCounter = AtomicInteger(0)
    private val version = 2
    private val revision = AtomicInteger(1)
    @Volatile private var sentRevision = 0

    internal fun nextId(): Int = idCounter.incrementAndGet()

    // ── Tab management ──

    fun addTab(title: String): OpButtonTab {
        tabs.removeAll { if (it.title != title) false else { it.detach(); true } }
        val tab = OpButtonTab(this, title)
        tabs.add(tab)
        revision.incrementAndGet()
        return tab
    }

    fun getTab(id: String): OpButtonTab? = tabs.firstOrNull { it.id == id }

    fun removeTab(id: String) {
        tabs.removeAll { if (it.id != id) false else { it.detach(); true } }
        revision.incrementAndGet()
    }

    // ── Widget callbacks ──

    internal fun registerCallback(widgetId: Int, callback: OpButtonWidgetCallback?) {
        if (callback != null) {
            callbacks[widgetId] = callback
        } else {
            callbacks.remove(widgetId)
        }
    }

    internal fun dispatchAction(action: OpButtonAction): Boolean {
        val cb = callbacks[action.widgetId] ?: return false
        cb.onOpButtonWidgetAction(action)
        return true
    }

    // ── Value providers ──

    internal fun registerProvider(widget: OpButtonWidget<*>) {
        providers.add(widget)
    }

    internal fun unregisterProvider(widget: OpButtonWidget<*>) {
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

    @Synchronized
    internal fun markSent(revision: Int) {
        sentRevision = maxOf(sentRevision, revision)
    }

    internal fun hasTabs(): Boolean = tabs.isNotEmpty()

    internal fun getRevision(): Int = revision.get()

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
