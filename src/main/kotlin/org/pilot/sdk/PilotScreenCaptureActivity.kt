package org.pilot.sdk

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle

class PilotScreenCaptureActivity : Activity() {
    companion object {
        private const val REQUEST_CODE = 7291

        @Volatile
        internal var callback: ScreenCaptureCallback? = null
    }

    internal fun interface ScreenCaptureCallback {
        fun onScreenCaptureResult(resultCode: Int, data: Intent?)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            finish()
            return
        }

        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        if (mgr == null) {
            deliverResult(Activity.RESULT_CANCELED, null)
            return
        }

        try {
            @Suppress("DEPRECATION")
            startActivityForResult(mgr.createScreenCaptureIntent(), REQUEST_CODE)
        } catch (_: Exception) {
            deliverResult(Activity.RESULT_CANCELED, null)
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            deliverResult(resultCode, data)
        }
    }

    private fun deliverResult(resultCode: Int, data: Intent?) {
        val cb = callback
        callback = null
        cb?.onScreenCaptureResult(resultCode, data)
        finish()
    }
}
