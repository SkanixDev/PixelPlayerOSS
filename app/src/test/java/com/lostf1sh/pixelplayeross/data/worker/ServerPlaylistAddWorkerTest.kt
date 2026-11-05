package com.lostf1sh.pixelplayeross.data.worker

import com.lostf1sh.pixelplayeross.data.network.navidrome.SubsonicApiException
import com.lostf1sh.pixelplayeross.data.worker.ServerPlaylistAddWorker.Companion.MAX_ATTEMPTS
import com.lostf1sh.pixelplayeross.data.worker.ServerPlaylistAddWorker.Companion.shouldRetryServerPlaylistAdd
import java.io.IOException
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ServerPlaylistAddWorkerTest {

    private val notFound = SubsonicApiException(70, "local track not available")

    @Test
    fun `external track still downloading is retried`() {
        assertTrue(shouldRetryServerPlaylistAdd(notFound, "ext-deezer-song-1", attempt = 0))
    }

    @Test
    fun `not found on a regular song is final`() {
        assertFalse(shouldRetryServerPlaylistAdd(notFound, "a1b2c3", attempt = 0))
    }

    @Test
    fun `network errors are retried for any song`() {
        assertTrue(shouldRetryServerPlaylistAdd(IOException(), "a1b2c3", attempt = 0))
    }

    @Test
    fun `other server errors are final`() {
        val denied = SubsonicApiException(50, "not authorized")

        assertFalse(shouldRetryServerPlaylistAdd(denied, "ext-deezer-song-1", attempt = 0))
    }

    @Test
    fun `retries stop at the last attempt`() {
        assertFalse(
            shouldRetryServerPlaylistAdd(notFound, "ext-deezer-song-1", attempt = MAX_ATTEMPTS - 1)
        )
    }
}
