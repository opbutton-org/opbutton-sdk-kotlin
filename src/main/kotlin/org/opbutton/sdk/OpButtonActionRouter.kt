package org.opbutton.sdk

internal typealias OpButtonCommand = (OpButtonAction, String) -> Unit

/** Routes reserved commands exclusively, then publishes application actions to observers. */
internal class OpButtonActionRouter(
    commands: Map<OpButtonActionType, OpButtonCommand>,
    private val widgets: (OpButtonAction) -> Unit,
    private val listeners: () -> List<OpButtonActionListener>
) {
    private val commands = commands.toMap()

    fun dispatch(action: OpButtonAction, token: String) {
        val command = commands[action.actionType]
        if (command != null) {
            try { command(action, token) }
            catch (error: Exception) { OpButtonLog.e("Internal command failed", error) }
            return
        }
        try { widgets(action) }
        catch (error: Exception) { OpButtonLog.e("Widget callback threw exception", error) }
        for (listener in listeners()) {
            try { listener.onOpButtonActionReceived(action) }
            catch (error: Exception) { OpButtonLog.e("Action listener threw exception", error) }
        }
    }
}
