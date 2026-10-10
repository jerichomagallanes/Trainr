package com.jericx.trainr.data.model

import android.app.ActivityManager
import android.content.Context
import android.os.Build

class DeviceEligibility(totalMemory: Long, isLowRamDevice: Boolean, primaryAbi: String?) {

    val isSupported: Boolean =
        totalMemory >= MIN_MEMORY_BYTES && !isLowRamDevice && primaryAbi in SUPPORTED_ABIS

    companion object {
        private const val MIN_MEMORY_BYTES = 3L shl 30
        private val SUPPORTED_ABIS = setOf("arm64-v8a", "x86_64")

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
