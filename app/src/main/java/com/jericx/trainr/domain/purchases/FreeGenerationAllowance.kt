package com.jericx.trainr.domain.purchases

// One generated week is free, ever. Recorded apart from the plans themselves so
// deleting every week does not hand the allowance back: that would be a way to
// generate without limit and without even reinstalling.
//
// A reinstall does clear this, and that is accepted rather than defended. The
// identifier that would survive one is tied to the signing key and resets on a
// factory reset anyway, and the leak costs one generation while taking away
// every set the person had logged.
interface FreeGenerationAllowance {
    fun hasBeenUsed(): Boolean
    fun markUsed()
}
