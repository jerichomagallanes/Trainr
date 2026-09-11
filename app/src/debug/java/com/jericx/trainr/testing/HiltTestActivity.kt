package com.jericx.trainr.testing

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

// An empty host a test can set its own content on. Compose needs a registered
// activity and Hilt needs an annotated one, and MainActivity is neither
// available to a test that wants to hand the screen a different ProGate nor
// free of its own start-up.
@AndroidEntryPoint
class HiltTestActivity : ComponentActivity()
