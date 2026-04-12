package org.pilot.sdk

fun interface PilotActionListener {
    fun onPilotActionReceived(action: PilotAction)
}
