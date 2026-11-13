package com.jericx.trainr.testing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import dagger.hilt.android.AndroidEntryPoint

// An empty host a test can set its own content on. Compose needs a registered
// activity and Hilt needs an annotated one, and MainActivity is neither
// available to a test that wants to hand the screen a different ProGate nor
// free of its own start-up.
@AndroidEntryPoint
class HiltTestActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        held?.let { setContent(content = it) }
    }

    companion object {
        private var held: (@Composable () -> Unit)? = null

        // Set before launching. A test that drives a real recreation needs the
        // content to come back by itself, the way MainActivity's does; content
        // handed to the Activity afterwards belongs to the instance that died.
        fun contentOnLaunch(content: (@Composable () -> Unit)?) {
            held = content
        }
    }
}
