package app.mediapicker

import android.app.Activity
import android.os.Bundle

/** "Open with" entry: reads only the data Uri and type of the request, then forwards it and closes. */
class OpenActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent?.data
        if (uri != null) Opener.forward(this, uri, intent?.type)
        finish()
    }
}
