package com.meshcentral.agent

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log

class ScreenCaptureActivity : Activity() {
    companion object {
        const val TAG = "ScreenCaptureActivity"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startProjection()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        println("onActivityResult, requestCode: $requestCode, resultCode: $resultCode, data: ${data.toString()}")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MainActivity.Companion.REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                startService(com.meshcentral.agent.ScreenCaptureService.getStartIntent(this, resultCode, data))
                finish();
                return
            }
        }

        var pad : PendingActivityData? = null
        for (b in pendingActivities) { if (b.id == requestCode) { pad = b } }

        if (pad != null) {
            if (resultCode == Activity.RESULT_OK) {
                println("Approved: ${pad.url}, ${pad.where}, ${pad.args}")
                pad.tunnel.deleteFileEx(pad)
            } else {
                println("Denied: ${pad.url}, ${pad.where}, ${pad.args}")
                pad.tunnel.deleteFileEx(pad)
            }
            pendingActivities.remove(pad)
        }
        finish();
    }

    private fun startProjection() {
        val mProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), MainActivity.Companion.REQUEST_CODE)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}