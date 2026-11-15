package org.opbutton.sdk

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask

/** Reads host-owned state and acknowledges only the captured values. */
internal class OpButtonHostState(
    private val attributes: OpButtonSessionAttributeBuilder,
    private val ui: OpButtonUI,
    private val mainHandler: Handler
) : OpButtonPollDelivery.State {
    private val attributeJournal = OpButtonAttributeJournal()

    override fun capture() = OpButtonPollDelivery.Snapshot(resolveChangedSessionAttributes(), snapshotUIOnMainThread())
    override fun panelSent(snapshot: OpButtonPollDelivery.Snapshot) { ui.markSent(snapshot.panel!!.getInt("revision")) }
    override fun attributesSent(snapshot: OpButtonPollDelivery.Snapshot) { acknowledgeAll(snapshot.attributes) }
    fun acknowledgeAll(values: Map<String, Any>) { attributeJournal.acknowledge(values) }
    private fun snapshotUIOnMainThread(): JSONObject? {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            ui.pollValues()
            return if (ui.hasUnsent()) detachedPanelSnapshot() else null
        }

        val task = FutureTask {
            ui.pollValues()
            if (ui.hasUnsent()) detachedPanelSnapshot() else null
        }
        mainHandler.post(task)

        return try {
            task.get()
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        } catch (e: ExecutionException) {
            OpButtonLog.e("Failed to snapshot UI on main thread", e)
            null
        }
    }

    fun buildUISnapshotOnMainThread(): JSONObject? {
        if (Looper.myLooper() == Looper.getMainLooper()) return detachedPanelSnapshot()

        val task = FutureTask { detachedPanelSnapshot() }
        mainHandler.post(task)

        return try {
            task.get()
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        } catch (e: ExecutionException) {
            OpButtonLog.e("Failed to build UI snapshot on main thread", e)
            null
        }
    }

    fun resolveAllSessionAttributes(): Map<String, Any> {
        val values = attributes.staticAttributes + resolveDynamicSessionAttributes()
        return OpButtonAttributeJournal.capture(values)
    }

    private fun resolveDynamicSessionAttributes(): Map<String, Any> = buildMap {
        for ((key, provider) in attributes.dynamicAttributes) {
            try {
                put(key, provider.getValue() ?: JSONObject.NULL)
            } catch (e: Exception) {
                OpButtonLog.e("Session attribute provider failed: $key", e)
            }
        }
    }

    private fun resolveChangedSessionAttributes(): Map<String, Any> =
        attributeJournal.snapshot(resolveDynamicSessionAttributes())

    private fun detachedPanelSnapshot() = JSONObject(ui.toJson().toString())

}
