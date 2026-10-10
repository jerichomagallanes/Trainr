package com.jericx.trainr.data.model

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.unstuck.intent.InstallFailure
import com.jericx.trainr.domain.unstuck.intent.ModelState
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ModelInstallerTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val supported = DeviceEligibility(
        totalMemory = 4L shl 30,
        isLowRamDevice = false,
        primaryAbi = "arm64-v8a"
    )

    // A byte pattern of the artifact's length whose hash is never the real one.
    private class PatternStream(
        private var position: Long,
        private val failAt: Long,
        private val onProgress: (Long) -> Unit
    ) : InputStream() {
        override fun read(): Int {
            val one = ByteArray(1)
            return if (read(one, 0, 1) < 0) -1 else one[0].toInt() and 0xff
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (position >= ModelArtifact.SIZE_BYTES) return -1
            if (position >= failAt) throw IOException("dropped")
            val count = minOf(len.toLong(), ModelArtifact.SIZE_BYTES - position).toInt()
            for (i in 0 until count) b[off + i] = ((position + i) % 251).toByte()
            position += count
            onProgress(position)
            return count
        }
    }

    private class FakeSource(
        private val honoursRange: Boolean = true,
        private val failAt: Long = Long.MAX_VALUE
    ) : ModelSource {
        val opens = mutableListOf<Long>()
        var onProgress: (Long) -> Unit = {}

        override fun open(from: Long): ModelDownload {
            opens += from
            val start = if (honoursRange) from else 0L
            return ModelDownload(PatternStream(start, failAt, onProgress), start)
        }
    }

    private val file get() = File(folder.root, ModelArtifact.FILE_NAME)
    private val part get() = File(folder.root, "${ModelArtifact.FILE_NAME}.part")

    private fun TestScope.installer(
        source: ModelSource = FakeSource(),
        eligibility: DeviceEligibility = supported,
        freeBytes: Long = 4 * ModelArtifact.SIZE_BYTES
    ) = ModelInstaller(
        directory = folder.root,
        eligibility = eligibility,
        source = source,
        freeSpace = { freeBytes },
        dispatcher = StandardTestDispatcher(testScheduler)
    )

    private fun partOf(length: Long) = RandomAccessFile(part, "rw").use { it.setLength(length) }

    @Test
    fun anUnsupportedDeviceNeverOffersTheModel() = runTest {
        val source = FakeSource()
        val installer = installer(
            source = source,
            eligibility = DeviceEligibility(totalMemory = 2L shl 30, isLowRamDevice = false, primaryAbi = "arm64-v8a")
        )

        installer.install()
        advanceUntilIdle()

        assertThat(installer.state.value).isEqualTo(ModelState.Unsupported)
        assertThat(source.opens).isEmpty()
    }

    @Test
    fun tooLittleFreeSpaceIsRefusedBeforeAnyByteIsFetched() = runTest {
        val source = FakeSource()
        val installer = installer(source = source, freeBytes = ModelArtifact.SIZE_BYTES + 1)

        installer.install()
        advanceUntilIdle()

        assertThat(installer.state.value).isEqualTo(ModelState.InsufficientStorage)
        assertThat(source.opens).isEmpty()
    }

    @Test
    fun aSecondTapWhileDownloadingOpensNoSecondConnection() = runTest {
        val source = FakeSource(failAt = 1_000_000)
        val installer = installer(source = source)

        installer.install()
        installer.install()
        advanceUntilIdle()

        assertThat(source.opens).containsExactly(0L)
    }

    @Test
    fun aResumeOnlyNeedsRoomForWhatIsStillMissing() = runTest {
        partOf(ModelArtifact.SIZE_BYTES - 1_000)
        val source = FakeSource()
        val installer = installer(source = source, freeBytes = ModelArtifact.SIZE_BYTES + 1_000)

        installer.install()
        advanceUntilIdle()

        assertThat(source.opens).containsExactly(ModelArtifact.SIZE_BYTES - 1_000)
        assertThat(installer.state.value).isEqualTo(ModelState.Failed(InstallFailure.HASH_MISMATCH))
    }

    @Test
    fun aDownloadResumesFromThePartFile() = runTest {
        partOf(1_000_000)
        val source = FakeSource(failAt = 1_500_000)
        val installer = installer(source = source)

        installer.install()
        advanceUntilIdle()

        assertThat(source.opens).containsExactly(1_000_000L)
        assertThat(installer.state.value).isEqualTo(ModelState.Failed(InstallFailure.DOWNLOAD))
        assertThat(part.length()).isAtLeast(1_500_000L)
        assertThat(part.length()).isLessThan(2_000_000L)
    }

    @Test
    fun aServerThatIgnoresTheRangeStartsOver() = runTest {
        partOf(1_000_000)
        val installer = installer(source = FakeSource(honoursRange = false, failAt = 2_000_000))

        installer.install()
        advanceUntilIdle()

        assertThat(installer.state.value).isEqualTo(ModelState.Failed(InstallFailure.DOWNLOAD))
        assertThat(part.length()).isAtLeast(2_000_000L)
        assertThat(part.length()).isLessThan(3_000_000L)
    }

    @Test
    fun aHashMismatchDeletesTheFileAndReportsIt() = runTest {
        val source = FakeSource()
        val installer = installer(source = source)
        var progress: ModelState? = null
        source.onProgress = { if (it == ModelArtifact.SIZE_BYTES) progress = installer.state.value }

        installer.install()
        advanceUntilIdle()

        assertThat(progress).isInstanceOf(ModelState.Downloading::class.java)
        assertThat(installer.state.value).isEqualTo(ModelState.Failed(InstallFailure.HASH_MISMATCH))
        assertThat(folder.root.listFiles().orEmpty()).isEmpty()
        assertThat(installer.readyFile()).isNull()
    }

    @Test
    fun cancellingKeepsThePartFileForResume() = runTest {
        val source = FakeSource()
        val installer = installer(source = source)
        source.onProgress = { if (it >= 1_000_000) installer.cancel() }

        installer.install()
        advanceUntilIdle()

        assertThat(installer.state.value).isEqualTo(ModelState.NotInstalled)
        assertThat(part.length()).isAtLeast(1_000_000L)
        assertThat(part.length()).isLessThan(2_000_000L)
    }

    @Test
    fun aCompleteFileOnDiskIsReadyWithoutAHash() = runTest {
        RandomAccessFile(file, "rw").use { it.setLength(ModelArtifact.SIZE_BYTES) }

        val installer = installer()
        advanceUntilIdle()

        assertThat(installer.state.value).isEqualTo(ModelState.Ready)
        assertThat(installer.readyFile()).isEqualTo(file)
    }

    @Test
    fun aFileOfTheWrongLengthIsVerifiedAndThrownAway() = runTest {
        file.writeBytes(ByteArray(10))

        val installer = installer()
        assertThat(installer.state.value).isEqualTo(ModelState.Verifying)
        advanceUntilIdle()

        assertThat(installer.state.value).isEqualTo(ModelState.Failed(InstallFailure.HASH_MISMATCH))
        assertThat(file.exists()).isFalse()
    }

    @Test
    fun nothingOnDiskIsNotInstalled() = runTest {
        assertThat(installer().state.value).isEqualTo(ModelState.NotInstalled)
    }
}
