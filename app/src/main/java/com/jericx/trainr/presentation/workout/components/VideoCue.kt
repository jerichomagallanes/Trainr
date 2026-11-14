package com.jericx.trainr.presentation.workout.components

// The player arrives after the composition that asked for a clip, and it
// outlives any number of clips, so the two are tracked apart and reconciled
// whenever either changes.
internal class VideoCue {

    private var cue: ((String) -> Unit)? = null
    private var wanted: String? = null
    private var showing: String? = null

    fun playerReady(cue: (String) -> Unit) {
        this.cue = cue
        showing = null
        reconcile()
    }

    fun playerGone() {
        cue = null
        showing = null
    }

    fun show(videoId: String) {
        wanted = videoId
        reconcile()
    }

    private fun reconcile() {
        val cue = cue ?: return
        val videoId = wanted ?: return
        if (videoId == showing) return
        showing = videoId
        cue(videoId)
    }
}
