package com.jericx.trainr.data.model

import android.app.ActivityManager
import android.content.Context
import android.os.Build

class DeviceEligibility(totalMemory: Long, isLowRamDevice: Boolean, primaryAbi: String?) {

    val isSupported: Boolean =
        memorySoldWith(totalMemory) >= MIN_MEMORY_BYTES &&
            !isLowRamDevice &&
            primaryAbi in SUPPORTED_ABIS

    companion object {
        private const val MIN_MEMORY_BYTES = 3L shl 30
        private const val HALF_GIBIBYTE = 1L shl 29
        private val SUPPORTED_ABIS = setOf("arm64-v8a", "x86_64")

        // totalMem is what the kernel can see, and firmware reserves hundreds of
        // megabytes on every device, so a 3 GB phone reports about 2.8 GiB.
        // Rounding up to the nearest half gibibyte recovers the size the device
        // was sold with, which is what the bar is expressed in and what iOS
        // already reports.
        private fun memorySoldWith(totalMemory: Long): Long =
            (totalMemory + HALF_GIBIBYTE - 1) / HALF_GIBIBYTE * HALF_GIBIBYTE

        fun of(context: Context): DeviceEligibility {
            val manager = context.getSystemService(ActivityManager::class.java)
            val memory = ActivityManager.MemoryInfo().also(manager::getMemoryInfo)
            return DeviceEligibility(
                totalMemory = memory.totalMem,
                isLowRamDevice = manager.isLowRamDevice,
                primaryAbi = Build.SUPPORTED_ABIS.firstOrNull()
            )
        }
    }
}
